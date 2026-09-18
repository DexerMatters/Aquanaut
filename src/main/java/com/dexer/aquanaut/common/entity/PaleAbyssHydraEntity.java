package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.ai.FishAttackMode;
import com.dexer.aquanaut.common.ai.FishResponseMode;
import com.dexer.aquanaut.core.MobEffectRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Pale Abyss Hydra — a pale boulder on the trench floor, until the boulder opens.
 *
 * <p>
 * Dormant it reads as scenery. When a diver crosses into reach it blooms: eighteen branched arms
 * unfold, the water pressure sickness starts, and the diver is dragged toward the core. It then
 * retracts and goes still again, which is what makes the trench floor worth checking twice.
 */
public class PaleAbyssHydraEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM_ANIMATION = RawAnimation.begin().thenLoop("swim");
    private static final RawAnimation ATTACK_ANIMATION = RawAnimation.begin().thenPlay("attack");

    private static final EntityDataAccessor<Byte> STATE = SynchedEntityData.defineId(
            PaleAbyssHydraEntity.class, EntityDataSerializers.BYTE);

    public static final byte STATE_DORMANT = 0;
    public static final byte STATE_UNFURLING = 1;
    public static final byte STATE_GRABBING = 2;
    public static final byte STATE_RETRACTING = 3;

    private static final double TRIGGER_RANGE = 5.0D;
    private static final double GRAB_RANGE = 4.0D;
    private static final int UNFURL_TICKS = 20;
    private static final int GRAB_TICKS = 60;
    private static final int RETRACT_TICKS = 40;
    private static final int DORMANT_COOLDOWN_TICKS = 120;
    private static final int GRAB_TICK_INTERVAL = 10;
    private static final float GRAB_DAMAGE = 1.0F;
    private static final int NARCOSIS_TICKS = 60;
    private static final double PULL_STRENGTH = 0.28D;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int stateTimer;
    private int dormantCooldown;
    private int targetId = -1;

    public PaleAbyssHydraEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, STATE_DORMANT);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            return switch (this.getState()) {
                case STATE_UNFURLING, STATE_GRABBING -> {
                    state.getController().setAnimationSpeed(1.0D);
                    yield state.setAndContinue(ATTACK_ANIMATION);
                }
                case STATE_RETRACTING -> {
                    state.getController().setAnimationSpeed(-0.7D);
                    yield state.setAndContinue(ATTACK_ANIMATION);
                }
                default -> {
                    state.getController().setAnimationSpeed(0.35D);
                    yield state.setAndContinue(SWIM_ANIMATION);
                }
            };
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 160.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.05D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .build();
    }

    public byte getState() {
        return this.entityData.get(STATE);
    }

    private void setState(byte state) {
        this.entityData.set(STATE, state);
    }

    @Override
    protected FishResponseMode getResponseMode() {
        return FishResponseMode.PASSIVE;
    }

    @Override
    protected FishAttackMode getAttackMode() {
        return FishAttackMode.NONE;
    }

    @Override
    protected double getCruiseAcceleration() {
        return 0.0015D;
    }

    @Override
    protected double getCruiseMaxSpeed() {
        return 0.020D;
    }

    @Override
    protected double getWaterDrag() {
        return 0.94D;
    }

    @Override
    protected double getPlayerDetectionRange() {
        return TRIGGER_RANGE;
    }

    @Override
    protected float getCruiseYawTurnRateDegrees() {
        return 0.2F;
    }

    @Override
    protected float getCruisePitchTurnRateDegrees() {
        return 0.15F;
    }

    @Override
    protected int getCruiseYawDecisionMinTicks() {
        return 240;
    }

    @Override
    protected int getCruisePitchDecisionMinTicks() {
        return 240;
    }

    @Override
    protected float getBodyTurnRateDegrees() {
        return 0.6F;
    }

    @Override
    protected float getMaxTiltDegrees() {
        return 8.0F;
    }

    @Override
    protected float getHitboxPickInflation() {
        return 0.32F;
    }

    @Override
    protected double getHitboxVisualYOffset() {
        return 2.94D;
    }

    @Override
    protected double getHitboxPitchPivotOffsetY() {
        return 0.5D;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.isEffectiveAi() || this.level().isClientSide) {
            return;
        }

        switch (this.getState()) {
            case STATE_UNFURLING -> this.tickUnfurling();
            case STATE_GRABBING -> this.tickGrabbing();
            case STATE_RETRACTING -> this.tickRetracting();
            default -> this.tickDormant();
        }
    }

    private void tickDormant() {
        if (this.dormantCooldown > 0) {
            this.dormantCooldown--;
            return;
        }

        Player target = this.level().getNearestPlayer(this, TRIGGER_RANGE);
        if (target == null || target.isCreative() || target.isSpectator()) {
            return;
        }

        this.targetId = target.getId();
        this.stateTimer = 0;
        this.setState(STATE_UNFURLING);
        this.getNavigation().stop();

        // Turn the whole crown toward the diver that woke it.
        Vec3 toTarget = target.position().subtract(this.position());
        float yaw = (float) (Mth.atan2(toTarget.z, toTarget.x) * (180.0D / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 0.8F, 0.6F);
    }

    private void tickUnfurling() {
        this.setDeltaMovement(this.getDeltaMovement().scale(0.4D));
        this.stateTimer++;
        if (this.stateTimer >= UNFURL_TICKS) {
            this.stateTimer = 0;
            this.setState(STATE_GRABBING);
        }
    }

    private void tickGrabbing() {
        this.setDeltaMovement(Vec3.ZERO);
        this.hasImpulse = true;
        this.stateTimer++;

        Player target = this.targetId >= 0 && this.level().getEntity(this.targetId) instanceof Player player
                ? player
                : null;

        if (target != null && this.distanceTo(target) <= GRAB_RANGE && this.stateTimer % GRAB_TICK_INTERVAL == 0) {
            // Reel them in: pressure sickness plus a steady pull toward the core.
            target.addEffect(new MobEffectInstance(MobEffectRegistry.NARCOSIS, NARCOSIS_TICKS, 2));
            target.hurt(this.damageSources().mobAttack(this), GRAB_DAMAGE);

            Vec3 pull = this.position().add(0.0D, this.getBbHeight() * 0.4D, 0.0D)
                    .subtract(target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D))
                    .normalize()
                    .scale(PULL_STRENGTH);
            target.push(pull.x, pull.y * 0.5D, pull.z);
            target.hurtMarked = true;

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SCULK_SOUL, target.getX(),
                        target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 8, 0.3D, 0.3D, 0.3D, 0.01D);
            }
        }

        if (this.stateTimer >= GRAB_TICKS || target == null) {
            this.stateTimer = 0;
            this.setState(STATE_RETRACTING);
        }
    }

    private void tickRetracting() {
        this.stateTimer++;
        if (this.stateTimer >= RETRACT_TICKS) {
            this.stateTimer = 0;
            this.targetId = -1;
            this.dormantCooldown = DORMANT_COOLDOWN_TICKS;
            this.setState(STATE_DORMANT);
        }
    }
}
