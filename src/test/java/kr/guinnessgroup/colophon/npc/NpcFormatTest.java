/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.npc;

import kr.guinnessgroup.colophon.DocumentException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The NPC document (format 1) and the placement record. Breaking these loses NPCs. */
class NpcFormatTest {

    static final String CHIEF = """
            { "format": 1, "npcs": [ { "id": "chief", "name": "촌장" } ] }
            """;

    @Test
    void readsTheDocumentedExample() {
        NpcDoc doc = NpcFormat.read(CHIEF);
        assertEquals(new NpcDoc.NpcDef("chief", "촌장"), doc.find("chief"));
        assertNull(doc.find("smith"));
    }

    @Test
    void writeThenReadGivesTheSameDocument() {
        NpcDoc doc = NpcFormat.read(CHIEF);
        assertEquals(doc, NpcFormat.read(NpcFormat.write(doc)));
    }

    @Test
    void looksAreOptionalAndWrittenOnlyWhenSet() {
        NpcDoc doc = NpcFormat.read("""
                { "format": 1, "npcs": [
                  { "id": "chief", "name": "촌장", "model": "chief", "idle": "animation.chief.wave" },
                  { "id": "smith", "name": "대장장이" } ] }
                """);
        assertEquals(new NpcDoc.NpcDef("chief", "촌장", "chief", "animation.chief.wave"), doc.find("chief"));
        assertEquals(new NpcDoc.NpcDef("smith", "대장장이", "", ""), doc.find("smith"));
        String written = NpcFormat.write(doc);
        assertEquals(1, written.split("\"model\"", -1).length - 1);
        assertEquals(doc, NpcFormat.read(written));
    }

    @Test
    void rejectsBadModelName() {
        assertThrows(DocumentException.class, () -> NpcFormat.read(
                "{\"format\":1,\"npcs\":[{\"id\":\"chief\",\"name\":\"a\",\"model\":\"Chief Model\"}]}"));
    }

    @Test
    void rejectsNewerFormat() {
        DocumentException e = assertThrows(DocumentException.class,
                () -> NpcFormat.read("{\"format\":2,\"npcs\":[]}"));
        assertTrue(e.errors().get(0).contains("newer"));
    }

    @Test
    void rejectsBadIdsMissingNamesAndDuplicates() {
        DocumentException e = assertThrows(DocumentException.class, () -> NpcFormat.read(
                "{\"format\":1,\"npcs\":["
                        + "{\"id\":\"Chief\",\"name\":\"a\"},"
                        + "{\"id\":\"smith\"},"
                        + "{\"id\":\"chief\",\"name\":\"b\"},{\"id\":\"chief\",\"name\":\"c\"}]}"));
        assertEquals(3, e.errors().size());
    }

    @Test
    void placementRecordRoundTrips() {
        Placement p = new Placement(UUID.fromString("00000000-0000-0000-0000-00000000000a"),
                "chief", "minecraft:overworld", 1.5, 64.0, -3.5);
        assertEquals("npc:00000000-0000-0000-0000-00000000000a", p.key());
        assertEquals(p, Placement.fromRecord(p.key(), p.toValue()));
    }

    @Test
    void placementIgnoresOtherRecordsAndBrokenValues() {
        assertNull(Placement.fromRecord("flag:greeted", "true"));
        assertNull(Placement.fromRecord("npc:not-a-uuid", "{}"));
        assertNull(Placement.fromRecord("npc:00000000-0000-0000-0000-00000000000a", "{\"npc\":\"chief\"}"));
    }
}
