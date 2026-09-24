/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench.client;

import kr.guinnessgroup.lorebench.Lorebench;
import kr.guinnessgroup.lorebench.npc.NpcEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.model.GeoModel;

/**
 * Where an NPC's files live, by model name (see docs/decisions/0003-npc-looks.md):
 * <pre>
 * assets/lorebench/geo/npc/&lt;model&gt;.geo.json
 * assets/lorebench/animations/npc/&lt;model&gt;.animation.json
 * assets/lorebench/textures/npc/&lt;model&gt;.png
 * </pre>
 */
public final class NpcGeoModel extends GeoModel<NpcEntity> {

    static ResourceLocation modelFile(String model) {
        return ResourceLocation.fromNamespaceAndPath(Lorebench.MODID, "geo/npc/" + model + ".geo.json");
    }

    static ResourceLocation animationFile(String model) {
        return ResourceLocation.fromNamespaceAndPath(Lorebench.MODID, "animations/npc/" + model + ".animation.json");
    }

    static ResourceLocation textureFile(String model) {
        return ResourceLocation.fromNamespaceAndPath(Lorebench.MODID, "textures/npc/" + model + ".png");
    }

    /** Whether this client loaded the model and animation files (GeckoLib throws if they are missing). */
    static boolean isAvailable(String model) {
        return !model.isEmpty()
                && GeckoLibCache.getBakedModels().containsKey(modelFile(model))
                && GeckoLibCache.getBakedAnimations().containsKey(animationFile(model));
    }

    @Override
    public ResourceLocation getModelResource(NpcEntity npc) {
        return modelFile(npc.model());
    }

    @Override
    public ResourceLocation getTextureResource(NpcEntity npc) {
        return textureFile(npc.model());
    }

    @Override
    public ResourceLocation getAnimationResource(NpcEntity npc) {
        return animationFile(npc.model());
    }
}
