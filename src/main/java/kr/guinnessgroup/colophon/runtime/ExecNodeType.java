/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

import com.google.gson.JsonObject;

import java.util.List;

/**
 * A node type that runs in the control-flow graph (contract a/c): it has flow ports,
 * may have side effects, and builds an {@link ExecNode}. The flow methods are
 * abstract so the compiler forces every exec node to declare its wiring.
 */
public interface ExecNodeType extends NodeType {

    /** Whether this node accepts an incoming execution edge. Triggers: false. */
    boolean hasFlowIn();

    /** Named flow outputs, in order. Straight: ["out"]; branch: ["true","false"]; terminal: []. */
    List<String> flowOutPorts();

    /** Builds the runnable exec node from saved config. */
    ExecNode create(JsonObject config);
}
