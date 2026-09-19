/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

import java.util.Map;

/**
 * A node instance placed in a graph, with its wiring to downstream nodes.
 * {@code outputs} maps an output port name to the id of the next node.
 */
public record GraphNode(String id, ExecNode node, Map<String, String> outputs) {

    public static final String DEFAULT_PORT = "out";

    /** Handle id of a node's single flow input (a node either has one or is an entry point). */
    public static final String FLOW_IN_PORT = "in";

    /** The id of the node wired to the given output port, or null if none. */
    public String next(String port) {
        return outputs == null ? null : outputs.get(port);
    }
}
