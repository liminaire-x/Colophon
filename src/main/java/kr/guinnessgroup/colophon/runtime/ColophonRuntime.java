/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.runtime;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import kr.guinnessgroup.colophon.DocumentException;
import kr.guinnessgroup.colophon.graph.GraphDoc;
import kr.guinnessgroup.colophon.graph.GraphFormat;
import kr.guinnessgroup.colophon.npc.NpcDoc;
import kr.guinnessgroup.colophon.npc.NpcFormat;
import kr.guinnessgroup.colophon.quest.QuestDoc;
import kr.guinnessgroup.colophon.quest.QuestFormat;
import kr.guinnessgroup.colophon.record.Owner;
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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Holds the published content (graphs, NPC definitions, quests) and starts graphs
 * when events happen. Publishing checks the documents together, replaces everything
 * at once with no server restart, and saves {@code graphs.json}, {@code npcs.json}
 * and {@code quests.json}.
 */
public final class ColophonRuntime {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final NodeRegistry registry;
    private final RecordStore records;
    private final Path graphsFile;
    private final Path npcsFile;
    private final Path questsFile;
    private final ContentChecks checks;
    private volatile Owner serverOwner = Owner.server("main");
    private volatile Runnable onPublish = () -> {};

    /** Everything that changes on publish, swapped in one step. */
    private record Active(GraphDoc graphs, NpcDoc npcs, QuestDoc quests, Map<String, List<Start>> startsByTrigger) {}

    private record Start(Graph graph, String nodeId) {}

    private static final Active EMPTY = new Active(new GraphDoc(List.of()), new NpcDoc(List.of()),
            new QuestDoc(List.of()), Map.of());

    private volatile Active active = EMPTY;

    /** @param checks how publish asks the running game whether quest items and mobs exist */
    public ColophonRuntime(NodeRegistry registry, RecordStore records, Path dir, ContentChecks checks) {
        this.registry = registry;
        this.records = records;
        this.graphsFile = dir.resolve("graphs.json");
        this.npcsFile = dir.resolve("npcs.json");
        this.questsFile = dir.resolve("quests.json");
        this.checks = checks;
    }

    /** Run after every accepted publish, on the publishing (web) thread. */
    public void onPublish(Runnable action) {
        this.onPublish = (action == null) ? () -> {} : action;
    }

    /**
     * Check, swap in, and save new content. The body is
     * {@code {"graphs": <graph document>, "npcs": <NPC document>, "quests": <quest document>}}.
     * Throws {@link DocumentException} if anything is rejected; then nothing changes.
     */
    public synchronized void publish(String json) {
        JsonObject body;
        try {
            body = JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new DocumentException(List.of("not a JSON object"));
        }
        JsonElement graphs = body.get("graphs");
        JsonElement npcs = body.get("npcs");
        JsonElement quests = body.get("quests");
        if (graphs == null || npcs == null || quests == null) {
            throw new DocumentException(List.of("publish needs 'graphs', 'npcs' and 'quests'"));
        }
        NpcDoc npcDoc = NpcFormat.read(npcs.toString());
        QuestDoc questDoc = QuestFormat.read(quests.toString());
        GraphDoc graphDoc = GraphFormat.read(graphs.toString());
        activate(graphDoc, npcDoc, questDoc);
        write(npcsFile, NpcFormat.write(npcDoc));
        write(questsFile, QuestFormat.write(questDoc));
        write(graphsFile, GraphFormat.write(graphDoc));
        onPublish.run();
    }

    /** Load saved content. If a file is broken it is left untouched and no graph runs. */
    public synchronized void load(Owner serverOwner) {
        this.serverOwner = serverOwner;
        try {
            NpcDoc npcDoc = Files.exists(npcsFile)
                    ? NpcFormat.read(Files.readString(npcsFile, StandardCharsets.UTF_8))
                    : new NpcDoc(List.of());
            QuestDoc questDoc = Files.exists(questsFile)
                    ? QuestFormat.read(Files.readString(questsFile, StandardCharsets.UTF_8))
                    : new QuestDoc(List.of());
            GraphDoc graphDoc = Files.exists(graphsFile)
                    ? GraphFormat.read(Files.readString(graphsFile, StandardCharsets.UTF_8))
                    : new GraphDoc(List.of());
            activate(graphDoc, npcDoc, questDoc);
        } catch (DocumentException e) {
            LOGGER.error("[Colophon] Saved content was not loaded; nothing will run until it is fixed or republished: {}",
                    e.errors());
        } catch (IOException e) {
            LOGGER.error("[Colophon] Reading saved content failed; nothing will run", e);
        }
    }

    private void activate(GraphDoc graphDoc, NpcDoc npcDoc, QuestDoc questDoc) {
        checkItems(questDoc);
        Set<String> npcIds = npcDoc.npcs().stream().map(NpcDoc.NpcDef::id).collect(Collectors.toUnmodifiableSet());
        Set<String> questIds = questDoc.quests().stream().map(QuestDoc.Quest::id).collect(Collectors.toUnmodifiableSet());
        List<Graph> graphs = GraphBuilder.build(graphDoc, registry, new Catalog(npcIds, questIds));
        Map<String, List<Start>> starts = new HashMap<>();
        for (Graph g : graphs) {
            for (Graph.Placed n : g.nodes().values()) {
                if (n.type().trigger()) {
                    starts.computeIfAbsent(n.type().id(), k -> new ArrayList<>()).add(new Start(g, n.id()));
                }
            }
        }
        starts.replaceAll((k, v) -> List.copyOf(v));
        this.active = new Active(graphDoc, npcDoc, questDoc, Map.copyOf(starts));
        LOGGER.info("[Colophon] {} graph(s), {} NPC(s), {} quest(s) active",
                graphs.size(), npcDoc.npcs().size(), questDoc.quests().size());
    }

    /**
     * Quest items and kill targets must exist in this game (a typo, a missing mod or
     * bad components is rejected).
     */
    private void checkItems(QuestDoc questDoc) {
        List<String> errors = new ArrayList<>();
        for (QuestDoc.Quest q : questDoc.quests()) {
            String where = "quest '" + q.title() + "' (" + q.id() + "): ";
            if (!q.icon().isEmpty()) {
                check(errors, where + "icon '" + q.icon() + "': ", checks.item(q.icon()));
            }
            for (QuestDoc.Goal g : q.goals()) {
                if (g.kind() == QuestDoc.Goal.Kind.ITEM) {
                    check(errors, where + "goal '" + g.target() + "': ", checks.itemCondition(g.target()));
                } else {
                    check(errors, where + "kill '" + g.target() + "': ", checks.entity(g.target()));
                }
            }
            for (QuestDoc.Stack s : q.rewards()) {
                check(errors, where + "reward '" + s.item() + "': ", checks.item(s.item()));
            }
        }
        if (!errors.isEmpty()) {
            throw new DocumentException(errors);
        }
    }

    private static void check(List<String> errors, String where, String problem) {
        if (problem != null) {
            errors.add(where + problem);
        }
    }

    private static void write(Path file, String content) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("[Colophon] Published, but saving {} failed", file, e);
        }
    }

    /** The current graph document, for the editor. */
    public String graphsJson() {
        return GraphFormat.write(active.graphs());
    }

    /** The current NPC document, for the editor. */
    public String npcsJson() {
        return NpcFormat.write(active.npcs());
    }

    /** The current quest document, for the editor. */
    public String questsJson() {
        return QuestFormat.write(active.quests());
    }

    /** An NPC definition, or {@code null} if none has that id. */
    public NpcDoc.NpcDef npc(String id) {
        return active.npcs().find(id);
    }

    public List<NpcDoc.NpcDef> npcs() {
        return active.npcs().npcs();
    }

    /** A quest, or {@code null} if none has that id. */
    public QuestDoc.Quest quest(String id) {
        return active.quests().find(id);
    }

    public List<QuestDoc.Quest> quests() {
        return active.quests().quests();
    }

    public Owner serverOwner() {
        return serverOwner;
    }

    /**
     * Run every graph that starts with the given trigger type. Call on the server thread.
     *
     * @param event facts about the event for trigger nodes, e.g. {@code npc -> npc_7ha2m0qe}
     */
    public void fire(String triggerType, MinecraftServer server, ServerPlayer player, Map<String, String> event) {
        List<Start> starts = active.startsByTrigger().getOrDefault(triggerType, List.of());
        for (Start s : starts) {
            Runner.run(s.graph(), s.nodeId(), new Context(server, player, records, serverOwner, event));
        }
    }

    public synchronized void clear() {
        this.active = EMPTY;
    }
}
