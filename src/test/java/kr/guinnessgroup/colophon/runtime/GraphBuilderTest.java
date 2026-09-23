/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.runtime;

import kr.guinnessgroup.colophon.DocumentException;
import kr.guinnessgroup.colophon.graph.GraphFormat;
import kr.guinnessgroup.colophon.nodes.BuiltinNodes;
import kr.guinnessgroup.colophon.record.Owner;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Publish-time checks: what a saved graph may contain beyond its shape. */
class GraphBuilderTest {

    private static NodeRegistry builtins() {
        NodeRegistry r = new NodeRegistry();
        BuiltinNodes.registerAll(r);
        return r;
    }

    /** Content published alongside: one NPC, "chief". */
    private static final Catalog CATALOG = new Catalog(Set.of("chief"));

    private static List<Graph> build(String graphsJson) {
        return GraphBuilder.build(GraphFormat.read("{\"format\":1,\"graphs\":[" + graphsJson + "]}"), builtins(), CATALOG);
    }

    private static String graph(String nodes, String links) {
        return "{\"id\":\"g\",\"name\":\"G\",\"nodes\":[" + nodes + "],\"links\":[" + links + "]}";
    }

    private static final String JOIN = "{\"id\":\"j\",\"type\":\"colophon:on_player_join\"}";
    private static final String CHECK = "{\"id\":\"c\",\"type\":\"colophon:has_flag\",\"config\":{\"flag\":\"greeted\"}}";
    private static final String SAY = "{\"id\":\"s\",\"type\":\"colophon:send_message\",\"config\":{\"message\":\"hi\"}}";

    @Test
    void buildsFirstGreeting() {
        Graph g = build(graph(JOIN + "," + CHECK + "," + SAY,
                "{\"from\":\"j\",\"to\":\"c\"},{\"from\":\"c\",\"out\":\"no\",\"to\":\"s\"}")).get(0);
        assertEquals("c", g.node("j").after("next"));
        assertEquals("s", g.node("c").after("no"));
        assertNull(g.node("c").after("yes"));
    }

    @Test
    void rejectsUnknownNodeType() {
        DocumentException e = assertThrows(DocumentException.class,
                () -> build(graph("{\"id\":\"x\",\"type\":\"colophon:nope\"}", "")));
        assertTrue(e.errors().get(0).contains("unknown type"));
    }

    @Test
    void rejectsUnusableSettings() {
        assertThrows(DocumentException.class, () -> build(graph(
                "{\"id\":\"f\",\"type\":\"colophon:has_flag\",\"config\":{\"flag\":\"Greeted!\"}}", "")));
        assertThrows(DocumentException.class, () -> build(graph(
                "{\"id\":\"m\",\"type\":\"colophon:send_message\",\"config\":{\"message\":\" \"}}", "")));
    }

    @Test
    void rejectsWayOutTheNodeDoesNotHave() {
        assertThrows(DocumentException.class, () -> build(graph(JOIN + "," + SAY,
                "{\"from\":\"j\",\"out\":\"yes\",\"to\":\"s\"}")));
    }

    @Test
    void rejectsLinkIntoTrigger() {
        assertThrows(DocumentException.class, () -> build(graph(JOIN + "," + SAY,
                "{\"from\":\"s\",\"to\":\"j\"}")));
    }

    @Test
    void rejectsTwoLinksFromOneWayOut() {
        assertThrows(DocumentException.class, () -> build(graph(JOIN + "," + CHECK + "," + SAY,
                "{\"from\":\"j\",\"to\":\"c\"},{\"from\":\"j\",\"to\":\"s\"}")));
    }

    private static String npcTrigger(String npc) {
        return "{\"id\":\"t\",\"type\":\"colophon:on_npc_interact\",\"config\":{\"npc\":\"" + npc + "\"}}";
    }

    @Test
    void npcTriggerMustNameAPublishedNpc() {
        build(graph(npcTrigger("chief"), ""));
        DocumentException e = assertThrows(DocumentException.class, () -> build(graph(npcTrigger("chef"), "")));
        assertTrue(e.errors().get(0).contains("no NPC with id 'chef'"));
        assertThrows(DocumentException.class, () -> build(graph(npcTrigger(""), "")));
    }

    @Test
    void npcTriggerRunsOnlyForItsNpc() {
        Node trigger = build(graph(npcTrigger("chief"), "")).get(0).node("t").node();
        assertEquals(NodeResult.next(), trigger.run(npcEvent("chief")));
        assertEquals(NodeResult.stop(), trigger.run(npcEvent("smith")));
    }

    private static Context npcEvent(String npc) {
        return new Context(null, null, null, Owner.server("main"), Map.of("npc", npc));
    }
}
