/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.client;

import kr.guinnessgroup.colophon.npc.NpcEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Temporary look for NPCs: the default Steve skin. GeckoLib replaces this in slice 3. */
public final class NpcRenderer extends HumanoidMobRenderer<NpcEntity, PlayerModel<NpcEntity>> {

    private static final ResourceLocation STEVE =
            ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");

    public NpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(NpcEntity entity) {
        return STEVE;
    }
}
