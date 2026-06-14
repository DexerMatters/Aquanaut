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

public class FlatfishEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.flatfish.swim");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public FlatfishEntity(EntityType<? extends WaterAnimal> t, Level l) { super(t, l); }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "c", 0, s -> {
            s.getController().setAnimationSpeed(animSpeed(0.5, 1.1, 1.4, 2.2));
            return s.setAndContinue(SWIM);
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0D).add(Attributes.MOVEMENT_SPEED, 0.12D).build();
    }
    @Override protected FishResponseMode getResponseMode() { return FishResponseMode.IRRITATE; }
    @Override protected FishAttackMode getAttackMode() { return FishAttackMode.NONE; }
    @Override protected double getPlayerDetectionRange() { return 8.0D; }
    @Override protected double getCruiseAcceleration() { return 0.010D; }
    @Override protected double getCruiseMaxSpeed() { return 0.14D; }
    @Override protected double getEscapeAcceleration() { return 0.035D; }
    @Override protected double getEscapeMaxSpeed() { return 0.38D; }
    @Override protected double getWaterDrag() { return 0.86D; }
    @Override protected float getCruiseYawTurnRateDegrees() { return 2.4F; }
    @Override protected float getCruisePitchTurnRateDegrees() { return 1.8F; }
    @Override protected int getCruiseYawDecisionMinTicks() { return 35; }
    @Override protected int getCruiseYawDecisionRandomTicks() { return 30; }
    @Override protected int getCruisePitchDecisionMinTicks() { return 40; }
    @Override protected int getCruisePitchDecisionRandomTicks() { return 35; }
    @Override protected double getCruiseDepthRange() { return 4.0D; }
    @Override protected double getCruiseVerticalAssist() { return 0.014D; }
    @Override protected float getBodyTurnRateDegrees() { return 5.0F; }
    @Override protected float getMaxTiltDegrees() { return 28.0F; }
    @Override protected float getEscapeTurnRateDegrees() { return 14.0F; }
    @Override protected int getReactiveMemoryTicks() { return 120; }
    @Override protected float getHitboxPickInflation() { return 0.2F; }
    @Override protected double getHitboxVisualYOffset() { return -0.25D; }
    @Override protected double getHitboxPitchPivotOffsetY() { return 0.15D; }
}
