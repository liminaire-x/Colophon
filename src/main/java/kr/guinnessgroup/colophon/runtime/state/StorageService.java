/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime.state;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The runtime-facing state store. Routes by {@link Scope} and keeps variables in
 * memory so reads/writes during graph execution never touch the disk. Durable
 * scopes (PLAYER/GLOBAL) are backed by a {@link StateBackend}; the cache is
 * flushed on the world save cycle so state and world stay on the same snapshot.
 * <p>
 * Lifecycle wiring (load on join, flush on world save, evict on quit) is attached
 * by the mod's event handlers; this class only provides the mechanisms.
 * <p>
 * LOCAL scope is delegated to a {@link LocalStore} (SavedData-backed), attached
 * once the world is available.
 */
public final class StorageService {

    private static final Logger LOGGER = LogUtils.getLogger();

    private StateBackend backend;

    // GLOBAL cache.
    private final Map<String, String> global = new HashMap<>();
    private final Set<String> globalDirty = new HashSet<>();

    // PLAYER caches, keyed by UUID.
    private final Map<UUID, PlayerState> players = new HashMap<>();

    // LOCAL: delegated to a SavedData-backed store, attached on world load.
    private LocalStore local;

    private static final class PlayerState {
        final Map<String, String> vars = new HashMap<>();
        final Set<String> dirty = new HashSet<>();
        boolean online = true;
    }

    /** Open the durable backend and load GLOBAL variables into cache. */
    public synchronized void open(StateBackend backend) {
        this.backend = backend;
        global.clear();
        globalDirty.clear();
        global.putAll(backend.loadGlobal());
        LOGGER.info("[Colophon] Storage opened: {} global var(s) loaded", global.size());
    }

    /** Attach the LOCAL-scope store (SavedData-backed). Called once the world is available. */
    public synchronized void setLocalStore(LocalStore local) {
        this.local = local;
    }

    /** Load one player's variables into cache. Call on join, before triggers fire. */
    public synchronized void loadPlayer(UUID player) {
        if (backend == null) {
            return;
        }
        PlayerState existing = players.get(player);
        if (existing != null) {
            // Still cached (e.g. a quick rejoin before the next flush/eviction). The
            // in-memory copy may hold unflushed writes, so reuse it and just mark the
            // player online again -- never clobber it by reloading from the backend.
            existing.online = true;
            return;
        }
        PlayerState st = new PlayerState();
        st.vars.putAll(backend.loadPlayer(player));
        players.put(player, st);
    }

    /** Mark a player offline; their dirty vars persist on the next flush, then they are evicted. */
    public synchronized void markOffline(UUID player) {
        PlayerState st = players.get(player);
        if (st != null) {
            st.online = false;
        }
    }

    /** Read a variable. Returns null if absent (or PLAYER scope with no loaded player). */
    public synchronized String get(Scope scope, String key, UUID player) {
        return switch (scope) {
            case GLOBAL -> global.get(key);
            case LOCAL -> {
                if (local == null) {
                    LOGGER.warn("[Colophon] LOCAL get '{}' before world load; returning null", key);
                    yield null;
                }
                yield local.get(key);
            }
            case PLAYER -> {
                PlayerState st = (player == null) ? null : players.get(player);
                if (st == null) {
                    LOGGER.warn("[Colophon] PLAYER get '{}' with no loaded player; returning null", key);
                    yield null;
                }
                yield st.vars.get(key);
            }
        };
    }

    /** Write a variable into cache and mark it dirty for the next flush. */
    public synchronized void set(Scope scope, String key, String value, UUID player) {
        switch (scope) {
            case GLOBAL -> {
                global.put(key, value);
                globalDirty.add(key);
            }
            case LOCAL -> {
                if (local == null) {
                    LOGGER.warn("[Colophon] LOCAL set '{}' before world load; ignored", key);
                    return;
                }
                local.set(key, value);
            }
            case PLAYER -> {
                PlayerState st = (player == null) ? null : players.get(player);
                if (st == null) {
                    LOGGER.warn("[Colophon] PLAYER set '{}' with no loaded player; ignored", key);
                    return;
                }
                st.vars.put(key, value);
                st.dirty.add(key);
            }
        }
    }

    /** Persist all dirty variables, then evict offline players. Call on the world save cycle. */
    public synchronized void flushDirty() {
        if (backend == null) {
            return;
        }
        List<StateBackend.Write> writes = new ArrayList<>();
        for (String k : globalDirty) {
            writes.add(StateBackend.Write.global(k, global.get(k)));
        }
        for (Map.Entry<UUID, PlayerState> e : players.entrySet()) {
            PlayerState st = e.getValue();
            for (String k : st.dirty) {
                writes.add(StateBackend.Write.player(e.getKey(), k, st.vars.get(k)));
            }
        }
        if (!writes.isEmpty()) {
            backend.flush(writes);
            LOGGER.info("[Colophon] Flushed {} dirty state write(s)", writes.size());
        }
        globalDirty.clear();
        players.values().forEach(st -> st.dirty.clear());
        players.entrySet().removeIf(e -> !e.getValue().online);
    }

    /** Flush everything and close the backend. Call on server stop. */
    public synchronized void close() {
        if (backend == null) {
            return;
        }
        flushDirty();
        backend.close();
        backend = null;
        LOGGER.info("[Colophon] Storage closed");
    }
}
