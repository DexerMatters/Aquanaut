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

public class OctopusEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM_ANIMATION = RawAnimation.begin().thenLoop("animation.octopus.swim");
    private static final RawAnimation SPRINT_ANIMATION = RawAnimation.begin().thenLoop("animation.octopus.sprint");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public OctopusEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (this.isSprintingAway()) {
                return state.setAndContinue(SPRINT_ANIMATION);
            }
            return state.setAndContinue(SWIM_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .build();
    }

    @Override
    protected FishResponseMode getResponseMode() {
        return FishResponseMode.STRESS;
    }

    @Override
    protected double getCruiseAcceleration() {
        return 0.014D;
    }

    @Override
    protected double getCruiseMaxSpeed() {
        return 0.18D;
    }

    @Override
    protected double getEscapeAcceleration() {
        return 0.04D;
    }

    @Override
    protected double getEscapeMaxSpeed() {
        return 0.42D;
    }

    @Override
    protected double getPlayerDetectionRange() {
        return 12.0D;
    }

    @Override
    protected int getCruisePropulsionIntervalTicks() {
        return 40;
    }

    @Override
    protected int getCruisePropulsionBurstTicks() {
        return 13;
    }

    @Override
    protected double getCruisePropulsionGlideAccelerationFactor() {
        return 0.28D;
    }

    @Override
    protected double getCruisePropulsionBurstAccelerationFactor() {
        return 1.45D;
    }

    @Override
    protected int getSprintPropulsionIntervalTicks() {
        return 20;
    }

    @Override
    protected int getSprintPropulsionBurstTicks() {
        return 7;
    }

    @Override
    protected double getSprintPropulsionGlideAccelerationFactor() {
        return 0.4D;
    }

    @Override
    protected double getSprintPropulsionBurstAccelerationFactor() {
        return 1.9D;
    }

    @Override
    protected float getBodyTurnRateDegrees() {
        return 7.5F;
    }

    @Override
    protected float getHitboxPickInflation() {
        return 0.40F;
    }

    @Override
    protected double getHitboxVisualYOffset() {
        return 0.75D;
    }
}
