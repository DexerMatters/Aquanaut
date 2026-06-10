package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.ai.FishResponseMode;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MantaRayEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM_ANIMATION = RawAnimation.begin().thenLoop("animation.manta_ray.swim");
    private static final double RIDER_SEAT_HEIGHT_FACTOR = 0.30D;
    private static final double RIDER_SEAT_BACK_OFFSET = -0.18D;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MantaRayEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            state.getController().setAnimationSpeed(animSpeed(0.35, 0.85, 1.25, 2.25));
            return state.setAndContinue(SWIM_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .build();
    }

    @Override
    public void aiStep() {
        super.aiStep();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && !player.isSecondaryUseActive() && !this.isVehicle()) {
            player.startRiding(this);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public LivingEntity getControllingPassenger() {
        Entity passenger = this.getFirstPassenger();
        return passenger instanceof LivingEntity living ? living : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty() && passenger instanceof Player;
    }

    @Override
    public void travel(Vec3 travelVector) {
        LivingEntity rider = this.getControllingPassenger();
        if (this.isAlive() && rider != null) {
            this.setXRot(0.0F);
            this.xRotO = 0.0F;
            this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED));

            float strafe = rider.xxa * 0.35F;
            float forward = rider.zza;
            if (forward <= 0.0F) {
                forward *= 0.35F;
            }

            float directionYaw;
            if (!this.level().isClientSide) {
                directionYaw = rider.getYRot();
            } else {
                directionYaw = rider.getYHeadRot();
            }

            Vec3 input = new Vec3(strafe, 0.0D, forward);
            if (input.lengthSqr() > 1.0E-4D) {
                float dirPitch = this.isInWater() ? rider.getXRot() * 0.65F : 0.0F;
                Vec3 direction = Vec3.directionFromRotation(dirPitch, directionYaw).normalize();
                Vec3 side = Vec3.directionFromRotation(0.0F, directionYaw + 90.0F).normalize();
                double speed = this.isInWater() ? 0.36D : 0.12D;
                Vec3 targetVelocity = direction.scale(forward * speed).add(side.scale(strafe * speed));
                this.setDeltaMovement(this.getDeltaMovement().scale(0.72D).add(targetVelocity.scale(0.28D)));
            } else {
                this.setDeltaMovement(this.getDeltaMovement().scale(0.86D));
            }

            if (!this.isInWater()) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.08D, 0.0D));
            }

            Vec3 vel = this.getDeltaMovement();
            double speedSqr = vel.lengthSqr();
            if (speedSqr > 0.005D) {
                float moveYaw = (float) (Mth.atan2(vel.z, vel.x) * Mth.RAD_TO_DEG) - 90.0F;
                this.setYRot(moveYaw);
            }

            this.moveRelative(0.0F, Vec3.ZERO);
            this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
            this.calculateEntityAnimation(false);
            return;
        }

        super.travel(travelVector);
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
        if (this.hasPassenger(passenger)) {
            Vec3 seat = new Vec3(0.0D, this.getBbHeight() * RIDER_SEAT_HEIGHT_FACTOR, RIDER_SEAT_BACK_OFFSET)
                    .yRot(-this.getYRot() * Mth.DEG_TO_RAD);
            callback.accept(passenger, this.getX() + seat.x, this.getY() + seat.y, this.getZ() + seat.z);

            if (passenger instanceof LivingEntity livingPassenger) {
                livingPassenger.yBodyRot = this.getYRot();
                livingPassenger.yBodyRotO = livingPassenger.yBodyRot;
            }
        }
    }

    @Override
    protected FishResponseMode getResponseMode() {
        return FishResponseMode.STRESS;
    }

    @Override
    protected double getCruiseAcceleration() {
        return 0.007D;
    }

    @Override
    protected double getCruiseMaxSpeed() {
        return 0.13D;
    }

    @Override
    protected float getCruiseTurnChance() {
        return 0.055F;
    }

    @Override
    protected float getCruiseTurnRangeDegrees() {
        return 24.0F;
    }

    @Override
    protected float getCruiseYawTurnRateDegrees() {
        return 1.2F;
    }

    @Override
    protected double getCruiseVerticalAssist() {
        return 0.012D;
    }

    @Override
    protected double getWaterDrag() {
        return 0.94D;
    }

    @Override
    protected double getEscapeAcceleration() {
        return 0.075D;
    }

    @Override
    protected double getEscapeMaxSpeed() {
        return 0.82D;
    }

    @Override
    protected float getEscapeTurnRateDegrees() {
        return 10.0F;
    }

    @Override
    protected int getReactiveMemoryTicks() {
        return 150;
    }

    @Override
    protected int getSprintPropulsionIntervalTicks() {
        return 5;
    }

    @Override
    protected int getSprintPropulsionBurstTicks() {
        return 8;
    }

    @Override
    protected double getSprintPropulsionBurstAccelerationFactor() {
        return 2.1D;
    }

    @Override
    protected float getBodyTurnRateDegrees() {
        return 5.0F;
    }

    @Override
    protected double getPlayerDetectionRange() {
        return 13.0D;
    }

    @Override
    protected float getHitboxPickInflation() {
        return 0.35F;
    }

    @Override
    protected double getHitboxVisualYOffset() {
        return -0.18D;
    }
}
