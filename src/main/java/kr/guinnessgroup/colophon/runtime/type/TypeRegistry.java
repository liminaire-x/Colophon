/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime.type;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds the known data types and exposes them to the editor. Open registry: the
 * engine knows no concrete types; core primitives (via {@link BuiltinTypes}) and
 * add-ons contribute here. Matching is nominal — a type's identity is its {@code id}.
 */
public final class TypeRegistry {

    private static final Map<String, TypeDescriptor> TYPES = new LinkedHashMap<>();

    private TypeRegistry() {}

    public static void register(TypeDescriptor type) {
        TYPES.put(type.id(), type);
    }

    public static TypeDescriptor get(String id) {
        return TYPES.get(id);
    }

    public static Collection<TypeDescriptor> all() {
        return TYPES.values();
    }

    /** The type palette served to the editor (id, handle color, kind) as part of the schema. */
    public static JsonArray typesJson() {
        JsonArray arr = new JsonArray();
        for (TypeDescriptor t : TYPES.values()) {
            JsonObject o = new JsonObject();
            o.addProperty("id", t.id());
            o.addProperty("displayName", t.displayName());
            o.addProperty("color", t.color());
            o.addProperty("kind", t.kind().name());
            o.addProperty("serializable", t.serializable());
            arr.add(o);
        }
        return arr;
    }
}
