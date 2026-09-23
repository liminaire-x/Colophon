/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.client;

import kr.guinnessgroup.colophon.npc.ColophonEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Client-only setup. Only referenced when running on a client, so a dedicated
 * server never loads client classes.
 */
public final class ColophonClient {

    private ColophonClient() {}

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(ColophonClient::onRegisterRenderers);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ColophonEntities.NPC.get(), NpcRenderer::new);
    }
}
