/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench.runtime;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import kr.guinnessgroup.lorebench.graph.GraphFormat;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** The node types the editor can place, and their description for the editor. */
public final class NodeRegistry {

    private final Map<String, NodeType> types = new LinkedHashMap<>();

    public void register(NodeType type) {
        if (types.putIfAbsent(type.id(), type) != null) {
            throw new IllegalStateException("Node type '" + type.id() + "' is registered twice");
        }
    }

    public NodeType get(String id) {
        return types.get(id);
    }

    public Collection<NodeType> all() {
        return Collections.unmodifiableCollection(types.values());
    }

    /** Served at /api/schema: the editor builds its palette and node shapes from this. */
    public String schemaJson() {
        JsonArray nodes = new JsonArray();
        for (NodeType t : types.values()) {
            JsonObject o = new JsonObject();
            o.addProperty("type", t.id());
            o.addProperty("label", t.label());
            o.addProperty("category", t.category());
            o.addProperty("trigger", t.trigger());
            JsonArray outs = new JsonArray();
            t.outs().forEach(outs::add);
            o.add("outs", outs);
            JsonArray fields = new JsonArray();
            for (Field f : t.fields()) {
                JsonObject fo = new JsonObject();
                fo.addProperty("id", f.id());
                fo.addProperty("label", f.label());
                fo.addProperty("default", f.defaultValue());
                fo.addProperty("kind", f.kind());
                fields.add(fo);
            }
            o.add("fields", fields);
            nodes.add(o);
        }
        JsonObject root = new JsonObject();
        root.addProperty("format", GraphFormat.VERSION);
        root.add("nodes", nodes);
        return root.toString();
    }
}
