package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.ai.FishAttackMode;
import com.dexer.aquanaut.common.ai.FishResponseMode;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Three-Headed Shark — three jaws on one spine, and they never bite together.
 *
 * <p>
 * The centre head takes the direct line; the outer two are splayed thirty-odd degrees off it, so a
 * diver who slips past the first jaw can still be met by the second or third. Each pass resolves as
 * a short sequence of independent bites, which is why retreating sideways is not enough.
 */
public class ThreeHeadedSharkEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM_ANIMATION = RawAnimation.begin().thenLoop("swim");
    private static final RawAnimation CHARGE_ANIMATION = RawAnimation.begin().thenLoop("charge");
    private static final RawAnimation ATTACK_ANIMATION = RawAnimation.begin().thenLoop("attack");

    private static final EntityDataAccessor<Byte> STATE = SynchedEntityData.defineId(
            ThreeHeadedSharkEntity.class, EntityDataSerializers.BYTE);

    public static final byte STATE_IDLE = 0;
    public static final byte STATE_CHARGING = 1;
    public static final byte STATE_BITING = 2;

    private static final double HUNT_RANGE = 16.0D;
    private static final int BITE_SEQUENCE_TICKS = 40;
    private static final int[] BITE_TICKS = {4, 8, 12};
    private static final float[] BITE_DAMAGE = {5.0F, 4.0F, 4.0F};
    private static final float[] BITE_ANGLE = {0.0F, -30.0F, 30.0F};
    private static final double BITE_REACH = 1.6D;
    private static final double BITE_RADIUS = 1.25D;
    private static final int BITE_COOLDOWN_TICKS = 30;
    /** How close a target has to be for the jaws to open (the mouth itself reaches ~2.9 blocks). */
    private static final double BITE_TRIGGER_RANGE = 2.75D;
    /** Cosine of the half-angle the jaws can reach into: about 70 degrees either side. */
    private static final double BITE_FRONT_DOT = 0.34D;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int stateTimer;
    private int biteCooldown;
    private int nextBiteIndex;

    public ThreeHeadedSharkEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, STATE_IDLE);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            return switch (this.getState()) {
                case STATE_BITING -> {
                    state.getController().setAnimationSpeed(1.0D);
                    yield state.setAndContinue(ATTACK_ANIMATION);
                }
                case STATE_CHARGING -> {
                    state.getController().setAnimationSpeed(1.0D);
                    yield state.setAndContinue(CHARGE_ANIMATION);
                }
                default -> {
                    state.getController().setAnimationSpeed(animSpeed(0.45, 1.0, 1.2, 1.6));
                    yield state.setAndContinue(SWIM_ANIMATION);
                }
            };
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 90.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .build();
    }

    public byte getState() {
        return this.entityData.get(STATE);
    }

    private void setState(byte state) {
        this.entityData.set(STATE, state);
    }

    @Override
    protected FishResponseMode getResponseMode() {
        return FishResponseMode.CHARGE;
    }

    @Override
    protected FishAttackMode getAttackMode() {
        return FishAttackMode.NONE;
    }

    @Override
    protected double getPlayerDetectionRange() {
        return HUNT_RANGE;
    }

    @Override
    protected double getCruiseAcceleration() {
        return 0.020D;
    }

    @Override
    protected double getCruiseMaxSpeed() {
        return 0.22D;
    }

    @Override
    protected double getChargeAcceleration() {
        return 0.070D;
    }

    @Override
    protected double getChargeMaxSpeed() {
        return 0.72D;
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
    protected double getWaterDrag() {
        return 0.85D;
    }

    @Override
    protected boolean getCurvedCruiseMovement() {
        return true;
    }

    @Override
    protected float getCruiseCurveTorqueDegrees() {
        return 0.7F;
    }

    @Override
    protected float getCruiseYawTurnRateDegrees() {
        return 3.0F;
    }

    @Override
    protected float getCruisePitchTurnRateDegrees() {
        return 2.2F;
    }

    @Override
    protected float getChargeTurnRateDegrees() {
        return 18.0F;
    }

    @Override
    protected float getBodyTurnRateDegrees() {
        return 7.0F;
    }

    @Override
    protected float getMaxTiltDegrees() {
        return 30.0F;
    }

    @Override
    protected float getHitboxPickInflation() {
        return 0.28F;
    }

    @Override
    protected double getHitboxVisualYOffset() {
        return 0.50D;
    }

    @Override
    protected double getHitboxPitchPivotOffsetY() {
        return 0.3D;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.isEffectiveAi() || this.level().isClientSide) {
            return;
        }

        if (this.getState() == STATE_BITING) {
            this.tickBiting();
            return;
        }

        if (this.biteCooldown > 0) {
            this.biteCooldown--;
        }

        // Charging must not mask the jaws: the shark keeps closing while it bites, so the animation
        // state is set first and the bite check runs either way.
        this.setState(this.isChargingPlayer() ? STATE_CHARGING : STATE_IDLE);

        if (this.biteCooldown > 0) {
            return;
        }

        if (this.playerIsInJaws()) {
            this.beginBiteSequence();
        }
    }

    /** True when a target is inside the jaws' reach, in front of the shark and in line of sight. */
    private boolean playerIsInJaws() {
        Player target = this.level().getNearestPlayer(this, BITE_TRIGGER_RANGE);
        if (target == null || !target.isAlive() || target.isCreative() || target.isSpectator()) {
            return false;
        }
        if (!this.hasLineOfSight(target)) {
            return false;
        }

        Vec3 toTarget = target.position()
                .add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                .subtract(this.position().add(0.0D, this.getBbHeight() * 0.5D, 0.0D));
        double distance = toTarget.length();
        if (distance > BITE_TRIGGER_RANGE) {
            return false;
        }
        if (distance < 0.6D) {
            // Overlapping: there is no meaningful facing test at this range.
            return true;
        }

        Vec3 facing = Vec3.directionFromRotation(0.0F, this.getYRot()).normalize();
        return facing.dot(toTarget.normalize()) > BITE_FRONT_DOT;
    }

    private void beginBiteSequence() {
        this.setState(STATE_BITING);
        this.stateTimer = 0;
        this.nextBiteIndex = 0;
        this.setDeltaMovement(this.getDeltaMovement().scale(0.35D));
    }

    private void tickBiting() {
        this.stateTimer++;

        while (this.nextBiteIndex < BITE_TICKS.length && this.stateTimer >= BITE_TICKS[this.nextBiteIndex]) {
            this.bite(this.nextBiteIndex);
            this.nextBiteIndex++;
        }

        if (this.stateTimer >= BITE_SEQUENCE_TICKS) {
            this.setState(STATE_IDLE);
            this.stateTimer = 0;
            this.biteCooldown = BITE_COOLDOWN_TICKS;
        }
    }

    private void bite(int index) {
        float yaw = this.getYRot() + BITE_ANGLE[index];
        Vec3 facing = Vec3.directionFromRotation(this.getXRot() * 0.4F, yaw);
        Vec3 mouth = this.position()
                .add(0.0D, this.getBbHeight() * 0.5D, 0.0D)
                .add(facing.scale(BITE_REACH));

        AABB area = new AABB(mouth, mouth).inflate(BITE_RADIUS);
        for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != this && entity.isAlive() && !(entity instanceof ThreeHeadedSharkEntity))) {
            victim.hurt(this.damageSources().mobAttack(this), BITE_DAMAGE[index]);
            Vec3 push = facing.normalize().scale(0.5D);
            victim.push(push.x, 0.2D, push.z);
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT, mouth.x, mouth.y, mouth.z, 8, 0.2D, 0.2D, 0.2D, 0.02D);
        }
        this.level().playSound(null, mouth.x, mouth.y, mouth.z,
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.0F, 0.7F + index * 0.1F);
    }
}
