package kr.guinnessgroup.colophon.runtime;

import java.util.Map;

/**
 * A resolved, runnable graph: node instances keyed by id. Building this from the
 * published JSON definition (and registering its triggers) is added later.
 */
public record Graph(Map<String, GraphNode> nodes) {

    public GraphNode node(String id) {
        return nodes.get(id);
    }
}
