/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench.runtime;

import java.util.Map;

/** A runnable graph, built from a saved one by {@link GraphBuilder}. */
public record Graph(String id, String name, Map<String, Placed> nodes) {

    public Placed node(String nodeId) {
        return nodes.get(nodeId);
    }

    /**
     * A node in this graph: its type, its runnable, and where each way out leads
     * ({@code next}: out port → node id).
     */
    public record Placed(String id, NodeType type, Node node, Map<String, String> next) {

        /** The node linked to {@code port}, or {@code null} if none. */
        public String after(String port) {
            return next.get(port);
        }
    }
}
