/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.runtime;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Runs a graph from a start node to the end, right away, on the calling (server)
 * thread. A node that fails or throws ends only this run, never the server.
 * <p>
 * There is no waiting yet: no story needs a delay. When one does, runs that pause
 * come back (see the tick scheduler in legacy-v2).
 */
public final class Runner {

    /** Guards against a loop of links that never ends. */
    static final int MAX_STEPS = 1_000;

    private static final Logger LOGGER = LogUtils.getLogger();

    private Runner() {}

    public static void run(Graph graph, String startId, Context ctx) {
        String current = startId;
        int steps = 0;
        while (current != null) {
            if (++steps > MAX_STEPS) {
                LOGGER.error("[Colophon] Graph '{}' ran more than {} steps (a loop?); stopped", graph.id(), MAX_STEPS);
                return;
            }
            Graph.Placed node = graph.node(current);
            if (node == null) {
                return;
            }
            NodeResult result;
            try {
                result = node.node().run(ctx);
            } catch (Exception e) {
                LOGGER.error("[Colophon] Graph '{}' node '{}' threw; stopped", graph.id(), node.id(), e);
                return;
            }
            switch (result) {
                case NodeResult.Next n -> current = node.after(n.port());
                case NodeResult.Fail f -> {
                    LOGGER.warn("[Colophon] Graph '{}' node '{}' failed: {}", graph.id(), node.id(), f.reason());
                    return;
                }
            }
        }
    }
}
