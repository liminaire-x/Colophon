/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

import kr.guinnessgroup.colophon.runtime.state.StorageService;
import kr.guinnessgroup.colophon.runtime.type.Type;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-execution context passed to every node. Carries the triggering server, the
 * acting player (if any), and a local variable scope for this run.
 * <p>
 * Persistent state (LOCAL / PLAYER / GLOBAL scopes) is reached through the
 * {@link StorageService}; per-execution locals live in {@link #locals()};
 * data-port values produced during this run live in {@link #values()} (contract c).
 */
public final class ExecContext {

    private final MinecraftServer server;
    private final ServerPlayer actor; // may be null for non-player triggers
    private final StorageService storage;
    private final Map<String, Object> locals = new HashMap<>();
    private final ValueStore values = new ValueStore();

    // Data-flow wiring, set by the scheduler for the current run/step (contract e).
    private ValueResolver resolver;
    private String currentNodeId;

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

    /** Data-port values produced during this execution (exec push / pure pull). */
    public ValueStore values() { return values; }

    // --- data flow (contract e), driven by the scheduler ---

    /** Bind the per-execution resolver (scheduler-owned). */
    public void bindResolver(ValueResolver resolver) { this.resolver = resolver; }

    /** Set the node whose execute() is running, so get/set target its ports. */
    public void setCurrentNodeId(String nodeId) { this.currentNodeId = nodeId; }

    /** The typed value of the current exec node's data input, or {@code null} if unset. */
    public <T> T get(String portId, Type<T> type) {
        return resolver == null ? null : resolver.input(currentNodeId, portId, type);
    }

    /** Push a data output of the current exec node so downstream consumers can read it. */
    public <T> void set(String portId, Type<T> type, T value) {
        values.put(currentNodeId, portId, value);
    }
}
