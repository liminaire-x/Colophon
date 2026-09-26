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
 * Looks: the server syncs the model name, the idle animation, the talk set, and
 * "play this once" requests to clients; the client animates with GeckoLib.
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
    private static final EntityDataAccessor<String> TALK_START =
            SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> TALK_LOOP =
            SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> TALK_END =
            SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    private String npcId = "";

    /** Client: where this NPC is in a talk with this player (docs/decisions/0011-talk-gestures.md). */
    private enum TalkPhase { NONE, START, LOOP, END }

    // Client-side animation state.
    private int seenActionSeq = -1;
    private String playingAction = "";
    private TalkPhase talkPhase = TalkPhase.NONE;
    /** Client: a line's animation that waits for the talk's start to finish. */
    private String waitingLine;
    /** Client: an animation to start on this screen only, see {@link #playLocally}. */
    private volatile String localRequest;
    /** Client: whether this player's dialogue screen is open on this NPC, see {@link #setTalking}. */
    private volatile boolean talkWanted;

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
        builder.define(TALK_START, "");
        builder.define(TALK_LOOP, "");
        builder.define(TALK_END, "");
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

    /** Server: play an animation once for everyone who sees this NPC, then go back to what it was doing. */
    public void playAnimation(String animation) {
        entityData.set(ACTION, animation);
        entityData.set(ACTION_SEQ, entityData.get(ACTION_SEQ) + 1);
    }

    /**
     * Client: play an animation once on this player's screen only (a dialogue line's
     * animation: only the player talking sees it), then go back to the talk loop.
     */
    public void playLocally(String animation) {
        localRequest = animation;
    }

    /**
     * Client: this player's dialogue screen opened ({@code true}) or closed on this NPC.
     * The NPC plays its talk set on this screen only. Opening and closing before the next
     * frame cancel out.
     */
    public void setTalking(boolean talking) {
        talkWanted = talking;
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
        NpcDoc.Talk talk = def.talk();
        if (!talk.start().equals(entityData.get(TALK_START))) {
            entityData.set(TALK_START, talk.start());
        }
        if (!talk.loop().equals(entityData.get(TALK_LOOP))) {
            entityData.set(TALK_LOOP, talk.loop());
        }
        if (!talk.end().equals(entityData.get(TALK_END))) {
            entityData.set(TALK_END, talk.end());
        }
    }

    // --- GeckoLib (animations run on the client) ---

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::animate));
    }

    /**
     * One NPC on one screen. Underneath: the idle loop, or while this player talks to it,
     * the talk set (start once, loop, end once when the talk closes). On top: animations
     * played once (a graph node for everyone, a dialogue line on this screen), after which
     * it goes back underneath. A line shown while the talk's start plays waits for it.
     */
    private PlayState animate(AnimationState<NpcEntity> state) {
        AnimationController<NpcEntity> controller = state.getController();

        // This player's dialogue screen opened or closed on this NPC.
        boolean talking = talkPhase == TalkPhase.START || talkPhase == TalkPhase.LOOP;
        if (talkWanted != talking) {
            String set = entityData.get(talkWanted ? TALK_START : TALK_END);
            talkPhase = set.isEmpty() ? (talkWanted ? TalkPhase.LOOP : TalkPhase.NONE)
                    : (talkWanted ? TalkPhase.START : TalkPhase.END);
            playingAction = "";
            waitingLine = null;
            controller.forceAnimationReset();
            if (!set.isEmpty()) {
                return state.setAndContinue(RawAnimation.begin().thenPlay(set));
            }
        }

        // Animations played once.
        int seq = entityData.get(ACTION_SEQ);
        String action = null;
        if (seenActionSeq == -1) {
            seenActionSeq = seq; // just appeared: do not replay an old request
        } else if (seq != seenActionSeq) {
            seenActionSeq = seq;
            action = entityData.get(ACTION);
        }
        String line = localRequest;
        localRequest = null;
        if (line != null) {
            if (talkPhase == TalkPhase.START) {
                waitingLine = line;
            } else if (action == null) {
                action = line;
            }
        }
        if (action != null && !action.isEmpty()) {
            return playOnce(state, action);
        }
        if (!playingAction.isEmpty()) {
            if (!controller.hasAnimationFinished()) {
                return PlayState.CONTINUE;
            }
            playingAction = "";
            controller.forceAnimationReset();
        }

        // The talk's start and end play once, then move on.
        if (talkPhase == TalkPhase.START || talkPhase == TalkPhase.END) {
            // Not hasAnimationFinished(): a misspelled name stops at once, and if nothing played
            // before, that would never count as finished.
            if (controller.getAnimationState() != AnimationController.State.STOPPED) {
                return PlayState.CONTINUE;
            }
            talkPhase = (talkPhase == TalkPhase.START) ? TalkPhase.LOOP : TalkPhase.NONE;
            controller.forceAnimationReset();
        }
        if (waitingLine != null) {
            String waited = waitingLine;
            waitingLine = null;
            return playOnce(state, waited);
        }

        String loop = entityData.get(talkPhase == TalkPhase.LOOP ? TALK_LOOP : IDLE);
        if (loop.isEmpty() && talkPhase == TalkPhase.LOOP) {
            loop = entityData.get(IDLE);
        }
        return loop.isEmpty() ? PlayState.STOP : state.setAndContinue(RawAnimation.begin().thenLoop(loop));
    }

    /** Plays an animation once over whatever plays underneath; it cuts a talk's start or end short. */
    private PlayState playOnce(AnimationState<NpcEntity> state, String animation) {
        if (talkPhase == TalkPhase.START) {
            talkPhase = TalkPhase.LOOP;
        } else if (talkPhase == TalkPhase.END) {
            talkPhase = TalkPhase.NONE;
        }
        playingAction = animation;
        state.getController().forceAnimationReset();
        return state.setAndContinue(RawAnimation.begin().thenPlay(animation));
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
