package kr.guinnessgroup.colophon.runtime;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parses the editor's published {nodes, edges} JSON into a runnable {@link Graph}. */
public final class GraphParser {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Result of parsing: the graph plus its trigger node ids grouped by node type. */
    public record Parsed(Graph graph, Map<String, List<String>> triggersByType) {}

    private GraphParser() {}

    public static Parsed parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray nodes = root.has("nodes") ? root.getAsJsonArray("nodes") : new JsonArray();
        JsonArray edges = root.has("edges") ? root.getAsJsonArray("edges") : new JsonArray();

        // Wiring: source node id -> (output port -> target node id)
        Map<String, Map<String, String>> outputs = new HashMap<>();
        for (JsonElement e : edges) {
            JsonObject edge = e.getAsJsonObject();
            String source = asString(edge, "source");
            String target = asString(edge, "target");
            if (source == null || target == null) {
                continue;
            }
            String port = asString(edge, "sourceHandle");
            if (port == null) {
                port = GraphNode.DEFAULT_PORT;
            }
            outputs.computeIfAbsent(source, k -> new HashMap<>()).put(port, target);
        }

        Map<String, GraphNode> graphNodes = new LinkedHashMap<>();
        Map<String, List<String>> triggersByType = new LinkedHashMap<>();

        for (JsonElement e : nodes) {
            JsonObject node = e.getAsJsonObject();
            String id = asString(node, "id");
            if (id == null) {
                continue;
            }
            JsonObject data = (node.has("data") && node.get("data").isJsonObject())
                    ? node.getAsJsonObject("data") : null;
            String type = data != null ? asString(data, "nodeType") : null;
            if (type == null) {
                LOGGER.warn("[Colophon] node '{}' has no nodeType; skipping", id);
                continue;
            }
            NodeType nt = NodeRegistry.get(type);
            if (nt == null) {
                LOGGER.warn("[Colophon] unknown node type '{}' (node '{}'); skipping", type, id);
                continue;
            }
            JsonObject config = (data.has("config") && data.get("config").isJsonObject())
                    ? data.getAsJsonObject("config") : new JsonObject();
            Node runtime = nt.create(config);
            Map<String, String> outs = outputs.getOrDefault(id, Map.of());
            graphNodes.put(id, new GraphNode(id, runtime, outs));
            if ("trigger".equals(nt.category())) {
                triggersByType.computeIfAbsent(type, k -> new ArrayList<>()).add(id);
            }
        }
        return new Parsed(new Graph(graphNodes), triggersByType);
    }

    private static String asString(JsonObject o, String key) {
        return (o.has(key) && o.get(key).isJsonPrimitive()) ? o.get(key).getAsString() : null;
    }
}
