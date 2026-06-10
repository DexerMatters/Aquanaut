package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.ai.FishAttackMode;
import com.dexer.aquanaut.common.ai.FishResponseMode;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class CatfishEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM_ANIMATION = RawAnimation.begin().thenLoop("swim");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CatfishEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            state.getController().setAnimationSpeed(animSpeed(0.6, 1.2, 1.5, 3.0));
            return state.setAndContinue(SWIM_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CAT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource damageSource) {
        return SoundEvents.CAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CAT_DEATH;
    }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .build();
    }

    @Override
    protected FishResponseMode getResponseMode() {
        return FishResponseMode.STRESS;
    }

    @Override
    protected FishAttackMode getAttackMode() {
        return FishAttackMode.NONE;
    }

    @Override
    protected double getCruiseAcceleration() {
        return 0.012D;
    }

    @Override
    protected double getCruiseMaxSpeed() {
        return 0.18D;
    }

    @Override
    protected double getWaterDrag() {
        return 0.88D;
    }

    @Override
    protected float getCruiseTurnChance() {
        return 0.14F;
    }

    @Override
    protected float getCruiseTurnRangeDegrees() {
        return 35.0F;
    }

    @Override
    protected float getCruiseYawTurnRateDegrees() {
        return 3.2F;
    }

    @Override
    protected float getCruisePitchTurnRateDegrees() {
        return 2.5F;
    }

    @Override
    protected double getCruiseVerticalAssist() {
        return 0.020D;
    }

    @Override
    protected float getBodyTurnRateDegrees() {
        return 10.0F;
    }

    @Override
    protected double getPlayerDetectionRange() {
        return 12.0D;
    }

    // --- Super-fast escape behavior ---

    @Override
    protected double getEscapeAcceleration() {
        return 0.075D;
    }

    @Override
    protected double getEscapeMaxSpeed() {
        return 1.2D;
    }

    @Override
    protected float getEscapeTurnRateDegrees() {
        return 16.0F;
    }

    @Override
    protected int getReactiveMemoryTicks() {
        return 140;
    }

    @Override
    protected int getSprintPropulsionIntervalTicks() {
        return 4;
    }

    @Override
    protected int getSprintPropulsionBurstTicks() {
        return 6;
    }

    @Override
    protected double getSprintPropulsionGlideAccelerationFactor() {
        return 1.2D;
    }

    @Override
    protected double getSprintPropulsionBurstAccelerationFactor() {
        return 2.5D;
    }

    @Override
    protected float getHitboxPickInflation() {
        return 0.20F;
    }

    @Override
    protected double getHitboxVisualYOffset() {
        return -0.45D;
    }
}
