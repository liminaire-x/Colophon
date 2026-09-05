/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-execution context passed to every node. Carries the triggering server, the
 * acting player (if any), and a local variable scope for this run.
 * <p>
 * Persistent state (PLAYER / GLOBAL scopes) will be reached through a storage
 * layer added later; this holds only per-execution locals for now.
 */
public final class ExecContext {

    private final MinecraftServer server;
    private final ServerPlayer actor; // may be null for non-player triggers
    private final Map<String, Object> locals = new HashMap<>();

    public ExecContext(MinecraftServer server, ServerPlayer actor) {
        this.server = server;
        this.actor = actor;
    }

    public MinecraftServer server() { return server; }

    public ServerPlayer actor() { return actor; }

    public Map<String, Object> locals() { return locals; }
}
