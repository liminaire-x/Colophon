/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.nodes.flow;

import com.google.gson.JsonObject;
import kr.guinnessgroup.colophon.runtime.DataPort;
import kr.guinnessgroup.colophon.runtime.InputSpec;
import kr.guinnessgroup.colophon.runtime.NodeKind;
import kr.guinnessgroup.colophon.runtime.NodeType;
import kr.guinnessgroup.colophon.runtime.PureNode;
import kr.guinnessgroup.colophon.runtime.type.Types;

import java.util.List;
import java.util.Map;

/**
 * Pure node (contract c/e): compares two numbers and produces a boolean. No flow,
 * no side effects — it is pulled when a consumer reads its {@code result} output.
 * If either input is unset, the result is unset (no value produced).
 */
public final class CompareNode implements NodeType {

    @Override public String id() { return "compare"; }
    @Override public String label() { return "Compare"; }
    @Override public String category() { return "flow"; }
    @Override public NodeKind kind() { return NodeKind.PURE; }
    @Override public List<InputSpec> inputs() {
        return List.of(
                InputSpec.data("a", "number", "A"),
                InputSpec.data("b", "number", "B"),
                InputSpec.enumKnob("op", ">", List.of(">", ">=", "<", "<=", "==", "!=")));
    }
    @Override public List<DataPort> dataOutPorts() {
        return List.of(new DataPort("result", "boolean", "Result"));
    }

    @Override
    public PureNode createPure(JsonObject config) {
        final String op = (config != null && config.has("op") && !config.get("op").isJsonNull())
                ? config.get("op").getAsString() : ">";
        return ctx -> {
            Double a = ctx.get("a", Types.NUMBER);
            Double b = ctx.get("b", Types.NUMBER);
            if (a == null || b == null) {
                return Map.of(); // unset input -> unset result
            }
            int cmp = Double.compare(a, b);
            boolean result = switch (op) {
                case ">" -> cmp > 0;
                case ">=" -> cmp >= 0;
                case "<" -> cmp < 0;
                case "<=" -> cmp <= 0;
                case "==" -> cmp == 0;
                case "!=" -> cmp != 0;
                default -> false;
            };
            return Map.of("result", result);
        };
    }
}
