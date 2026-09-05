/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.nodes.action;

import com.google.gson.JsonObject;
import kr.guinnessgroup.colophon.runtime.FieldSpec;
import kr.guinnessgroup.colophon.runtime.Node;
import kr.guinnessgroup.colophon.runtime.NodeResult;
import kr.guinnessgroup.colophon.runtime.NodeType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Action: sends a chat message to the acting player. */
public final class SendMessageNode implements NodeType {

    @Override public String id() { return "send_message"; }
    @Override public String label() { return "Send Message"; }
    @Override public String category() { return "action"; }
    @Override public List<FieldSpec> fields() { return List.of(new FieldSpec("message", "string", "")); }
    @Override public boolean hasFlowIn() { return true; }
    @Override public List<String> flowOutPorts() { return List.of("out"); }

    @Override
    public Node create(JsonObject config) {
        final String message = (config != null && config.has("message") && !config.get("message").isJsonNull())
                ? config.get("message").getAsString() : "";
        return ctx -> {
            ServerPlayer player = ctx.actor();
            if (player != null) {
                player.sendSystemMessage(Component.literal(message));
            }
            return NodeResult.cont();
        };
    }
}
