/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon;

import kr.guinnessgroup.colophon.client.ColophonClient;
import kr.guinnessgroup.colophon.nodes.BuiltinNodes;
import kr.guinnessgroup.colophon.nodes.OnPlayerJoin;
import kr.guinnessgroup.colophon.npc.ColophonEntities;
import kr.guinnessgroup.colophon.npc.NpcCommands;
import kr.guinnessgroup.colophon.npc.Npcs;
import kr.guinnessgroup.colophon.quest.QuestSyncPayload;
import kr.guinnessgroup.colophon.quest.Quests;
import kr.guinnessgroup.colophon.record.H2RecordBackend;
import kr.guinnessgroup.colophon.record.Owner;
import kr.guinnessgroup.colophon.record.RecordStore;
import kr.guinnessgroup.colophon.runtime.ColophonRuntime;
import kr.guinnessgroup.colophon.runtime.NodeRegistry;
import kr.guinnessgroup.colophon.web.ColophonWebServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.nio.file.Path;
import java.util.Map;

/** Mod entry point: wires the pieces together and connects them to game events. */
@Mod(Colophon.MODID)
public final class Colophon {

    public static final String MODID = "colophon";

    private final Path dir = FMLPaths.CONFIGDIR.get().resolve(MODID);
    private final NodeRegistry nodes = new NodeRegistry();
    private final RecordStore records = new RecordStore();
    private final ColophonRuntime runtime = new ColophonRuntime(nodes, records, dir,
            Quests::itemProblem, Quests::entityProblem);
    private final Npcs npcs = new Npcs(runtime, records);
    private final Quests quests = new Quests(runtime, records);
    private final ColophonWebServer web = new ColophonWebServer(runtime, nodes, npcs);

    public Colophon(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, ColophonConfig.SPEC);
        ColophonEntities.TYPES.register(modEventBus);
        modEventBus.addListener(ColophonEntities::onAttributes);
        modEventBus.addListener(QuestSyncPayload::register);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ColophonClient.init(modEventBus);
        }
        BuiltinNodes.registerAll(nodes);
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        Owner server = Owner.server(ColophonConfig.serverName());
        records.open(new H2RecordBackend(dir.resolve("records")), server);
        runtime.load(server);
        npcs.start();
        quests.start();
        // Publish arrives on the web thread; quest content goes out on the server thread.
        MinecraftServer mc = event.getServer();
        runtime.onPublish(() -> mc.execute(() -> quests.syncAll(mc)));
        web.start();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        web.stop();
        runtime.onPublish(null);
        quests.stop();
        npcs.stop();
        runtime.clear();
        records.close();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        NpcCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Load the player's records before any graph can read them.
            records.load(Owner.player(player.getUUID()));
            quests.sync(player);
            runtime.fire(OnPlayerJoin.ID, player.getServer(), player, Map.of());
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            records.release(Owner.player(player.getUUID()));
            // The game saves the player's inventory as they leave; save their records
            // now too, so a crash before the next world save cannot split the two
            // (e.g. keep a quest reward but lose the "done" record).
            records.flush();
        }
    }

    /**
     * Last in line, and only if no other mod cancelled the death, so only real kills
     * count. The killer is whoever the damage came from (the shooter, for arrows).
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            quests.onKill(player, event.getEntity());
        }
    }

    @SubscribeEvent
    public void onWorldSave(LevelEvent.Save event) {
        // Save records together with the world. The first dimension's save writes
        // everything; later ones find nothing left to write.
        records.flush();
    }
}
