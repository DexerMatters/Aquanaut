package com.dexer.aquanaut.common.ai;

import com.dexer.aquanaut.common.entity.BaseFishEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class FishMovementController {
    private static final double BARRIER_CHECK_DISTANCE = 3.5D;
    private static final double BARRIER_CHECK_STEP = 0.7D;
    private static final double WALL_PROXIMITY_SLOWDOWN = 1.8D;
    private static final double WALL_PROXIMITY_STRONG = 0.9D;

    private final MovementState state = new MovementState();
    private final FishSchoolingAI schoolingAI = new FishSchoolingAI();
    private final PlayerAvoidanceLogic avoidanceLogic = new PlayerAvoidanceLogic();

    private int reactivePlayerId = -1;
    private int reactiveMemoryTicks;

    private int activeChargeTargetId = -1;
    private int lostChargeTargetTicks;

    private int biteCooldownTicks;
    private int passByRetreatTicks;
    private int passByReengageCooldownTicks;
    private int escapeMinimumTicks;
    private int escapeLaunchTicksRemaining;
    private boolean escapeLaunchBurstTriggered;
    private Vec3 escapeLaunchDirection = Vec3.ZERO;

    public void tick(BaseFishEntity fish) {
        if (!fish.isInWater()) {
            fish.setNoGravity(false);
            this.resetRuntimeState(fish);
            return;
        }

        fish.setNoGravity(true);
        this.tickCooldowns();
        this.applyCollisionRecovery(fish);

        BehaviorDecision decision = this.resolveBehaviorDecision(fish);
        this.state.setSprintingAway(decision.mode == MovementMode.ESCAPE);
        this.state.setChargingPlayer(decision.mode == MovementMode.CHARGE);

        boolean flexible = fish.isFlexibleBody();

        if (decision.mode == MovementMode.CRUISE) {
            FishSchoolingAI.SchoolingDecision schoolingDecision = this.schoolingAI.resolve(fish);
            if (schoolingDecision.active()) {
                if (this.state.steeringLockTicks() <= 0) {
                    this.applySchoolSteering(fish, schoolingDecision, flexible);
                }

                this.applyForwardMotion(fish, fish.cruiseAcceleration() * schoolingDecision.speedMultiplier(),
                        fish.cruiseMaxSpeed() * schoolingDecision.speedMultiplier(), true, false, flexible);
                this.updateBodyAndHeadRotation(fish);
                return;
            }

            if (this.state.steeringLockTicks() <= 0) {
                if (fish.curvedCruiseMovement()) {
                    this.applyCurvedCruiseSteering(fish, flexible);
                } else {
                    this.applyCruiseSteering(fish, flexible);
                }
            }
            this.applyForwardMotion(fish, fish.cruiseAcceleration(), fish.cruiseMaxSpeed(), true, false, flexible);
        } else if (decision.mode == MovementMode.ESCAPE) {
            if (fish.escapeLaunchBehaviorEnabled()) {
                this.applyEscapeLaunchMotion(fish, decision.target, flexible);
            } else {
                if (this.state.steeringLockTicks() <= 0) {
                    this.applyEscapeSteering(fish, decision.target, flexible);
                }
                this.applyForwardMotion(fish, fish.escapeAcceleration(), fish.escapeMaxSpeed(), !flexible, true, flexible);
            }
        } else if (decision.mode == MovementMode.INERTIA) {
            this.clearEscapeLaunchState();
            this.applyPassByInertiaMotion(fish);
        } else {
            this.clearEscapeLaunchState();
            if (this.state.steeringLockTicks() <= 0) {
                this.applyChargeSteering(fish, decision.target, flexible);
            }
            this.applyForwardMotion(fish, fish.chargeAcceleration(), fish.chargeMaxSpeed(), !flexible, true, flexible);
            this.tryAttackPlayer(fish, decision.target);
        }

        this.updateBodyAndHeadRotation(fish);
    }

    public void onHurt(BaseFishEntity fish, DamageSource source) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof Player player)) {
            return;
        }

        if (!this.isTargetablePlayer(fish, player, 3.0D)) {
            return;
        }

        this.reactivePlayerId = player.getId();
        this.reactiveMemoryTicks = fish.reactiveMemoryTicks();

        if (fish.responseMode() == FishResponseMode.IRRITATE) {
            this.activeChargeTargetId = player.getId();
            this.lostChargeTargetTicks = 0;
            this.passByReengageCooldownTicks = 0;
        }
    }

    public boolean isSprintingAway() {
        return this.state.isSprintingAway();
    }

    public boolean isChargingPlayer() {
        return this.state.isChargingPlayer();
    }

    public boolean isEscapeLaunching() {
        return this.escapeLaunchTicksRemaining > 0;
    }

    private void resetRuntimeState(BaseFishEntity fish) {
        this.avoidanceLogic.reset();

        this.reactivePlayerId = -1;
        this.reactiveMemoryTicks = 0;

        this.activeChargeTargetId = -1;
        this.lostChargeTargetTicks = 0;

        this.biteCooldownTicks = 0;
        this.passByRetreatTicks = 0;
        this.passByReengageCooldownTicks = 0;
        this.escapeMinimumTicks = 0;
        this.clearEscapeLaunchState();

        this.state.reset(fish.getYRot(), fish.getY());
    }

    private void tickCooldowns() {
        if (this.reactiveMemoryTicks > 0) {
            this.reactiveMemoryTicks--;
        }
        if (this.biteCooldownTicks > 0) {
            this.biteCooldownTicks--;
        }
        if (this.passByRetreatTicks > 0) {
            this.passByRetreatTicks--;
        }
        if (this.passByReengageCooldownTicks > 0) {
            this.passByReengageCooldownTicks--;
        }
        if (this.escapeMinimumTicks > 0) {
            this.escapeMinimumTicks--;
        }

        if (this.state.collisionTurnCooldown() > 0) {
            this.state.setCollisionTurnCooldown(this.state.collisionTurnCooldown() - 1);
        }
        if (this.state.steeringLockTicks() > 0) {
            this.state.setSteeringLockTicks(this.state.steeringLockTicks() - 1);
        }
    }

    private BehaviorDecision resolveBehaviorDecision(BaseFishEntity fish) {
        Player reactiveTarget = this.resolveReactiveTarget(fish);
        Player nearestTarget = this.findNearestThreatPlayer(fish, 1.0D, true);

        if (this.passByRetreatTicks > 0) {
            return new BehaviorDecision(MovementMode.INERTIA, null);
        }

        FishResponseMode responseMode = fish.responseMode();
        if (responseMode.isEscapeMode()) {
            Player escapeTarget = responseMode == FishResponseMode.STRESS ? reactiveTarget : nearestTarget;
            boolean wasEscaping = this.state.isSprintingAway();
            if (escapeTarget != null) {
                this.escapeMinimumTicks = 40;
                return new BehaviorDecision(MovementMode.ESCAPE, escapeTarget);
            }
            if (this.isEscapeLaunchActive(fish)) {
                return new BehaviorDecision(MovementMode.ESCAPE, null);
            }
            if (this.escapeMinimumTicks > 0) {
                return new BehaviorDecision(MovementMode.ESCAPE, null);
            }
        }

        if (this.passByReengageCooldownTicks > 0) {
            this.clearEscapeLaunchState();
            return new BehaviorDecision(MovementMode.CRUISE, null);
        }

        if (responseMode.isChargeMode()) {
            Player chargeTriggerTarget = responseMode == FishResponseMode.IRRITATE
                    ? reactiveTarget
                    : this.findNearestThreatPlayer(fish, 1.0D, false);
            Player chargeTarget = this.resolveChargeTarget(fish, chargeTriggerTarget);
            if (chargeTarget != null) {
                return new BehaviorDecision(MovementMode.CHARGE, chargeTarget);
            }
        }

        this.activeChargeTargetId = -1;
        this.lostChargeTargetTicks = 0;
        this.clearEscapeLaunchState();
        return new BehaviorDecision(MovementMode.CRUISE, null);
    }

    private boolean isEscapeLaunchActive(BaseFishEntity fish) {
        return fish.escapeLaunchBehaviorEnabled() && this.escapeLaunchTicksRemaining > 0;
    }

    private Player resolveReactiveTarget(BaseFishEntity fish) {
        if (this.reactiveMemoryTicks <= 0) {
            this.reactivePlayerId = -1;
            return null;
        }

        Player player = this.findPlayerById(fish, this.reactivePlayerId);
        if (player == null || !this.isTargetablePlayer(fish, player, fish.behaviorPersistenceRangeMultiplier())) {
            return null;
        }

        return player;
    }

    private Player resolveChargeTarget(BaseFishEntity fish, Player triggerTarget) {
        Player currentTarget = this.findPlayerById(fish, this.activeChargeTargetId);
        if (currentTarget != null
                && this.isTargetablePlayer(fish, currentTarget, fish.behaviorPersistenceRangeMultiplier())) {
            if (fish.hasLineOfSight(currentTarget)) {
                this.lostChargeTargetTicks = 0;
            } else {
                this.lostChargeTargetTicks++;
            }

            if (this.lostChargeTargetTicks <= fish.chargeTargetLostResetTicks()) {
                return currentTarget;
            }
        }

        if (triggerTarget != null
                && this.isTargetablePlayer(fish, triggerTarget, fish.behaviorPersistenceRangeMultiplier())) {
            this.activeChargeTargetId = triggerTarget.getId();
            this.lostChargeTargetTicks = 0;
            return triggerTarget;
        }

        this.activeChargeTargetId = -1;
        this.lostChargeTargetTicks = 0;
        return null;
    }

    private Player findNearestThreatPlayer(BaseFishEntity fish, double rangeMultiplier, boolean requireLineOfSight) {
        double detectionRange = fish.playerDetectionRange() * rangeMultiplier;
        Player nearest = fish.level().getNearestPlayer(fish, detectionRange);

        if (!this.isTargetablePlayer(fish, nearest, rangeMultiplier)) {
            return null;
        }

        if (requireLineOfSight && !fish.hasLineOfSight(nearest)) {
            return null;
        }

        return nearest;
    }

    private Player findPlayerById(BaseFishEntity fish, int playerId) {
        if (playerId < 0) {
            return null;
        }

        Entity entity = fish.level().getEntity(playerId);
        return entity instanceof Player player ? player : null;
    }

    private boolean isTargetablePlayer(BaseFishEntity fish, Player player, double rangeMultiplier) {
        if (player == null || !player.isAlive() || player.isCreative() || player.isSpectator()) {
            return false;
        }

        double maxRange = fish.playerDetectionRange() * Math.max(1.0D, rangeMultiplier);
        return fish.distanceToSqr(player) <= maxRange * maxRange;
    }

    private void applyCollisionRecovery(BaseFishEntity fish) {
        boolean flexible = fish.isFlexibleBody();

        if (fish.horizontalCollision && this.state.collisionTurnCooldown() <= 0) {
            float currentYaw = fish.getYRot();
            float avoidBias = (fish.getRandom().nextFloat() - 0.5F) * 50.0F;
            float reboundYaw = currentYaw + 110.0F * (fish.getRandom().nextBoolean() ? 1.0F : -1.0F) + avoidBias;
            fish.setYRot(reboundYaw);
            fish.yBodyRot = reboundYaw;
            fish.yHeadRot = reboundYaw;
            this.state.setCruiseYawTarget(reboundYaw);

            Vec3 reboundDir = Vec3.directionFromRotation(this.motionPitchFor(fish), reboundYaw).normalize();
            reboundDir = this.steerAroundBarriers(fish, reboundDir);
            double speedReduction = 0.3D + fish.getRandom().nextDouble() * 0.15D;
            fish.setDeltaMovement(fish.getDeltaMovement().scale(speedReduction).add(reboundDir.scale(0.06D)));
            fish.hasImpulse = true;
            float reboundPitch = Mth.clamp(this.pitchFromDirection(reboundDir), -this.motionPitchLimit(fish),
                    this.motionPitchLimit(fish));
            this.state.setMotionPitchTarget(reboundPitch);
            this.state.setCruisePitchTarget(reboundPitch);
            if (flexible) {
                float maxTilt = fish.maxTiltDegrees();
                fish.setXRot(this.rotateTowards(fish.getXRot(), Mth.clamp(reboundPitch, -maxTilt, maxTilt),
                        fish.escapeTurnRateDegrees() * 0.55F));
            }
            this.state.setCollisionTurnCooldown(fish.collisionTurnCooldownTicks());
            this.state.setSteeringLockTicks(Math.max(this.state.steeringLockTicks(), 4));
        }

        if (fish.verticalCollision && this.state.collisionTurnCooldown() <= 0) {
            float recoveryPitch;
            if (flexible) {
                float maxTilt = fish.maxTiltDegrees();
                float reboundPitch = Mth.clamp(-fish.getXRot() * 0.75F, -maxTilt, maxTilt);
                if (!this.hasWaterAbove(fish)) {
                    reboundPitch = Mth.clamp(Math.abs(reboundPitch) + 10.0F, 6.0F, maxTilt);
                } else if (!this.hasWaterBelow(fish)) {
                    reboundPitch = Mth.clamp(-Math.abs(reboundPitch) - 10.0F, -maxTilt, -6.0F);
                }
                fish.setXRot(reboundPitch);
                recoveryPitch = reboundPitch;
            } else {
                fish.setXRot(0.0F);
                double verticalPush = !this.hasWaterAbove(fish) ? -0.08D
                        : !this.hasWaterBelow(fish) ? 0.08D : 0.0D;
                fish.setDeltaMovement(fish.getDeltaMovement().add(0.0D, verticalPush, 0.0D));
                if (verticalPush < 0.0D) {
                    recoveryPitch = Math.max(12.0F, this.motionPitchLimit(fish) * 0.32F);
                } else if (verticalPush > 0.0D) {
                    recoveryPitch = -Math.max(12.0F, this.motionPitchLimit(fish) * 0.32F);
                } else {
                    recoveryPitch = this.motionPitchFor(fish);
                }
            }
            this.state.setMotionPitchTarget(recoveryPitch);
            this.state.setCruisePitchTarget(recoveryPitch);
            this.state.setCollisionTurnCooldown(fish.collisionTurnCooldownTicks());
            this.state.setSteeringLockTicks(Math.max(this.state.steeringLockTicks(), 2));
        }
    }

    private void applyEscapeSteering(BaseFishEntity fish, Player escapeFrom, boolean flexible) {
        Vec3 rawDirection = this.resolveEscapeDirection(fish, escapeFrom);
        Vec3 desiredDirection = this.barrierAwareEscapeDirection(fish, rawDirection, flexible);
        if (desiredDirection.lengthSqr() < 1.0E-6D) {
            return;
        }
        float targetYaw = this.yawFromDirection(desiredDirection);
        float targetPitch = Mth.clamp(this.pitchFromDirection(desiredDirection), -this.motionPitchLimit(fish),
                this.motionPitchLimit(fish));
        fish.setYRot(this.rotateTowards(fish.getYRot(), targetYaw, fish.escapeTurnRateDegrees()));
        this.state.setMotionPitchTarget(targetPitch);
        this.state.setCruisePitchTarget(targetPitch);

        if (flexible) {
            float maxTilt = fish.maxTiltDegrees();
            fish.setXRot(this.rotateTowards(fish.getXRot(), Mth.clamp(targetPitch, -maxTilt, maxTilt),
                    fish.escapeTurnRateDegrees() * 0.7F));
        } else {
            fish.setXRot(0.0F);
        }
        this.state.setCruiseTargetY(fish.getY() + desiredDirection.y() * fish.cruiseDepthEmergencyOffset());
        this.state.setCruiseYawTarget(fish.getYRot());
    }

    private void applyEscapeLaunchMotion(BaseFishEntity fish, Player escapeFrom, boolean flexible) {
        Vec3 rawDirection = this.resolveEscapeDirection(fish, escapeFrom);
        Vec3 desiredDirection = this.barrierAwareEscapeDirection(fish, rawDirection, flexible);
        if (desiredDirection.lengthSqr() < 1.0E-6D) {
            desiredDirection = this.escapeLaunchDirection.lengthSqr() > 1.0E-6D
                    ? this.escapeLaunchDirection
                    : Vec3.directionFromRotation(fish.getXRot(), fish.getYRot()).normalize();
        }

        desiredDirection = desiredDirection.normalize();
        float targetPitch = Mth.clamp(this.pitchFromDirection(desiredDirection), -this.motionPitchLimit(fish),
                this.motionPitchLimit(fish));
        this.state.setMotionPitchTarget(targetPitch);
        this.state.setCruisePitchTarget(targetPitch);
        this.state.setCruiseTargetY(fish.getY() + desiredDirection.y() * fish.cruiseDepthEmergencyOffset());

        if (this.escapeLaunchTicksRemaining <= 0) {
            this.escapeLaunchTicksRemaining = Math.max(1, fish.escapeLaunchAnimationTicks());
            this.escapeLaunchBurstTriggered = false;
            this.escapeLaunchDirection = desiredDirection;
        }

        if (!this.escapeLaunchBurstTriggered) {
            this.escapeLaunchDirection = desiredDirection;

            if (this.state.steeringLockTicks() <= 0) {
                float targetYaw = this.yawFromDirection(this.escapeLaunchDirection);
                fish.setYRot(this.rotateTowards(fish.getYRot(), targetYaw, fish.escapeTurnRateDegrees()));
                if (flexible) {
                    float maxTilt = fish.maxTiltDegrees();
                    fish.setXRot(this.rotateTowards(fish.getXRot(), Mth.clamp(targetPitch, -maxTilt, maxTilt),
                            fish.escapeTurnRateDegrees() * 0.7F));
                } else {
                    fish.setXRot(0.0F);
                }
                this.state.setCruiseYawTarget(fish.getYRot());
            }

            fish.setDeltaMovement(fish.getDeltaMovement().scale(fish.escapeLaunchPrepDrag()));
            fish.hasImpulse = true;

            if (this.escapeLaunchTicksRemaining <= fish.escapeLaunchBurstLeadTicks()) {
                this.triggerEscapeLaunchBurst(fish);
            }
        } else {
            this.applyEscapeLaunchSustainMotion(fish);
        }

        this.escapeLaunchTicksRemaining--;
        if (this.escapeLaunchTicksRemaining <= 0) {
            this.clearEscapeLaunchState();
        }
    }

    private void triggerEscapeLaunchBurst(BaseFishEntity fish) {
        Vec3 burstDirection = this.escapeLaunchDirection.lengthSqr() > 1.0E-6D
                ? this.escapeLaunchDirection.normalize()
                : Vec3.directionFromRotation(fish.getXRot(), fish.getYRot()).normalize();
        double burstSpeed = fish.escapeLaunchBurstSpeed();

        fish.setDeltaMovement(burstDirection.scale(burstSpeed));
        fish.hasImpulse = true;
        this.spawnEscapeLaunchSplash(fish, burstDirection, burstSpeed);
        this.escapeLaunchBurstTriggered = true;
        this.state.setSteeringLockTicks(
                Math.max(this.state.steeringLockTicks(), Math.max(0, fish.escapeLaunchSteeringLockTicks())));
    }

    private void spawnEscapeLaunchSplash(BaseFishEntity fish, Vec3 burstDirection, double burstSpeed) {
        if (!(fish.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 trailOffset = burstDirection.scale(-Math.max(0.12D, fish.getBbWidth() * 0.45D));
        Vec3 splashOrigin = fish.position()
                .add(0.0D, fish.getBbHeight() * 0.45D, 0.0D)
                .add(trailOffset);
        double spread = 0.08D + Math.min(0.16D, burstSpeed * 0.04D);
        double speed = 0.02D + Math.min(0.08D, burstSpeed * 0.025D);

        level.sendParticles(ParticleTypes.SPLASH,
                splashOrigin.x, splashOrigin.y, splashOrigin.z,
                10,
                spread, spread * 0.55D, spread,
                speed);
        level.sendParticles(ParticleTypes.BUBBLE,
                splashOrigin.x, splashOrigin.y, splashOrigin.z,
                14,
                spread * 0.9D, spread * 0.6D, spread * 0.9D,
                speed * 0.9D);
    }

    private void applyEscapeLaunchSustainMotion(BaseFishEntity fish) {
        Vec3 forwardDirection = this.escapeLaunchDirection.lengthSqr() > 1.0E-6D
                ? this.escapeLaunchDirection.normalize()
                : Vec3.directionFromRotation(fish.getXRot(), fish.getYRot()).normalize();
        Vec3 nextVelocity = fish.getDeltaMovement().scale(fish.escapeLaunchPostBurstDrag())
                .add(forwardDirection.scale(fish.escapeLaunchSustainAcceleration()));

        double maxSpeed = fish.escapeLaunchMaxSpeed();
        double maxSpeedSqr = maxSpeed * maxSpeed;
        if (nextVelocity.lengthSqr() > maxSpeedSqr) {
            nextVelocity = nextVelocity.normalize().scale(maxSpeed);
        }

        fish.setDeltaMovement(nextVelocity);
        fish.hasImpulse = true;
    }

    private Vec3 resolveEscapeDirection(BaseFishEntity fish, Player escapeFrom) {
        PlayerAvoidanceLogic.AvoidanceResult avoidanceResult = escapeFrom != null
                ? this.avoidanceLogic.computeAgainst(fish, escapeFrom)
                : this.avoidanceLogic.compute(fish, fish.playerDetectionRange());

        if (avoidanceResult == null) {
            return Vec3.ZERO;
        }

        return avoidanceResult.escapeDirection();
    }

    private Vec3 barrierAwareEscapeDirection(BaseFishEntity fish, Vec3 rawDirection, boolean flexible) {
        return this.steerAroundBarriers(fish, rawDirection);
    }

    private void clearEscapeLaunchState() {
        this.escapeLaunchTicksRemaining = 0;
        this.escapeLaunchBurstTriggered = false;
        this.escapeLaunchDirection = Vec3.ZERO;
    }

    private void applyChargeSteering(BaseFishEntity fish, Player target, boolean flexible) {
        if (target == null) {
            return;
        }

        if (fish.attackMode() == FishAttackMode.PASS_BY_BITE) {
            double noTurnDistance = fish.chargeNoTurnDistance();
            if (noTurnDistance > 0.0D && fish.distanceToSqr(target) <= noTurnDistance * noTurnDistance) {
                return;
            }
        }

        Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.45D, 0.0D);
        Vec3 ownCenter = fish.position().add(0.0D, fish.getBbHeight() * 0.45D, 0.0D);
        Vec3 desiredDirection = targetCenter.subtract(ownCenter);
        if (desiredDirection.lengthSqr() < 1.0E-6D) {
            return;
        }

        desiredDirection = this.steerAroundBarriers(fish, desiredDirection);
        if (desiredDirection.lengthSqr() < 1.0E-6D) {
            return;
        }

        float targetYaw = this.yawFromDirection(desiredDirection);
        float targetPitch = Mth.clamp(this.pitchFromDirection(desiredDirection), -this.motionPitchLimit(fish),
                this.motionPitchLimit(fish));

        if (this.isDirectionBlocked(fish, targetYaw, targetPitch, BARRIER_CHECK_DISTANCE)) {
            float clearYaw = this.findClearYaw(fish, targetYaw, 70.0F);
            targetYaw = this.rotateTowards(fish.getYRot(), clearYaw, fish.chargeTurnRateDegrees());
        }

        fish.setYRot(this.rotateTowards(fish.getYRot(), targetYaw, fish.chargeTurnRateDegrees()));
        this.state.setMotionPitchTarget(targetPitch);
        this.state.setCruisePitchTarget(targetPitch);

        if (flexible) {
            float chargeMaxTilt = fish.maxTiltDegrees();
            fish.setXRot(this.rotateTowards(fish.getXRot(), Mth.clamp(targetPitch, -chargeMaxTilt, chargeMaxTilt),
                    fish.chargePitchTurnRateDegrees()));
        } else {
            fish.setXRot(0.0F);
        }

        this.state.setCruiseTargetY(targetCenter.y);
        this.state.setCruiseYawTarget(fish.getYRot());
    }

    private void applyPassByInertiaMotion(BaseFishEntity fish) {
        Vec3 forwardDirection = Vec3.directionFromRotation(this.motionPitchFor(fish), fish.getYRot()).normalize();
        double inertiaAcceleration = fish.chargeAcceleration() * 0.65D;
        Vec3 nextVelocity = fish.getDeltaMovement()
                .scale(fish.waterDrag())
                .add(forwardDirection.scale(inertiaAcceleration));

        double maxInertiaSpeed = fish.chargeMaxSpeed() * fish.passByInertiaMaxSpeedMultiplier();
        double maxInertiaSpeedSqr = maxInertiaSpeed * maxInertiaSpeed;
        if (nextVelocity.lengthSqr() > maxInertiaSpeedSqr) {
            nextVelocity = nextVelocity.normalize().scale(maxInertiaSpeed);
        }

        fish.setDeltaMovement(nextVelocity);
        fish.hasImpulse = true;
    }

    private void applyCruiseSteering(BaseFishEntity fish, boolean flexible) {
        this.updateCruiseYawTarget(fish);
        fish.setYRot(this.rotateTowards(fish.getYRot(), this.state.cruiseYawTarget(), fish.cruiseYawTurnRateDegrees()));

        this.updateCruiseDepthTarget(fish);
        this.updateCruisePitchTarget(fish);
        if (flexible) {
            fish.setXRot(
                    this.rotateTowards(fish.getXRot(), this.state.cruisePitchTarget(), fish.cruisePitchTurnRateDegrees()));
        } else {
            fish.setXRot(0.0F);
        }
    }

    private void applyCurvedCruiseSteering(BaseFishEntity fish, boolean flexible) {
        this.updateCruiseDepthTarget(fish);
        this.updateCruisePitchTarget(fish);
        if (flexible) {
            fish.setXRot(
                    this.rotateTowards(fish.getXRot(), this.state.cruisePitchTarget(), fish.cruisePitchTurnRateDegrees()));
        } else {
            fish.setXRot(0.0F);
        }

        float curveTorque = fish.cruiseCurveTorqueDegrees();
        if (Math.abs(curveTorque) <= 1.0E-4F) {
            return;
        }

        float curveDirection = (fish.getUUID().hashCode() & 1) == 0 ? 1.0F : -1.0F;
        float newYaw = fish.getYRot() + curveDirection * curveTorque;

        if (this.isDirectionBlocked(fish, newYaw, this.motionPitchFor(fish), BARRIER_CHECK_DISTANCE * 0.6D)) {
            curveDirection *= -1.0F;
            newYaw = fish.getYRot() + curveDirection * curveTorque * 0.5F;
        }

        fish.setYRot(newYaw);
        this.state.setCruiseYawTarget(fish.getYRot());
    }

    private void applySchoolSteering(BaseFishEntity fish, FishSchoolingAI.SchoolingDecision schoolingDecision,
            boolean flexible) {
        Vec3 desiredDirection = this.steerAroundBarriers(fish, schoolingDecision.desiredDirection());
        if (desiredDirection.lengthSqr() < 1.0E-6D) {
            return;
        }

        float targetYaw = this.yawFromDirection(desiredDirection);
        float targetPitch = Mth.clamp(this.pitchFromDirection(desiredDirection), -this.motionPitchLimit(fish),
                this.motionPitchLimit(fish));

        if (this.isDirectionBlocked(fish, targetYaw, targetPitch, BARRIER_CHECK_DISTANCE)) {
            float clearYaw = this.findClearYaw(fish, targetYaw, 80.0F);
            targetYaw = this.rotateTowards(fish.getYRot(), clearYaw, fish.cruiseYawTurnRateDegrees());
        }

        fish.setYRot(this.rotateTowards(fish.getYRot(), targetYaw, fish.cruiseYawTurnRateDegrees()));
        this.state.setMotionPitchTarget(targetPitch);
        if (flexible) {
            float maxTilt = fish.maxTiltDegrees();
            fish.setXRot(this.rotateTowards(fish.getXRot(), Mth.clamp(targetPitch, -maxTilt, maxTilt),
                    fish.cruisePitchTurnRateDegrees()));
        } else {
            fish.setXRot(0.0F);
        }
        this.state.setCruisePitchTarget(targetPitch);
        this.state.setCruiseYawTarget(fish.getYRot());

        double depthDelta = schoolingDecision.schoolCenterY() - fish.getY();
        double depthCorrection = Math.abs(depthDelta) < 0.8D ? 0.0D : Mth.clamp(depthDelta * 0.05D, -0.09D, 0.09D);
        this.state.setCruiseTargetY(fish.getY() + depthCorrection);
    }

    private void updateCruiseYawTarget(BaseFishEntity fish) {
        if (fish.horizontalCollision) {
            this.state.setCruiseYawTarget(fish.getYRot() + 180.0F + (fish.getRandom().nextFloat() - 0.5F) * 40.0F);
            this.state.setCruiseYawDecisionCooldown(fish.cruiseYawDecisionMinTicks());
            return;
        }

        this.state.setCruiseYawDecisionCooldown(this.state.cruiseYawDecisionCooldown() - 1);
        if (this.state.cruiseYawDecisionCooldown() <= 0) {
            this.state.setCruiseYawDecisionCooldown(
                    fish.cruiseYawDecisionMinTicks() + fish.getRandom().nextInt(fish.cruiseYawDecisionRandomTicks()));
            if (fish.getRandom().nextFloat() < fish.cruiseTurnChance()) {
                float yawOffset = (fish.getRandom().nextFloat() - 0.5F) * fish.cruiseTurnRangeDegrees();
                float candidateYaw = fish.getYRot() + yawOffset;

                if (this.isDirectionBlocked(fish, candidateYaw, this.motionPitchFor(fish), BARRIER_CHECK_DISTANCE)) {
                    candidateYaw = this.findClearYaw(fish, candidateYaw, fish.cruiseTurnRangeDegrees() * 1.5F);
                }

                this.state.setCruiseYawTarget(candidateYaw);
            } else {
                float currentYaw = fish.getYRot();
                if (this.isDirectionBlocked(fish, currentYaw, this.motionPitchFor(fish), WALL_PROXIMITY_STRONG)) {
                    float clearYaw = this.findClearYaw(fish, currentYaw, 90.0F);
                    this.state.setCruiseYawTarget(clearYaw);
                } else {
                    this.state.setCruiseYawTarget(currentYaw);
                }
            }
        }
    }

    private void updateCruiseDepthTarget(BaseFishEntity fish) {
        this.state.setCruiseDepthDecisionCooldown(this.state.cruiseDepthDecisionCooldown() - 1);

        if (this.state.cruiseDepthDecisionCooldown() <= 0) {
            this.state.setCruiseDepthDecisionCooldown(fish.cruisePitchDecisionMinTicks()
                    + fish.getRandom().nextInt(fish.cruisePitchDecisionRandomTicks()));
            double offsetMagnitude = 0.6D
                    + fish.getRandom().nextDouble() * Math.max(0.1D, fish.cruiseDepthRange() * 0.55D);
            boolean nearSurface = this.isNearSurface(fish);
            boolean nearFloor = this.isNearFloor(fish);
            if (nearSurface && !nearFloor) {
                offsetMagnitude = -offsetMagnitude;
            } else if (nearFloor && !nearSurface) {
                offsetMagnitude = Math.abs(offsetMagnitude);
            } else {
                offsetMagnitude = fish.getRandom().nextBoolean() ? offsetMagnitude : -offsetMagnitude;
            }
            double desiredY = fish.getY() + offsetMagnitude;
            this.state.setCruiseTargetY(
                    Mth.lerp(0.35D, this.state.cruiseTargetY(), desiredY));
        }

        if (!this.hasWaterAbove(fish)) {
            this.state.setCruiseTargetY(
                    Mth.lerp(0.5D, this.state.cruiseTargetY(), fish.getY() - fish.cruiseDepthEmergencyOffset()));
        } else if (this.isNearSurface(fish)) {
            double pushDown = fish.getY() - fish.cruiseDepthEmergencyOffset() * 0.5D;
            this.state.setCruiseTargetY(
                    Mth.lerp(0.4D, this.state.cruiseTargetY(), pushDown));
        } else if (!this.hasWaterBelow(fish)) {
            this.state.setCruiseTargetY(
                    Mth.lerp(0.5D, this.state.cruiseTargetY(), fish.getY() + fish.cruiseDepthEmergencyOffset()));
        } else if (this.isNearFloor(fish)) {
            double pushUp = fish.getY() + fish.cruiseDepthEmergencyOffset() * 0.5D;
            this.state.setCruiseTargetY(
                    Mth.lerp(0.4D, this.state.cruiseTargetY(), pushUp));
        } else {
            double targetY = this.state.cruiseTargetY();
            BlockPos targetPos = new BlockPos(
                    (int) Math.floor(fish.getX()), (int) Math.floor(targetY), (int) Math.floor(fish.getZ()));
            BlockState blockAtTarget = fish.level().getBlockState(targetPos);
            if (blockAtTarget.isSolid()) {
                double alt = targetY > fish.getY() ? fish.getY() + fish.cruiseDepthEmergencyOffset()
                        : fish.getY() - fish.cruiseDepthEmergencyOffset();
                this.state.setCruiseTargetY(Mth.lerp(0.35D, targetY, alt));
            }
        }
    }

    private void updateCruisePitchTarget(BaseFishEntity fish) {
        this.state.setCruisePitchDecisionCooldown(this.state.cruisePitchDecisionCooldown() - 1);

        if (this.state.cruisePitchDecisionCooldown() <= 0) {
            this.state.setCruisePitchDecisionCooldown(fish.cruisePitchDecisionMinTicks()
                    + fish.getRandom().nextInt(fish.cruisePitchDecisionRandomTicks()));
            double drift = (fish.getRandom().nextFloat() - 0.5F) * 0.7D;
            this.state.setCruiseTargetY(
                    Mth.lerp(0.25D, this.state.cruiseTargetY(), this.state.cruiseTargetY() + drift));
        }

        double depthDelta = this.state.cruiseTargetY() - fish.getY();
        float depthPitchTarget = (float) (-(Mth.atan2(depthDelta, fish.cruiseDepthPitchDistance()) * Mth.RAD_TO_DEG));
        if (depthDelta > 0.0D && this.isNearSurface(fish)) {
            depthPitchTarget *= 0.65F;
        } else if (depthDelta < 0.0D && this.isNearFloor(fish)) {
            depthPitchTarget *= 0.85F;
        }

        float motionPitchTarget = Mth.clamp(depthPitchTarget, -this.motionPitchLimit(fish), this.motionPitchLimit(fish));
        this.state.setMotionPitchTarget(motionPitchTarget);
        this.state.setCruisePitchTarget(Mth.clamp(depthPitchTarget, -fish.maxTiltDegrees(), fish.maxTiltDegrees()));
    }

    private void applyForwardMotion(BaseFishEntity fish, double acceleration, double maxSpeed,
            boolean includeVerticalAssist, boolean useSprintPropulsion, boolean flexible) {
        float pitchForMotion = this.motionPitchFor(fish);
        double propulsionMultiplier = this.computePropulsionMultiplier(fish, useSprintPropulsion);
        Vec3 movementDirection = Vec3.directionFromRotation(pitchForMotion, fish.getYRot()).normalize();

        double speedNearWall = this.wallProximitySpeedFactor(fish, movementDirection);
        Vec3 nextVelocity = fish.getDeltaMovement().scale(fish.waterDrag())
                .add(movementDirection.scale(acceleration * propulsionMultiplier * speedNearWall));

        if (includeVerticalAssist) {
            double depthDelta = this.state.cruiseTargetY() - fish.getY();
            double verticalAssist = Mth.clamp(depthDelta * 0.18D, -1.0D, 1.0D)
                    * fish.cruiseVerticalAssist();
            if (!flexible) {
                verticalAssist *= 0.78D;
            }
            if (verticalAssist > 0.0D) {
                verticalAssist *= this.isNearSurface(fish) ? 0.58D : 0.88D;
            } else if (verticalAssist < 0.0D) {
                verticalAssist *= this.isNearFloor(fish) ? 0.82D : 1.0D;
            }
            if (!this.hasWaterAbove(fish) && depthDelta > 0) {
                verticalAssist = -Math.abs(verticalAssist) * 1.15D;
            } else if (!this.hasWaterBelow(fish) && depthDelta < 0) {
                verticalAssist = Math.abs(verticalAssist) * 1.15D;
            }
            nextVelocity = nextVelocity.add(0.0D, verticalAssist, 0.0D);
        }

        double maxSpeedSqr = maxSpeed * maxSpeed;
        if (nextVelocity.lengthSqr() > maxSpeedSqr) {
            nextVelocity = nextVelocity.normalize().scale(maxSpeed);
        }

        fish.setDeltaMovement(nextVelocity);
        fish.hasImpulse = true;
    }

    private double wallProximitySpeedFactor(BaseFishEntity fish, Vec3 direction) {
        double currentSpeed = fish.getDeltaMovement().length();
        if (currentSpeed < 0.06D) {
            return 1.0D;
        }

        float yaw = this.yawFromDirection(direction);
        float pitch = this.pitchFromDirection(direction);
        double factor = 1.0D;

        double minDist = this.wallDistance(fish, yaw, pitch);
        if (minDist < WALL_PROXIMITY_SLOWDOWN && minDist >= 0.0D) {
            factor = Mth.clamp(minDist / WALL_PROXIMITY_SLOWDOWN, 0.35D, 1.0D);
        }
        return factor;
    }

    private double computePropulsionMultiplier(BaseFishEntity fish, boolean sprinting) {
        int intervalTicks = Math.max(1,
                sprinting ? fish.sprintPropulsionIntervalTicks() : fish.cruisePropulsionIntervalTicks());
        int burstTicks = Mth.clamp(sprinting ? fish.sprintPropulsionBurstTicks() : fish.cruisePropulsionBurstTicks(), 1,
                intervalTicks);

        if (this.state.propulsionSprintingMode() != sprinting
                || this.state.propulsionBurstDurationTicks() != burstTicks) {
            this.state.setPropulsionSprintingMode(sprinting);
            this.state.setPropulsionBurstDurationTicks(burstTicks);
            this.state.setPropulsionBurstTicksRemaining(0);
            this.state.setPropulsionTicksUntilBurst(0);
        }

        if (this.state.propulsionBurstTicksRemaining() <= 0) {
            if (this.state.propulsionTicksUntilBurst() > 0) {
                this.state.setPropulsionTicksUntilBurst(this.state.propulsionTicksUntilBurst() - 1);
                return sprinting ? fish.sprintPropulsionGlideAccelerationFactor()
                        : fish.cruisePropulsionGlideAccelerationFactor();
            }

            this.state.setPropulsionBurstTicksRemaining(burstTicks);
            this.state.setPropulsionTicksUntilBurst(Math.max(0, intervalTicks - burstTicks));
        }

        int ticksRemaining = this.state.propulsionBurstTicksRemaining();
        int durationTicks = Math.max(1, this.state.propulsionBurstDurationTicks());
        float phase = 1.0F - (((float) ticksRemaining - 0.5F) / (float) durationTicks);
        phase = Mth.clamp(phase, 0.0F, 1.0F);
        double envelope = Mth.sin(phase * Mth.PI);

        this.state.setPropulsionBurstTicksRemaining(ticksRemaining - 1);

        double pulseFactor = sprinting ? fish.sprintPropulsionBurstAccelerationFactor()
                : fish.cruisePropulsionBurstAccelerationFactor();
        return pulseFactor * (0.72D + envelope * 0.28D);
    }

    private void tryAttackPlayer(BaseFishEntity fish, Player target) {
        if (target == null || fish.attackMode() == FishAttackMode.NONE || this.biteCooldownTicks > 0) {
            return;
        }

        if (!this.isTargetablePlayer(fish, target, fish.behaviorPersistenceRangeMultiplier())) {
            return;
        }

        if (fish.getBoundingBox().inflate(0.28D).intersects(target.getBoundingBox().inflate(0.15D))) {
            this.dealBiteDamage(fish, target);
            return;
        }

        double closeContactReach = Math.max(1.45D, fish.getBbWidth() * 0.95D + target.getBbWidth() * 0.85D);
        if (fish.distanceToSqr(target) <= closeContactReach * closeContactReach) {
            this.dealBiteDamage(fish, target);
            return;
        }

        if (fish.attackMode() == FishAttackMode.PASS_BY_BITE) {
            if (!this.isPassByStrikeWindow(fish, target)) {
                return;
            }
        } else {
            double biteReach = fish.getBbWidth() * 0.7D + target.getBbWidth() * 0.6D + fish.biteReachBonus();
            if (fish.distanceToSqr(target) > biteReach * biteReach) {
                return;
            }
        }

        this.dealBiteDamage(fish, target);
    }

    private void dealBiteDamage(BaseFishEntity fish, Player target) {
        float damage = (float) Math.max(fish.baseBiteDamage(), fish.getAttributeValue(Attributes.ATTACK_DAMAGE));
        if (!target.hurt(fish.damageSources().mobAttack(fish), damage)) {
            return;
        }

        this.biteCooldownTicks = fish.biteCooldownTicks();
        fish.onSuccessfulBite(target);

        if (fish.attackMode() == FishAttackMode.PASS_BY_BITE) {
            this.passByRetreatTicks = fish.passByRetreatTicks();
            this.passByReengageCooldownTicks = fish.passByReengageCooldownTicks();

            Vec3 forwardDirection = Vec3.directionFromRotation(this.motionPitchFor(fish), fish.getYRot()).normalize();
            Vec3 boostedVelocity = fish.getDeltaMovement().add(forwardDirection.scale(fish.passByInertiaBoost()));
            double maxInertiaSpeed = fish.chargeMaxSpeed() * fish.passByInertiaMaxSpeedMultiplier();
            double maxInertiaSpeedSqr = maxInertiaSpeed * maxInertiaSpeed;
            if (boostedVelocity.lengthSqr() > maxInertiaSpeedSqr) {
                boostedVelocity = boostedVelocity.normalize().scale(maxInertiaSpeed);
            }

            fish.setDeltaMovement(boostedVelocity);
            fish.hasImpulse = true;
        }
    }

    private boolean isPassByStrikeWindow(BaseFishEntity fish, Player target) {
        Vec3 forwardDirection = Vec3.directionFromRotation(this.motionPitchFor(fish), fish.getYRot()).normalize();
        Vec3 fishCenter = fish.position().add(0.0D, fish.getBbHeight() * 0.45D, 0.0D);
        Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.45D, 0.0D);

        Vec3 toTarget = targetCenter.subtract(fishCenter);
        double forwardDistance = toTarget.dot(forwardDirection);

        double maxForwardDistance = Math.max(0.4D, fish.passByStrikeForwardDistance());
        double maxRearAllowance = Math.max(0.1D, fish.passByStrikeRearAllowance());
        if (forwardDistance > maxForwardDistance || forwardDistance < -maxRearAllowance) {
            return false;
        }

        Vec3 lateralOffset = toTarget.subtract(forwardDirection.scale(forwardDistance));
        double strikeRadius = fish.passByStrikeRadius() + target.getBbWidth() * 0.45D;
        return lateralOffset.lengthSqr() <= strikeRadius * strikeRadius;
    }

    private void updateBodyAndHeadRotation(BaseFishEntity fish) {
        float bodyTurnRate = fish.bodyTurnRateDegrees();
        fish.yBodyRot = this.rotateTowards(fish.yBodyRot, fish.getYRot(), bodyTurnRate);
        fish.yHeadRot = this.rotateTowards(fish.yHeadRot, fish.getYRot(), bodyTurnRate * 1.25F);
    }

    private Vec3 steerAroundBarriers(BaseFishEntity fish, Vec3 desiredDirection) {
        if (desiredDirection.lengthSqr() < 1.0E-6D) {
            return desiredDirection;
        }

        FishSteeringMath.Direction direction = FishSteeringMath.fromVec3(desiredDirection);
        direction = FishSteeringMath.normalizeOrFallback(direction, direction);
        direction = FishSteeringMath.applyVerticalBias(direction, this.isNearSurface(fish), this.isNearFloor(fish),
                this.hasWaterAbove(fish), this.hasWaterBelow(fish));
        direction = FishSteeringMath.chooseBarrierAwareDirection(direction, true,
                candidate -> this.directionClearanceScore(fish, candidate));
        return FishSteeringMath.toVec3(direction);
    }

    private double directionClearanceScore(BaseFishEntity fish, FishSteeringMath.Direction direction) {
        Vec3 candidate = FishSteeringMath.toVec3(direction);
        if (candidate.lengthSqr() < 1.0E-6D) {
            return 0.0D;
        }

        float yaw = this.yawFromDirection(candidate);
        float pitch = this.pitchFromDirection(candidate);
        double score = this.wallDistance(fish, yaw, pitch);

        if (candidate.y() > 0.0D) {
            if (!this.hasWaterAbove(fish)) {
                score -= 4.0D;
            } else if (this.isNearSurface(fish)) {
                score -= 1.4D * candidate.y();
            } else {
                score -= 0.35D * candidate.y();
            }
        } else if (candidate.y() < 0.0D) {
            if (!this.hasWaterBelow(fish)) {
                score -= 3.5D;
            } else if (this.isNearFloor(fish)) {
                score -= 0.85D * Math.abs(candidate.y());
            }
        }

        return score;
    }

    private float motionPitchFor(BaseFishEntity fish) {
        return fish.isFlexibleBody() ? fish.getXRot() : this.state.motionPitchTarget();
    }

    private float motionPitchLimit(BaseFishEntity fish) {
        return Math.max(45.0F, fish.maxTiltDegrees());
    }

    private boolean isBarrierAt(BaseFishEntity fish, BlockPos pos) {
        return !fish.level().getFluidState(pos).is(FluidTags.WATER) && fish.level().getBlockState(pos).isSolid();
    }

    private boolean isProbePointBlocked(BaseFishEntity fish, Vec3 point, double horizontalRadius, double verticalRadius) {
        BlockPos center = new BlockPos((int) Math.floor(point.x), (int) Math.floor(point.y), (int) Math.floor(point.z));
        int xRadius = Math.max(1, (int) Math.ceil(horizontalRadius));
        int yRadius = Math.max(1, (int) Math.ceil(verticalRadius));

        for (int dx = -xRadius; dx <= xRadius; dx++) {
            for (int dy = -yRadius; dy <= yRadius; dy++) {
                for (int dz = -xRadius; dz <= xRadius; dz++) {
                    if (this.isBarrierAt(fish, center.offset(dx, dy, dz))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isDirectionBlocked(BaseFishEntity fish, float yaw, float pitch, double distance) {
        Vec3 direction = Vec3.directionFromRotation(pitch, yaw).normalize();
        Vec3 origin = fish.position().add(0.0D, fish.getBbHeight() * 0.5D, 0.0D);
        double fishWidth = fish.getBbWidth() * 0.5D;
        double fishHeight = fish.getBbHeight() * 0.5D;

        int steps = Math.max(2, (int) Math.ceil(distance / BARRIER_CHECK_STEP));
        for (int i = 1; i <= steps; i++) {
            double t = i * (distance / steps);
            Vec3 point = origin.add(direction.scale(t));

            if (this.isProbePointBlocked(fish, point, fishWidth, fishHeight)) {
                return true;
            }
        }

        return false;
    }

    private float findClearYaw(BaseFishEntity fish, float preferredYaw, float searchRange) {
        float pitch = this.motionPitchFor(fish);
        if (!this.isDirectionBlocked(fish, preferredYaw, pitch, BARRIER_CHECK_DISTANCE)) {
            return preferredYaw;
        }

        float bestYaw = preferredYaw;
        float bestBlocked = Float.MAX_VALUE;

        for (float offset = 10.0F; offset <= searchRange; offset += 8.0F) {
            float candidate = preferredYaw + offset;
            if (!this.isDirectionBlocked(fish, candidate, pitch, BARRIER_CHECK_DISTANCE)) {
                return candidate;
            }
            double dist = this.wallDistance(fish, candidate, pitch);
            if (dist < bestBlocked) {
                bestBlocked = (float) dist;
                bestYaw = candidate;
            }

            candidate = preferredYaw - offset;
            if (!this.isDirectionBlocked(fish, candidate, pitch, BARRIER_CHECK_DISTANCE)) {
                return candidate;
            }
            dist = this.wallDistance(fish, candidate, pitch);
            if (dist < bestBlocked) {
                bestBlocked = (float) dist;
                bestYaw = candidate;
            }
        }

        return bestYaw;
    }

    private double wallDistance(BaseFishEntity fish, float yaw, float pitch) {
        Vec3 direction = Vec3.directionFromRotation(pitch, yaw).normalize();
        Vec3 origin = fish.position().add(0.0D, fish.getBbHeight() * 0.5D, 0.0D);
        double fishWidth = fish.getBbWidth() * 0.5D;
        double fishHeight = fish.getBbHeight() * 0.5D;

        double maxDist = BARRIER_CHECK_DISTANCE + 1.0D;
        int steps = Math.max(2, (int) Math.ceil(maxDist / BARRIER_CHECK_STEP));

        for (int i = 1; i <= steps; i++) {
            double t = i * (maxDist / steps);
            Vec3 point = origin.add(direction.scale(t));

            if (this.isProbePointBlocked(fish, point, fishWidth, fishHeight)) {
                return t;
            }
        }

        return maxDist;
    }

    private boolean hasWaterAbove(BaseFishEntity fish) {
        return fish.level().getFluidState(fish.blockPosition().above()).is(FluidTags.WATER)
                && fish.level().getFluidState(fish.blockPosition().above(2)).is(FluidTags.WATER);
    }

    private boolean hasWaterBelow(BaseFishEntity fish) {
        return fish.level().getFluidState(fish.blockPosition().below()).is(FluidTags.WATER)
                && fish.level().getFluidState(fish.blockPosition().below(2)).is(FluidTags.WATER);
    }

    private boolean isNearSurface(BaseFishEntity fish) {
        return fish.level().getFluidState(fish.blockPosition().above(2)).is(FluidTags.WATER)
                && !fish.level().getFluidState(fish.blockPosition().above(4)).is(FluidTags.WATER);
    }

    private boolean isNearFloor(BaseFishEntity fish) {
        return fish.level().getFluidState(fish.blockPosition().below(2)).is(FluidTags.WATER)
                && !fish.level().getFluidState(fish.blockPosition().below(4)).is(FluidTags.WATER);
    }

    private float rotateTowards(float current, float target, float maxChange) {
        float delta = Mth.wrapDegrees(target - current);
        return current + Mth.clamp(delta, -maxChange, maxChange);
    }

    private float yawFromDirection(Vec3 direction) {
        return (float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0F;
    }

    private float pitchFromDirection(Vec3 direction) {
        return (float) (-(Mth.atan2(direction.y, direction.horizontalDistance()) * Mth.RAD_TO_DEG));
    }

    private enum MovementMode {
        CRUISE,
        ESCAPE,
        INERTIA,
        CHARGE
    }

    private record BehaviorDecision(MovementMode mode, Player target) {
    }
}
