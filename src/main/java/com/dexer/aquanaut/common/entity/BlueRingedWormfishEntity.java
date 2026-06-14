package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.ai.FishAttackMode;
import com.dexer.aquanaut.common.ai.FishResponseMode;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class BlueRingedWormfishEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.blue_ringed_fish.swim");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public BlueRingedWormfishEntity(EntityType<? extends WaterAnimal> t, Level l) { super(t, l); }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "c", 0, s -> {
            s.getController().setAnimationSpeed(animSpeed(0.7, 1.5, 1.8, 3.2));
            return s.setAndContinue(SWIM);
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0D).add(Attributes.MOVEMENT_SPEED, 0.15D).build();
    }
    @Override protected FishResponseMode getResponseMode() { return FishResponseMode.AVOIDANCE; }
    @Override protected FishAttackMode getAttackMode() { return FishAttackMode.NONE; }
    @Override protected boolean getSchoolingEnabled() { return true; }
    @Override protected double getSchoolingSeparationRadius() { return 1.8D; }
    @Override protected double getSchoolingFollowDistance() { return 2.8D; }
    @Override protected double getPlayerDetectionRange() { return 8.0D; }
    @Override protected double getCruiseAcceleration() { return 0.016D; }
    @Override protected double getCruiseMaxSpeed() { return 0.20D; }
    @Override protected double getEscapeAcceleration() { return 0.045D; }
    @Override protected double getEscapeMaxSpeed() { return 0.48D; }
    @Override protected double getWaterDrag() { return 0.84D; }
    @Override protected float getCruiseYawTurnRateDegrees() { return 3.0F; }
    @Override protected float getCruisePitchTurnRateDegrees() { return 2.2F; }
    @Override protected int getCruiseYawDecisionMinTicks() { return 26; }
    @Override protected int getCruiseYawDecisionRandomTicks() { return 22; }
    @Override protected int getCruisePitchDecisionMinTicks() { return 30; }
    @Override protected int getCruisePitchDecisionRandomTicks() { return 24; }
    @Override protected double getCruiseDepthRange() { return 4.5D; }
    @Override protected double getCruiseVerticalAssist() { return 0.020D; }
    @Override protected float getBodyTurnRateDegrees() { return 6.0F; }
    @Override protected float getMaxTiltDegrees() { return 32.0F; }
    @Override protected float getEscapeTurnRateDegrees() { return 15.0F; }
    @Override protected int getReactiveMemoryTicks() { return 100; }
    @Override protected float getHitboxPickInflation() { return 0.2F; }
    @Override protected double getHitboxVisualYOffset() { return -0.3D; }
    @Override protected double getHitboxPitchPivotOffsetY() { return 0.15D; }
}
