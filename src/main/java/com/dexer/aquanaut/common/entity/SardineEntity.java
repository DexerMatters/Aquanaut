package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.ai.FishResponseMode;
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

public class SardineEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM_ANIMATION = RawAnimation.begin().thenLoop("swim");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SardineEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            state.getController().setAnimationSpeed(
                    this.isSprintingAway() ? this.getSprintAnimationSpeed() : this.getCruiseAnimationSpeed());
            return state.setAndContinue(SWIM_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .build();
    }

    @Override
    protected FishResponseMode getResponseMode() {
        return FishResponseMode.AVOIDANCE;
    }

    @Override
    protected boolean getSchoolingEnabled() {
        return true;
    }

    @Override
    protected boolean getCurvedCruiseMovement() {
        return true;
    }

    @Override
    protected float getCruiseCurveTorqueDegrees() {
        return 0.82F;
    }

    protected double getCruiseAnimationSpeed() {
        return 1.12D;
    }

    protected double getSprintAnimationSpeed() {
        return 2.05D;
    }

    @Override
    protected double getCruiseAcceleration() {
        return 0.022D;
    }

    @Override
    protected double getCruiseMaxSpeed() {
        return 0.29D;
    }

    @Override
    protected double getEscapeAcceleration() {
        return 0.065D;
    }

    @Override
    protected double getEscapeMaxSpeed() {
        return 0.64D;
    }

    @Override
    protected double getPlayerDetectionRange() {
        return 11.5D;
    }

    @Override
    protected float getEscapeTurnRateDegrees() {
        return 18.0F;
    }

    @Override
    protected float getBodyTurnRateDegrees() {
        return 10.0F;
    }

    @Override
    protected float getMaxTiltDegrees() {
        return 30.0F;
    }

    @Override
    protected float getHitboxPickInflation() {
        return 0.2F;
    }

    @Override
    protected double getHitboxVisualYOffset() {
        return -0.54D;
    }
}
