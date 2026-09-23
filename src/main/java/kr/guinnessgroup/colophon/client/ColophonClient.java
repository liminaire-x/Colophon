/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.client;

import kr.guinnessgroup.colophon.npc.ColophonEntities;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/**
 * Client-only setup. Only referenced when running on a client, so a dedicated
 * server never loads client classes.
 */
public final class ColophonClient {

    /** Opens (and closes) the quest screen. J by default; players can rebind it. */
    static final KeyMapping OPEN_QUESTS = new KeyMapping("key.colophon.quests", GLFW.GLFW_KEY_J, "key.categories.colophon");

    private ColophonClient() {}

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(ColophonClient::onRegisterRenderers);
        modEventBus.addListener(ColophonClient::onRegisterKeys);
        NeoForge.EVENT_BUS.addListener(ColophonClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(ColophonClient::onLoggingOut);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ColophonEntities.NPC.get(), NpcRenderer::new);
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_QUESTS);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (OPEN_QUESTS.consumeClick()) {
            if (mc.screen == null && mc.player != null) {
                mc.setScreen(new QuestScreen());
            }
        }
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientQuests.clear();
    }
}
