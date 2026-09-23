/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.record;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The records DB file (schema 1). Breaking these loses players' saved records. */
class H2RecordBackendTest {

    private static final Owner SERVER = Owner.server("main");
    private final Owner alex = Owner.player(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private final Owner sam = Owner.player(UUID.fromString("00000000-0000-0000-0000-000000000002"));

    @Test
    void recordsSurviveReopen(@TempDir Path dir) {
        Path base = dir.resolve("records");
        H2RecordBackend db = new H2RecordBackend(base);
        db.write(List.of(
                new RecordBackend.Write(alex, "flag_greeted", "true"),
                new RecordBackend.Write(sam, "flag_greeted", "true"),
                new RecordBackend.Write(SERVER, "k", "v")));
        db.close();

        H2RecordBackend reopened = new H2RecordBackend(base);
        assertEquals(Map.of("flag_greeted", "true"), reopened.load(alex));
        assertEquals(Map.of("k", "v"), reopened.load(SERVER));
        reopened.close();
    }

    @Test
    void ownersDoNotSeeEachOthersRecords(@TempDir Path dir) {
        H2RecordBackend db = new H2RecordBackend(dir.resolve("records"));
        db.write(List.of(new RecordBackend.Write(alex, "flag_greeted", "true")));
        assertTrue(db.load(sam).isEmpty());
        assertTrue(db.load(SERVER).isEmpty());
        db.close();
    }

    @Test
    void serversSharingOneDbKeepTheirOwnRecords(@TempDir Path dir) {
        H2RecordBackend db = new H2RecordBackend(dir.resolve("records"));
        db.write(List.of(new RecordBackend.Write(SERVER, "placement_x", "main's")));
        assertTrue(db.load(Owner.server("lobby")).isEmpty());
        db.close();
    }

    @Test
    void overwriteAndDelete(@TempDir Path dir) {
        H2RecordBackend db = new H2RecordBackend(dir.resolve("records"));
        db.write(List.of(new RecordBackend.Write(alex, "a", "1"), new RecordBackend.Write(alex, "b", "1")));
        db.write(List.of(new RecordBackend.Write(alex, "a", "2"), new RecordBackend.Write(alex, "b", null)));
        assertEquals(Map.of("a", "2"), db.load(alex));
        db.close();
    }

    @Test
    void refusesNewerSchema(@TempDir Path dir) throws Exception {
        Path base = dir.resolve("records");
        new H2RecordBackend(base).close();
        try (Connection c = DriverManager.getConnection(H2RecordBackend.url(base), "sa", "");
             Statement st = c.createStatement()) {
            st.execute("UPDATE meta SET v = '2' WHERE k = 'schema_version'");
        }
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> new H2RecordBackend(base));
        assertTrue(e.getMessage().contains("newer"));
    }
}
