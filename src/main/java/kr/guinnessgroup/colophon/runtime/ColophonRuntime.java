package kr.guinnessgroup.colophon.runtime;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Holds the currently published graph and starts executions when triggers fire.
 * <p>
 * v0: a single active graph, kept in memory and persisted as raw JSON under the
 * config directory so it survives a restart. Publishing hot-swaps it with no
 * server restart.
 */
public final class ColophonRuntime {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final TickScheduler scheduler;
    private volatile Graph activeGraph;
    private volatile Map<String, List<String>> triggersByType = Map.of();

    public ColophonRuntime(TickScheduler scheduler) {
        this.scheduler = scheduler;
    }

    private Path saveFile() {
        return FMLPaths.CONFIGDIR.get().resolve("colophon").resolve("graph.json");
    }

    /** Parse, hot-swap the active graph, then persist it. Throws on malformed JSON. */
    public synchronized void publish(String json) {
        GraphParser.Parsed parsed = GraphParser.parse(json);
        this.activeGraph = parsed.graph();
        this.triggersByType = parsed.triggersByType();
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

    /** Start every trigger node of the given type against the active graph. */
    public void fireTrigger(String triggerType, MinecraftServer server, ServerPlayer player) {
        Graph graph = activeGraph;
        if (graph == null) {
            return;
        }
        List<String> ids = triggersByType.getOrDefault(triggerType, List.of());
        for (String id : ids) {
            scheduler.start(graph, new ExecContext(server, player), id);
        }
    }

    public void clear() {
        this.activeGraph = null;
        this.triggersByType = Map.of();
    }
}
