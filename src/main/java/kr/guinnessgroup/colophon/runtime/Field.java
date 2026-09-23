/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.runtime;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * One setting of a node, shown in the editor.
 *
 * @param id           key in the node's saved config. Never rename.
 * @param label        shown in the editor
 * @param defaultValue used when the config has no value
 * @param kind         how the editor asks for it: {@code text}, or {@code npc} (pick a defined NPC)
 */
public record Field(String id, String label, String defaultValue, String kind) {

    public static Field text(String id, String label) {
        return new Field(id, label, "", "text");
    }

    public static Field npc(String id, String label) {
        return new Field(id, label, "", "npc");
    }

    /** This field's value in {@code config}, or the default. */
    public String read(JsonObject config) {
        JsonElement e = (config == null) ? null : config.get(id);
        return (e != null && e.isJsonPrimitive()) ? e.getAsString() : defaultValue;
    }
}
