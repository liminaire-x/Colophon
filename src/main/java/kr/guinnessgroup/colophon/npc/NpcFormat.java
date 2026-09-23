/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.npc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kr.guinnessgroup.colophon.DocumentException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Reads and writes the NPC document (format 1):
 * <pre>{ "format": 1, "npcs": [ { "id": "chief", "name": "촌장", "model": "chief", "idle": "wave" } ] }</pre>
 * {@code model} and {@code idle} are optional (a plain NPC has neither).
 * This file has its own format number so NPCs can grow (looks, animations,
 * cinematics) without touching the graph document.
 */
public final class NpcFormat {

    public static final int VERSION = 1;

    /** NPC ids are stable references: lowercase letters, digits, underscore. */
    public static final Pattern ID = Pattern.compile("[a-z0-9_]+");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private NpcFormat() {}

    public static NpcDoc read(String json) {
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new DocumentException(List.of("NPC document is not a JSON object"));
        }
        JsonElement format = root.get("format");
        if (format == null || !format.isJsonPrimitive() || !format.getAsJsonPrimitive().isNumber()) {
            throw new DocumentException(List.of("NPC document: missing 'format' number"));
        }
        int version = format.getAsInt();
        if (version > VERSION) {
            throw new DocumentException(List.of("NPC document format " + version
                    + " is newer than this Colophon supports (" + VERSION + ")"));
        }
        if (version != VERSION) {
            throw new DocumentException(List.of("NPC document: unknown format " + version));
        }
        JsonElement arr = root.get("npcs");
        if (arr == null || !arr.isJsonArray()) {
            throw new DocumentException(List.of("NPC document: missing 'npcs' list"));
        }

        List<String> errors = new ArrayList<>();
        List<NpcDoc.NpcDef> npcs = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (JsonElement el : (JsonArray) arr) {
            if (!el.isJsonObject()) {
                errors.add("an NPC is not an object");
                continue;
            }
            JsonObject o = el.getAsJsonObject();
            String id = string(o, "id");
            String name = string(o, "name");
            if (id == null || !ID.matcher(id).matches()) {
                errors.add("NPC id " + (id == null ? "is missing" : "'" + id + "' must use a-z, 0-9, _"));
                continue;
            }
            if (name == null || name.isBlank()) {
                errors.add("NPC '" + id + "': missing 'name'");
                continue;
            }
            if (!ids.add(id)) {
                errors.add("duplicate NPC id '" + id + "'");
                continue;
            }
            String model = optional(o, "model");
            String idle = optional(o, "idle");
            if (!model.isEmpty() && !ID.matcher(model).matches()) {
                errors.add("NPC '" + id + "': model '" + model + "' must use a-z, 0-9, _");
                continue;
            }
            npcs.add(new NpcDoc.NpcDef(id, name, model, idle));
        }
        if (!errors.isEmpty()) {
            throw new DocumentException(errors);
        }
        return new NpcDoc(List.copyOf(npcs));
    }

    public static String write(NpcDoc doc) {
        JsonArray arr = new JsonArray();
        for (NpcDoc.NpcDef n : doc.npcs()) {
            JsonObject o = new JsonObject();
            o.addProperty("id", n.id());
            o.addProperty("name", n.name());
            if (!n.model().isEmpty()) {
                o.addProperty("model", n.model());
            }
            if (!n.idle().isEmpty()) {
                o.addProperty("idle", n.idle());
            }
            arr.add(o);
        }
        JsonObject root = new JsonObject();
        root.addProperty("format", VERSION);
        root.add("npcs", arr);
        return GSON.toJson(root);
    }

    /** An optional text field: trimmed, "" when absent. */
    private static String optional(JsonObject o, String key) {
        String s = string(o, key);
        return s == null ? "" : s.trim();
    }

    private static String string(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) ? e.getAsString() : null;
    }
}
