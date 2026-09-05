/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses and validates the editor's published {nodes, edges} JSON into a runnable
 * {@link Graph}. Validation is port-aware: an edge is valid only if its source
 * port exists on the source node and the target node accepts a flow input.
 * Invalid graphs throw {@link GraphValidationException} with all errors.
 */
public final class GraphParser {

    /** Result of parsing: the graph plus its trigger node ids grouped by node type. */
    public record Parsed(Graph graph, Map<String, List<String>> triggersByType) {}

    private GraphParser() {}

    public static Parsed parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray nodesArr = root.has("nodes") ? root.getAsJsonArray("nodes") : new JsonArray();
        JsonArray edgesArr = root.has("edges") ? root.getAsJsonArray("edges") : new JsonArray();

        List<String> errors = new ArrayList<>();

        // Pass 1: resolve node types.
        Map<String, NodeType> typeById = new LinkedHashMap<>();
        Map<String, JsonObject> configById = new HashMap<>();
        for (JsonElement e : nodesArr) {
            JsonObject node = e.getAsJsonObject();
            String id = asString(node, "id");
            if (id == null) {
                errors.add("a node is missing 'id'");
                continue;
            }
            JsonObject data = (node.has("data") && node.get("data").isJsonObject())
                    ? node.getAsJsonObject("data") : null;
            String type = data != null ? asString(data, "nodeType") : null;
            if (type == null) {
                errors.add("node '" + id + "': missing nodeType");
                continue;
            }
            NodeType nt = NodeRegistry.get(type);
            if (nt == null) {
                errors.add("node '" + id + "': unknown type '" + type + "'");
                continue;
            }
            typeById.put(id, nt);
            configById.put(id, (data.has("config") && data.get("config").isJsonObject())
                    ? data.getAsJsonObject("config") : new JsonObject());
        }

        // Pass 2: validate edges and build wiring (source id -> port -> target id).
        Map<String, Map<String, String>> outputs = new HashMap<>();
        for (JsonElement e : edgesArr) {
            JsonObject edge = e.getAsJsonObject();
            String source = asString(edge, "source");
            String target = asString(edge, "target");
            if (source == null || target == null) {
                errors.add("an edge is missing source/target");
                continue;
            }
            NodeType sType = typeById.get(source);
            NodeType tType = typeById.get(target);
            if (sType == null) {
                errors.add("edge references missing node '" + source + "'");
                continue;
            }
            if (tType == null) {
                errors.add("edge references missing node '" + target + "'");
                continue;
            }
            String port = asString(edge, "sourceHandle");
            if (port == null) {
                port = GraphNode.DEFAULT_PORT;
            }
            if (!sType.flowOutPorts().contains(port)) {
                errors.add("node '" + source + "' has no output port '" + port + "'");
                continue;
            }
            if (!tType.hasFlowIn()) {
                errors.add("node '" + target + "' cannot receive a connection (it is an entry point)");
                continue;
            }
            Map<String, String> m = outputs.computeIfAbsent(source, k -> new HashMap<>());
            if (m.containsKey(port)) {
                errors.add("node '" + source + "' output '" + port + "' is connected more than once");
                continue;
            }
            m.put(port, target);
        }

        if (!errors.isEmpty()) {
            throw new GraphValidationException(errors);
        }

        // Build runnable graph.
        Map<String, GraphNode> graphNodes = new LinkedHashMap<>();
        Map<String, List<String>> triggersByType = new LinkedHashMap<>();
        for (Map.Entry<String, NodeType> entry : typeById.entrySet()) {
            String id = entry.getKey();
            NodeType nt = entry.getValue();
            Node runtime = nt.create(configById.get(id));
            graphNodes.put(id, new GraphNode(id, runtime, outputs.getOrDefault(id, Map.of())));
            if ("trigger".equals(nt.category())) {
                triggersByType.computeIfAbsent(nt.id(), k -> new ArrayList<>()).add(id);
            }
        }
        return new Parsed(new Graph(graphNodes), triggersByType);
    }

    private static String asString(JsonObject o, String key) {
        return (o.has(key) && o.get(key).isJsonPrimitive()) ? o.get(key).getAsString() : null;
    }
}
