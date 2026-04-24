package com.dexer.aquanaut.common.ai;

import com.dexer.aquanaut.common.entity.BaseFishEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class FishMovementController {
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

        if (decision.mode == MovementMode.CRUISE) {
            FishSchoolingAI.SchoolingDecision schoolingDecision = this.schoolingAI.resolve(fish);
            if (schoolingDecision.active()) {
                if (this.state.steeringLockTicks() <= 0) {
                    this.applySchoolSteering(fish, schoolingDecision);
                }

                this.applyForwardMotion(fish, fish.cruiseAcceleration() * schoolingDecision.speedMultiplier(),
                        fish.cruiseMaxSpeed() * schoolingDecision.speedMultiplier(), true, false);
                this.updateBodyAndHeadRotation(fish);
                return;
            }

            if (this.state.steeringLockTicks() <= 0) {
                if (fish.curvedCruiseMovement()) {
                    this.applyCurvedCruiseSteering(fish);
                } else {
                    this.applyCruiseSteering(fish);
                }
            }
            this.applyForwardMotion(fish, fish.cruiseAcceleration(), fish.cruiseMaxSpeed(), true, false);
        } else if (decision.mode == MovementMode.ESCAPE) {
            if (fish.escapeLaunchBehaviorEnabled()) {
                this.applyEscapeLaunchMotion(fish, decision.target);
            } else {
                if (this.state.steeringLockTicks() <= 0) {
                    this.applyEscapeSteering(fish, decision.target);
                }
                this.applyForwardMotion(fish, fish.escapeAcceleration(), fish.escapeMaxSpeed(), false, true);
            }
        } else if (decision.mode == MovementMode.INERTIA) {
            this.clearEscapeLaunchState();
            this.applyPassByInertiaMotion(fish);
        } else {
            this.clearEscapeLaunchState();
            if (this.state.steeringLockTicks() <= 0) {
                this.applyChargeSteering(fish, decision.target);
            }
            this.applyForwardMotion(fish, fish.chargeAcceleration(), fish.chargeMaxSpeed(), false, true);
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
            if (escapeTarget != null || this.isEscapeLaunchActive(fish)) {
                return new BehaviorDecision(MovementMode.ESCAPE, escapeTarget);
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
        if (fish.horizontalCollision && this.state.collisionTurnCooldown() <= 0) {
            float reboundYaw = fish.getYRot() + 150.0F + (fish.getRandom().nextFloat() - 0.5F) * 70.0F;
            fish.setYRot(reboundYaw);
            fish.yBodyRot = reboundYaw;
            fish.yHeadRot = reboundYaw;
            this.state.setCruiseYawTarget(reboundYaw);

            Vec3 reboundDir = Vec3.directionFromRotation(fish.getXRot(), reboundYaw).normalize();
            fish.setDeltaMovement(fish.getDeltaMovement().scale(0.45D).add(reboundDir.scale(0.09D)));
            fish.hasImpulse = true;
            this.state.setCollisionTurnCooldown(fish.collisionTurnCooldownTicks());
            this.state.setSteeringLockTicks(Math.max(this.state.steeringLockTicks(), 3));
        }

        if (fish.verticalCollision && this.state.collisionTurnCooldown() <= 0) {
            float reboundPitch = Mth.clamp(-fish.getXRot() * 0.8F, -fish.maxTiltDegrees(), fish.maxTiltDegrees());
            if (!this.hasWaterAbove(fish)) {
                reboundPitch = Mth.clamp(Math.abs(reboundPitch) + 12.0F, 8.0F, fish.maxTiltDegrees());
            } else if (!this.hasWaterBelow(fish)) {
                reboundPitch = Mth.clamp(-Math.abs(reboundPitch) - 12.0F, -fish.maxTiltDegrees(), -8.0F);
            }

            fish.setXRot(reboundPitch);
            this.state.setCollisionTurnCooldown(fish.collisionTurnCooldownTicks());
            this.state.setSteeringLockTicks(Math.max(this.state.steeringLockTicks(), 2));
        }
    }

    private void applyEscapeSteering(BaseFishEntity fish, Player escapeFrom) {
        Vec3 desiredDirection = this.resolveEscapeDirection(fish, escapeFrom);
        if (desiredDirection.lengthSqr() < 1.0E-6D) {
            return;
        }
        float targetYaw = this.yawFromDirection(desiredDirection);
        float targetPitch = Mth.clamp(this.pitchFromDirection(desiredDirection), -fish.maxTiltDegrees(),
                fish.maxTiltDegrees());

        fish.setYRot(this.rotateTowards(fish.getYRot(), targetYaw, fish.escapeTurnRateDegrees()));
        fish.setXRot(this.rotateTowards(fish.getXRot(), targetPitch, fish.escapeTurnRateDegrees() * 0.7F));
        this.state.setCruiseYawTarget(fish.getYRot());
    }

    private void applyEscapeLaunchMotion(BaseFishEntity fish, Player escapeFrom) {
        Vec3 desiredDirection = this.resolveEscapeDirection(fish, escapeFrom);
        if (desiredDirection.lengthSqr() < 1.0E-6D) {
            desiredDirection = this.escapeLaunchDirection.lengthSqr() > 1.0E-6D
                    ? this.escapeLaunchDirection
                    : Vec3.directionFromRotation(fish.getXRot(), fish.getYRot()).normalize();
        }

        desiredDirection = desiredDirection.normalize();

        if (this.escapeLaunchTicksRemaining <= 0) {
            this.escapeLaunchTicksRemaining = Math.max(1, fish.escapeLaunchAnimationTicks());
            this.escapeLaunchBurstTriggered = false;
            this.escapeLaunchDirection = desiredDirection;
        }

        if (!this.escapeLaunchBurstTriggered) {
            this.escapeLaunchDirection = desiredDirection;

            if (this.state.steeringLockTicks() <= 0) {
                float targetYaw = this.yawFromDirection(this.escapeLaunchDirection);
                float targetPitch = Mth.clamp(this.pitchFromDirection(this.escapeLaunchDirection),
                        -fish.maxTiltDegrees(), fish.maxTiltDegrees());
                fish.setYRot(this.rotateTowards(fish.getYRot(), targetYaw, fish.escapeTurnRateDegrees()));
                fish.setXRot(this.rotateTowards(fish.getXRot(), targetPitch, fish.escapeTurnRateDegrees() * 0.7F));
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

    private void clearEscapeLaunchState() {
        this.escapeLaunchTicksRemaining = 0;
        this.escapeLaunchBurstTriggered = false;
        this.escapeLaunchDirection = Vec3.ZERO;
    }

    private void applyChargeSteering(BaseFishEntity fish, Player target) {
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

        desiredDirection = desiredDirection.normalize();
        float targetYaw = this.yawFromDirection(desiredDirection);
        float targetPitch = Mth.clamp(this.pitchFromDirection(desiredDirection), -fish.maxTiltDegrees(),
                fish.maxTiltDegrees());

        fish.setYRot(this.rotateTowards(fish.getYRot(), targetYaw, fish.chargeTurnRateDegrees()));
        fish.setXRot(this.rotateTowards(fish.getXRot(), targetPitch, fish.chargePitchTurnRateDegrees()));
        this.state.setCruiseYawTarget(fish.getYRot());
    }

    private void applyPassByInertiaMotion(BaseFishEntity fish) {
        Vec3 forwardDirection = Vec3.directionFromRotation(fish.getXRot(), fish.getYRot()).normalize();
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

    private void applyCruiseSteering(BaseFishEntity fish) {
        this.updateCruiseYawTarget(fish);
        fish.setYRot(this.rotateTowards(fish.getYRot(), this.state.cruiseYawTarget(), fish.cruiseYawTurnRateDegrees()));

        this.updateCruiseDepthTarget(fish);
        this.updateCruisePitchTarget(fish);
        fish.setXRot(
                this.rotateTowards(fish.getXRot(), this.state.cruisePitchTarget(), fish.cruisePitchTurnRateDegrees()));
    }

    private void applyCurvedCruiseSteering(BaseFishEntity fish) {
        this.updateCruiseDepthTarget(fish);
        this.updateCruisePitchTarget(fish);
        fish.setXRot(
                this.rotateTowards(fish.getXRot(), this.state.cruisePitchTarget(), fish.cruisePitchTurnRateDegrees()));

        float curveTorque = fish.cruiseCurveTorqueDegrees();
        if (Math.abs(curveTorque) <= 1.0E-4F) {
            return;
        }

        float curveDirection = (fish.getUUID().hashCode() & 1) == 0 ? 1.0F : -1.0F;
        fish.setYRot(fish.getYRot() + curveDirection * curveTorque);
        this.state.setCruiseYawTarget(fish.getYRot());
    }

    private void applySchoolSteering(BaseFishEntity fish, FishSchoolingAI.SchoolingDecision schoolingDecision) {
        Vec3 desiredDirection = schoolingDecision.desiredDirection();
        if (desiredDirection.lengthSqr() < 1.0E-6D) {
            return;
        }

        float targetYaw = this.yawFromDirection(desiredDirection);
        float targetPitch = Mth.clamp(this.pitchFromDirection(desiredDirection), -fish.maxTiltDegrees(),
                fish.maxTiltDegrees());

        fish.setYRot(this.rotateTowards(fish.getYRot(), targetYaw, fish.cruiseYawTurnRateDegrees()));
        fish.setXRot(this.rotateTowards(fish.getXRot(), targetPitch, fish.cruisePitchTurnRateDegrees()));
        this.state.setCruiseYawTarget(fish.getYRot());
        this.state.setCruisePitchTarget(targetPitch);

        double depthDelta = schoolingDecision.schoolCenterY() - fish.getY();
        double depthCorrection = Math.abs(depthDelta) < 0.6D ? 0.0D : Mth.clamp(depthDelta * 0.08D, -0.14D, 0.14D);
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
                this.state.setCruiseYawTarget(fish.getYRot() + yawOffset);
            } else {
                this.state.setCruiseYawTarget(fish.getYRot());
            }
        }
    }

    private void updateCruiseDepthTarget(BaseFishEntity fish) {
        this.state.setCruiseDepthDecisionCooldown(this.state.cruiseDepthDecisionCooldown() - 1);

        if (this.state.cruiseDepthDecisionCooldown() <= 0) {
            this.state.setCruiseDepthDecisionCooldown(fish.cruisePitchDecisionMinTicks()
                    + fish.getRandom().nextInt(fish.cruisePitchDecisionRandomTicks()));
            double offsetMagnitude = 1.5D
                    + fish.getRandom().nextDouble() * Math.max(0.1D, fish.cruiseDepthRange() - 1.5D);
            double offset = fish.getRandom().nextBoolean() ? offsetMagnitude : -offsetMagnitude;
            this.state.setCruiseTargetY(fish.getY() + offset);
        }

        if (!this.hasWaterAbove(fish)) {
            this.state.setCruiseTargetY(fish.getY() - fish.cruiseDepthEmergencyOffset());
        } else if (!this.hasWaterBelow(fish)) {
            this.state.setCruiseTargetY(fish.getY() + fish.cruiseDepthEmergencyOffset());
        }
    }

    private void updateCruisePitchTarget(BaseFishEntity fish) {
        this.state.setCruisePitchDecisionCooldown(this.state.cruisePitchDecisionCooldown() - 1);

        if (this.state.cruisePitchDecisionCooldown() <= 0) {
            this.state.setCruisePitchDecisionCooldown(fish.cruisePitchDecisionMinTicks()
                    + fish.getRandom().nextInt(fish.cruisePitchDecisionRandomTicks()));
            this.state.setCruiseTargetY(this.state.cruiseTargetY() + (fish.getRandom().nextFloat() - 0.5F) * 1.5F);
        }

        double depthDelta = this.state.cruiseTargetY() - fish.getY();
        float depthPitchTarget = (float) (-(Mth.atan2(depthDelta, fish.cruiseDepthPitchDistance()) * Mth.RAD_TO_DEG));
        this.state.setCruisePitchTarget(Mth.clamp(depthPitchTarget, -fish.maxTiltDegrees(), fish.maxTiltDegrees()));
    }

    private void applyForwardMotion(BaseFishEntity fish, double acceleration, double maxSpeed,
            boolean includeVerticalAssist, boolean useSprintPropulsion) {
        double propulsionMultiplier = this.computePropulsionMultiplier(fish, useSprintPropulsion);
        Vec3 movementDirection = Vec3.directionFromRotation(fish.getXRot(), fish.getYRot()).normalize();
        Vec3 nextVelocity = fish.getDeltaMovement().scale(fish.waterDrag())
                .add(movementDirection.scale(acceleration * propulsionMultiplier));

        if (includeVerticalAssist) {
            double verticalAssist = Mth.clamp(this.state.cruiseTargetY() - fish.getY(), -1.0D, 1.0D)
                    * fish.cruiseVerticalAssist();
            nextVelocity = nextVelocity.add(0.0D, verticalAssist, 0.0D);
        }

        double maxSpeedSqr = maxSpeed * maxSpeed;
        if (nextVelocity.lengthSqr() > maxSpeedSqr) {
            nextVelocity = nextVelocity.normalize().scale(maxSpeed);
        }

        fish.setDeltaMovement(nextVelocity);
        fish.hasImpulse = true;
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

            Vec3 forwardDirection = Vec3.directionFromRotation(fish.getXRot(), fish.getYRot()).normalize();
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
        Vec3 forwardDirection = Vec3.directionFromRotation(fish.getXRot(), fish.getYRot()).normalize();
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

    private boolean hasWaterAbove(BaseFishEntity fish) {
        return fish.level().getFluidState(fish.blockPosition().above()).is(net.minecraft.tags.FluidTags.WATER)
                && fish.level().getFluidState(fish.blockPosition().above(2)).is(net.minecraft.tags.FluidTags.WATER);
    }

    private boolean hasWaterBelow(BaseFishEntity fish) {
        return fish.level().getFluidState(fish.blockPosition().below()).is(net.minecraft.tags.FluidTags.WATER)
                && fish.level().getFluidState(fish.blockPosition().below(2)).is(net.minecraft.tags.FluidTags.WATER);
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
