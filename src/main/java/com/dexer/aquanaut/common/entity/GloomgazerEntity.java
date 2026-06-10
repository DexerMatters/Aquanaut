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

public class GloomgazerEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.eyeball_fish.swim");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GloomgazerEntity(EntityType<? extends WaterAnimal> type, Level level) { super(type, level); }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "ctrl", 0, s -> {
            s.getController().setAnimationSpeed(animSpeed(0.5, 1.2));
            return s.setAndContinue(SWIM);
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0D).add(Attributes.MOVEMENT_SPEED, 0.08D).build();
    }

    @Override protected FishResponseMode getResponseMode() { return FishResponseMode.AVOIDANCE; }
    @Override protected FishAttackMode getAttackMode() { return FishAttackMode.NONE; }
    @Override protected double getPlayerDetectionRange() { return 6.0D; }
    @Override protected double getCruiseAcceleration() { return 0.004D; }
    @Override protected double getCruiseMaxSpeed() { return 0.05D; }
    @Override protected double getEscapeAcceleration() { return 0.025D; }
    @Override protected double getEscapeMaxSpeed() { return 0.30D; }
    @Override protected double getWaterDrag() { return 0.87D; }
    @Override protected float getCruiseYawTurnRateDegrees() { return 0.6F; }
    @Override protected float getCruisePitchTurnRateDegrees() { return 0.5F; }
    @Override protected int getCruiseYawDecisionMinTicks() { return 60; }
    @Override protected int getCruiseYawDecisionRandomTicks() { return 50; }
    @Override protected int getCruisePitchDecisionMinTicks() { return 60; }
    @Override protected int getCruisePitchDecisionRandomTicks() { return 50; }
    @Override protected double getCruiseDepthRange() { return 3.0D; }
    @Override protected double getCruiseVerticalAssist() { return 0.008D; }
    @Override protected float getBodyTurnRateDegrees() { return 3.0F; }
    @Override protected float getMaxTiltDegrees() { return 24.0F; }
    @Override protected float getHitboxPickInflation() { return 0.2F; }
    @Override protected double getHitboxVisualYOffset() { return 0.0D; }
    @Override protected double getHitboxPitchPivotOffsetY() { return 0.15D; }
}
