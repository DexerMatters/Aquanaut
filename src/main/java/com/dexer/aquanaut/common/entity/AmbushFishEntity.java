package com.dexer.aquanaut.common.entity;

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

public final class AmbushFishEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("swim");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation BURST_ESCAPE = RawAnimation.begin().thenPlay("burst_escape");
    /** Below this horizontal speed the fish reads as lying in wait rather than cruising. */
    private static final double MOVE_SPEED_SQR = 4.0E-4D;
    /** Ticks a resting flounder waits before it shuffles to a new spot. */
    private static final int DART_MIN_TICKS = 30;
    private static final int DART_RANDOM_TICKS = 70;
    /** Ticks one bolt lasts: about five blocks of travel. */
    private static final int DART_TICKS = 16;
    private int dartCooldown = DART_MIN_TICKS + 20;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public AmbushFishEntity(EntityType<? extends WaterAnimal> type, Level level) { super(type, level); }
    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D).build();
    }
    @Override protected FishResponseMode getResponseMode() { return FishResponseMode.AVOIDANCE; }
    /** Real-time watch: a player inside three blocks springs the ambush. */
    @Override protected double getPlayerDetectionRange() { return 3.0D; }
    @Override protected double getEscapeMaxSpeed() { return 0.50D; }
    @Override protected double getEscapeAcceleration() { return 0.09D; }
    @Override protected int getEscapeMinimumTicks() { return DART_TICKS; }
    /** One bolt: a two-tick wind-up, then a burst that carries it about five blocks. */
    @Override protected boolean getEscapeLaunchBehaviorEnabled() { return true; }
    @Override protected int getEscapeLaunchAnimationTicks() { return DART_TICKS; }
    @Override protected int getEscapeLaunchBurstLeadTicks() { return DART_TICKS - 2; }
    @Override protected double getEscapeLaunchBurstSpeed() { return 0.45D; }
    @Override protected double getEscapeLaunchSustainAcceleration() { return 0.03D; }
    @Override protected double getEscapeLaunchMaxSpeed() { return 0.50D; }
    @Override protected int getEscapeLaunchSteeringLockTicks() { return 4; }
    /** Lies in wait on the sediment like a lump of sand; only the burst gives it away. */
    @Override protected double getCruiseMaxSpeed() { return 0.01D; }
    @Override protected double getCruiseAcceleration() { return 0.004D; }
    @Override protected double getCruiseFloorBias() { return 0.95D; }
    @Override protected double getCruiseDepthRange() { return 1.2D; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (isEscapeLaunching() || isSprintingAway()) {
                return state.setAndContinue(BURST_ESCAPE);
            }
            return state.setAndContinue(
                    getDeltaMovement().horizontalDistanceSqr() > MOVE_SPEED_SQR ? SWIM : IDLE);
        }));
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        // Already bolting: hold the rest until it has settled again.
        if (isSprintingAway() || isEscapeLaunching()) {
            dartCooldown = DART_MIN_TICKS + random.nextInt(DART_RANDOM_TICKS);
            return;
        }
        if (--dartCooldown > 0) {
            return;
        }
        dartCooldown = DART_MIN_TICKS + random.nextInt(DART_RANDOM_TICKS);
        startle(DART_TICKS);
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
