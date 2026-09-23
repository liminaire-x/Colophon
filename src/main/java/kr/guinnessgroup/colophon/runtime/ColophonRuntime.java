/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.runtime;

import com.mojang.logging.LogUtils;
import kr.guinnessgroup.colophon.graph.GraphDoc;
import kr.guinnessgroup.colophon.graph.GraphException;
import kr.guinnessgroup.colophon.graph.GraphFormat;
import kr.guinnessgroup.colophon.record.RecordStore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the published graphs and starts them when events happen. Publishing
 * replaces every graph at once, with no server restart, and saves the document to
 * {@code graphs.json} so it survives a restart.
 */
public final class ColophonRuntime {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final NodeRegistry registry;
    private final RecordStore records;
    private final Path file;

    /** Everything that changes on publish, swapped in one step. */
    private record Active(GraphDoc doc, Map<String, List<Start>> startsByTrigger) {}

    private record Start(Graph graph, String nodeId) {}

    private static final Active EMPTY = new Active(new GraphDoc(List.of()), Map.of());

    private volatile Active active = EMPTY;

    public ColophonRuntime(NodeRegistry registry, RecordStore records, Path file) {
        this.registry = registry;
        this.records = records;
        this.file = file;
    }

    /** Check, swap in, and save a new document. Throws {@link GraphException} if rejected. */
    public synchronized void publish(String json) {
        GraphDoc doc = GraphFormat.read(json);
        activate(doc);
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, GraphFormat.write(doc), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("[Colophon] Published, but saving {} failed", file, e);
        }
    }

    /** Load the saved document. A broken file is left untouched and nothing runs. */
    public synchronized void load() {
        if (!Files.exists(file)) {
            return;
        }
        try {
            activate(GraphFormat.read(Files.readString(file, StandardCharsets.UTF_8)));
        } catch (GraphException e) {
            LOGGER.error("[Colophon] {} was not loaded; no graph will run until it is fixed or republished: {}",
                    file, e.errors());
        } catch (IOException e) {
            LOGGER.error("[Colophon] Reading {} failed; no graph will run", file, e);
        }
    }

    private void activate(GraphDoc doc) {
        List<Graph> graphs = GraphBuilder.build(doc, registry);
        Map<String, List<Start>> starts = new HashMap<>();
        for (Graph g : graphs) {
            for (Graph.Placed n : g.nodes().values()) {
                if (n.type().trigger()) {
                    starts.computeIfAbsent(n.type().id(), k -> new ArrayList<>()).add(new Start(g, n.id()));
                }
            }
        }
        starts.replaceAll((k, v) -> List.copyOf(v));
        this.active = new Active(doc, Map.copyOf(starts));
        LOGGER.info("[Colophon] {} graph(s) active", graphs.size());
    }

    /** The current document, for the editor. */
    public String documentJson() {
        return GraphFormat.write(active.doc());
    }

    /** Run every graph that starts with the given trigger type. Call on the server thread. */
    public void fire(String triggerType, MinecraftServer server, ServerPlayer player) {
        List<Start> starts = active.startsByTrigger().getOrDefault(triggerType, List.of());
        for (Start s : starts) {
            Runner.run(s.graph(), s.nodeId(), new Context(server, player, records));
        }
    }

    public synchronized void clear() {
        this.active = EMPTY;
    }
}
