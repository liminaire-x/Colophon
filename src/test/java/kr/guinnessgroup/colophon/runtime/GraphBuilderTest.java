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

    /** Content published alongside: one NPC, "npc_chief", and one quest, "quest_wheat". */
    private static final Catalog CATALOG = new Catalog(Set.of("npc_chief"), Set.of("quest_wheat"));

    private static List<Graph> build(String graphsJson) {
        return GraphBuilder.build(GraphFormat.read("{\"format\":1,\"graphs\":[" + graphsJson + "]}"), builtins(), CATALOG);
    }

    private static String graph(String nodes, String links) {
        return "{\"id\":\"graph_g\",\"name\":\"G\",\"nodes\":[" + nodes + "],\"links\":[" + links + "]}";
    }

    private static final String JOIN = "{\"id\":\"node_j\",\"type\":\"colophon:on_player_join\"}";
    private static final String CHECK = "{\"id\":\"node_c\",\"type\":\"colophon:has_flag\",\"config\":{\"flag\":\"greeted\"}}";
    private static final String SAY = "{\"id\":\"node_s\",\"type\":\"colophon:send_message\",\"config\":{\"message\":\"hi\"}}";

    @Test
    void buildsFirstGreeting() {
        Graph g = build(graph(JOIN + "," + CHECK + "," + SAY,
                "{\"from\":\"node_j\",\"to\":\"node_c\"},{\"from\":\"node_c\",\"out\":\"no\",\"to\":\"node_s\"}")).get(0);
        assertEquals("node_c", g.node("node_j").after("next"));
        assertEquals("node_s", g.node("node_c").after("no"));
        assertNull(g.node("node_c").after("yes"));
    }

    @Test
    void rejectsUnknownNodeType() {
        DocumentException e = assertThrows(DocumentException.class,
                () -> build(graph("{\"id\":\"node_x\",\"type\":\"colophon:nope\"}", "")));
        assertTrue(e.errors().get(0).contains("unknown type"));
    }

    @Test
    void rejectsUnusableSettings() {
        assertThrows(DocumentException.class, () -> build(graph(
                "{\"id\":\"node_f\",\"type\":\"colophon:has_flag\",\"config\":{\"flag\":\"Greeted!\"}}", "")));
        assertThrows(DocumentException.class, () -> build(graph(
                "{\"id\":\"node_m\",\"type\":\"colophon:send_message\",\"config\":{\"message\":\" \"}}", "")));
    }

    @Test
    void rejectsWayOutTheNodeDoesNotHave() {
        assertThrows(DocumentException.class, () -> build(graph(JOIN + "," + SAY,
                "{\"from\":\"node_j\",\"out\":\"yes\",\"to\":\"node_s\"}")));
    }

    @Test
    void rejectsLinkIntoTrigger() {
        assertThrows(DocumentException.class, () -> build(graph(JOIN + "," + SAY,
                "{\"from\":\"node_s\",\"to\":\"node_j\"}")));
    }

    @Test
    void rejectsTwoLinksFromOneWayOut() {
        assertThrows(DocumentException.class, () -> build(graph(JOIN + "," + CHECK + "," + SAY,
                "{\"from\":\"node_j\",\"to\":\"node_c\"},{\"from\":\"node_j\",\"to\":\"node_s\"}")));
    }

    private static String npcTrigger(String npc) {
        return "{\"id\":\"node_t\",\"type\":\"colophon:on_npc_interact\",\"config\":{\"npc\":\"" + npc + "\"}}";
    }

    @Test
    void npcTriggerMustNameAPublishedNpc() {
        build(graph(npcTrigger("npc_chief"), ""));
        DocumentException e = assertThrows(DocumentException.class, () -> build(graph(npcTrigger("npc_chef"), "")));
        assertTrue(e.errors().get(0).contains("no NPC with id 'npc_chef'"));
        assertThrows(DocumentException.class, () -> build(graph(npcTrigger(""), "")));
    }

    @Test
    void npcTriggerRunsOnlyForItsNpc() {
        Node trigger = build(graph(npcTrigger("npc_chief"), "")).get(0).node("node_t").node();
        assertEquals(NodeResult.next(), trigger.run(npcEvent("npc_chief")));
        assertEquals(NodeResult.stop(), trigger.run(npcEvent("npc_smith")));
    }

    @Test
    void playAnimationNeedsAPublishedNpcAndAnAnimationName() {
        String ok = "{\"id\":\"node_a\",\"type\":\"colophon:play_npc_animation\",\"config\":{\"npc\":\"npc_chief\",\"animation\":\"happy\"}}";
        build(graph(ok, ""));
        assertThrows(DocumentException.class, () -> build(graph(
                "{\"id\":\"node_a\",\"type\":\"colophon:play_npc_animation\",\"config\":{\"npc\":\"npc_chef\",\"animation\":\"happy\"}}", "")));
        assertThrows(DocumentException.class, () -> build(graph(
                "{\"id\":\"node_a\",\"type\":\"colophon:play_npc_animation\",\"config\":{\"npc\":\"npc_chief\",\"animation\":\" \"}}", "")));
    }

    private static String questNode(String type, String quest) {
        return "{\"id\":\"node_q\",\"type\":\"colophon:" + type + "\",\"config\":{\"quest\":\"" + quest + "\"}}";
    }

    @Test
    void questNodesNeedAPublishedQuest() {
        for (String type : new String[] {"quest_state", "reveal_quest", "complete_quest"}) {
            build(graph(questNode(type, "quest_wheat"), ""));
            DocumentException e = assertThrows(DocumentException.class, () -> build(graph(questNode(type, "quest_wheet"), "")));
            assertTrue(e.errors().get(0).contains("no quest with id 'quest_wheet'"));
            assertThrows(DocumentException.class, () -> build(graph(questNode(type, ""), "")));
        }
    }

    @Test
    void questStateLeavesThroughItsFourStates() {
        Graph g = build(graph(questNode("quest_state", "quest_wheat") + "," + SAY,
                "{\"from\":\"node_q\",\"out\":\"ready\",\"to\":\"node_s\"}")).get(0);
        assertEquals("node_s", g.node("node_q").after("ready"));
        assertThrows(DocumentException.class, () -> build(graph(questNode("quest_state", "quest_wheat") + "," + SAY,
                "{\"from\":\"node_q\",\"out\":\"next\",\"to\":\"node_s\"}")));
    }

    private static Context npcEvent(String npc) {
        return new Context(null, null, null, Owner.server("main"), Map.of("npc", npc));
    }
}
