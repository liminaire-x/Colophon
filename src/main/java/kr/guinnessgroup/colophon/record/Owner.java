/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.record;

import java.util.UUID;

/**
 * Whose record it is. A new kind (e.g. party) is added here; the stored table does
 * not change. {@code id} is empty for the server.
 */
public record Owner(Kind kind, String id) {

    public enum Kind {
        PLAYER("player"),
        SERVER("server");

        /** The stored name. Never rename an existing one. */
        public final String key;

        Kind(String key) {
            this.key = key;
        }

        public static Kind fromKey(String key) {
            for (Kind k : values()) {
                if (k.key.equals(key)) {
                    return k;
                }
            }
            throw new IllegalArgumentException("unknown owner kind '" + key + "'");
        }
    }

    private static final Owner SERVER = new Owner(Kind.SERVER, "");

    public static Owner player(UUID uuid) {
        return new Owner(Kind.PLAYER, uuid.toString());
    }

    public static Owner server() {
        return SERVER;
    }
}
