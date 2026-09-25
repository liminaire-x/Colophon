/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Folders shared by the quest, NPC and graph documents. Breaking these loses the editor's trees. */
class FoldersTest {

    static List<Folders.Folder> read(String folders, List<String> errors) {
        return Folders.read(JsonParser.parseString(folders), "doc", errors);
    }

    @Test
    void foldersNestAndRoundTrip() {
        List<String> errors = new ArrayList<>();
        List<Folders.Folder> folders = read("""
                [ { "id": "folder_town", "name": "마을" },
                  { "id": "folder_chief", "name": " 촌장 ", "parent": "folder_town" },
                  { "id": "folder_empty", "name": "빈 폴더" } ]
                """, errors);
        assertEquals(List.of(), errors);
        assertEquals(List.of(new Folders.Folder("folder_town", "마을", ""),
                new Folders.Folder("folder_chief", "촌장", "folder_town"),
                new Folders.Folder("folder_empty", "빈 폴더", "")), folders);

        JsonObject root = new JsonObject();
        Folders.write(root, folders);
        assertEquals(folders, Folders.read(root.get("folders"), "doc", errors));
        assertFalse(root.toString().contains("\"parent\":\"\""), root.toString());
    }

    @Test
    void noFoldersIsNotWritten() {
        assertEquals(List.of(), Folders.read(null, "doc", new ArrayList<>()));
        JsonObject root = new JsonObject();
        Folders.write(root, List.of());
        assertFalse(root.has("folders"));
    }

    @Test
    void foldersMustFormATree() {
        for (String folders : new String[] {
                "{ \"id\": \"town\", \"name\": \"a\" }",                                                   // not a folder id
                "{ \"id\": \"folder_a\", \"name\": \" \" }",                                               // no name
                "{ \"id\": \"folder_a\", \"name\": \"a\" }, { \"id\": \"folder_a\", \"name\": \"b\" }",    // duplicate
                "{ \"id\": \"folder_a\", \"name\": \"a\", \"parent\": \"folder_x\" }",                     // unknown parent
                "{ \"id\": \"folder_a\", \"name\": \"a\", \"parent\": \"folder_a\" }",                     // inside itself
                "{ \"id\": \"folder_a\", \"name\": \"a\", \"parent\": \"folder_b\" }, "
                        + "{ \"id\": \"folder_b\", \"name\": \"b\", \"parent\": \"folder_a\" }"}) {        // loop
            List<String> errors = new ArrayList<>();
            read("[" + folders + "]", errors);
            assertFalse(errors.isEmpty(), folders);
        }
    }

    @Test
    void aPlacementMustNameAFolderInTheList() {
        List<String> errors = new ArrayList<>();
        List<Folders.Folder> folders = read("[ { \"id\": \"folder_a\", \"name\": \"a\" } ]", errors);
        JsonObject in = JsonParser.parseString("{ \"folder\": \"folder_a\" }").getAsJsonObject();
        JsonObject top = new JsonObject();
        JsonObject missing = JsonParser.parseString("{ \"folder\": \"folder_x\" }").getAsJsonObject();
        assertEquals("folder_a", Folders.placement(in, folders, "thing", errors));
        assertEquals("", Folders.placement(top, folders, "thing", errors));
        assertTrue(errors.isEmpty(), errors.toString());
        Folders.placement(missing, folders, "thing", errors);
        assertEquals(1, errors.size(), errors.toString());

        JsonObject written = new JsonObject();
        Folders.writePlacement(written, "");
        assertFalse(written.has("folder"));
    }
}
