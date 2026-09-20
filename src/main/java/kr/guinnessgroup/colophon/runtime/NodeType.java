/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A registered node type: its editor-facing descriptor (label, category,
 * config fields, flow ports) plus a factory that builds a runnable {@link ExecNode}
 * from a node's saved config.
 * <p>
 * Flow ports model control flow. A node with no flow-in is an entry point
 * (a trigger); a node's flow-out ports name its downstream connections
 * ("out" for a straight action, "true"/"false" for a branch, none for a
 * terminal).
 * <p>
 * Data ports ({@link DataPort}) carry typed values and are a separate category
 * from flow ports: a data port may only connect to another data port of the same
 * {@code typeId}, and never to a flow port (contract b). Both port lists default
 * to empty, so existing node types need no change; values do not flow yet
 * (contract b locks the port surface and connection validation only).
 */
public interface NodeType {

    String id();

    String label();

    /** "trigger" (an entry point bound to a game event) or "action". */
    String category();

    List<FieldSpec> fields();

    /**
     * Unified inputs (contract d): config fields and data inputs as one list.
     * Defaults to bridging {@link #fields()} (inline-only knobs) and
     * {@link #dataInPorts()} (connectable data), so existing node types need no
     * change; nodes will later declare {@code inputs()} directly.
     */
    default List<InputSpec> inputs() {
        List<InputSpec> all = new ArrayList<>();
        for (FieldSpec f : fields()) {
            all.add(InputSpec.fromField(f));
        }
        for (DataPort p : dataInPorts()) {
            all.add(InputSpec.fromDataPort(p));
        }
        return all;
    }

    /** Whether this node accepts an incoming execution edge. Triggers: false. */
    boolean hasFlowIn();

    /** Named execution outputs, in order. Straight node: ["out"]; branch: ["true","false"]; terminal: []. */
    List<String> flowOutPorts();

    /** Typed data inputs this node accepts, in order. Default: none. (Contract b.) */
    default List<DataPort> dataInPorts() {
        return List.of();
    }

    /** Typed data outputs this node produces, in order. Default: none. (Contract b.) */
    default List<DataPort> dataOutPorts() {
        return List.of();
    }

    /** This node's kind. Default EXEC; a pure value producer overrides to PURE. (Contract c.) */
    default NodeKind kind() {
        return NodeKind.EXEC;
    }

    /** Builds the runnable exec node from saved config. Only valid for EXEC types. */
    ExecNode create(JsonObject config);

    /** Builds the runnable pure node from saved config. Only PURE types override this. (Contract c.) */
    default PureNode createPure(JsonObject config) {
        throw new UnsupportedOperationException("not a pure node: " + id());
    }
}
