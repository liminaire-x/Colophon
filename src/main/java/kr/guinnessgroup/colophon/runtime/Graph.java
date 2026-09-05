/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

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
