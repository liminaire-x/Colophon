/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.nodes;

import java.util.regex.Pattern;

/**
 * A flag is a named mark left on a player, e.g. {@code greeted}. It is stored as the
 * record {@code flag:<name>} so it never collides with other records (quest state
 * later). Flag names end up in saved records: lowercase letters, digits, underscore.
 */
final class Flags {

    static final String VALUE = "true";

    private static final Pattern NAME = Pattern.compile("[a-z0-9_]+");

    private Flags() {}

    static String key(String name) {
        return "flag:" + name;
    }

    /** Checks a flag name from a node's config, for a readable publish error. */
    static String requireName(String name) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("flag name is empty");
        }
        if (!NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("flag name '" + name + "' must use a-z, 0-9, _");
        }
        return name;
    }
}
