package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.ai.FishAttackMode;
import com.dexer.aquanaut.common.ai.FishResponseMode;
import com.dexer.aquanaut.common.mud.MudZoneConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The mud zone's hostile arthropod, built on the shared fish body so it cruises, charges and
 * bites exactly like the other swimmers instead of bobbing on the buoyancy. Its one special
 * habit: strong light drives it off — it drops the hunt and scuttles toward the dark.
 */
public final class MudSilverfishEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");
    private static final EntityDataAccessor<Integer> ATTACK_TIMER = SynchedEntityData.defineId(
            MudSilverfishEntity.class, EntityDataSerializers.INT);
    /** Ticks the bite clip plays for after a landed hit. */
    private static final int ATTACK_ANIM_TICKS = 10;
    private static final double WALK_SPEED_SQR = 4.0E-4D;
    /** Ticks between light probes while fleeing a bright spot. */
    private static final int FLEE_SCAN_INTERVAL = 10;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int fleeScanCooldown;
    private Vec3 fleeDirection = Vec3.ZERO;

    public MudSilverfishEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ATTACK_TIMER, 0);
    }

    @Override
    public void onSuccessfulBite(Player player) {
        if (!level().isClientSide) {
            entityData.set(ATTACK_TIMER, ATTACK_ANIM_TICKS);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && attackTimer() > 0) {
            entityData.set(ATTACK_TIMER, attackTimer() - 1);
        }
    }

    @Override
    public void aiStep() {
        boolean bright = !level().isClientSide && isInBrightLight();
        if (!level().isClientSide && !hasEffect(MobEffects.WATER_BREATHING)) {
            // An arthropod cannot breathe water on its own; keep it supplied so hunting never
            // ends in drowning.
            addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, -1, 0, true, false));
        }
        // In strong light the shared controller is suspended and the crab-sized body is steered
        // straight toward the darkest neighbour; otherwise it runs the normal swim brain.
        setMovementSuspended(bright);
        super.aiStep();
        if (bright) {
            if (--fleeScanCooldown <= 0) {
                fleeScanCooldown = FLEE_SCAN_INTERVAL;
                fleeDirection = darkestDirection();
            }
            setDeltaMovement(getDeltaMovement().scale(0.6D).add(fleeDirection.scale(0.035D)));
            hasImpulse = true;
        } else {
            fleeDirection = Vec3.ZERO;
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (attackTimer() > 0) {
                return state.setAndContinue(ATTACK);
            }
            if (isMovementSuspended() || getDeltaMovement().horizontalDistanceSqr() > WALK_SPEED_SQR) {
                return state.setAndContinue(WALK);
            }
            return state.setAndContinue(IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private boolean isInBrightLight() {
        return level().getBrightness(LightLayer.BLOCK, BlockPos.containing(position()))
                >= MudZoneConfig.BRIGHT_LIGHT_THRESHOLD;
    }

    /** The neighbouring direction the block light is weakest in, preferring deeper water. */
    private Vec3 darkestDirection() {
        BlockPos origin = BlockPos.containing(position());
        Vec3 best = new Vec3(0.0D, 0.0D, 0.0D);
        int bestLight = Integer.MAX_VALUE;
        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP) {
                continue;
            }
            BlockPos probe = origin.relative(direction);
            int light = level().getBrightness(LightLayer.BLOCK, probe);
            if (light < bestLight) {
                bestLight = light;
                best = new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
            }
        }
        return best;
    }

    private int attackTimer() {
        return entityData.get(ATTACK_TIMER);
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
    protected double getBaseBiteDamage() {
        return 1.0D;
    }

    @Override
    protected double getCruiseMaxSpeed() {
        return 0.14D;
    }

    @Override
    protected double getCruiseAcceleration() {
        return 0.012D;
    }

    @Override
    protected double getEscapeMaxSpeed() {
        return 0.26D;
    }

    @Override
    protected double getChargeMaxSpeed() {
        return 0.30D;
    }

    @Override
    protected double getPlayerDetectionRange() {
        return 8.0D;
    }

    @Override
    protected double getCruiseFloorBias() {
        return 0.7D;
    }

    @Override
    protected double getCruiseDepthRange() {
        return 1.2D;
    }
}
