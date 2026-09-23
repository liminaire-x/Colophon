/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon;

import kr.guinnessgroup.colophon.nodes.BuiltinNodes;
import kr.guinnessgroup.colophon.nodes.OnPlayerJoin;
import kr.guinnessgroup.colophon.record.H2RecordBackend;
import kr.guinnessgroup.colophon.record.Owner;
import kr.guinnessgroup.colophon.record.RecordStore;
import kr.guinnessgroup.colophon.runtime.ColophonRuntime;
import kr.guinnessgroup.colophon.runtime.NodeRegistry;
import kr.guinnessgroup.colophon.web.ColophonWebServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.nio.file.Path;

/** Mod entry point: wires the pieces together and connects them to game events. */
@Mod(Colophon.MODID)
public final class Colophon {

    public static final String MODID = "colophon";

    private final Path dir = FMLPaths.CONFIGDIR.get().resolve(MODID);
    private final NodeRegistry nodes = new NodeRegistry();
    private final RecordStore records = new RecordStore();
    private final ColophonRuntime runtime = new ColophonRuntime(nodes, records, dir.resolve("graphs.json"));
    private final ColophonWebServer web = new ColophonWebServer(runtime, nodes);

    public Colophon(IEventBus modEventBus) {
        BuiltinNodes.registerAll(nodes);
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        records.open(new H2RecordBackend(dir.resolve("records")));
        runtime.load();
        web.start();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        web.stop();
        runtime.clear();
        records.close();
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Load the player's records before any graph can read them.
            records.load(Owner.player(player.getUUID()));
            runtime.fire(OnPlayerJoin.ID, player.getServer(), player);
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            records.release(Owner.player(player.getUUID()));
        }
    }

    @SubscribeEvent
    public void onWorldSave(LevelEvent.Save event) {
        // Save records together with the world. The first dimension's save writes
        // everything; later ones find nothing left to write.
        records.flush();
    }
}
