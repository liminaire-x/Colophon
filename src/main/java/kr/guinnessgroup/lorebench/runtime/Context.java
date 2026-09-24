/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench.runtime;

import kr.guinnessgroup.lorebench.record.Owner;
import kr.guinnessgroup.lorebench.record.RecordStore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/**
 * What a node can reach during one run.
 *
 * @param server      the server
 * @param player      the player of the event that started this run; {@code null} if the
 *                    event has none. Actions apply to this player.
 * @param records     saved records (flags now, quest state later)
 * @param serverOwner this server's record owner
 * @param event       facts about the event, e.g. {@code npc -> npc_7ha2m0qe} for an NPC interaction
 */
public record Context(MinecraftServer server, ServerPlayer player, RecordStore records,
                      Owner serverOwner, Map<String, String> event) {

    /** A fact about the event, or {@code null}. */
    public String event(String key) {
        return event.get(key);
    }
}
