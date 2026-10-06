package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.ai.FishAttackMode;
import com.dexer.aquanaut.common.ai.FishResponseMode;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A buried ambush predator released by disturbed sediment. It keeps its life as a small state
 * machine — surge up (EMERGING), strike (ATTACKING), sink back (RETREATING), lie dormant
 * (COOLDOWN) — but the strike itself is driven by the shared fish brain, so it lunges at what
 * disturbed it instead of paddling vertically. Outside the strike the brain is suspended and the
 * rise or sink is steered directly, which keeps the buried phases perfectly still.
 */
public final class SedimentWormEntity extends BaseFishEntity implements GeoEntity {
    private static final EntityDataAccessor<Integer> STRIKE = SynchedEntityData.defineId(
            SedimentWormEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STRIKE_TIMER = SynchedEntityData.defineId(
            SedimentWormEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ATTACK_TIMER = SynchedEntityData.defineId(
            SedimentWormEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation EMERGE = RawAnimation.begin().thenPlay("emerge");
    private static final RawAnimation RETREAT = RawAnimation.begin().thenPlay("retreat");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");

    private static final int EMERGE_TICKS = 14;
    private static final int ATTACK_TICKS = 70;
    private static final int RETREAT_TICKS = 14;
    private static final int COOLDOWN_TICKS = 100;
    /** Ticks the bite clip plays for after a landed hit. */
    private static final int ATTACK_ANIM_TICKS = 12;
    private static final double RISE_SPEED = 0.10D;
    private static final double SINK_SPEED = -0.11D;
    private static final double REARM_RANGE = 8.0D;
    private static final Strike[] STRIKES = Strike.values();
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SedimentWormEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.18D);
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STRIKE, Strike.EMERGING.ordinal());
        builder.define(STRIKE_TIMER, EMERGE_TICKS);
        builder.define(ATTACK_TIMER, 0);
    }

    @Override
    public void onSuccessfulBite(Player player) {
        if (!level().isClientSide) {
            entityData.set(ATTACK_TIMER, ATTACK_ANIM_TICKS);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (attackTimer() > 0) {
                return state.setAndContinue(ATTACK);
            }
            return switch (strike()) {
                case EMERGING -> state.setAndContinue(EMERGE);
                case RETREATING -> state.setAndContinue(RETREAT);
                case ATTACKING, COOLDOWN -> state.setAndContinue(IDLE);
            };
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void aiStep() {
        if (!level().isClientSide) {
            if (!hasEffect(MobEffects.WATER_BREATHING)) {
                // An arthropod cannot breathe water on its own; keep it supplied while it burrows.
                addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, -1, 0, true, false));
            }
            if (attackTimer() > 0) {
                entityData.set(ATTACK_TIMER, attackTimer() - 1);
            }
            int timer = strikeTimer();
            if (timer > 0) {
                timer--;
                entityData.set(STRIKE_TIMER, timer);
            }
            Strike current = strike();
            // The strike swims on the shared brain; every buried phase is steered here.
            setMovementSuspended(current != Strike.ATTACKING);
            switch (current) {
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
    }

    private Strike strike() {
        return STRIKES[entityData.get(STRIKE)];
    }

    private int strikeTimer() {
        return entityData.get(STRIKE_TIMER);
    }

    private int attackTimer() {
        return entityData.get(ATTACK_TIMER);
    }

    @Override
    protected FishResponseMode getResponseMode() {
        return FishResponseMode.CHARGE;
    }

    @Override
    protected FishAttackMode getAttackMode() {
        return FishAttackMode.TRACKING_BITE;
    }

    @Override
    protected double getBaseBiteDamage() {
        return 2.0D;
    }

    @Override
    protected double getCruiseMaxSpeed() {
        return 0.10D;
    }

    @Override
    protected double getCruiseAcceleration() {
        return 0.008D;
    }

    @Override
    protected double getChargeMaxSpeed() {
        return 0.30D;
    }

    @Override
    protected double getChargeAcceleration() {
        return 0.03D;
    }

    @Override
    protected double getPlayerDetectionRange() {
        return 12.0D;
    }

    @Override
    protected double getCruiseFloorBias() {
        return 0.9D;
    }

    @Override
    protected double getCruiseDepthRange() {
        return 0.8D;
    }

    private enum Strike {
        EMERGING, ATTACKING, RETREATING, COOLDOWN
    }
}
