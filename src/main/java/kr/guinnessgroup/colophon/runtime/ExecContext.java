/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

import kr.guinnessgroup.colophon.runtime.state.StorageService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-execution context passed to every node. Carries the triggering server, the
 * acting player (if any), and a local variable scope for this run.
 * <p>
 * Persistent state (LOCAL / PLAYER / GLOBAL scopes) is reached through the
 * {@link StorageService}; per-execution locals live in {@link #locals()}.
 */
public final class ExecContext {

    private final MinecraftServer server;
    private final ServerPlayer actor; // may be null for non-player triggers
    private final StorageService storage;
    private final Map<String, Object> locals = new HashMap<>();

    public ExecContext(MinecraftServer server, ServerPlayer actor, StorageService storage) {
        this.server = server;
        this.actor = actor;
        this.storage = storage;
    }

    public MinecraftServer server() { return server; }

    public ServerPlayer actor() { return actor; }

    /** Persistent state store (scope-routed variables). */
    public StorageService storage() { return storage; }

    public Map<String, Object> locals() { return locals; }
}
