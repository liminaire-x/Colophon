/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

import kr.guinnessgroup.colophon.nodes.flow.BranchIfNode;
import kr.guinnessgroup.colophon.nodes.flow.CompareNode;
import kr.guinnessgroup.colophon.nodes.flow.FormatTextNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tier 1 contract test (ADR 0004) for {@link GraphParser}: the locked parse/validate
 * surface — nominal type match on data edges, the flow/data category split, the
 * single-wire rule, instance-derived ports, and flow wiring. Registers only the node
 * types it uses (not {@code BuiltinNodes.registerAll}, which needs the MC ModList).
 */
class GraphParserTest {

    @BeforeAll
    static void registerNodes() {
        NodeRegistry.register(new CompareNode());
        NodeRegistry.register(new FormatTextNode());
        NodeRegistry.register(new BranchIfNode());
    }

    /** One node: {@code {"id":..,"data":{"nodeType":..,"config":{..}}}}. */
    private static String node(String id, String type, String configJson) {
        return "{\"id\":\"" + id + "\",\"data\":{\"nodeType\":\"" + type + "\",\"config\":" + configJson + "}}";
    }

    private static String graph(String nodes, String edges) {
        return "{\"nodes\":[" + nodes + "],\"edges\":[" + edges + "]}";
    }

    @Test
    void unknownTypeRejected() {
        String json = graph(node("n", "nope", "{}"), "");
        GraphValidationException ex = assertThrows(GraphValidationException.class,
                () -> GraphParser.parse(json));
        assertTrue(ex.errors().stream().anyMatch(e -> e.contains("unknown type")), ex.getMessage());
    }

    @Test
    void validDataEdgeWiresSource() {
        // format_text 'a' produces text:string; format_text 'b' has an instance-derived
        // string input 'y' (from its template token). Same type -> valid, and wired.
        String nodes = node("a", "format_text", "{\"template\":\"{x}\"}") + ","
                + node("b", "format_text", "{\"template\":\"{y}\"}");
        String edge = "{\"source\":\"a\",\"target\":\"b\",\"sourceHandle\":\"text\",\"targetHandle\":\"y\"}";

        GraphParser.Parsed parsed = GraphParser.parse(graph(nodes, edge));

        assertEquals(new PortRef("a", "text"), parsed.graph().node("b").dataSource("y"));
    }

    @Test
    void typeMismatchRejected() {
        // compare 'a' produces result:boolean; format_text 'b' input 'y' is string.
        String nodes = node("a", "compare", "{}") + ","
                + node("b", "format_text", "{\"template\":\"{y}\"}");
        String edge = "{\"source\":\"a\",\"target\":\"b\",\"sourceHandle\":\"result\",\"targetHandle\":\"y\"}";

        GraphValidationException ex = assertThrows(GraphValidationException.class,
                () -> GraphParser.parse(graph(nodes, edge)));
        assertTrue(ex.errors().stream().anyMatch(e -> e.contains("type mismatch")), ex.getMessage());
    }

    @Test
    void flowOutputToDataInputRejected() {
        // branch_if flow output 'true' may not connect to a data input.
        String nodes = node("a", "branch_if", "{}") + ","
                + node("b", "format_text", "{\"template\":\"{y}\"}");
        String edge = "{\"source\":\"a\",\"target\":\"b\",\"sourceHandle\":\"true\",\"targetHandle\":\"y\"}";

        GraphValidationException ex = assertThrows(GraphValidationException.class,
                () -> GraphParser.parse(graph(nodes, edge)));
        assertTrue(ex.errors().stream().anyMatch(e -> e.contains("cannot connect flow output")), ex.getMessage());
    }

    @Test
    void dataInputConnectedTwiceRejected() {
        // Two producers into the same data input violates the single-wire rule.
        String nodes = node("a1", "format_text", "{\"template\":\"{x}\"}") + ","
                + node("a2", "format_text", "{\"template\":\"{x}\"}") + ","
                + node("b", "format_text", "{\"template\":\"{y}\"}");
        String edges = "{\"source\":\"a1\",\"target\":\"b\",\"sourceHandle\":\"text\",\"targetHandle\":\"y\"},"
                + "{\"source\":\"a2\",\"target\":\"b\",\"sourceHandle\":\"text\",\"targetHandle\":\"y\"}";

        GraphValidationException ex = assertThrows(GraphValidationException.class,
                () -> GraphParser.parse(graph(nodes, edges)));
        assertTrue(ex.errors().stream().anyMatch(e -> e.contains("connected more than once")), ex.getMessage());
    }

    @Test
    void flowEdgeWiresNextNode() {
        String nodes = node("a", "branch_if", "{}") + "," + node("b", "branch_if", "{}");
        String edge = "{\"source\":\"a\",\"target\":\"b\",\"sourceHandle\":\"true\"}";

        GraphParser.Parsed parsed = GraphParser.parse(graph(nodes, edge));

        assertEquals("b", parsed.graph().node("a").next("true"));
    }
}
