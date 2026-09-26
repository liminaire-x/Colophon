/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench.record;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        public List<String> ownersWith(Owner.Kind kind, String key) {
            return data.entrySet().stream()
                    .filter(e -> e.getKey().kind() == kind && e.getValue().containsKey(key))
                    .map(e -> e.getKey().id()).toList();
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
    void findsOwnersWithAKeySavedOrNot() {
        Owner sam = Owner.player(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        Owner kim = Owner.player(UUID.fromString("00000000-0000-0000-0000-000000000003"));
        backend.data.put(alex, new HashMap<>(Map.of("quest_a", "done")));
        backend.data.put(kim, new HashMap<>(Map.of("quest_a", "active")));
        store.load(sam);
        store.set(sam, "quest_a", "active"); // not saved yet
        store.load(kim);
        store.set(kim, "quest_a", null);     // deleted, not saved yet
        assertEquals(Set.of(alex, sam), Set.copyOf(store.ownersWith(Owner.Kind.PLAYER, "quest_a")));
        assertEquals(List.of(), store.ownersWith(Owner.Kind.SERVER, "quest_a"));
    }

    @Test
    void anyOwnerCanBeReadAndChangedEvenWhenAway() {
        backend.data.put(alex, new HashMap<>(Map.of("quest_a", "done", "flag_greeted", "true")));
        assertEquals("done", store.peek(alex, "quest_a"));
        store.setAny(alex, "quest_a", null);
        store.flush();
        assertEquals(Map.of("flag_greeted", "true"), backend.data.get(alex)); // only that key changed
        assertNull(store.get(alex, "flag_greeted"));                        // and they are not kept loaded
    }

    @Test
    void unloadedOwnerReadsNullAndIgnoresWrites() {
        store.set(alex, "flag_greeted", "true");
        assertNull(store.get(alex, "flag_greeted"));
        store.flush();
        assertTrue(backend.batches.isEmpty());
    }
}
