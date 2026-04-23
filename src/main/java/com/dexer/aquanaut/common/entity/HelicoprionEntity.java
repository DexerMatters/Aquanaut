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

public class HelicoprionEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM_ANIMATION = RawAnimation.begin().thenLoop("swim");
    private static final RawAnimation OPEN_TO_SWIM_OPENED_ANIMATION = RawAnimation.begin()
            .thenPlay("mouth_close_to_open")
            .thenLoop("swim_mouth_opened");
    private static final RawAnimation CLOSE_TO_SWIM_ANIMATION = RawAnimation.begin()
            .thenPlay("mouth_open_to_close")
            .thenLoop("swim");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean mouthOpenedVisual;

    public HelicoprionEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            boolean charging = this.isChargingPlayer();
            state.getController()
                    .setAnimationSpeed(charging ? this.getChargeAnimationSpeed() : this.getSwimAnimationSpeed());

            if (charging) {
                if (!this.mouthOpenedVisual) {
                    this.mouthOpenedVisual = true;
                    state.resetCurrentAnimation();
                }

                return state.setAndContinue(OPEN_TO_SWIM_OPENED_ANIMATION);
            }

            if (this.mouthOpenedVisual) {
                this.mouthOpenedVisual = false;
                state.resetCurrentAnimation();
                return state.setAndContinue(CLOSE_TO_SWIM_ANIMATION);
            }

            return state.setAndContinue(SWIM_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 26.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .build();
    }

    protected double getSwimAnimationSpeed() {
        return 1.0D;
    }

    protected double getChargeAnimationSpeed() {
        return 1.45D;
    }

    @Override
    protected FishResponseMode getResponseMode() {
        return FishResponseMode.CHARGE;
    }

    @Override
    protected FishAttackMode getAttackMode() {
        return FishAttackMode.PASS_BY_BITE;
    }

    @Override
    protected boolean getCurvedCruiseMovement() {
        return true;
    }

    @Override
    protected float getCruiseCurveTorqueDegrees() {
        return 0.58F;
    }

    @Override
    protected double getCruiseAcceleration() {
        return 0.022D;
    }

    @Override
    protected double getCruiseMaxSpeed() {
        return 0.34D;
    }

    @Override
    protected double getEscapeAcceleration() {
        return 0.09D;
    }

    @Override
    protected double getEscapeMaxSpeed() {
        return 0.92D;
    }

    @Override
    protected double getChargeAcceleration() {
        return 0.11D;
    }

    @Override
    protected double getChargeMaxSpeed() {
        return 1.02D;
    }

    @Override
    protected double getWaterDrag() {
        return 0.9D;
    }

    @Override
    protected double getPlayerDetectionRange() {
        return 20.0D;
    }

    @Override
    protected float getEscapeTurnRateDegrees() {
        return 20.0F;
    }

    @Override
    protected float getChargeTurnRateDegrees() {
        return 22.5F;
    }

    @Override
    protected float getBodyTurnRateDegrees() {
        return 11.0F;
    }

    @Override
    protected float getMaxTiltDegrees() {
        return 26.0F;
    }

    @Override
    protected int getBiteCooldownTicks() {
        return 14;
    }

    @Override
    protected double getBiteReachBonus() {
        return 1.15D;
    }

    @Override
    protected double getBaseBiteDamage() {
        return 4.0D;
    }

    @Override
    protected int getPassByRetreatTicks() {
        return 30;
    }

    @Override
    protected int getPassByReengageCooldownTicks() {
        return 12;
    }

    @Override
    protected double getPassByInertiaBoost() {
        return 0.34D;
    }

    @Override
    protected double getPassByInertiaMaxSpeedMultiplier() {
        return 1.22D;
    }

    @Override
    protected double getChargeNoTurnDistance() {
        return 4.4D;
    }

    @Override
    protected double getPassByStrikeForwardDistance() {
        return 3.2D;
    }

    @Override
    protected double getPassByStrikeRearAllowance() {
        return 1.4D;
    }

    @Override
    protected double getPassByStrikeRadius() {
        return 1.25D;
    }

    @Override
    protected float getHitboxPickInflation() {
        return 0.95F;
    }

    @Override
    protected double getHitboxVisualYOffset() {
        return -0.525D;
    }
}
