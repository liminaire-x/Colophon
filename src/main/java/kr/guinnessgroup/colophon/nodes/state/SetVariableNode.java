/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.nodes.state;

import com.google.gson.JsonObject;
import kr.guinnessgroup.colophon.runtime.InputSpec;
import kr.guinnessgroup.colophon.runtime.ExecNode;
import kr.guinnessgroup.colophon.runtime.NodeResult;
import kr.guinnessgroup.colophon.runtime.ExecNodeType;
import kr.guinnessgroup.colophon.runtime.state.Scope;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

/**
 * Action: stores a variable in the given scope. Flow-only &mdash; the value is a
 * constant from config, not a data-port input.
 */
public final class SetVariableNode implements ExecNodeType {

    @Override public String id() { return "set_variable"; }
    @Override public String label() { return "Set Variable"; }
    @Override public String category() { return "state"; }
    @Override public List<InputSpec> inputs() {
        return List.of(
                InputSpec.enumKnob("scope", "PLAYER", List.of("PLAYER", "GLOBAL", "LOCAL")),
                InputSpec.knob("key", "string", ""),
                InputSpec.knob("value", "string", ""));
    }
    @Override public boolean hasFlowIn() { return true; }
    @Override public List<String> flowOutPorts() { return List.of("out"); }

    @Override
    public ExecNode create(JsonObject config) {
        final Scope scope = Scope.parse(readString(config, "scope"), Scope.PLAYER);
        final String key = readString(config, "key");
        final String value = readString(config, "value");
        return ctx -> {
            ServerPlayer actor = ctx.actor();
            UUID uuid = (actor != null) ? actor.getUUID() : null;
            ctx.storage().set(scope, key, value, uuid);
            return NodeResult.cont();
        };
    }

    private static String readString(JsonObject config, String k) {
        return (config != null && config.has(k) && !config.get(k).isJsonNull())
                ? config.get(k).getAsString() : "";
    }
}
