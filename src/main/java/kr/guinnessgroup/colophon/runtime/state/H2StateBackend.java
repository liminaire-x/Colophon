/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime.state;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Embedded H2 implementation of {@link StateBackend} for single-server operation.
 * Two tables: {@code player_var(uuid, k, v)} and {@code global_var(k, v)}.
 * <p>
 * Moving to multi-server later means swapping this for a shared-DB backend; the
 * {@link StorageService} above it does not change.
 */
public final class H2StateBackend implements StateBackend {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final Connection conn;

    /**
     * @param dbFileBase path with no extension; H2 appends {@code .mv.db}
     *                   (e.g. {@code config/colophon/state}).
     */
    public H2StateBackend(Path dbFileBase) {
        try {
            Files.createDirectories(dbFileBase.getParent());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create state DB directory", e);
        }
        try {
            // Register the driver explicitly so it is found under the module system.
            org.h2.Driver.load();
            String url = "jdbc:h2:file:" + dbFileBase.toAbsolutePath() + ";DB_CLOSE_ON_EXIT=FALSE";
            this.conn = DriverManager.getConnection(url, "sa", "");
            bootstrap();
            LOGGER.info("[Colophon] State DB ready at {}.mv.db", dbFileBase.toAbsolutePath());
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to open Colophon state DB", e);
        }
    }

    private void bootstrap() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS player_var ("
                    + "uuid VARCHAR(36) NOT NULL, k VARCHAR(255) NOT NULL, v VARCHAR, "
                    + "PRIMARY KEY (uuid, k))");
            st.execute("CREATE TABLE IF NOT EXISTS global_var ("
                    + "k VARCHAR(255) NOT NULL PRIMARY KEY, v VARCHAR)");
        }
    }

    @Override
    public synchronized Map<String, String> loadPlayer(UUID player) {
        Map<String, String> out = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT k, v FROM player_var WHERE uuid = ?")) {
            ps.setString(1, player.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.put(rs.getString(1), rs.getString(2));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("[Colophon] loadPlayer failed for {}", player, e);
        }
        return out;
    }

    @Override
    public synchronized Map<String, String> loadGlobal() {
        Map<String, String> out = new HashMap<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT k, v FROM global_var")) {
            while (rs.next()) {
                out.put(rs.getString(1), rs.getString(2));
            }
        } catch (SQLException e) {
            LOGGER.error("[Colophon] loadGlobal failed", e);
        }
        return out;
    }

    @Override
    public synchronized void flush(List<Write> writes) {
        if (writes.isEmpty()) {
            return;
        }
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement upPlayer = conn.prepareStatement(
                        "MERGE INTO player_var (uuid, k, v) KEY (uuid, k) VALUES (?, ?, ?)");
                 PreparedStatement delPlayer = conn.prepareStatement(
                        "DELETE FROM player_var WHERE uuid = ? AND k = ?");
                 PreparedStatement upGlobal = conn.prepareStatement(
                        "MERGE INTO global_var (k, v) KEY (k) VALUES (?, ?)");
                 PreparedStatement delGlobal = conn.prepareStatement(
                        "DELETE FROM global_var WHERE k = ?")) {
                for (Write w : writes) {
                    switch (w.scope()) {
                        case PLAYER -> {
                            if (w.deleted()) {
                                delPlayer.setString(1, w.player().toString());
                                delPlayer.setString(2, w.key());
                                delPlayer.addBatch();
                            } else {
                                upPlayer.setString(1, w.player().toString());
                                upPlayer.setString(2, w.key());
                                upPlayer.setString(3, w.value());
                                upPlayer.addBatch();
                            }
                        }
                        case GLOBAL -> {
                            if (w.deleted()) {
                                delGlobal.setString(1, w.key());
                                delGlobal.addBatch();
                            } else {
                                upGlobal.setString(1, w.key());
                                upGlobal.setString(2, w.value());
                                upGlobal.addBatch();
                            }
                        }
                        case LOCAL -> {
                            // LOCAL is SavedData, not persisted here.
                        }
                    }
                }
                upPlayer.executeBatch();
                delPlayer.executeBatch();
                upGlobal.executeBatch();
                delGlobal.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.error("[Colophon] state flush failed; rolled back", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.error("[Colophon] state flush transaction error", e);
        }
    }

    @Override
    public synchronized void close() {
        try {
            conn.close();
        } catch (SQLException e) {
            LOGGER.error("[Colophon] closing state DB failed", e);
        }
    }
}
