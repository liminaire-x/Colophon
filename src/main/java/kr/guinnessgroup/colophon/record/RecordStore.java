/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.record;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Records as graphs see them. Reads and writes hit memory only; changes reach the
 * backend when {@link #flush()} runs on the world save, so records and the world
 * are saved at the same moment.
 * <p>
 * Lifecycle (called by the mod's event handlers): {@link #open} on server start
 * (loads the server's records), {@link #load} when a player joins, {@link #release}
 * when they leave (their records stay until the next flush, then are dropped from
 * memory), {@link #close} on server stop.
 */
public final class RecordStore {

    private static final Logger LOGGER = LogUtils.getLogger();

    private RecordBackend backend;
    private final Map<Owner, Entry> cache = new HashMap<>();

    private static final class Entry {
        final Map<String, String> values = new HashMap<>();
        final Set<String> dirty = new HashSet<>();
        boolean online = true;
    }

    public synchronized void open(RecordBackend backend) {
        this.backend = backend;
        cache.clear();
        load(Owner.server());
    }

    /** Bring an owner's records into memory. Call before any graph can read them. */
    public synchronized void load(Owner owner) {
        if (backend == null) {
            return;
        }
        Entry existing = cache.get(owner);
        if (existing != null) {
            // Still cached (e.g. rejoined before the next flush). It may hold unsaved
            // changes, so keep it rather than reloading from the backend.
            existing.online = true;
            return;
        }
        Entry e = new Entry();
        e.values.putAll(backend.load(owner));
        cache.put(owner, e);
    }

    /** The owner went away: keep their records until the next flush, then drop them. */
    public synchronized void release(Owner owner) {
        Entry e = cache.get(owner);
        if (e != null && owner.kind() != Owner.Kind.SERVER) {
            e.online = false;
        }
    }

    /** The value, or {@code null} if there is none (or the owner is not loaded). */
    public synchronized String get(Owner owner, String key) {
        Entry e = cache.get(owner);
        if (e == null) {
            LOGGER.warn("[Colophon] Read '{}' of {} which is not loaded", key, owner);
            return null;
        }
        return e.values.get(key);
    }

    /** Set a value; {@code null} deletes it. Saved on the next flush. */
    public synchronized void set(Owner owner, String key, String value) {
        Entry e = cache.get(owner);
        if (e == null) {
            LOGGER.warn("[Colophon] Write '{}' of {} which is not loaded; ignored", key, owner);
            return;
        }
        if (value == null) {
            e.values.remove(key);
        } else {
            e.values.put(key, value);
        }
        e.dirty.add(key);
    }

    /** Save every change, then drop owners who have left. */
    public synchronized void flush() {
        if (backend == null) {
            return;
        }
        List<RecordBackend.Write> writes = new ArrayList<>();
        for (Map.Entry<Owner, Entry> me : cache.entrySet()) {
            Entry e = me.getValue();
            for (String key : e.dirty) {
                writes.add(new RecordBackend.Write(me.getKey(), key, e.values.get(key)));
            }
        }
        if (!writes.isEmpty()) {
            backend.write(writes);
            LOGGER.debug("[Colophon] Saved {} record change(s)", writes.size());
        }
        cache.values().forEach(e -> e.dirty.clear());
        cache.values().removeIf(e -> !e.online);
    }

    public synchronized void close() {
        if (backend == null) {
            return;
        }
        flush();
        backend.close();
        backend = null;
        cache.clear();
    }
}
