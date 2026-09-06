/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime.state;

/**
 * Where a state variable lives and who it belongs to.
 *
 * <ul>
 *   <li>{@code LOCAL}  &mdash; this server (world) only, not shared across servers. Backed by SavedData.</li>
 *   <li>{@code PLAYER} &mdash; per-player (UUID + key). Backed by the {@link StateBackend} (H2 now, shared DB later).</li>
 *   <li>{@code GLOBAL} &mdash; one value shared by every server. Backed by the {@link StateBackend}.</li>
 * </ul>
 *
 * The distinction between LOCAL and GLOBAL is cross-server sharing; both are
 * keyed by name only (no player).
 */
public enum Scope {
    LOCAL,
    PLAYER,
    GLOBAL;

    /** Parse a config string (case-insensitive) to a Scope, falling back on unknown input. */
    public static Scope parse(String raw, Scope fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return Scope.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
