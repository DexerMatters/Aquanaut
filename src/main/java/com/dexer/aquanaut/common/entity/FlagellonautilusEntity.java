package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.ai.FishAttackMode;
import com.dexer.aquanaut.common.ai.FishResponseMode;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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

import java.util.List;

/**
 * Flagellonautilus — a nautilus whose shell is a spring.
 *
 * <p>
 * It drifts with the current until something comes within reach, then unfurls all forty-five shell
 * segments into a whip, sweeps the water around it and jets clear on its siphon. The retreat is the
 * point: it never stays to trade blows.
 */
public class FlagellonautilusEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM_ANIMATION = RawAnimation.begin().thenLoop("swim");
    private static final RawAnimation ATTACK_ANIMATION = RawAnimation.begin().thenPlay("attack");

    private static final EntityDataAccessor<Byte> STATE = SynchedEntityData.defineId(
            FlagellonautilusEntity.class, EntityDataSerializers.BYTE);

    public static final byte STATE_IDLE = 0;
    public static final byte STATE_ATTACK = 1;
    public static final byte STATE_RETREAT = 2;

    private static final double TRIGGER_RANGE = 4.0D;
    private static final int ATTACK_TICKS = 25;
    private static final int STRIKE_TICK = 12;
    private static final int RETREAT_TICKS = 60;
    private static final int IDLE_COOLDOWN_TICKS = 60;
    private static final double SWEEP_RADIUS = 2.5D;
    private static final float SWEEP_DAMAGE = 6.0F;
    private static final double RETREAT_SPEED = 0.42D;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int stateTimer;
    private int idleCooldown;
    private boolean struck;
    private int targetId = -1;

    public FlagellonautilusEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, STATE_IDLE);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            return switch (this.getState()) {
                case STATE_ATTACK -> {
                    state.getController().setAnimationSpeed(1.0D);
                    yield state.setAndContinue(ATTACK_ANIMATION);
                }
                case STATE_RETREAT -> {
                    state.getController().setAnimationSpeed(1.7D);
                    yield state.setAndContinue(SWIM_ANIMATION);
                }
                default -> {
                    state.getController().setAnimationSpeed(0.5D);
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
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
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

    /**
     * The nautilus is a jet swimmer: it travels shell-first with the tentacles trailing, and the whip
     * it strikes with is part of that shell, so the strike is what has to be turned onto the victim.
     */
    @Override
    protected boolean getSwimsTailFirst() {
        return true;
    }

    @Override
    protected boolean getIsFacingThreat() {
        return this.getState() == STATE_ATTACK;
    }

    @Override
    protected double getCruiseAcceleration() {
        return 0.006D;
    }

    @Override
    protected double getCruiseMaxSpeed() {
        return 0.075D;
    }

    @Override
    protected double getEscapeAcceleration() {
        return 0.030D;
    }

    @Override
    protected double getEscapeMaxSpeed() {
        return 0.34D;
    }

    @Override
    protected double getWaterDrag() {
        return 0.88D;
    }

    @Override
    protected double getPlayerDetectionRange() {
        return TRIGGER_RANGE;
    }

    @Override
    protected float getCruiseYawTurnRateDegrees() {
        return 1.2F;
    }

    @Override
    protected float getCruisePitchTurnRateDegrees() {
        return 0.9F;
    }

    @Override
    protected int getCruiseYawDecisionMinTicks() {
        return 80;
    }

    @Override
    protected int getCruiseYawDecisionRandomTicks() {
        return 60;
    }

    @Override
    protected double getCruiseDepthRange() {
        return 3.0D;
    }

    @Override
    protected float getBodyTurnRateDegrees() {
        return 5.0F;
    }

    @Override
    protected float getMaxTiltDegrees() {
        return 24.0F;
    }

    @Override
    protected float getHitboxPickInflation() {
        return 0.22F;
    }

    @Override
    protected double getHitboxVisualYOffset() {
        return -0.04D;
    }

    @Override
    protected double getHitboxPitchPivotOffsetY() {
        return 0.2D;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.isEffectiveAi() || this.isVehicle()) {
            return;
        }

        if (this.level().isClientSide) {
            return;
        }

        switch (this.getState()) {
            case STATE_ATTACK -> this.tickAttack();
            case STATE_RETREAT -> this.tickRetreat();
            default -> this.tickIdle();
        }
    }

    private void tickIdle() {
        if (this.idleCooldown > 0) {
            this.idleCooldown--;
            return;
        }

        Player target = this.level().getNearestPlayer(this, TRIGGER_RANGE);
        if (target == null || target.isCreative() || target.isSpectator()) {
            return;
        }

        this.targetId = target.getId();
        this.stateTimer = 0;
        this.struck = false;
        this.setState(STATE_ATTACK);
        this.faceTowards(target.position().subtract(this.position()));
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
    }

    private void tickAttack() {
        this.setDeltaMovement(Vec3.ZERO);
        this.hasImpulse = true;
        this.stateTimer++;

        // The strike's half turn happens relative to the heading, so the heading has to stay pinned
        // on the victim for the whole swing: the cruise controller is still ticking underneath and
        // would otherwise steer the nautilus away mid-strike.
        Player target = this.targetPlayer();
        if (target != null) {
            this.faceTowards(target.position().subtract(this.position()));
        }

        if (!this.struck && this.stateTimer >= STRIKE_TICK) {
            this.struck = true;
            this.sweep();
        }

        if (this.stateTimer >= ATTACK_TICKS) {
            this.setState(STATE_RETREAT);
            this.stateTimer = 0;
        }
    }

    private void sweep() {
        List<LivingEntity> victims = this.level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(SWEEP_RADIUS), entity -> entity != this && entity.isAlive());

        for (LivingEntity victim : victims) {
            if (victim instanceof FlagellonautilusEntity) {
                continue;
            }
            victim.hurt(this.damageSources().mobAttack(this), SWEEP_DAMAGE);
            Vec3 push = victim.position().subtract(this.position()).normalize().scale(0.7D);
            victim.push(push.x, 0.25D, push.z);
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX(), this.getY() + 0.4D, this.getZ(),
                    6, SWEEP_RADIUS / 2.0D, 0.2D, SWEEP_RADIUS / 2.0D, 0.0D);
        }
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 0.8F);
    }

    private void tickRetreat() {
        this.stateTimer++;

        Player target = this.targetPlayer();
        Vec3 away = target != null
                ? this.position().subtract(target.position()).normalize()
                : new Vec3(0.0D, 0.0D, 1.0D).yRot(-this.getYRot() * ((float) Math.PI / 180.0F));

        // Point the body along the escape route. The renderer draws a moving nautilus shell-first,
        // so a heading still aimed at the player from the attack would put the snout in front and
        // make the retreat look like it is swimming back into the fight instead of jetting away.
        this.faceTowards(away);
        this.setDeltaMovement(away.scale(RETREAT_SPEED));
        this.hasImpulse = true;

        if (this.stateTimer >= RETREAT_TICKS) {
            this.setState(STATE_IDLE);
            this.stateTimer = 0;
            this.idleCooldown = IDLE_COOLDOWN_TICKS;
            this.targetId = -1;
        }
    }

    private Player targetPlayer() {
        return this.targetId >= 0 && this.level().getEntity(this.targetId) instanceof Player player
                ? player
                : null;
    }

    /**
     * Points the body and the head along a direction, using the same yaw and pitch convention as the
     * rest of the fish AI ({@code yawFromDirection}), so the renderer's tail-first flip always reads
     * the same way.
     */
    private void faceTowards(Vec3 direction) {
        if (direction.lengthSqr() < 1.0E-6D) {
            return;
        }
        Vec3 unit = direction.normalize();
        float yaw = (float) (Mth.atan2(unit.z, unit.x) * (180.0D / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        this.setXRot(Mth.clamp(
                (float) (-Mth.atan2(unit.y, unit.horizontalDistance()) * (180.0D / Math.PI)),
                -this.getMaxTiltDegrees(), this.getMaxTiltDegrees()));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && this.getState() != STATE_ATTACK) {
            this.targetId = source.getEntity() == null ? -1 : source.getEntity().getId();
            this.setState(STATE_RETREAT);
            this.stateTimer = 0;
        }
        return hurt;
    }
}
