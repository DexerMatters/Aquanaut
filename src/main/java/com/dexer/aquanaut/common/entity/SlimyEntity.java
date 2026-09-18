package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.ai.FishAttackMode;
import com.dexer.aquanaut.common.ai.FishResponseMode;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Slimy — a bag of lubricant with fins.
 *
 * <p>
 * Almost no drag and a powerful, irregular propulsion burst make it rebound off terrain instead of
 * swimming around it. It is harmless, but anything it bumps into is left slicked and slow, and it
 * sheds slime when struck.
 */
public class SlimyEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM_ANIMATION = RawAnimation.begin().thenLoop("swim");

    private static final double STICKY_RANGE = 0.85D;
    private static final int SLOWNESS_TICKS = 80;
    private static final float SLIME_SHED_CHANCE = 0.25F;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int stickCooldown;

    public SlimyEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 2, state -> {
            state.getController().setAnimationSpeed(animSpeed(0.6, 1.3, 1.1, 1.9));
            return state.setAndContinue(SWIM_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .build();
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
        return 0.030D;
    }

    @Override
    protected double getCruiseMaxSpeed() {
        return 0.20D;
    }

    @Override
    protected double getEscapeAcceleration() {
        return 0.060D;
    }

    @Override
    protected double getEscapeMaxSpeed() {
        return 0.50D;
    }

    @Override
    protected double getWaterDrag() {
        return 0.995D;
    }

    @Override
    protected int getCruisePropulsionIntervalTicks() {
        return 22;
    }

    @Override
    protected int getCruisePropulsionBurstTicks() {
        return 6;
    }

    @Override
    protected double getCruisePropulsionGlideAccelerationFactor() {
        return 0.15D;
    }

    @Override
    protected double getCruisePropulsionBurstAccelerationFactor() {
        return 2.6D;
    }

    @Override
    protected float getCruiseTurnChance() {
        return 0.12F;
    }

    @Override
    protected float getCruiseTurnRangeDegrees() {
        return 60.0F;
    }

    @Override
    protected float getCruiseYawTurnRateDegrees() {
        return 3.0F;
    }

    @Override
    protected float getBodyTurnRateDegrees() {
        return 12.0F;
    }

    @Override
    protected float getMaxTiltDegrees() {
        return 40.0F;
    }

    @Override
    protected float getHitboxPickInflation() {
        return 0.22F;
    }

    @Override
    protected double getHitboxVisualYOffset() {
        return 0.38D;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.isEffectiveAi() || this.level().isClientSide) {
            return;
        }

        if (this.stickCooldown > 0) {
            this.stickCooldown--;
            return;
        }

        Player player = this.level().getNearestPlayer(this, STICKY_RANGE);
        if (player == null || player.isCreative() || player.isSpectator()) {
            return;
        }

        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOWNESS_TICKS, 0));
        this.stickCooldown = SLOWNESS_TICKS;
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.SLIME_BLOCK_STEP, SoundSource.NEUTRAL, 0.7F, 1.2F);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ITEM_SLIME, this.getX(), this.getY() + 0.3D, this.getZ(),
                    8, 0.2D, 0.15D, 0.2D, 0.01D);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && this.random.nextFloat() < SLIME_SHED_CHANCE) {
            ItemStack slime = new ItemStack(Items.SLIME_BALL);
            ItemEntity drop = new ItemEntity(this.level(), this.getX(),
                    this.getY() + this.getBbHeight() * 0.5D, this.getZ(), slime);
            this.level().addFreshEntity(drop);
        }
        return hurt;
    }
}
