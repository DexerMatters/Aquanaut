package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.ai.FishAttackMode;
import com.dexer.aquanaut.common.ai.FishResponseMode;
import net.minecraft.util.Mth;
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

public class SpringfishEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM_ANIMATION = RawAnimation.begin().thenLoop("swim");
    private static final RawAnimation LAUNCH_ANIMATION = RawAnimation.begin().thenLoop("launch");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SpringfishEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (this.isEscapeLaunching()) {
                state.getController().setAnimationSpeed(this.getLaunchAnimationSpeed());
                return state.setAndContinue(LAUNCH_ANIMATION);
            }

            double speed = this.getDeltaMovement().length();
            double referenceSpeed = Math.max(0.001D, this.cruiseMaxSpeed());
            double normalizedSpeed = Mth.clamp(speed / referenceSpeed, 0.0D, 1.0D);
            state.getController().setAnimationSpeed(Mth.lerp(normalizedSpeed, 0.8D, 1.2D));
            return state.setAndContinue(SWIM_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .build();
    }

    protected double getLaunchAnimationSpeed() {
        return 1.0D;
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
    protected double getCruiseAcceleration() {
        return 0.012D;
    }

    @Override
    protected double getCruiseMaxSpeed() {
        return 0.16D;
    }

    @Override
    protected double getWaterDrag() {
        return 0.91D;
    }

    @Override
    protected float getCruiseTurnChance() {
        return 0.16F;
    }

    @Override
    protected float getCruiseTurnRangeDegrees() {
        return 30.0F;
    }

    @Override
    protected float getCruiseYawTurnRateDegrees() {
        return 2.9F;
    }

    @Override
    protected float getCruisePitchTurnRateDegrees() {
        return 2.2F;
    }

    @Override
    protected double getCruiseVerticalAssist() {
        return 0.018D;
    }

    @Override
    protected float getBodyTurnRateDegrees() {
        return 9.0F;
    }

    @Override
    protected float getMaxTiltDegrees() {
        return 32.0F;
    }

    @Override
    protected double getPlayerDetectionRange() {
        return 11.5D;
    }

    @Override
    protected float getEscapeTurnRateDegrees() {
        return 12.5F;
    }

    @Override
    protected boolean getEscapeLaunchBehaviorEnabled() {
        return true;
    }

    @Override
    protected int getEscapeLaunchAnimationTicks() {
        return 34;
    }

    @Override
    protected int getEscapeLaunchBurstLeadTicks() {
        return 15;
    }

    @Override
    protected double getEscapeLaunchPrepDrag() {
        return 0.74D;
    }

    @Override
    protected double getEscapeLaunchBurstSpeed() {
        return 1.45D;
    }

    @Override
    protected double getEscapeLaunchPostBurstDrag() {
        return 0.985D;
    }

    @Override
    protected double getEscapeLaunchSustainAcceleration() {
        return 0.045D;
    }

    @Override
    protected double getEscapeLaunchMaxSpeed() {
        return 1.6D;
    }

    @Override
    protected int getEscapeLaunchSteeringLockTicks() {
        return 12;
    }

    @Override
    protected float getHitboxPickInflation() {
        return 0.22F;
    }

    @Override
    protected double getHitboxVisualYOffset() {
        return -0.52D;
    }
}
