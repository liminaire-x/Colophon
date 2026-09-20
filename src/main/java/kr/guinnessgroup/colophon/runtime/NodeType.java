/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

import java.util.List;

/**
 * The shared descriptor of a registered node type: its identity, editor-facing
 * inputs, and data outputs. A node type is one of two kinds, and the compiler
 * enforces the split (contract a/c): {@link ExecNodeType} (flow + side effects,
 * builds an {@link ExecNode}) or {@link PureNodeType} (side-effect-free value
 * producer, builds a {@link PureNode}). This base carries only what both share;
 * the runnable factory and flow ports live on the two sub-interfaces.
 * <p>
 * Inputs ({@link InputSpec}) unify config knobs and typed data inputs (contract d):
 * a connectable input is a data port that may only connect to a data output of the
 * same {@code typeId} (never a flow port), an inline-only input is a config knob.
 * Data outputs ({@link DataPort}) are declared via {@link #dataOutPorts()}.
 */
public interface NodeType {

    String id();

    String label();

    /** Editor palette group, e.g. "trigger", "action", "flow", "state", "economy". */
    String category();

    /** Unified inputs (contract d): config knobs (inline-only) and data inputs (connectable). */
    default List<InputSpec> inputs() {
        return List.of();
    }

    /** Typed data outputs this node produces, in order. Default: none. (Contract b.) */
    default List<DataPort> dataOutPorts() {
        return List.of();
    }

    /** This node's kind. {@link ExecNodeType} is EXEC; {@link PureNodeType} overrides to PURE. */
    default NodeKind kind() {
        return NodeKind.EXEC;
    }
}
