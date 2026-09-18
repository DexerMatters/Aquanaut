package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.ai.FishAttackMode;
import com.dexer.aquanaut.common.ai.FishResponseMode;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Shared rig for the three carp variants shipped with models.zip.
 *
 * <p>
 * Golden, silver and skeleton carp are the same Blockbench model with a different atlas and
 * behaviour, so the swimming school, the movement envelope and the swing of the tail live here
 * and each variant only adds its own trait.
 */
public abstract class AbstractCarpEntity extends BaseFishEntity implements GeoEntity {
    protected static final RawAnimation SWIM_ANIMATION = RawAnimation.begin().thenLoop("swim");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected AbstractCarpEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 2, state -> {
            state.getController().setAnimationSpeed(animSpeed(0.55, 1.15, 1.3, 2.1));
            return state.setAndContinue(SWIM_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    protected FishResponseMode getResponseMode() {
        return FishResponseMode.AVOIDANCE;
    }

    @Override
    protected FishAttackMode getAttackMode() {
        return FishAttackMode.NONE;
    }

    @Override
    protected boolean getSchoolingEnabled() {
        return true;
    }

    @Override
    protected double getSchoolingSearchRadius() {
        return 10.0D;
    }

    @Override
    protected double getSchoolingSeparationRadius() {
        return 1.35D;
    }

    @Override
    protected double getSchoolingFollowDistance() {
        return 2.0D;
    }

    @Override
    protected boolean getCurvedCruiseMovement() {
        return true;
    }

    @Override
    protected float getCruiseCurveTorqueDegrees() {
        return 0.55F;
    }

    @Override
    protected double getCruiseAcceleration() {
        return 0.020D;
    }

    @Override
    protected double getCruiseMaxSpeed() {
        return 0.24D;
    }

    @Override
    protected double getEscapeAcceleration() {
        return 0.050D;
    }

    @Override
    protected double getEscapeMaxSpeed() {
        return 0.55D;
    }

    @Override
    protected double getPlayerDetectionRange() {
        return 9.0D;
    }

    @Override
    protected double getWaterDrag() {
        return 0.90D;
    }

    @Override
    protected float getEscapeTurnRateDegrees() {
        return 17.0F;
    }

    @Override
    protected float getBodyTurnRateDegrees() {
        return 9.0F;
    }

    @Override
    protected float getMaxTiltDegrees() {
        return 22.0F;
    }

    @Override
    protected float getHitboxPickInflation() {
        return 0.18F;
    }

    @Override
    protected double getHitboxVisualYOffset() {
        return 0.16D;
    }

    @Override
    protected double getHitboxPitchPivotOffsetY() {
        return 0.10D;
    }
}
