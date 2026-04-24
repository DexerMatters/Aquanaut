package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.ai.FishAttackMode;
import com.dexer.aquanaut.common.ai.FishResponseMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class DonutfishEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM_ANIMATION = RawAnimation.begin().thenLoop("swim");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** Absorption search radius in blocks. */
    private static final double ABSORB_RADIUS = 2.5;

    public DonutfishEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
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
                .add(Attributes.MAX_HEALTH, 7.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.08D)
                .build();
    }

    protected double getSwimAnimationSpeed() {
        return 0.42D;
    }

    @Override
    public void tick() {
        super.tick();

        // Only run on server, and only if the hole is currently empty
        if (this.level().isClientSide || this.getPassengers().stream().anyMatch(p -> p instanceof AirBubbleEntity)) {
            return;
        }

        AABB searchBox = this.getBoundingBox().inflate(ABSORB_RADIUS);
        List<AirBubbleEntity> nearby = this.level().getEntitiesOfClass(
                AirBubbleEntity.class, searchBox,
                bubble -> !bubble.isVehicle() && bubble.getVehicle() == null && !bubble.isBursting());

        if (!nearby.isEmpty()) {
            // Absorb the closest one
            AirBubbleEntity closest = nearby.stream()
                    .min((a, b) -> Double.compare(a.distanceToSqr(this), b.distanceToSqr(this)))
                    .get();
            closest.startRiding(this, true);
        }
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
        return 0.0036D;
    }

    @Override
    protected double getCruiseMaxSpeed() {
        return 0.046D;
    }

    @Override
    protected double getWaterDrag() {
        return 0.9D;
    }

    @Override
    protected float getCruiseTurnChance() {
        return 0.08F;
    }

    @Override
    protected float getCruiseTurnRangeDegrees() {
        return 18.0F;
    }

    @Override
    protected float getCruiseYawTurnRateDegrees() {
        return 0.55F;
    }

    @Override
    protected int getCruiseYawDecisionMinTicks() {
        return 110;
    }

    @Override
    protected int getCruiseYawDecisionRandomTicks() {
        return 90;
    }

    @Override
    protected float getCruisePitchTurnRateDegrees() {
        return 0.35F;
    }

    @Override
    protected int getCruisePitchDecisionMinTicks() {
        return 140;
    }

    @Override
    protected int getCruisePitchDecisionRandomTicks() {
        return 120;
    }

    @Override
    protected double getCruiseDepthRange() {
        return 1.6D;
    }

    @Override
    protected double getCruiseDepthPitchDistance() {
        return 2.4D;
    }

    @Override
    protected double getCruiseDepthEmergencyOffset() {
        return 1.4D;
    }

    @Override
    protected double getCruiseVerticalAssist() {
        return 0.004D;
    }

    @Override
    protected float getBodyTurnRateDegrees() {
        return 1.8F;
    }

    @Override
    protected float getMaxTiltDegrees() {
        return 18.0F;
    }

    @Override
    protected float getHitboxPickInflation() {
        return 0.02F;
    }

    @Override
    protected double getHitboxVisualYOffset() {
        return -0.10D;
    }

    /**
     * Position an AirBubbleEntity at the donut hole center.
     * The inner hole is centered at model Y=16 px (= 1.0 block above the entity's
     * feet)
     * and at x=0, z=0 (model space center).
     */
    @Override
    public void positionRider(Entity passenger, MoveFunction callback) {
        if (passenger instanceof AirBubbleEntity) {
            // Hole center is 1.0 block above entity feet; align bubble center to hole
            // center
            double holeWorldY = this.getY() + 1.4;
            double bubbleHalfHeight = passenger.getBbHeight() / 2.0;
            callback.accept(passenger, this.getX(), holeWorldY - bubbleHalfHeight, this.getZ());
        } else {
            super.positionRider(passenger, callback);
        }
    }
}
