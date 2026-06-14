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

public class BlueJellyfishEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.blue_jellyfish.swim");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public BlueJellyfishEntity(EntityType<? extends WaterAnimal> t, Level l) { super(t, l); }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "c", 0, s -> {
            s.getController().setAnimationSpeed(animSpeed(0.15, 0.35));
            return s.setAndContinue(SWIM);
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0D).add(Attributes.MOVEMENT_SPEED, 0.05D).build();
    }
    @Override protected FishResponseMode getResponseMode() { return FishResponseMode.PASSIVE; }
    @Override protected FishAttackMode getAttackMode() { return FishAttackMode.NONE; }
    @Override protected double getCruiseAcceleration() { return 0.002D; }
    @Override protected double getCruiseMaxSpeed() { return 0.035D; }
    @Override protected double getWaterDrag() { return 0.90D; }
    @Override protected float getCruiseYawTurnRateDegrees() { return 0.4F; }
    @Override protected float getCruisePitchTurnRateDegrees() { return 0.3F; }
    @Override protected int getCruiseYawDecisionMinTicks() { return 120; }
    @Override protected int getCruiseYawDecisionRandomTicks() { return 80; }
    @Override protected int getCruisePitchDecisionMinTicks() { return 100; }
    @Override protected int getCruisePitchDecisionRandomTicks() { return 80; }
    @Override protected double getCruiseDepthRange() { return 2.5D; }
    @Override protected double getCruiseVerticalAssist() { return 0.005D; }
    @Override protected float getBodyTurnRateDegrees() { return 1.5F; }
    @Override protected float getMaxTiltDegrees() { return 18.0F; }
    @Override protected float getHitboxPickInflation() { return 0.1F; }
    @Override protected double getHitboxVisualYOffset() { return -1.0D; }
    @Override protected double getHitboxPitchPivotOffsetY() { return 0.25D; }
}
