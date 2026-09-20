/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

import java.util.Map;

/**
 * A node instance placed in a graph. Holds its built runnable — an {@link ExecNode}
 * (when {@code type.kind() == EXEC}) or a {@link PureNode} (when PURE), exactly one
 * of which is non-null — plus its wiring: {@code outputs} maps a flow output port to
 * the next node id, and {@code dataSources} maps each data input port to the producer
 * output that feeds it. Pure nodes have no flow wiring; they are pulled on demand.
 */
public record GraphNode(String id, NodeType type, ExecNode exec, PureNode pure,
                        Map<String, String> outputs, Map<String, PortRef> dataSources) {

    public static final String DEFAULT_PORT = "out";

    /** Handle id of a node's single flow input (a node either has one or is an entry point). */
    public static final String FLOW_IN_PORT = "in";

    /** The id of the node wired to the given flow output port, or null if none. */
    public String next(String port) {
        return outputs == null ? null : outputs.get(port);
    }

    /** The producer (node + output port) feeding the given data input port, or null if unwired. */
    public PortRef dataSource(String inputPortId) {
        return dataSources == null ? null : dataSources.get(inputPortId);
    }
}
