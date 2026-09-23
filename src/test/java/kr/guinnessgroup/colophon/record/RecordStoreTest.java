/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.record;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** When records are loaded, kept, saved and dropped. */
class RecordStoreTest {

    /** An in-memory backend that remembers every write batch. */
    static final class MemoryBackend implements RecordBackend {
        final Map<Owner, Map<String, String>> data = new HashMap<>();
        final List<List<Write>> batches = new ArrayList<>();
        int loads;

        @Override
        public Map<String, String> load(Owner owner) {
            loads++;
            return new HashMap<>(data.getOrDefault(owner, Map.of()));
        }

        @Override
        public void write(List<Write> writes) {
            batches.add(List.copyOf(writes));
            for (Write w : writes) {
                Map<String, String> m = data.computeIfAbsent(w.owner(), k -> new HashMap<>());
                if (w.value() == null) {
                    m.remove(w.key());
                } else {
                    m.put(w.key(), w.value());
                }
            }
        }

        @Override
        public void close() {}
    }

    private static final Owner SERVER = Owner.server("main");
    private final Owner alex = Owner.player(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private MemoryBackend backend;
    private RecordStore store;

    @BeforeEach
    void setUp() {
        backend = new MemoryBackend();
        store = new RecordStore();
        store.open(backend, SERVER);
    }

    @Test
    void writesStayInMemoryUntilFlush() {
        store.load(alex);
        store.set(alex, "flag_greeted", "true");
        assertEquals("true", store.get(alex, "flag_greeted"));
        assertTrue(backend.batches.isEmpty());

        store.flush();
        assertEquals("true", backend.data.get(alex).get("flag_greeted"));
    }

    @Test
    void loadsSavedRecordsOnJoin() {
        backend.data.put(alex, new HashMap<>(Map.of("flag_greeted", "true")));
        store.load(alex);
        assertEquals("true", store.get(alex, "flag_greeted"));
    }

    @Test
    void nullDeletes() {
        backend.data.put(alex, new HashMap<>(Map.of("flag_greeted", "true")));
        store.load(alex);
        store.set(alex, "flag_greeted", null);
        store.flush();
        assertNull(backend.data.get(alex).get("flag_greeted"));
    }

    @Test
    void leftPlayerIsSavedThenDropped() {
        store.load(alex);
        store.set(alex, "flag_greeted", "true");
        store.release(alex);
        store.flush();
        assertEquals("true", backend.data.get(alex).get("flag_greeted"));
        assertNull(store.get(alex, "flag_greeted")); // no longer in memory
    }

    @Test
    void quickRejoinKeepsUnsavedChanges() {
        store.load(alex);
        store.set(alex, "flag_greeted", "true");
        store.release(alex);
        int loadsBefore = backend.loads;
        store.load(alex); // rejoin before any flush
        assertEquals(loadsBefore, backend.loads);
        assertEquals("true", store.get(alex, "flag_greeted"));
    }

    @Test
    void serverRecordsAreAlwaysLoaded() {
        store.set(SERVER, "k", "v");
        store.release(SERVER);
        store.flush();
        assertEquals("v", store.get(SERVER, "k"));
    }

    @Test
    void unloadedOwnerReadsNullAndIgnoresWrites() {
        store.set(alex, "flag_greeted", "true");
        assertNull(store.get(alex, "flag_greeted"));
        store.flush();
        assertTrue(backend.batches.isEmpty());
    }
}
