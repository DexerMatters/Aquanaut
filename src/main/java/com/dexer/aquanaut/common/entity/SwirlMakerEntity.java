package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.ai.FishAttackMode;
import com.dexer.aquanaut.common.ai.FishResponseMode;
import com.dexer.aquanaut.core.EntityRegistry;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
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

public class SwirlMakerEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM_ANIM = RawAnimation.begin().thenLoop("swim");
    private static final RawAnimation OPEN_ANIM = RawAnimation.begin().thenPlay("open");
    private static final RawAnimation CHARGE_ANIM = RawAnimation.begin().thenLoop("charge");
    private static final RawAnimation CLOSE_ANIM = RawAnimation.begin().thenPlay("close");

    private static final EntityDataAccessor<Byte> ATTACK_STATE = SynchedEntityData.defineId(
            SwirlMakerEntity.class, EntityDataSerializers.BYTE);

    private static final byte STATE_IDLE = 0;
    private static final byte STATE_OPENING = 1;
    private static final byte STATE_CHARGING = 2;
    private static final byte STATE_CLOSING = 3;

    private static final int OPEN_TICKS = 27;
    private static final int CLOSE_TICKS = 20;
    private static final double DETECTION_RANGE = 12.0D;
    private static final double ATTACK_RANGE = 10.0D;
    private static final double MOUTH_DAMAGE_RANGE = 1.8D;
    private static final float DAMAGE_PER_HALF_SEC = 3.0F;
    private static final int DAMAGE_INTERVAL = 10;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int attackTimer = 0;
    private int attackTargetId = -1;
    private int damageCooldown = 0;
    private int swirlRespawnTimer = 0;
    private int captureTimer = 0;
    private int attackCooldown = 0;
    private float attackYaw;
    private float attackPitch;
    private static final int CAPTURE_DURATION = 50;

    public SwirlMakerEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ATTACK_STATE, STATE_IDLE);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 3, state -> {
            byte atkState = this.getAttackState();
            return switch (atkState) {
                case STATE_OPENING -> state.setAndContinue(OPEN_ANIM);
                case STATE_CHARGING -> state.setAndContinue(CHARGE_ANIM);
                case STATE_CLOSING -> state.setAndContinue(CLOSE_ANIM);
                default -> {
                    state.getController().setAnimationSpeed(this.getSwimAnimationSpeed());
                    yield state.setAndContinue(SWIM_ANIM);
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
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.1D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .build();
    }

    protected double getSwimAnimationSpeed() {
        return 0.65D;
    }

    public byte getAttackState() {
        return this.entityData.get(ATTACK_STATE);
    }

    private void setAttackState(byte state) {
        this.entityData.set(ATTACK_STATE, state);
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
    public void aiStep() {
        super.aiStep();

        if (!this.isEffectiveAi() || this.isVehicle()) {
            return;
        }

        byte state = getAttackState();

        if (state == STATE_IDLE) {
            if (attackCooldown > 0) {
                attackCooldown--;
                return;
            }
            // Check for nearby player to start attack
            Player nearest = this.level().getNearestPlayer(this, ATTACK_RANGE);
            if (nearest != null && !nearest.isCreative() && !nearest.isSpectator() && this.hasLineOfSight(nearest)) {
                startAttack(nearest);
                return;
            }
            return;
        }

        // During attack: freeze movement, lock facing to initial target position
        this.setDeltaMovement(Vec3.ZERO);
        this.hasImpulse = true;
        this.setYRot(attackYaw);
        this.yBodyRot = attackYaw;
        this.yHeadRot = attackYaw;
        this.setXRot(attackPitch);

        Player target = resolveAttackTarget();
        if (target == null) {
            endAttack();
            return;
        }

        attackTimer++;

        switch (state) {
            case STATE_OPENING:
                if (attackTimer >= OPEN_TICKS) {
                    attackTimer = 0;
                    setAttackState(STATE_CHARGING);
                }
                break;

            case STATE_CHARGING:
                handleCharging(target);
                if (captureTimer >= CAPTURE_DURATION) {
                    attackTimer = 0;
                    setAttackState(STATE_CLOSING);
                    clearExistingSwirls();
                } else if (!isPlayerInSwirlCone(target)) {
                    endAttack();
                }
                break;

            case STATE_CLOSING:
                if (attackTimer >= CLOSE_TICKS) {
                    endAttack(captureTimer >= CAPTURE_DURATION);
                }
                break;
        }
    }

    private void startAttack(Player target) {
        setAttackState(STATE_OPENING);
        attackTimer = 0;
        attackTargetId = target.getId();
        damageCooldown = 0;
        swirlRespawnTimer = 0;
        captureTimer = 0;

        // Lock facing direction at attack start — don't track during inhale
        Vec3 toTarget = target.position().add(0, target.getBbHeight() * 0.5, 0)
                .subtract(this.position().add(0, this.getBbHeight() * 0.4, 0));
        attackYaw = (float) (Mth.atan2(toTarget.z, toTarget.x) * Mth.RAD_TO_DEG) - 90.0F;
        attackPitch = (float) (-Mth.atan2(toTarget.y, toTarget.horizontalDistance()) * Mth.RAD_TO_DEG);
        attackPitch = Mth.clamp(attackPitch, -this.getMaxTiltDegrees(), this.getMaxTiltDegrees());
    }

    private void endAttack(boolean captured) {
        setAttackState(STATE_IDLE);
        attackTimer = 0;
        attackTargetId = -1;
        damageCooldown = 0;
        swirlRespawnTimer = 0;
        captureTimer = 0;
        clearExistingSwirls();
        if (captured) {
            attackCooldown = 60;
        }
    }

    private void endAttack() {
        endAttack(false);
    }

    private Player resolveAttackTarget() {
        if (attackTargetId < 0) return null;
        return this.level().getEntity(attackTargetId) instanceof Player p ? p : null;
    }

    private boolean isPlayerInSwirlCone(Player target) {
        Vec3 mouthPos = getMouthPosition();
        Vec3 playerCenter = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 swirlAxis = Vec3.directionFromRotation(attackPitch, attackYaw);
        double swirlLen = ATTACK_RANGE;

        Vec3 toPlayer = playerCenter.subtract(mouthPos);
        double axialDist = toPlayer.dot(swirlAxis);
        double radialDist = Math.sqrt(Math.max(0, toPlayer.lengthSqr() - axialDist * axialDist));

        if (axialDist < 0.1D || axialDist > swirlLen) return false;

        double tipR = 0.3D;
        double baseR = 2.5D;
        double coneR = tipR + (baseR - tipR) * (axialDist / swirlLen);
        return radialDist < coneR + 1.0D;
    }

    private void handleCharging(Player target) {
        Vec3 mouthPos = getMouthPosition();
        Vec3 playerCenter = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 swirlAxis = Vec3.directionFromRotation(attackPitch, attackYaw);
        double swirlLen = ATTACK_RANGE;

        Vec3 toPlayer = playerCenter.subtract(mouthPos);
        double axialDist = toPlayer.dot(swirlAxis);
        double radialDist = Math.sqrt(Math.max(0, toPlayer.lengthSqr() - axialDist * axialDist));

        double tipR = 0.3D;
        double baseR = 2.5D;
        double coneR = tipR + (baseR - tipR) * Math.min(axialDist / swirlLen, 1.0D);
        boolean inside = axialDist > 0.1D && axialDist < swirlLen && radialDist < coneR + 1.0D;

        if (inside) {
            double dist = Math.max(axialDist, 0.15D);
            double baseStrength = 0.12D / Math.max(dist * 0.12D, 0.12D);
            baseStrength = Mth.clamp(baseStrength, 0.06D, 0.32D);

            // Closest point on the swirl axis
            Vec3 closestOnAxis = mouthPos.add(swirlAxis.scale(axialDist));

            // 1) Pull directly toward the mouth — the dominant force
            Vec3 toMouth = mouthPos.subtract(playerCenter);
            double mouthDist = toMouth.length();
            if (mouthDist > 0.05D) {
                toMouth = toMouth.scale(baseStrength / mouthDist);
            }
            Vec3 mouthPull = toMouth;

            // 2) Pull toward the axis to center the player in the cone
            Vec3 axisCenterPull = Vec3.ZERO;
            if (radialDist > 0.05D) {
                Vec3 toAxis = closestOnAxis.subtract(playerCenter);
                double radialFrac = Math.min(radialDist / Math.max(coneR, 0.1D), 1.0D);
                axisCenterPull = toAxis.normalize().scale(baseStrength * radialFrac);
            }

            // 3) Tangential spin
            Vec3 up = new Vec3(0, 1, 0);
            Vec3 tangent = swirlAxis.cross(up);
            if (tangent.lengthSqr() < 0.001) tangent = swirlAxis.cross(new Vec3(1, 0, 0));
            tangent = tangent.normalize();
            Vec3 spin = tangent.scale(baseStrength * 0.5D);

            Vec3 force = mouthPull.add(axisCenterPull).add(spin);

            // Apply both via delta (direct velocity) and input axes (movement intention)
            target.addDeltaMovement(force.scale(0.5D));
            double yawRad = target.getYRot() * Mth.DEG_TO_RAD;
            double fwd = -force.x * Mth.sin((float) yawRad) + force.z * Mth.cos((float) yawRad);
            double strafe = force.x * Mth.cos((float) yawRad) + force.z * Mth.sin((float) yawRad);
            target.xxa += (float) Mth.clamp(strafe * 8.0D, -1.0F, 1.0F);
            target.zza += (float) Mth.clamp(fwd * 8.0D, -1.0F, 1.0F);
            target.hurtMarked = true;
        }

        double dist = mouthPos.distanceTo(playerCenter);

        // Spawn one swirl at attack start, maintain it
        swirlRespawnTimer--;
        if (swirlRespawnTimer <= 0 && this.level().getEntitiesOfClass(SwirlEntity.class,
                this.getBoundingBox().inflate(6.0D)).size() < 1) {
            spawnAttackSwirl(target, dist);
            swirlRespawnTimer = 60;
        }

        // Damage player and track capture time while close to mouth
        damageCooldown--;
        if (damageCooldown <= 0 && dist < MOUTH_DAMAGE_RANGE) {
            float dmg = DAMAGE_PER_HALF_SEC * DAMAGE_INTERVAL / 10.0F;
            target.hurt(this.damageSources().mobAttack(this), dmg);
            damageCooldown = DAMAGE_INTERVAL;
        }

        if (dist < MOUTH_DAMAGE_RANGE) {
            captureTimer++;
        }
    }

    private void spawnAttackSwirl(Player target, double dist) {
        Vec3 mouthPos = getMouthPosition();

        // Swirl direction locked at attack start, not tracking the player
        float swirlStrength = Mth.clamp((float) ((dist + 1.0D) / SwirlEntity.BASE_LENGTH), 0.5F, 4.0F);

        SwirlEntity swirl = new SwirlEntity(EntityRegistry.SWIRL.get(), this.level());
        swirl.setPos(mouthPos);
        swirl.setOwner(this);
        swirl.configure(SwirlEntity.TYPE_INHALE, attackYaw, attackPitch, swirlStrength, 100);
        this.level().addFreshEntity(swirl);
    }

    private void clearExistingSwirls() {
        for (SwirlEntity s : this.level().getEntitiesOfClass(SwirlEntity.class,
                this.getBoundingBox().inflate(8.0D))) {
            if (s.distanceToSqr(this) < 36.0D) {
                s.discard();
            }
        }
    }

    private Vec3 getMouthPosition() {
        Vec3 forward = Vec3.directionFromRotation(this.getXRot(), this.getYRot());
        return this.position().add(0, this.getBbHeight() * 0.6, 0).add(forward.scale(1.0D));
    }

    @Override
    protected double getPlayerDetectionRange() {
        return DETECTION_RANGE;
    }

    @Override
    protected double getCruiseAcceleration() {
        return 0.008D;
    }

    @Override
    protected double getCruiseMaxSpeed() {
        return 0.09D;
    }

    @Override
    protected double getWaterDrag() {
        return 0.87D;
    }

    @Override
    protected float getCruiseTurnChance() {
        return 0.09F;
    }

    @Override
    protected float getCruiseTurnRangeDegrees() {
        return 36.0F;
    }

    @Override
    protected float getCruiseYawTurnRateDegrees() {
        return 1.8F;
    }

    @Override
    protected int getCruiseYawDecisionMinTicks() {
        return 40;
    }

    @Override
    protected int getCruiseYawDecisionRandomTicks() {
        return 40;
    }

    @Override
    protected float getCruisePitchTurnRateDegrees() {
        return 1.5F;
    }

    @Override
    protected int getCruisePitchDecisionMinTicks() {
        return 40;
    }

    @Override
    protected int getCruisePitchDecisionRandomTicks() {
        return 50;
    }

    @Override
    protected double getCruiseDepthRange() {
        return 5.0D;
    }

    @Override
    protected double getCruiseDepthPitchDistance() {
        return 2.5D;
    }

    @Override
    protected double getCruiseDepthEmergencyOffset() {
        return 2.5D;
    }

    @Override
    protected double getCruiseVerticalAssist() {
        return 0.018D;
    }

    @Override
    protected float getBodyTurnRateDegrees() {
        return 5.0F;
    }

    @Override
    protected float getMaxTiltDegrees() {
        return 30.0F;
    }

    @Override
    protected float getHitboxPickInflation() {
        return 0.4F;
    }

    @Override
    protected double getHitboxVisualYOffset() {
        return -0.5D;
    }

    @Override
    protected double getHitboxPitchPivotOffsetY() {
        return 0.4D;
    }
}
