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

public class AnglerfishEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM_ANIMATION = RawAnimation.begin().thenLoop("swim");
    private static final RawAnimation CHARGE_ANIMATION = RawAnimation.begin().thenLoop("charge");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public AnglerfishEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (this.isChargingPlayer()) {
                state.getController().setAnimationSpeed(this.getChargeAnimationSpeed());
                return state.setAndContinue(CHARGE_ANIMATION);
            }

            state.getController().setAnimationSpeed(this.getSwimAnimationSpeed());
            return state.setAndContinue(SWIM_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.ATTACK_DAMAGE, 3.5D)
                .build();
    }

    protected double getSwimAnimationSpeed() {
        return 1.0D;
    }

    protected double getChargeAnimationSpeed() {
        return 1.35D;
    }

    @Override
    protected FishResponseMode getResponseMode() {
        return FishResponseMode.CHARGE;
    }

    @Override
    protected FishAttackMode getAttackMode() {
        return FishAttackMode.TRACKING_BITE;
    }

    @Override
    protected double getPlayerDetectionRange() {
        return 16.0D;
    }

    @Override
    protected int getChargeTargetLostResetTicks() {
        return 24;
    }

    @Override
    protected int getBiteCooldownTicks() {
        return 8;
    }

    @Override
    protected double getCruiseAcceleration() {
        return 0.015D;
    }

    @Override
    protected double getCruiseMaxSpeed() {
        return 0.2D;
    }

    @Override
    protected double getChargeAcceleration() {
        return 0.053D;
    }

    @Override
    protected double getChargeMaxSpeed() {
        return 0.58D;
    }

    @Override
    protected double getWaterDrag() {
        return 0.9D;
    }

    @Override
    protected float getCruiseYawTurnRateDegrees() {
        return 2.6F;
    }

    @Override
    protected float getCruisePitchTurnRateDegrees() {
        return 1.7F;
    }

    @Override
    protected float getChargeTurnRateDegrees() {
        return 15.0F;
    }

    @Override
    protected float getBodyTurnRateDegrees() {
        return 7.8F;
    }

    @Override
    protected float getMaxTiltDegrees() {
        return 32.0F;
    }

    @Override
    protected double getCruiseVerticalAssist() {
        return 0.02D;
    }

    @Override
    protected int getCruiseYawDecisionMinTicks() {
        return 26;
    }

    @Override
    protected int getCruiseYawDecisionRandomTicks() {
        return 34;
    }

    @Override
    protected int getCruisePitchDecisionMinTicks() {
        return 34;
    }

    @Override
    protected int getCruisePitchDecisionRandomTicks() {
        return 40;
    }

    @Override
    protected double getCruiseDepthRange() {
        return 5.2D;
    }

    @Override
    protected double getCruiseDepthPitchDistance() {
        return 2.4D;
    }

    @Override
    protected double getCruiseDepthEmergencyOffset() {
        return 2.9D;
    }

    @Override
    protected int getCollisionTurnCooldownTicks() {
        return 9;
    }

    @Override
    protected float getHitboxPickInflation() {
        return 0.5F;
    }

    @Override
    protected double getHitboxVisualYOffset() {
        return -1.0D;
    }
}
