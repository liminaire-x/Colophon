/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.graph;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The saved graph format (format 1). Breaking these breaks every saved graph. */
class GraphFormatTest {

    static final String FIRST_GREETING = """
            {
              "format": 1,
              "graphs": [
                {
                  "id": "first_greeting",
                  "name": "첫 인사",
                  "nodes": [
                    { "id": "n1", "type": "colophon:on_player_join", "config": {}, "pos": [100, 80] },
                    { "id": "n2", "type": "colophon:has_flag", "config": { "flag": "greeted" }, "pos": [300, 80] },
                    { "id": "n3", "type": "colophon:send_message", "config": { "message": "환영합니다" }, "pos": [500, 140] },
                    { "id": "n4", "type": "colophon:set_flag", "config": { "flag": "greeted" }, "pos": [700, 140] }
                  ],
                  "links": [
                    { "from": "n1", "to": "n2" },
                    { "from": "n2", "out": "no", "to": "n3" },
                    { "from": "n3", "to": "n4" }
                  ]
                }
              ]
            }
            """;

    @Test
    void readsTheDocumentedExample() {
        GraphDoc doc = GraphFormat.read(FIRST_GREETING);
        GraphDoc.DocGraph g = doc.graphs().get(0);
        assertEquals("first_greeting", g.id());
        assertEquals("첫 인사", g.name());
        assertEquals(4, g.nodes().size());
        assertEquals("greeted", g.nodes().get(1).config().get("flag").getAsString());
        assertEquals(300, g.nodes().get(1).x());
        assertEquals(new GraphDoc.DocLink("n1", GraphFormat.DEFAULT_OUT, "n2"), g.links().get(0));
        assertEquals(new GraphDoc.DocLink("n2", "no", "n3"), g.links().get(1));
    }

    @Test
    void writeThenReadGivesTheSameDocument() {
        GraphDoc doc = GraphFormat.read(FIRST_GREETING);
        assertEquals(doc, GraphFormat.read(GraphFormat.write(doc)));
    }

    @Test
    void writeLeavesOutTheDefaultWayOut() {
        String json = GraphFormat.write(GraphFormat.read(FIRST_GREETING));
        assertTrue(json.contains("\"out\": \"no\""));
        assertEquals(1, json.split("\"out\"", -1).length - 1);
    }

    @Test
    void positionsAreWholeNumbersOnOneLine() {
        String json = GraphFormat.write(GraphFormat.read(FIRST_GREETING));
        assertTrue(json.contains("\"pos\": [300, 80]"), json);
    }

    @Test
    void fractionalPositionsFromOlderSavesAreRounded() {
        GraphDoc doc = GraphFormat.read("{\"format\":1,\"graphs\":[{\"id\":\"a\",\"name\":\"x\","
                + "\"nodes\":[{\"id\":\"n1\",\"type\":\"t\",\"pos\":[-22.0,148.6]}],\"links\":[]}]}");
        GraphDoc.DocNode n = doc.graphs().get(0).nodes().get(0);
        assertEquals(-22, n.x());
        assertEquals(149, n.y());
    }

    @Test
    void emptyDocumentIsValid() {
        assertEquals(0, GraphFormat.read("{\"format\":1,\"graphs\":[]}").graphs().size());
    }

    @Test
    void rejectsMissingOrNewerFormat() {
        assertThrows(GraphException.class, () -> GraphFormat.read("{\"graphs\":[]}"));
        GraphException newer = assertThrows(GraphException.class, () -> GraphFormat.read("{\"format\":2,\"graphs\":[]}"));
        assertTrue(newer.errors().get(0).contains("newer"));
    }

    @Test
    void rejectsNonJson() {
        assertThrows(GraphException.class, () -> GraphFormat.read("not json"));
        assertThrows(GraphException.class, () -> GraphFormat.read(""));
    }

    @Test
    void rejectsBadOrDuplicateGraphIds() {
        assertThrows(GraphException.class, () -> GraphFormat.read(
                "{\"format\":1,\"graphs\":[{\"id\":\"First Greeting\",\"name\":\"x\",\"nodes\":[],\"links\":[]}]}"));
        assertThrows(GraphException.class, () -> GraphFormat.read(
                "{\"format\":1,\"graphs\":["
                        + "{\"id\":\"a\",\"name\":\"x\",\"nodes\":[],\"links\":[]},"
                        + "{\"id\":\"a\",\"name\":\"y\",\"nodes\":[],\"links\":[]}]}"));
    }

    @Test
    void rejectsLinksToMissingNodesAndReportsEveryProblem() {
        GraphException e = assertThrows(GraphException.class, () -> GraphFormat.read(
                "{\"format\":1,\"graphs\":[{\"id\":\"a\",\"name\":\"x\","
                        + "\"nodes\":[{\"id\":\"n1\",\"type\":\"t\"},{\"id\":\"n1\",\"type\":\"t\"}],"
                        + "\"links\":[{\"from\":\"n1\",\"to\":\"n9\"}]}]}"));
        assertEquals(2, e.errors().size()); // duplicate node id + unknown link target
    }
}
