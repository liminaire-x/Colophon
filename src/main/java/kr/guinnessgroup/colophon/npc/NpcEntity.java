/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.npc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * One placement of an NPC in the world ({@code colophon:npc}). It cannot be hurt,
 * does not move, and never despawns. The placement record is the source of truth:
 * once a second the entity checks that its record and its NPC definition still
 * exist, and disappears if not. That one rule covers remove commands, deleting a
 * definition in the editor, and entities in chunks that were unloaded meanwhile.
 */
public class NpcEntity extends PathfinderMob {

    private static final String TAG_NPC = "colophon_npc";

    private String npcId = "";

    public NpcEntity(EntityType<? extends NpcEntity> type, Level level) {
        super(type, level);
        setNoAi(true);
        setPersistenceRequired();
        setInvulnerable(true);
        setCustomNameVisible(true);
    }

    public String npcId() {
        return npcId;
    }

    public void setNpcId(String npcId) {
        this.npcId = npcId;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount % 20 == 1) {
            followRecord();
        }
    }

    private void followRecord() {
        Npcs npcs = Npcs.current();
        if (npcs == null) {
            return;
        }
        Placement placement = npcs.placement(getUUID());
        NpcDoc.NpcDef def = (placement == null) ? null : npcs.definition(placement.npc());
        if (def == null) {
            discard();
            return;
        }
        npcId = placement.npc();
        Component name = Component.literal(def.name());
        if (!name.equals(getCustomName())) {
            setCustomName(name);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            Npcs npcs = Npcs.current();
            if (npcs != null && !npcId.isEmpty()) {
                npcs.interact(npcId, serverPlayer);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Only damage that ignores invulnerability (e.g. /kill, the void) gets through,
        // so even creative players cannot punch an NPC away.
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(source, amount);
    }

    @Override
    public void remove(RemovalReason reason) {
        // Gone for good (killed or discarded): the placement goes too. Unloading keeps it.
        if (!level().isClientSide && reason.shouldDestroy()) {
            Npcs npcs = Npcs.current();
            if (npcs != null) {
                npcs.unplace(getUUID());
            }
        }
        super.remove(reason);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(TAG_NPC, npcId);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        npcId = tag.getString(TAG_NPC);
    }
}
