/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime.type;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Typed handles for the built-in types (type-system handoff §5). These pair with
 * the descriptors registered in {@link BuiltinTypes} by matching {@code id}. Nodes
 * use these constants with {@code ctx.get(portId, Types.NUMBER)} etc.
 * <p>
 * {@code number} is a single IEEE754 double. {@code player} is a reference: stored
 * as a UUID, resolved to a live {@link ServerPlayer} at use (null if offline).
 */
public final class Types {

    private Types() {}

    public static final Type<String> STRING =
            new Type<>("string", s -> s, (v, ctx) -> String.valueOf(v));

    public static final Type<Double> NUMBER =
            new Type<>("number", Double::parseDouble, (v, ctx) -> ((Number) v).doubleValue());

    public static final Type<Boolean> BOOLEAN =
            new Type<>("boolean", Boolean::parseBoolean, (v, ctx) -> (Boolean) v);

    public static final Type<ServerPlayer> PLAYER =
            new Type<>("colophon:player", null, (v, ctx) -> resolvePlayer(v, ctx.server()));

    private static ServerPlayer resolvePlayer(Object stored, net.minecraft.server.MinecraftServer server) {
        // The transient value store may hold a live player directly, or a UUID handle.
        if (stored instanceof ServerPlayer p) {
            return p;
        }
        if (server == null) {
            return null;
        }
        try {
            UUID id = (stored instanceof UUID u) ? u : UUID.fromString(String.valueOf(stored));
            return server.getPlayerList().getPlayer(id);
        } catch (IllegalArgumentException e) {
            return null; // malformed handle -> unresolved (unset), never crash
        }
    }
}
