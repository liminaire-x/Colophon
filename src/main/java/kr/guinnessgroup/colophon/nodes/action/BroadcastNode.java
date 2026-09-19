/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.nodes.action;

import com.google.gson.JsonObject;
import kr.guinnessgroup.colophon.runtime.FieldSpec;
import kr.guinnessgroup.colophon.runtime.ExecNode;
import kr.guinnessgroup.colophon.runtime.NodeResult;
import kr.guinnessgroup.colophon.runtime.NodeType;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Action: broadcasts a chat message to every player on the server. */
public final class BroadcastNode implements NodeType {

    @Override public String id() { return "broadcast"; }
    @Override public String label() { return "Broadcast"; }
    @Override public String category() { return "action"; }
    @Override public List<FieldSpec> fields() { return List.of(new FieldSpec("message", "string", "")); }
    @Override public boolean hasFlowIn() { return true; }
    @Override public List<String> flowOutPorts() { return List.of("out"); }

    @Override
    public ExecNode create(JsonObject config) {
        final String message = (config != null && config.has("message") && !config.get("message").isJsonNull())
                ? config.get("message").getAsString() : "";
        return ctx -> {
            if (ctx.server() != null) {
                ctx.server().getPlayerList().broadcastSystemMessage(Component.literal(message), false);
            }
            return NodeResult.cont();
        };
    }
}
