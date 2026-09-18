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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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

import java.util.List;

/**
 * Ionfin — a suspended ion cell in a spinning cage.
 *
 * <p>
 * It grazes on charge: near a powered lightning generator it stays permanently hot, and it spins
 * up whenever a conductive diver comes close. The discharge is short, rude and never lethal on its
 * own — unlike the electrofish, it does not call lightning.
 */
public class IonfinEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM_ANIMATION = RawAnimation.begin().thenLoop("swim");

    private static final EntityDataAccessor<Byte> STATE = SynchedEntityData.defineId(
            IonfinEntity.class, EntityDataSerializers.BYTE);

    public static final byte STATE_IDLE = 0;
    public static final byte STATE_CHARGING = 1;
    public static final byte STATE_DISCHARGING = 2;

    private static final double DETECTION_RANGE = 6.0D;
    private static final int CHARGE_TICKS = 40;
    private static final int DISCHARGE_TICKS = 15;
    private static final int COOLDOWN_TICKS = 100;
    private static final double DISCHARGE_RADIUS = 2.5D;
    private static final float DISCHARGE_DAMAGE = 4.0F;
    private static final int WEAKNESS_TICKS = 100;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int stateTimer;
    private int cooldown;

    public IonfinEntity(EntityType<? extends WaterAnimal> type, Level level) {
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
            if (this.getState() != STATE_IDLE) {
                state.getController().setAnimationSpeed(this.getState() == STATE_DISCHARGING ? 2.4D : 1.4D);
            } else {
                state.getController().setAnimationSpeed(animSpeed(0.5, 1.1, 1.2, 1.9));
            }
            return state.setAndContinue(SWIM_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
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
        return 0.014D;
    }

    @Override
    protected double getCruiseMaxSpeed() {
        return 0.17D;
    }

    @Override
    protected double getEscapeAcceleration() {
        return 0.040D;
    }

    @Override
    protected double getEscapeMaxSpeed() {
        return 0.40D;
    }

    @Override
    protected double getWaterDrag() {
        return 0.88D;
    }

    @Override
    protected double getPlayerDetectionRange() {
        return DETECTION_RANGE;
    }

    @Override
    protected boolean getCurvedCruiseMovement() {
        return true;
    }

    @Override
    protected float getCruiseCurveTorqueDegrees() {
        return 1.1F;
    }

    @Override
    protected float getBodyTurnRateDegrees() {
        return 7.0F;
    }

    @Override
    protected float getMaxTiltDegrees() {
        return 34.0F;
    }

    @Override
    protected float getHitboxPickInflation() {
        return 0.24F;
    }

    @Override
    protected double getHitboxVisualYOffset() {
        return 0.75D;
    }

    @Override
    protected double getHitboxPitchPivotOffsetY() {
        return 0.35D;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.isEffectiveAi() || this.level().isClientSide) {
            return;
        }

        switch (this.getState()) {
            case STATE_CHARGING -> this.tickCharging();
            case STATE_DISCHARGING -> this.tickDischarging();
            default -> this.tickIdle();
        }
    }

    private void tickIdle() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return;
        }

        Player target = this.level().getNearestPlayer(this, DETECTION_RANGE);
        if (target == null || target.isCreative() || target.isSpectator()) {
            return;
        }

        this.setState(STATE_CHARGING);
        this.stateTimer = 0;
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 0.5F, 1.9F);
    }

    private void tickCharging() {
        this.stateTimer++;
        if (this.tickCount % 5 == 0 && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 0.5D, this.getZ(),
                    4, 0.3D, 0.3D, 0.3D, 0.02D);
        }

        if (this.stateTimer >= CHARGE_TICKS) {
            this.discharge();
            this.setState(STATE_DISCHARGING);
            this.stateTimer = 0;
        }
    }

    private void discharge() {
        List<LivingEntity> victims = this.level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(DISCHARGE_RADIUS), entity -> entity != this && entity.isAlive());

        for (LivingEntity victim : victims) {
            if (victim instanceof IonfinEntity) {
                continue;
            }
            victim.hurt(this.damageSources().mobAttack(this), DISCHARGE_DAMAGE);
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_TICKS, 0));
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 0.5D, this.getZ(),
                    30, DISCHARGE_RADIUS / 2.0D, 0.4D, DISCHARGE_RADIUS / 2.0D, 0.08D);
        }
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 0.6F, 1.7F);
    }

    private void tickDischarging() {
        this.stateTimer++;
        if (this.stateTimer >= DISCHARGE_TICKS) {
            this.setState(STATE_IDLE);
            this.stateTimer = 0;
            this.cooldown = COOLDOWN_TICKS;
        }
    }
}
