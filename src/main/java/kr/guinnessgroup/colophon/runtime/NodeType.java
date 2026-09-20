/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

import com.google.gson.JsonObject;

import java.util.List;

/**
 * A registered node type: its editor-facing descriptor (label, category, inputs,
 * flow/data ports) plus a factory that builds a runnable {@link ExecNode} from a
 * node's saved config.
 * <p>
 * Flow ports model control flow. A node with no flow-in is an entry point
 * (a trigger); a node's flow-out ports name its downstream connections
 * ("out" for a straight action, "true"/"false" for a branch, none for a
 * terminal).
 * <p>
 * Inputs ({@link InputSpec}) unify config knobs and typed data inputs (contract d):
 * a connectable input is a data port that may only connect to a data output of the
 * same {@code typeId} (never a flow port), an inline-only input is a config knob.
 * Data outputs ({@link DataPort}) are declared separately via {@link #dataOutPorts()}.
 */
public interface NodeType {

    String id();

    String label();

    /** "trigger" (an entry point bound to a game event) or "action". */
    String category();

    /** Unified inputs (contract d): config knobs (inline-only) and data inputs (connectable). */
    default List<InputSpec> inputs() {
        return List.of();
    }

    /** Whether this node accepts an incoming execution edge. Triggers: false. */
    boolean hasFlowIn();

    /** Named execution outputs, in order. Straight node: ["out"]; branch: ["true","false"]; terminal: []. */
    List<String> flowOutPorts();

    /** Typed data outputs this node produces, in order. Default: none. (Contract b.) */
    default List<DataPort> dataOutPorts() {
        return List.of();
    }

    /** This node's kind. Default EXEC; a pure value producer overrides to PURE. (Contract c.) */
    default NodeKind kind() {
        return NodeKind.EXEC;
    }

    /** Builds the runnable exec node from saved config. EXEC types override this. (Contract c.) */
    default ExecNode create(JsonObject config) {
        throw new UnsupportedOperationException("not an exec node: " + id());
    }

    /** Builds the runnable pure node from saved config. PURE types override this. (Contract c.) */
    default PureNode createPure(JsonObject config) {
        throw new UnsupportedOperationException("not a pure node: " + id());
    }
}
