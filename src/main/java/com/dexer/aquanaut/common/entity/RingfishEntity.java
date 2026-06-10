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

public class RingfishEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.ring_fish.swim");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public RingfishEntity(EntityType<? extends WaterAnimal> type, Level level) { super(type, level); }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "ctrl", 0, s -> {
            s.getController().setAnimationSpeed(animSpeed(0.8, 2.0, 1.8, 3.0));
            return s.setAndContinue(SWIM);
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D).add(Attributes.MOVEMENT_SPEED, 0.16D).build();
    }

    @Override protected FishResponseMode getResponseMode() { return FishResponseMode.AVOIDANCE; }
    @Override protected FishAttackMode getAttackMode() { return FishAttackMode.NONE; }
    @Override protected double getPlayerDetectionRange() { return 8.0D; }
    @Override protected double getCruiseAcceleration() { return 0.012D; }
    @Override protected double getCruiseMaxSpeed() { return 0.16D; }
    @Override protected double getEscapeAcceleration() { return 0.040D; }
    @Override protected double getEscapeMaxSpeed() { return 0.42D; }
    @Override protected double getWaterDrag() { return 0.84D; }
    @Override protected float getCruiseYawTurnRateDegrees() { return 3.2F; }
    @Override protected float getCruisePitchTurnRateDegrees() { return 2.4F; }
    @Override protected int getCruiseYawDecisionMinTicks() { return 22; }
    @Override protected int getCruiseYawDecisionRandomTicks() { return 18; }
    @Override protected int getCruisePitchDecisionMinTicks() { return 25; }
    @Override protected int getCruisePitchDecisionRandomTicks() { return 20; }
    @Override protected double getCruiseDepthRange() { return 4.5D; }
    @Override protected double getCruiseVerticalAssist() { return 0.020D; }
    @Override protected float getBodyTurnRateDegrees() { return 6.0F; }
    @Override protected float getMaxTiltDegrees() { return 32.0F; }
    @Override protected float getHitboxPickInflation() { return 0.15F; }
    @Override protected double getHitboxVisualYOffset() { return 0.0D; }
    @Override protected double getHitboxPitchPivotOffsetY() { return 0.15D; }
}
