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
import java.util.List;
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

            // Typed data outputs (contract b). Data inputs live in `inputs` below
            // (connectable ones); the editor draws both as typed handles.
            o.add("dataOut", dataPortsJson(t.dataOutPorts()));

            // Unified inputs (contract d): config knobs + data inputs as one list.
            JsonArray inputs = new JsonArray();
            for (InputSpec in : t.inputs()) {
                JsonObject io = new JsonObject();
                io.addProperty("id", in.id());
                io.addProperty("type", in.typeId());
                io.addProperty("label", in.label());
                io.addProperty("default", in.defaultValue());
                io.addProperty("connectable", in.connectable());
                JsonArray opts = new JsonArray();
                for (String opt : in.options()) {
                    opts.add(opt);
                }
                io.add("options", opts);
                inputs.add(io);
            }
            o.add("inputs", inputs);

            arr.add(o);
        }
        JsonObject root = new JsonObject();
        root.add("nodes", arr);
        root.add("types", TypeRegistry.typesJson());
        return root.toString();
    }

    /** Serializes a node's data ports (id, type id, label) for the editor schema. */
    private static JsonArray dataPortsJson(List<DataPort> ports) {
        JsonArray arr = new JsonArray();
        for (DataPort p : ports) {
            JsonObject o = new JsonObject();
            o.addProperty("id", p.id());
            o.addProperty("type", p.typeId());
            o.addProperty("label", p.label());
            arr.add(o);
        }
        return arr;
    }
}
