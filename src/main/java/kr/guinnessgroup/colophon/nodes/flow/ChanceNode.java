/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.nodes.flow;

import com.google.gson.JsonObject;
import kr.guinnessgroup.colophon.runtime.InputSpec;
import kr.guinnessgroup.colophon.runtime.ExecNode;
import kr.guinnessgroup.colophon.runtime.NodeResult;
import kr.guinnessgroup.colophon.runtime.NodeType;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Flow: branches true/false by a percentage chance (exercises Branch). */
public final class ChanceNode implements NodeType {

    @Override public String id() { return "chance"; }
    @Override public String label() { return "Chance"; }
    @Override public String category() { return "flow"; }
    @Override public List<InputSpec> inputs() { return List.of(InputSpec.knob("percent", "number", "50")); }
    @Override public boolean hasFlowIn() { return true; }
    @Override public List<String> flowOutPorts() { return List.of("true", "false"); }

    @Override
    public ExecNode create(JsonObject config) {
        final double percent = readDouble(config, "percent", 50.0);
        return ctx -> {
            boolean hit = ThreadLocalRandom.current().nextDouble(100.0) < percent;
            return NodeResult.branch(hit ? "true" : "false");
        };
    }

    private static double readDouble(JsonObject config, String key, double fallback) {
        if (config != null && config.has(key) && config.get(key).isJsonPrimitive()) {
            try {
                return config.get(key).getAsDouble();
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return fallback;
    }
}
