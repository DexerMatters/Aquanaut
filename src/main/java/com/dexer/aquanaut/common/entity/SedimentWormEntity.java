package com.dexer.aquanaut.common.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A buried ambush predator released by disturbed sediment. Its life is a small state
 * machine: it surges up out of the mud (EMERGING), strikes for a while (ATTACKING), sinks
 * back in (RETREATING) and lies dormant (COOLDOWN) before it either strikes again or
 * dissolves back into the sediment. AI is only live during ATTACKING, so the buried phases
 * read as a still, half-buried body. The clip names line up with the states: emerge /
 * idle / retreat.
 */
public final class SedimentWormEntity extends Silverfish implements GeoEntity {
    private static final EntityDataAccessor<Integer> STRIKE = SynchedEntityData.defineId(
            SedimentWormEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STRIKE_TIMER = SynchedEntityData.defineId(
            SedimentWormEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation EMERGE = RawAnimation.begin().thenPlay("emerge");
    private static final RawAnimation RETREAT = RawAnimation.begin().thenPlay("retreat");

    private static final int EMERGE_TICKS = 14;
    private static final int ATTACK_TICKS = 70;
    private static final int RETREAT_TICKS = 14;
    private static final int COOLDOWN_TICKS = 100;
    private static final double RISE_SPEED = 0.10D;
    private static final double SINK_SPEED = -0.11D;
    private static final double REARM_RANGE = 8.0D;
    private static final Strike[] STRIKES = Strike.values();
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SedimentWormEntity(EntityType<? extends Silverfish> type, Level level) {
        super(type, level);
        setNoAi(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Silverfish.createAttributes().add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D).add(Attributes.MOVEMENT_SPEED, 0.18D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STRIKE, Strike.EMERGING.ordinal());
        builder.define(STRIKE_TIMER, EMERGE_TICKS);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> switch (strike()) {
            case EMERGING -> state.setAndContinue(EMERGE);
            case RETREATING -> state.setAndContinue(RETREAT);
            case ATTACKING, COOLDOWN -> state.setAndContinue(IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1D, false));
        goalSelector.addGoal(7, new RandomStrollGoal(this, 0.6D));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        if (!level().isClientSide) {
            int timer = strikeTimer();
            if (timer > 0) {
                timer--;
                entityData.set(STRIKE_TIMER, timer);
            }
            switch (strike()) {
                case EMERGING -> {
                    setDeltaMovement(getDeltaMovement().x, RISE_SPEED, getDeltaMovement().z);
                    if (timer <= 0) {
                        enter(Strike.ATTACKING, ATTACK_TICKS);
                    }
                }
                case ATTACKING -> {
                    if (timer <= 0) {
                        enter(Strike.RETREATING, RETREAT_TICKS);
                    }
                }
                case RETREATING -> {
                    setDeltaMovement(getDeltaMovement().x, SINK_SPEED, getDeltaMovement().z);
                    if (timer <= 0) {
                        enter(Strike.COOLDOWN, COOLDOWN_TICKS);
                    }
                }
                case COOLDOWN -> {
                    if (timer <= 0) {
                        if (nearestPlayerWithin(REARM_RANGE)) {
                            enter(Strike.EMERGING, EMERGE_TICKS);
                        } else {
                            discard();
                        }
                    }
                }
            }
        }
        super.aiStep();
    }

    private boolean nearestPlayerWithin(double range) {
        Player player = level().getNearestPlayer(this, range);
        return player != null && player.isAlive() && !player.isCreative() && !player.isSpectator();
    }

    private void enter(Strike next, int ticks) {
        entityData.set(STRIKE, next.ordinal());
        entityData.set(STRIKE_TIMER, ticks);
        setNoAi(next != Strike.ATTACKING);
    }

    private Strike strike() {
        return STRIKES[entityData.get(STRIKE)];
    }

    private int strikeTimer() {
        return entityData.get(STRIKE_TIMER);
    }

    private enum Strike {
        EMERGING, ATTACKING, RETREATING, COOLDOWN
    }
}
