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
import kr.guinnessgroup.colophon.runtime.type.TypeId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses and validates the editor's published {nodes, edges} JSON into a runnable
 * {@link Graph}. Validation is port-aware and category-aware: a flow edge is valid
 * only if its source flow port exists and the target accepts a flow input; a data
 * edge is valid only if it connects two data ports of the same type. Flow and data
 * ports may not be cross-connected. Data edges are validated but not wired — values
 * flow in contract c. Invalid graphs throw {@link GraphValidationException} with all
 * errors.
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

        // Pass 2: validate edges and build wiring. Flow edges build the flow wiring;
        // data edges are validated by nominal type match and wired as data sources
        // (target input port -> producer output). Flow and data ports are separate
        // categories and may not be connected to each other. (Contracts b, e.)
        Map<String, Map<String, String>> outputs = new HashMap<>();       // flow wiring: source -> port -> target
        Map<String, Map<String, PortRef>> dataSources = new HashMap<>();   // target -> input port -> producer PortRef
        Map<String, Set<String>> dataInputsUsed = new HashMap<>();         // target id -> its connected data-in port ids
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
            String srcHandle = asString(edge, "sourceHandle");
            String tgtHandle = asString(edge, "targetHandle");

            // Classify the source handle: a flow output (default "out") or a data output.
            // Only exec node types have flow outputs; pure nodes never do.
            String flowPort = srcHandle == null ? GraphNode.DEFAULT_PORT : srcHandle;
            boolean srcIsFlow = (sType instanceof ExecNodeType se) && se.flowOutPorts().contains(flowPort);
            DataPort srcData = srcIsFlow ? null : findPort(sType.dataOutPorts(), srcHandle);
            if (!srcIsFlow && srcData == null) {
                errors.add("node '" + source + "' has no output port '"
                        + (srcHandle == null ? flowPort : srcHandle) + "'");
                continue;
            }

            if (srcIsFlow) {
                // Flow edge: target must accept a flow input and must not be a data input.
                if (tgtHandle != null && !GraphNode.FLOW_IN_PORT.equals(tgtHandle)
                        && findDataInput(tType, configById.get(target), tgtHandle) != null) {
                    errors.add("cannot connect flow output '" + flowPort + "' of '" + source
                            + "' to data input '" + tgtHandle + "' of '" + target + "'");
                    continue;
                }
                if (!(tType instanceof ExecNodeType te) || !te.hasFlowIn()) {
                    errors.add("node '" + target + "' cannot receive a connection (it is an entry point)");
                    continue;
                }
                Map<String, String> m = outputs.computeIfAbsent(source, k -> new HashMap<>());
                if (m.containsKey(flowPort)) {
                    errors.add("node '" + source + "' output '" + flowPort + "' is connected more than once");
                    continue;
                }
                m.put(flowPort, target);
            } else {
                // Data edge: target must be a data input of the SAME type (nominal match).
                if (tgtHandle == null || GraphNode.FLOW_IN_PORT.equals(tgtHandle)) {
                    errors.add("cannot connect data output '" + srcData.id() + "' of '" + source
                            + "' to a flow input of '" + target + "'");
                    continue;
                }
                InputSpec tgtData = findDataInput(tType, configById.get(target), tgtHandle);
                if (tgtData == null) {
                    errors.add("node '" + target + "' has no data input port '" + tgtHandle + "'");
                    continue;
                }
                // Nominal match through the canonical TypeId (exact-match only for now;
                // subtyping/conversion attach here later — type-system handoff §7).
                if (!TypeId.parse(srcData.typeId()).equals(TypeId.parse(tgtData.typeId()))) {
                    errors.add("type mismatch: '" + source + "." + srcData.id() + "' (" + srcData.typeId()
                            + ") cannot connect to '" + target + "." + tgtData.id() + "' (" + tgtData.typeId() + ")");
                    continue;
                }
                if (!dataInputsUsed.computeIfAbsent(target, k -> new HashSet<>()).add(tgtData.id())) {
                    errors.add("data input '" + tgtData.id() + "' of '" + target + "' is connected more than once");
                    continue;
                }
                // Wire the data source: the target's input port is fed by the producer's output.
                dataSources.computeIfAbsent(target, k -> new HashMap<>())
                        .put(tgtData.id(), new PortRef(source, srcData.id()));
            }
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
            JsonObject config = configById.get(id);
            ExecNode exec = null;
            PureNode pure = null;
            if (nt instanceof PureNodeType p) {
                pure = p.createPure(config);
            } else if (nt instanceof ExecNodeType e) {
                exec = e.create(config);
            }
            graphNodes.put(id, new GraphNode(id, nt, exec, pure, config,
                    outputs.getOrDefault(id, Map.of()),
                    dataSources.getOrDefault(id, Map.of())));
            if ("trigger".equals(nt.category())) {
                triggersByType.computeIfAbsent(nt.id(), k -> new ArrayList<>()).add(id);
            }
        }
        return new Parsed(new Graph(graphNodes), triggersByType);
    }

    private static String asString(JsonObject o, String key) {
        return (o.has(key) && o.get(key).isJsonPrimitive()) ? o.get(key).getAsString() : null;
    }

    /** The data port with the given id, or null. */
    private static DataPort findPort(List<DataPort> ports, String id) {
        if (id == null) {
            return null;
        }
        for (DataPort p : ports) {
            if (p.id().equals(id)) {
                return p;
            }
        }
        return null;
    }

    /**
     * The connectable (data) input of the given node instance with this id, or null.
     * Uses {@code instanceInputs} so instance-derived ports (e.g. a {@code format_text}
     * template's tokens) validate, not just the static schema.
     */
    private static InputSpec findDataInput(NodeType type, JsonObject config, String id) {
        if (id == null) {
            return null;
        }
        for (InputSpec in : type.instanceInputs(config)) {
            if (in.connectable() && in.id().equals(id)) {
                return in;
            }
        }
        return null;
    }
}
