/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

import com.google.gson.JsonObject;

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

    /**
     * The inputs of one placed instance, given its saved config (contract e). Almost
     * every node's ports are fixed, so this defaults to {@link #inputs()}; a node with
     * instance-dependent ports (e.g. {@code format_text}, whose data inputs come from
     * its template) overrides this to derive them from {@code config}. The parser and
     * value resolver consult this — not {@link #inputs()} — so dynamic ports validate
     * and resolve; the schema still exposes only the static {@link #inputs()}, and the
     * editor derives the rest per instance.
     */
    default List<InputSpec> instanceInputs(JsonObject config) {
        return inputs();
    }

    /**
     * Typed data outputs this node can produce, in order. For a node with selectable
     * outputs (e.g. {@code player_info}) this is the full catalog; {@link #instanceOutputs}
     * narrows it per instance. Default: none. (Contract b.)
     */
    default List<DataPort> dataOutPorts() {
        return List.of();
    }

    /**
     * The data outputs of one placed instance, given its config (contract e) — the
     * output counterpart to {@link #instanceInputs}. Defaults to the full
     * {@link #dataOutPorts()} catalog; a node with selectable outputs overrides this
     * to return only the chosen ports. The parser validates source handles against
     * this, so only selected outputs can be wired; the schema still exposes the whole
     * catalog and the editor shows only the selected ones.
     */
    default List<DataPort> instanceOutputs(JsonObject config) {
        return dataOutPorts();
    }

    /** This node's kind. {@link ExecNodeType} is EXEC; {@link PureNodeType} overrides to PURE. */
    default NodeKind kind() {
        return NodeKind.EXEC;
    }
}
