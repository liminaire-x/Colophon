/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench.record;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Records in an embedded H2 database. One table holds every owner's records:
 * {@code records(owner_kind, owner_id, k, v)}. A {@code meta} table stores the
 * schema version so a future change can recognize and upgrade old files.
 */
public final class H2RecordBackend implements RecordBackend {

    /** The schema this code reads and writes. */
    public static final int SCHEMA_VERSION = 1;

    private static final Logger LOGGER = LogUtils.getLogger();

    private final Connection conn;

    /** @param fileBase path without extension; H2 adds {@code .mv.db}. */
    public H2RecordBackend(Path fileBase) {
        try {
            Files.createDirectories(fileBase.toAbsolutePath().getParent());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create records directory", e);
        }
        try {
            // Load the driver explicitly so it is found under the module system.
            org.h2.Driver.load();
            this.conn = DriverManager.getConnection(url(fileBase), "sa", "");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to open Lorebench records DB", e);
        }
        try {
            bootstrap();
        } catch (SQLException e) {
            closeQuietly();
            throw new IllegalStateException("Failed to prepare records DB", e);
        } catch (RuntimeException e) {
            closeQuietly();
            throw e;
        }
        LOGGER.info("[Lorebench] Records DB ready at {}.mv.db", fileBase.toAbsolutePath());
    }

    static String url(Path fileBase) {
        return "jdbc:h2:file:" + fileBase.toAbsolutePath() + ";DB_CLOSE_ON_EXIT=FALSE";
    }

    private void bootstrap() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS meta (k VARCHAR(64) PRIMARY KEY, v VARCHAR(255) NOT NULL)");
            st.execute("CREATE TABLE IF NOT EXISTS records ("
                    + "owner_kind VARCHAR(16) NOT NULL, owner_id VARCHAR(64) NOT NULL, "
                    + "k VARCHAR(255) NOT NULL, v VARCHAR NOT NULL, "
                    + "PRIMARY KEY (owner_kind, owner_id, k))");
        }
        String stored = null;
        try (PreparedStatement ps = conn.prepareStatement("SELECT v FROM meta WHERE k = 'schema_version'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                stored = rs.getString(1);
            }
        }
        if (stored == null) {
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO meta (k, v) VALUES ('schema_version', ?)")) {
                ps.setString(1, Integer.toString(SCHEMA_VERSION));
                ps.executeUpdate();
            }
            return;
        }
        int version = Integer.parseInt(stored);
        if (version > SCHEMA_VERSION) {
            throw new IllegalStateException("Records DB schema " + version
                    + " is newer than this Lorebench supports (" + SCHEMA_VERSION + ")");
        }
        if (version != SCHEMA_VERSION) {
            throw new IllegalStateException("Unknown records DB schema " + version);
        }
    }

    @Override
    public synchronized Map<String, String> load(Owner owner) {
        Map<String, String> out = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT k, v FROM records WHERE owner_kind = ? AND owner_id = ?")) {
            ps.setString(1, owner.kind().key);
            ps.setString(2, owner.id());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.put(rs.getString(1), rs.getString(2));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("[Lorebench] Loading records of {} failed", owner, e);
        }
        return out;
    }

    @Override
    public synchronized List<String> ownersWith(Owner.Kind kind, String key) {
        List<String> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT owner_id FROM records WHERE owner_kind = ? AND k = ?")) {
            ps.setString(1, kind.key);
            ps.setString(2, key);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("[Lorebench] Finding owners of '{}' failed", key, e);
        }
        return out;
    }

    @Override
    public synchronized void write(List<Write> writes) {
        if (writes.isEmpty()) {
            return;
        }
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement upsert = conn.prepareStatement(
                         "MERGE INTO records (owner_kind, owner_id, k, v) KEY (owner_kind, owner_id, k) VALUES (?, ?, ?, ?)");
                 PreparedStatement delete = conn.prepareStatement(
                         "DELETE FROM records WHERE owner_kind = ? AND owner_id = ? AND k = ?")) {
                for (Write w : writes) {
                    PreparedStatement ps = (w.value() == null) ? delete : upsert;
                    ps.setString(1, w.owner().kind().key);
                    ps.setString(2, w.owner().id());
                    ps.setString(3, w.key());
                    if (w.value() != null) {
                        ps.setString(4, w.value());
                    }
                    ps.addBatch();
                }
                upsert.executeBatch();
                delete.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.error("[Lorebench] Writing records failed; rolled back", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.error("[Lorebench] Records transaction error", e);
        }
    }

    @Override
    public synchronized void close() {
        closeQuietly();
    }

    private void closeQuietly() {
        try {
            conn.close();
        } catch (SQLException e) {
            LOGGER.error("[Lorebench] Closing records DB failed", e);
        }
    }
}
