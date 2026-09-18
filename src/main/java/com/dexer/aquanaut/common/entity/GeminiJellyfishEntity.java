package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.AirSupplyHelper;
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
 * Gemini jellyfish — two bells, one body, and a light that changes allegiance.
 *
 * <p>
 * The twin nervous systems alternate on a fixed cycle, which is where the mod's gaze lore comes
 * from: in <em>favor</em> it drifts and breathes air back into nearby divers, in <em>devour</em> it
 * runs them down and lashes them with neurotoxin. The luminous core is the tell — bright means
 * favor.
 */
public class GeminiJellyfishEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM_ANIMATION = RawAnimation.begin().thenLoop("swim");

    private static final EntityDataAccessor<Boolean> DEVOURING = SynchedEntityData.defineId(
            GeminiJellyfishEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int CYCLE_TICKS = 400;
    private static final int EFFECT_INTERVAL = 40;
    private static final double FAVOR_RANGE = 8.0D;
    private static final double HUNT_RANGE = 16.0D;
    private static final double SWEEP_RADIUS = 3.0D;
    private static final float SWEEP_DAMAGE = 4.0F;
    private static final int NARCOSIS_TICKS = 60;
    private static final int REGENERATION_TICKS = 100;
    private static final int AIR_GIFT_TICKS = 40;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int cycleTimer;

    public GeminiJellyfishEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DEVOURING, false);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 3, state -> {
            state.getController().setAnimationSpeed(this.isDevouring()
                    ? animSpeed(0.6, 1.4, 1.4, 2.0)
                    : animSpeed(0.25, 0.7, 0.7, 1.1));
            return state.setAndContinue(SWIM_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 120.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .build();
    }

    public boolean isDevouring() {
        return this.entityData.get(DEVOURING);
    }

    private void setDevouring(boolean devouring) {
        this.entityData.set(DEVOURING, devouring);
    }

    @Override
    protected FishResponseMode getResponseMode() {
        return this.isDevouring() ? FishResponseMode.CHARGE : FishResponseMode.PASSIVE;
    }

    @Override
    protected FishAttackMode getAttackMode() {
        return FishAttackMode.NONE;
    }

    @Override
    protected double getCruiseAcceleration() {
        return 0.004D;
    }

    @Override
    protected double getCruiseMaxSpeed() {
        return 0.055D;
    }

    @Override
    protected double getChargeAcceleration() {
        return 0.016D;
    }

    @Override
    protected double getChargeMaxSpeed() {
        return 0.20D;
    }

    @Override
    protected double getEscapeAcceleration() {
        return 0.010D;
    }

    @Override
    protected double getEscapeMaxSpeed() {
        return 0.10D;
    }

    @Override
    protected double getWaterDrag() {
        return 0.92D;
    }

    @Override
    protected double getPlayerDetectionRange() {
        return HUNT_RANGE;
    }

    @Override
    protected float getCruiseYawTurnRateDegrees() {
        return 0.6F;
    }

    @Override
    protected float getCruisePitchTurnRateDegrees() {
        return 0.5F;
    }

    @Override
    protected float getBodyTurnRateDegrees() {
        return 2.0F;
    }

    @Override
    protected float getMaxTiltDegrees() {
        return 16.0F;
    }

    @Override
    protected float getHitboxPickInflation() {
        return 0.3F;
    }

    @Override
    protected double getHitboxVisualYOffset() {
        return 0.25D;
    }

    @Override
    protected double getHitboxPitchPivotOffsetY() {
        return 0.4D;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.isEffectiveAi() || this.level().isClientSide) {
            return;
        }

        this.cycleTimer++;
        if (this.cycleTimer >= CYCLE_TICKS) {
            this.cycleTimer = 0;
            boolean devouring = !this.isDevouring();
            this.setDevouring(devouring);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    devouring ? SoundEvents.CONDUIT_DEACTIVATE : SoundEvents.CONDUIT_ACTIVATE,
                    SoundSource.HOSTILE, 1.0F, 0.7F);
        }

        if (this.tickCount % EFFECT_INTERVAL != 0) {
            return;
        }

        if (this.isDevouring()) {
            this.sweepToxin();
        } else {
            this.grantFavor();
        }
    }

    private void grantFavor() {
        List<Player> nearby = this.level().getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(FAVOR_RANGE), player -> !player.isCreative() && !player.isSpectator());

        for (Player player : nearby) {
            AirSupplyHelper.addAir(player, AIR_GIFT_TICKS);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGENERATION_TICKS, 0, true, false));
        }

        if (!nearby.isEmpty() && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.5D, this.getZ(),
                    8, 1.2D, 1.2D, 1.2D, 0.01D);
        }
    }

    private void sweepToxin() {
        List<LivingEntity> victims = this.level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(SWEEP_RADIUS),
                entity -> entity != this && entity.isAlive() && !(entity instanceof GeminiJellyfishEntity));

        for (LivingEntity victim : victims) {
            victim.hurt(this.damageSources().mobAttack(this), SWEEP_DAMAGE);
            victim.addEffect(new MobEffectInstance(MobEffectRegistry.NARCOSIS, NARCOSIS_TICKS, 1));
        }

        if (!victims.isEmpty() && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 0.5D, this.getZ(),
                    20, SWEEP_RADIUS / 2.0D, 0.8D, SWEEP_RADIUS / 2.0D, 0.01D);
        }
    }
}
