/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.npc;

import kr.guinnessgroup.colophon.Colophon;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Colophon's entity types. The ids are saved in worlds: never rename. */
public final class ColophonEntities {

    public static final DeferredRegister<EntityType<?>> TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Colophon.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<NpcEntity>> NPC = TYPES.register("npc",
            () -> EntityType.Builder.<NpcEntity>of(NpcEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f)
                    .build("npc"));

    private ColophonEntities() {}

    /** Mod bus: living entities need their attributes registered. */
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(NPC.get(), Mob.createMobAttributes().build());
    }
}
