/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.nodes.flow;

import kr.guinnessgroup.colophon.runtime.InputSpec;
import kr.guinnessgroup.colophon.runtime.PureContext;
import kr.guinnessgroup.colophon.testkit.Fixtures;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tier 1 contract tests (ADR 0004) for the pure value nodes: the {@code createPure}
 * evaluate contract (inputs -> outputs). No runtime, no mocks — a fixed pure context
 * feeds inputs directly.
 */
class PureNodesTest {

    @Test
    void compareProducesBooleanForOp() {
        var eval = new CompareNode().createPure(Fixtures.config("op", ">"));
        PureContext pc = Fixtures.pureContext(Map.of("a", 3.0, "b", 2.0));

        assertEquals(true, eval.evaluate(pc).get("result"));
    }

    @Test
    void compareEqualsOp() {
        var eval = new CompareNode().createPure(Fixtures.config("op", "=="));
        PureContext pc = Fixtures.pureContext(Map.of("a", 2.0, "b", 2.0));

        assertEquals(true, eval.evaluate(pc).get("result"));
    }

    @Test
    void compareUnsetInputProducesNoResult() {
        var eval = new CompareNode().createPure(Fixtures.config("op", ">"));
        // 'b' missing -> unset -> empty output (defined result, not a crash).
        PureContext pc = Fixtures.pureContext(Map.of("a", 3.0));

        assertTrue(eval.evaluate(pc).isEmpty());
    }

    @Test
    void formatTextInterpolatesTokens() {
        var eval = new FormatTextNode().createPure(Fixtures.config("template", "Hi {name}!"));
        PureContext pc = Fixtures.pureContext(Map.of("name", "Bob"));

        assertEquals("Hi Bob!", eval.evaluate(pc).get("text"));
    }

    @Test
    void formatTextUnsetTokenBecomesEmpty() {
        var eval = new FormatTextNode().createPure(Fixtures.config("template", "Hi {name}!"));
        Map<String, Object> none = new HashMap<>(); // token unset
        PureContext pc = Fixtures.pureContext(none);

        assertEquals("Hi !", eval.evaluate(pc).get("text"));
    }

    @Test
    void formatTextDerivesConnectableInputPerToken() {
        // instanceInputs: the template knob plus one connectable input per token.
        var node = new FormatTextNode();
        boolean hasNameInput = node.instanceInputs(Fixtures.config("template", "{name} scored {pts}"))
                .stream().anyMatch(in -> in.id().equals("name") && in.connectable());
        boolean templateIsKnob = node.instanceInputs(Fixtures.config("template", "{name}"))
                .stream().anyMatch(in -> in.id().equals("template") && !in.connectable());

        assertTrue(hasNameInput);
        assertTrue(templateIsKnob);
        // A plain template with no tokens has no connectable inputs.
        assertFalse(node.instanceInputs(Fixtures.config("template", "hello"))
                .stream().anyMatch(InputSpec::connectable));
    }
}
