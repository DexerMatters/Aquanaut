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

public final class AmbushFishEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("swim");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation BURST_ESCAPE = RawAnimation.begin().thenPlay("burst_escape");
    /** Below this horizontal speed the fish reads as lying in wait rather than cruising. */
    private static final double MOVE_SPEED_SQR = 4.0E-4D;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public AmbushFishEntity(EntityType<? extends WaterAnimal> type, Level level) { super(type, level); }
    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D).build();
    }
    @Override protected FishResponseMode getResponseMode() { return FishResponseMode.AVOIDANCE; }
    @Override protected double getPlayerDetectionRange() { return 1.0D; }
    @Override protected double getEscapeMaxSpeed() { return 0.62D; }
    @Override protected double getEscapeAcceleration() { return 0.065D; }
    @Override protected double getCruiseMaxSpeed() { return 0.08D; }
    @Override protected double getCruiseFloorBias() { return 0.95D; }
    @Override protected double getCruiseDepthRange() { return 1.5D; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (isEscapeLaunching() || isSprintingAway()) {
                return state.setAndContinue(BURST_ESCAPE);
            }
            return state.setAndContinue(
                    getDeltaMovement().horizontalDistanceSqr() > MOVE_SPEED_SQR ? SWIM : IDLE);
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
