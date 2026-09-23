/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.nodes;

import com.google.gson.JsonObject;
import kr.guinnessgroup.colophon.runtime.Field;
import kr.guinnessgroup.colophon.runtime.Catalog;
import kr.guinnessgroup.colophon.runtime.Node;
import kr.guinnessgroup.colophon.runtime.NodeResult;
import kr.guinnessgroup.colophon.runtime.NodeType;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Sends a chat message to the player. */
public final class SendMessage implements NodeType {

    private static final Field MESSAGE = Field.text("message", "Message");

    @Override public String id() { return "colophon:send_message"; }
    @Override public String label() { return "Send Message"; }
    @Override public String category() { return "action"; }
    @Override public List<Field> fields() { return List.of(MESSAGE); }

    @Override
    public Node create(JsonObject config, Catalog catalog) {
        String message = MESSAGE.read(config);
        if (message.isBlank()) {
            throw new IllegalArgumentException("message is empty");
        }
        return ctx -> {
            if (ctx.player() == null) {
                return NodeResult.fail("no player in this event");
            }
            ctx.player().sendSystemMessage(Component.literal(message));
            return NodeResult.next();
        };
    }
}
