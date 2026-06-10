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
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class TripodEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.tentacle_fish.swim");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TripodEntity(EntityType<? extends WaterAnimal> type, Level level) { super(type, level); }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "ctrl", 0, s -> {
            s.getController().setAnimationSpeed(animSpeed(0.9, 2.4, 2.0, 3.5));
            return s.setAndContinue(SWIM);
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D).add(Attributes.MOVEMENT_SPEED, 0.18D)
                .add(Attributes.ATTACK_DAMAGE, 2.5D).build();
    }

    @Override protected FishResponseMode getResponseMode() { return FishResponseMode.CHARGE; }
    @Override protected FishAttackMode getAttackMode() { return FishAttackMode.TRACKING_BITE; }
    @Override protected double getPlayerDetectionRange() { return 12.0D; }
    @Override protected boolean getSchoolingEnabled() { return true; }
    @Override protected double getSchoolingSearchRadius() { return 12.0D; }
    @Override protected double getSchoolingSeparationRadius() { return 2.2D; }
    @Override protected double getSchoolingFollowDistance() { return 3.5D; }
    @Override protected double getCruiseAcceleration() { return 0.014D; }
    @Override protected double getCruiseMaxSpeed() { return 0.18D; }
    @Override protected double getChargeAcceleration() { return 0.055D; }
    @Override protected double getChargeMaxSpeed() { return 0.55D; }
    @Override protected double getWaterDrag() { return 0.85D; }
    @Override protected float getCruiseYawTurnRateDegrees() { return 3.5F; }
    @Override protected float getCruisePitchTurnRateDegrees() { return 2.6F; }
    @Override protected int getCruiseYawDecisionMinTicks() { return 20; }
    @Override protected int getCruiseYawDecisionRandomTicks() { return 16; }
    @Override protected int getCruisePitchDecisionMinTicks() { return 22; }
    @Override protected int getCruisePitchDecisionRandomTicks() { return 18; }
    @Override protected double getCruiseDepthRange() { return 5.0D; }
    @Override protected double getCruiseVerticalAssist() { return 0.022D; }
    @Override protected float getChargeTurnRateDegrees() { return 18.0F; }
    @Override protected float getBodyTurnRateDegrees() { return 7.0F; }
    @Override protected float getMaxTiltDegrees() { return 34.0F; }
    @Override protected float getHitboxPickInflation() { return 0.25F; }
    @Override protected double getHitboxVisualYOffset() { return 0.0D; }
    @Override protected double getHitboxPitchPivotOffsetY() { return 0.15D; }
    @Override protected int getChargeTargetLostResetTicks() { return 20; }
}
