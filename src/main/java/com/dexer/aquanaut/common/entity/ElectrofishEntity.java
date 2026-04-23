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

public class ElectrofishEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation FLOAT_ANIMATION = RawAnimation.begin().thenLoop("float");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ElectrofishEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            state.getController().setAnimationSpeed(this.getFloatAnimationSpeed());
            return state.setAndContinue(FLOAT_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.1D)
                .build();
    }

    protected double getFloatAnimationSpeed() {
        return 0.52D;
    }

    @Override
    protected FishResponseMode getResponseMode() {
        return FishResponseMode.PASSIVE;
    }

    @Override
    protected FishAttackMode getAttackMode() {
        return FishAttackMode.NONE;
    }

    @Override
    protected double getCruiseAcceleration() {
        return 0.003D;
    }

    @Override
    protected double getCruiseMaxSpeed() {
        return 0.052D;
    }

    @Override
    protected double getWaterDrag() {
        return 0.86D;
    }

    @Override
    protected float getCruiseTurnChance() {
        return 0.55F;
    }

    @Override
    protected float getCruiseTurnRangeDegrees() {
        return 56.0F;
    }

    @Override
    protected float getCruiseYawTurnRateDegrees() {
        return 0.45F;
    }

    @Override
    protected int getCruiseYawDecisionMinTicks() {
        return 90;
    }

    @Override
    protected int getCruiseYawDecisionRandomTicks() {
        return 80;
    }

    @Override
    protected float getCruisePitchTurnRateDegrees() {
        return 0.45F;
    }

    @Override
    protected int getCruisePitchDecisionMinTicks() {
        return 70;
    }

    @Override
    protected int getCruisePitchDecisionRandomTicks() {
        return 70;
    }

    @Override
    protected double getCruiseDepthRange() {
        return 4.2D;
    }

    @Override
    protected double getCruiseDepthPitchDistance() {
        return 3.2D;
    }

    @Override
    protected double getCruiseDepthEmergencyOffset() {
        return 2.4D;
    }

    @Override
    protected double getCruiseVerticalAssist() {
        return 0.008D;
    }

    @Override
    protected float getBodyTurnRateDegrees() {
        return 2.0F;
    }

    @Override
    protected float getMaxTiltDegrees() {
        return 28.0F;
    }

    @Override
    protected float getHitboxPickInflation() {
        return 0.35F;
    }

    @Override
    protected double getHitboxVisualYOffset() {
        return -0.45D;
    }

    @Override
    protected double getHitboxPitchPivotOffsetY() {
        return 0.25D;
    }
}
