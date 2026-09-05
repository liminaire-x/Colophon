package kr.guinnessgroup.colophon.runtime;

import java.util.Map;

/**
 * A node instance placed in a graph, with its wiring to downstream nodes.
 * {@code outputs} maps an output port name to the id of the next node.
 */
public record GraphNode(String id, Node node, Map<String, String> outputs) {

    public static final String DEFAULT_PORT = "out";

    /** The id of the node wired to the given output port, or null if none. */
    public String next(String port) {
        return outputs == null ? null : outputs.get(port);
    }
}
