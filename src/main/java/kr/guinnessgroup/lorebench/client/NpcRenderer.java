/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench.client;

import com.mojang.blaze3d.vertex.PoseStack;
import kr.guinnessgroup.lorebench.npc.NpcEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Draws an NPC with its GeckoLib model when this client has the model's files
 * (from a resource pack), and as Steve otherwise, so a missing pack never crashes
 * the game.
 */
public final class NpcRenderer extends EntityRenderer<NpcEntity> {

    private static final ResourceLocation STEVE =
            ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");

    private final HumanoidMobRenderer<NpcEntity, PlayerModel<NpcEntity>> steve;
    private final GeoEntityRenderer<NpcEntity> geo;

    public NpcRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5f;
        this.steve = new HumanoidMobRenderer<>(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f) {
            @Override
            public ResourceLocation getTextureLocation(NpcEntity entity) {
                return STEVE;
            }
        };
        this.geo = new GeoEntityRenderer<>(context, new NpcGeoModel());
    }

    @Override
    public void render(NpcEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        if (NpcGeoModel.isAvailable(entity.model())) {
            geo.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        } else {
            steve.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(NpcEntity entity) {
        return NpcGeoModel.isAvailable(entity.model()) ? geo.getTextureLocation(entity) : STEVE;
    }
}
