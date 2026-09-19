/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import kr.guinnessgroup.colophon.runtime.type.TypeRegistry;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds the known node types and exposes their schema to the editor. The engine
 * knows nothing about concrete node types; they are contributed via
 * {@link #register(NodeType)} (built-ins and, later, add-ons).
 */
public final class NodeRegistry {

    private static final Map<String, NodeType> TYPES = new LinkedHashMap<>();

    private NodeRegistry() {}

    public static void register(NodeType type) {
        TYPES.put(type.id(), type);
    }

    public static NodeType get(String id) {
        return TYPES.get(id);
    }

    public static Collection<NodeType> all() {
        return TYPES.values();
    }

    /** JSON schema of all node types, served to the editor as its node palette. */
    public static String schemaJson() {
        JsonArray arr = new JsonArray();
        for (NodeType t : TYPES.values()) {
            JsonObject o = new JsonObject();
            o.addProperty("type", t.id());
            o.addProperty("label", t.label());
            o.addProperty("category", t.category());
            o.addProperty("hasFlowIn", t.hasFlowIn());

            JsonArray flowOut = new JsonArray();
            for (String port : t.flowOutPorts()) {
                flowOut.add(port);
            }
            o.add("flowOut", flowOut);

            JsonArray fields = new JsonArray();
            for (FieldSpec f : t.fields()) {
                JsonObject fo = new JsonObject();
                fo.addProperty("name", f.name());
                fo.addProperty("type", f.type());
                fo.addProperty("default", f.defaultValue());
                JsonArray opts = new JsonArray();
                for (String opt : f.options()) {
                    opts.add(opt);
                }
                fo.add("options", opts);
                fields.add(fo);
            }
            o.add("fields", fields);

            arr.add(o);
        }
        JsonObject root = new JsonObject();
        root.add("nodes", arr);
        root.add("types", TypeRegistry.typesJson());
        return root.toString();
    }
}
