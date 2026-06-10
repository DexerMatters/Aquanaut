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

public class OxygenBreederEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.oxygen_breeder.swim");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public OxygenBreederEntity(EntityType<? extends WaterAnimal> type, Level level) { super(type, level); }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "ctrl", 0, s -> {
            s.getController().setAnimationSpeed(animSpeed(0.12, 0.32));
            return s.setAndContinue(SWIM);
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D).add(Attributes.MOVEMENT_SPEED, 0.04D).build();
    }

    @Override protected FishResponseMode getResponseMode() { return FishResponseMode.PASSIVE; }
    @Override protected FishAttackMode getAttackMode() { return FishAttackMode.NONE; }
    @Override protected double getCruiseAcceleration() { return 0.0015D; }
    @Override protected double getCruiseMaxSpeed() { return 0.025D; }
    @Override protected double getWaterDrag() { return 0.90D; }
    @Override protected float getCruiseTurnChance() { return 0.0F; }
    @Override protected float getCruiseYawTurnRateDegrees() { return 0.0F; }
    @Override protected float getCruisePitchTurnRateDegrees() { return 0.0F; }
    @Override protected int getCruiseYawDecisionMinTicks() { return 1000; }
    @Override protected int getCruiseYawDecisionRandomTicks() { return 1; }
    @Override protected int getCruisePitchDecisionMinTicks() { return 1000; }
    @Override protected int getCruisePitchDecisionRandomTicks() { return 1; }
    @Override protected double getCruiseDepthRange() { return 0.0D; }
    @Override protected double getCruiseVerticalAssist() { return 0.0D; }
    @Override protected float getBodyTurnRateDegrees() { return 0.0F; }
    @Override protected float getMaxTiltDegrees() { return 0.0F; }
    @Override protected float getHitboxPickInflation() { return 0.1F; }
    @Override protected double getHitboxVisualYOffset() { return 0.0D; }
    @Override protected double getHitboxPitchPivotOffsetY() { return 0.0D; }
    @Override protected boolean getShouldApplyPitchRotation() { return false; }
}
