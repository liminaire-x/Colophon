/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.nodes.action;

import com.google.gson.JsonObject;
import kr.guinnessgroup.colophon.runtime.InputSpec;
import kr.guinnessgroup.colophon.runtime.ExecNode;
import kr.guinnessgroup.colophon.runtime.NodeResult;
import kr.guinnessgroup.colophon.runtime.ExecNodeType;
import kr.guinnessgroup.colophon.runtime.type.Types;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Action: sends a chat message to a player. The recipient comes from the
 * connectable {@code target} data input (contract e) when wired — e.g. from a
 * trigger's player output — and falls back to the acting player when the input is
 * unset. Does nothing if neither resolves to an online player.
 */
public final class SendMessageNode implements ExecNodeType {

    @Override public String id() { return "send_message"; }
    @Override public String label() { return "Send Message"; }
    @Override public String category() { return "action"; }
    @Override public List<InputSpec> inputs() {
        return List.of(
                InputSpec.data("target", "colophon:player", "Target"),
                InputSpec.knob("message", "string", ""));
    }
    @Override public boolean hasFlowIn() { return true; }
    @Override public List<String> flowOutPorts() { return List.of("out"); }

    @Override
    public ExecNode create(JsonObject config) {
        final String message = (config != null && config.has("message") && !config.get("message").isJsonNull())
                ? config.get("message").getAsString() : "";
        return ctx -> {
            // Wired target wins; unset target falls back to the acting player.
            ServerPlayer target = ctx.get("target", Types.PLAYER);
            ServerPlayer player = (target != null) ? target : ctx.actor();
            if (player != null) {
                player.sendSystemMessage(Component.literal(message));
            }
            return NodeResult.cont();
        };
    }
}
