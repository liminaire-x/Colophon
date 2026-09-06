/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime.state;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The durable store behind the PLAYER and GLOBAL scopes. This is the seam that
 * gets swapped when moving from a single server (embedded H2) to a multi-server
 * cluster (shared DB): {@link StorageService} routes and caches, the backend
 * only persists.
 * <p>
 * LOCAL scope is not handled here &mdash; it is Minecraft SavedData, which rides
 * the world save cycle on its own.
 */
public interface StateBackend {

    /** All stored variables for one player, as an immutable snapshot. Empty if none. */
    Map<String, String> loadPlayer(UUID player);

    /** All stored GLOBAL variables, as an immutable snapshot. Empty if none. */
    Map<String, String> loadGlobal();

    /** Apply a batch of writes in one shot (upserts and deletes). Called at world-save flush. */
    void flush(List<Write> writes);

    /** Release resources (close the connection). */
    void close();

    /**
     * A single pending write. {@code player} is non-null only for {@link Scope#PLAYER}.
     * {@code deleted} removes the row instead of upserting it.
     */
    record Write(Scope scope, UUID player, String key, String value, boolean deleted) {
        public static Write global(String key, String value) {
            return new Write(Scope.GLOBAL, null, key, value, false);
        }

        public static Write player(UUID player, String key, String value) {
            return new Write(Scope.PLAYER, player, key, value, false);
        }
    }
}
