/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

import kr.guinnessgroup.colophon.nodes.flow.CompareNode;
import kr.guinnessgroup.colophon.nodes.flow.FormatTextNode;
import kr.guinnessgroup.colophon.runtime.type.Types;
import kr.guinnessgroup.colophon.testkit.Fixtures;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tier 1 contract test (ADR 0004) for {@link ValueResolver}: the public value-flow
 * surface — inline defaults, pure pull + memoize, exec-output-unset, and the pure
 * data-cycle guard. Lives in the runtime package to reach the package-private
 * {@code output} accessor. No mocks; real nodes and a real resolver.
 */
class ValueResolverTest {

    @Test
    void inlineDefaultParsesFromConfig() {
        // An unwired data input reads its per-instance inline value, typed.
        GraphNode c = Fixtures.pure("c", new CompareNode(), Fixtures.config("a", "3", "b", "2"));
        Graph g = Fixtures.graph(c);
        ExecContext ctx = Fixtures.ctx();
        ValueResolver r = Fixtures.resolver(g, ctx);

        assertEquals(3.0, r.input("c", "a", Types.NUMBER));
    }

    @Test
    void pullsPureProducerThroughDataSource() {
        // A consumer input wired to a compare producer: resolving it pulls the pure
        // node (3 > 2 -> true), memoizes the output, and resolves the boolean.
        GraphNode compare = Fixtures.pure("cmp", new CompareNode(),
                Fixtures.config("a", "3", "b", "2", "op", ">"));
        GraphNode consumer = Fixtures.pure("consumer", new CompareNode(), Fixtures.config(),
                Map.of("condition", new PortRef("cmp", "result")));
        Graph g = Fixtures.graph(compare, consumer);
        ExecContext ctx = Fixtures.ctx();
        ValueResolver r = Fixtures.resolver(g, ctx);

        // input() resolves via the wired producer using the requested type, not the
        // consumer's port schema.
        assertEquals(Boolean.TRUE, r.input("consumer", "condition", Types.BOOLEAN));
        // Memoized into the store after the first pull.
        assertTrue(ctx.values().has("cmp", "result"));
    }

    @Test
    void execOutputUnsetBeforePush() {
        // A pure node with an unset input produces no output (compare returns empty).
        GraphNode compare = Fixtures.pure("cmp", new CompareNode(), Fixtures.config("op", ">"));
        Graph g = Fixtures.graph(compare);
        ValueResolver r = Fixtures.resolver(g, Fixtures.ctx());

        assertNull(r.output("cmp", "result"));
    }

    @Test
    void pureDataCycleResolvesToUnsetInsteadOfLooping() {
        // A.text <- B.text and B.text <- A.text. The cycle guard must break the
        // recursion (returning unset) rather than overflow the stack.
        GraphNode a = Fixtures.pure("A", new FormatTextNode(), Fixtures.config("template", "{x}"),
                Map.of("x", new PortRef("B", "text")));
        GraphNode b = Fixtures.pure("B", new FormatTextNode(), Fixtures.config("template", "{y}"),
                Map.of("y", new PortRef("A", "text")));
        Graph g = Fixtures.graph(a, b);
        ValueResolver r = Fixtures.resolver(g, Fixtures.ctx());

        // The guard breaks the recursion: evaluation terminates (no stack overflow)
        // and the back-edge resolves to unset, which the template renders as "".
        Object out = assertDoesNotThrow(() -> r.output("A", "text"));
        assertEquals("", out);
    }
}
