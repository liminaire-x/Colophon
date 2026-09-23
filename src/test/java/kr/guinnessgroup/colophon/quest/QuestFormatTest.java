/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.quest;

import kr.guinnessgroup.colophon.DocumentException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The quest document (format 1). Breaking these loses quests. */
class QuestFormatTest {

    static final String WHEAT = """
            { "format": 1, "quests": [ {
              "id": "quest_k3f9x2ma", "title": "밀 배달", "icon": "minecraft:wheat",
              "text": "촌장에게 밀 10개를 가져다주자.\\n빨리!",
              "goals":   [ { "item": "minecraft:wheat",   "count": 10 } ],
              "rewards": [ { "item": "minecraft:emerald", "count": 5 } ] } ] }
            """;

    @Test
    void readsTheDocumentedExample() {
        QuestDoc doc = QuestFormat.read(WHEAT);
        QuestDoc.Quest q = doc.find("quest_k3f9x2ma");
        assertEquals(new QuestDoc.Quest("quest_k3f9x2ma", "밀 배달", "minecraft:wheat", "촌장에게 밀 10개를 가져다주자.\n빨리!",
                List.of(new QuestDoc.Stack("minecraft:wheat", 10)),
                List.of(new QuestDoc.Stack("minecraft:emerald", 5))), q);
        assertNull(doc.find("quest_other"));
    }

    @Test
    void writeThenReadGivesTheSameDocument() {
        QuestDoc doc = QuestFormat.read(WHEAT);
        assertEquals(doc, QuestFormat.read(QuestFormat.write(doc)));
    }

    @Test
    void iconTextAndEmptyListsAreAllowed() {
        QuestDoc doc = QuestFormat.read("""
                { "format": 1, "quests": [ { "id": "quest_a", "title": "A", "goals": [], "rewards": [] } ] }
                """);
        QuestDoc.Quest q = doc.find("quest_a");
        assertEquals("", q.icon());
        assertEquals("", q.text());
        String written = QuestFormat.write(doc);
        assertTrue(!written.contains("\"icon\"") && !written.contains("\"text\""), written);
        assertEquals(doc, QuestFormat.read(written));
    }

    @Test
    void rewardsMayCarryComponentsAsGiveWritesThem() {
        String sword = "minecraft:iron_sword[custom_name='\\\"대장장이의 칼\\\"',enchantments={levels:{'minecraft:sharpness':2}}]";
        QuestDoc doc = QuestFormat.read("""
                { "format": 1, "quests": [ { "id": "quest_a", "title": "A", "goals": [],
                  "rewards": [ { "item": "%s", "count": 1 } ] } ] }
                """.formatted(sword));
        assertEquals(sword.replace("\\\"", "\""), doc.find("quest_a").rewards().get(0).item());
        assertEquals(doc, QuestFormat.read(QuestFormat.write(doc)));
    }

    @Test
    void goalsAndIconsAreItemIdsOnly() {
        DocumentException e = assertThrows(DocumentException.class, () -> QuestFormat.read("""
                { "format": 1, "quests": [ { "id": "quest_a", "title": "A",
                  "goals": [ { "item": "minecraft:wheat[custom_name='x']", "count": 1 } ], "rewards": [] } ] }
                """));
        assertTrue(e.errors().get(0).contains("goals cannot have [components]"), e.errors().toString());
        assertThrows(DocumentException.class, () -> QuestFormat.read("""
                { "format": 1, "quests": [ { "id": "quest_a", "title": "A", "icon": "minecraft:wheat[x=1]",
                  "goals": [], "rewards": [] } ] }
                """));
    }

    @Test
    void rejectsNewerFormat() {
        DocumentException e = assertThrows(DocumentException.class,
                () -> QuestFormat.read("{\"format\":2,\"quests\":[]}"));
        assertTrue(e.errors().get(0).contains("newer"));
    }

    @Test
    void idsMustCarryTheQuestKind() {
        for (String id : new String[] {"wheat", "npc_wheat", "quest_", "quest_Wheat"}) {
            assertThrows(DocumentException.class, () -> QuestFormat.read(
                    "{\"format\":1,\"quests\":[{\"id\":\"" + id + "\",\"title\":\"a\",\"goals\":[],\"rewards\":[]}]}"), id);
        }
    }

    @Test
    void reportsEveryProblem() {
        DocumentException e = assertThrows(DocumentException.class, () -> QuestFormat.read("""
                { "format": 1, "quests": [
                  { "id": "quest_a", "goals": [], "rewards": [] },
                  { "id": "quest_b", "title": "B", "icon": "wheat", "goals": [], "rewards": [] },
                  { "id": "quest_c", "title": "C", "goals": [ { "item": "minecraft:wheat", "count": 0 } ], "rewards": [] },
                  { "id": "quest_d", "title": "D", "goals": [ { "item": "minecraft:wheat", "count": 2.5 } ], "rewards": [] },
                  { "id": "quest_e", "title": "E", "goals": [], "rewards": [ { "item": "Emerald", "count": 1 } ] },
                  { "id": "quest_f", "title": "F", "goals": [] },
                  { "id": "quest_g", "title": "G", "goals": [], "rewards": [] },
                  { "id": "quest_g", "title": "G2", "goals": [], "rewards": [] } ] }
                """));
        // missing title, bad icon, count 0, fractional count, bad item, missing rewards, duplicate id
        assertEquals(7, e.errors().size(), e.errors().toString());
    }
}
