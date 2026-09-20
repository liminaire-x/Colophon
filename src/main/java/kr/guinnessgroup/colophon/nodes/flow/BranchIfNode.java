/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.nodes.flow;

import com.google.gson.JsonObject;
import kr.guinnessgroup.colophon.runtime.ExecNode;
import kr.guinnessgroup.colophon.runtime.InputSpec;
import kr.guinnessgroup.colophon.runtime.NodeResult;
import kr.guinnessgroup.colophon.runtime.ExecNodeType;
import kr.guinnessgroup.colophon.runtime.type.Types;

import java.util.List;

/**
 * Flow branch driven by a boolean data input (contract e): reads {@code condition}
 * and continues along "true" or "false". This is the exec side that consumes a
 * value a pure node produced (e.g. {@link CompareNode}). An unset condition takes
 * "false" (defined result).
 */
public final class BranchIfNode implements ExecNodeType {

    @Override public String id() { return "branch_if"; }
    @Override public String label() { return "Branch (if)"; }
    @Override public String category() { return "flow"; }
    @Override public List<InputSpec> inputs() {
        return List.of(new InputSpec("condition", "boolean", "Condition", "false", true, List.of()));
    }
    @Override public boolean hasFlowIn() { return true; }
    @Override public List<String> flowOutPorts() { return List.of("true", "false"); }

    @Override
    public ExecNode create(JsonObject config) {
        return ctx -> {
            Boolean condition = ctx.get("condition", Types.BOOLEAN);
            return NodeResult.branch(Boolean.TRUE.equals(condition) ? "true" : "false");
        };
    }
}
