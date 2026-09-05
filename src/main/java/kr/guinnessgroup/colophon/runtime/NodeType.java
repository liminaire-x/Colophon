/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

import com.google.gson.JsonObject;

import java.util.List;

/**
 * A registered node type: its editor-facing descriptor (label, category,
 * config fields, flow ports) plus a factory that builds a runnable {@link Node}
 * from a node's saved config.
 * <p>
 * Flow ports model control flow. A node with no flow-in is an entry point
 * (a trigger); a node's flow-out ports name its downstream connections
 * ("out" for a straight action, "true"/"false" for a branch, none for a
 * terminal). Data ports (typed values) come later.
 */
public interface NodeType {

    String id();

    String label();

    /** "trigger" (an entry point bound to a game event) or "action". */
    String category();

    List<FieldSpec> fields();

    /** Whether this node accepts an incoming execution edge. Triggers: false. */
    boolean hasFlowIn();

    /** Named execution outputs, in order. Straight node: ["out"]; branch: ["true","false"]; terminal: []. */
    List<String> flowOutPorts();

    Node create(JsonObject config);
}
