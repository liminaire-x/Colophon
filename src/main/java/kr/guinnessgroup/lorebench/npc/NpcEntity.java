/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench.npc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * One placement of an NPC in the world ({@code lorebench:npc}). It cannot be hurt,
 * does not move, and never despawns. The placement record is the source of truth:
 * once a second the entity checks that its record and its NPC definition still
 * exist, and disappears if not. That one rule covers remove commands, deleting a
 * definition in the editor, and entities in chunks that were unloaded meanwhile.
 * <p>
 * Looks: the server syncs the model name, the idle animation, and "play this
 * once" requests to clients; the client animates with GeckoLib.
 */
public class NpcEntity extends PathfinderMob implements GeoEntity {

    private static final String TAG_NPC = "lorebench_npc";

    private static final EntityDataAccessor<String> MODEL =
            SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> IDLE =
            SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> ACTION =
            SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    /** Bumped on every play request, so the same animation can be played again. */
    private static final EntityDataAccessor<Integer> ACTION_SEQ =
            SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    private String npcId = "";

    // Client-side animation state.
    private int seenActionSeq = -1;
    private String playingAction = "";
    /** Client: an animation to start on this screen only, see {@link #playLocally}. */
    private volatile String localRequest;

    public NpcEntity(EntityType<? extends NpcEntity> type, Level level) {
        super(type, level);
        setNoAi(true);
        setPersistenceRequired();
        setInvulnerable(true);
        setCustomNameVisible(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MODEL, "");
        builder.define(IDLE, "");
        builder.define(ACTION, "");
        builder.define(ACTION_SEQ, 0);
    }

    public String npcId() {
        return npcId;
    }

    public void setNpcId(String npcId) {
        this.npcId = npcId;
    }

    /** The GeckoLib model name, or "" for the default look. */
    public String model() {
        return entityData.get(MODEL);
    }

    /** Server: play an animation once for everyone who sees this NPC, then return to idle. */
    public void playAnimation(String animation) {
        entityData.set(ACTION, animation);
        entityData.set(ACTION_SEQ, entityData.get(ACTION_SEQ) + 1);
    }

    /**
     * Client: play an animation once on this player's screen only (a dialogue line's
     * animation: only the player talking sees it), then return to idle.
     */
    public void playLocally(String animation) {
        localRequest = animation;
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
        if (!def.model().equals(entityData.get(MODEL))) {
            entityData.set(MODEL, def.model());
        }
        if (!def.idle().equals(entityData.get(IDLE))) {
            entityData.set(IDLE, def.idle());
        }
    }

    // --- GeckoLib (animations run on the client) ---

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::animate));
    }

    private PlayState animate(AnimationState<NpcEntity> state) {
        AnimationController<NpcEntity> controller = state.getController();
        int seq = entityData.get(ACTION_SEQ);
        String start = null;
        if (seenActionSeq == -1) {
            seenActionSeq = seq; // just appeared: do not replay an old request
        } else if (seq != seenActionSeq) {
            seenActionSeq = seq;
            start = entityData.get(ACTION);
        }
        if (start == null && localRequest != null) {
            start = localRequest;
        }
        localRequest = null;
        if (start != null) {
            playingAction = start;
            controller.forceAnimationReset();
            if (!playingAction.isEmpty()) {
                return state.setAndContinue(RawAnimation.begin().thenPlay(playingAction));
            }
        }
        if (!playingAction.isEmpty()) {
            if (!controller.hasAnimationFinished()) {
                return PlayState.CONTINUE;
            }
            playingAction = "";
            controller.forceAnimationReset();
        }
        String idle = entityData.get(IDLE);
        return idle.isEmpty() ? PlayState.STOP : state.setAndContinue(RawAnimation.begin().thenLoop(idle));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    // --- interaction and protection ---

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            Npcs npcs = Npcs.current();
            if (npcs != null && !npcId.isEmpty()) {
                npcs.interact(this, serverPlayer);
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
