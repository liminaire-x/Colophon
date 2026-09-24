/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench.npc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.UUID;

/**
 * One NPC standing in a world. Stored as a server record:
 * key {@code placement_<entity uuid>}, value
 * {@code {"npc":"npc_7ha2m0qe","dim":"minecraft:overworld","x":1.5,"y":64.0,"z":-3.5}}.
 * The record is the source of truth; the entity follows it.
 */
public record Placement(UUID entity, String npc, String dimension, double x, double y, double z) {

    public static final String KEY_PREFIX = "placement_";

    public String key() {
        return KEY_PREFIX + entity;
    }

    public String toValue() {
        JsonObject o = new JsonObject();
        o.addProperty("npc", npc);
        o.addProperty("dim", dimension);
        o.addProperty("x", x);
        o.addProperty("y", y);
        o.addProperty("z", z);
        return o.toString();
    }

    /** Parse a stored record; {@code null} if the key is not a placement or the value is broken. */
    public static Placement fromRecord(String key, String value) {
        if (key == null || !key.startsWith(KEY_PREFIX) || value == null) {
            return null;
        }
        try {
            UUID entity = UUID.fromString(key.substring(KEY_PREFIX.length()));
            JsonObject o = JsonParser.parseString(value).getAsJsonObject();
            return new Placement(entity, o.get("npc").getAsString(), o.get("dim").getAsString(),
                    o.get("x").getAsDouble(), o.get("y").getAsDouble(), o.get("z").getAsDouble());
        } catch (RuntimeException e) {
            return null;
        }
    }
}
