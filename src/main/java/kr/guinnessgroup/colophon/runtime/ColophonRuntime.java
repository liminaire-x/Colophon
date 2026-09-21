/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import kr.guinnessgroup.colophon.runtime.state.StorageService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Holds the currently published graph and starts executions when triggers fire.
 * <p>
 * v0/v1: a single active graph, kept in memory (plus its raw JSON for the editor
 * to reload) and persisted under the config directory so it survives a restart.
 * Publishing hot-swaps it with no server restart.
 */
public final class ColophonRuntime {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String EMPTY_GRAPH = "{\"nodes\":[],\"edges\":[]}";

    private final TickScheduler scheduler;
    private final StorageService storage;
    private volatile Graph activeGraph;
    private volatile Map<String, List<String>> triggersByType = Map.of();
    private volatile String lastPublishedJson;

    public ColophonRuntime(TickScheduler scheduler, StorageService storage) {
        this.scheduler = scheduler;
        this.storage = storage;
    }

    private Path saveFile() {
        return FMLPaths.CONFIGDIR.get().resolve("colophon").resolve("graph.json");
    }

    /** Parse+validate, hot-swap the active graph, then persist. Throws on invalid input. */
    public synchronized void publish(String json) {
        GraphParser.Parsed parsed = GraphParser.parse(json);
        this.activeGraph = parsed.graph();
        this.triggersByType = parsed.triggersByType();
        this.lastPublishedJson = json;
        int triggers = triggersByType.values().stream().mapToInt(List::size).sum();
        LOGGER.info("[Colophon] Published graph: {} nodes, {} triggers",
                parsed.graph().nodes().size(), triggers);
        save(json);
    }

    /** Load a previously published graph from disk, if present. */
    public synchronized void load() {
        Path file = saveFile();
        if (!Files.exists(file)) {
            return;
        }
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            GraphParser.Parsed parsed = GraphParser.parse(json);
            this.activeGraph = parsed.graph();
            this.triggersByType = parsed.triggersByType();
            this.lastPublishedJson = json;
            LOGGER.info("[Colophon] Loaded graph from {}", file);
        } catch (Exception e) {
            LOGGER.error("[Colophon] Failed to load graph from {}", file, e);
        }
    }

    private void save(String json) {
        Path file = saveFile();
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("[Colophon] Failed to save graph to {}", file, e);
        }
    }

    /** Raw JSON of the active graph for the editor to load; empty graph if none. */
    public String graphJson() {
        String json = lastPublishedJson;
        return json != null ? json : EMPTY_GRAPH;
    }

    /** Start every trigger node of the given type against the active graph. */
    public void fireTrigger(String triggerType, MinecraftServer server, ServerPlayer player) {
        fireTrigger(triggerType, server, player, Map.of());
    }

    /**
     * Start every trigger node of the given type, seeding its explicit data outputs
     * (contract d/e). A trigger is the only place that knows its event's subjects
     * (e.g. victim/killer), so it pushes them into the value store before the flow
     * runs; downstream nodes then read them as data. A {@code null} output value is
     * left unset (absent), not stored as null.
     */
    public void fireTrigger(String triggerType, MinecraftServer server, ServerPlayer player,
                            Map<String, Object> triggerOutputs) {
        Graph graph = activeGraph;
        if (graph == null) {
            return;
        }
        List<String> ids = triggersByType.getOrDefault(triggerType, List.of());
        for (String id : ids) {
            ExecContext ctx = new ExecContext(server, player, storage);
            triggerOutputs.forEach((port, value) -> {
                if (value != null) {
                    ctx.values().put(id, port, value);
                }
            });
            scheduler.start(graph, ctx, id);
        }
    }

    public void clear() {
        this.activeGraph = null;
        this.triggersByType = Map.of();
        this.lastPublishedJson = null;
    }
}
