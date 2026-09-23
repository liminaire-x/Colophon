/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.runtime;

import kr.guinnessgroup.colophon.record.RecordStore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * What a node can reach during one run.
 *
 * @param server  the server
 * @param player  the player of the event that started this run; {@code null} if the
 *                event has none. Actions apply to this player.
 * @param records saved records (flags now, quest state later)
 */
public record Context(MinecraftServer server, ServerPlayer player, RecordStore records) {}
