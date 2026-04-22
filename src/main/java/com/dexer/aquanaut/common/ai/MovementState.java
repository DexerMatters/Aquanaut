package com.dexer.aquanaut.common.ai;

public class MovementState {
    private boolean sprintingAway;
    private boolean chargingPlayer;
    private boolean propulsionSprintingMode;
    private int propulsionTicksUntilBurst;
    private int propulsionBurstTicksRemaining;
    private int propulsionBurstDurationTicks;
    private int collisionTurnCooldown;
    private int steeringLockTicks;
    private int cruiseYawDecisionCooldown;
    private int cruisePitchDecisionCooldown;
    private int cruiseDepthDecisionCooldown;
    private float cruiseYawTarget;
    private float cruisePitchTarget;
    private double cruiseTargetY;

    public boolean isSprintingAway() {
        return this.sprintingAway;
    }

    public void setSprintingAway(boolean sprintingAway) {
        this.sprintingAway = sprintingAway;
    }

    public boolean isChargingPlayer() {
        return this.chargingPlayer;
    }

    public void setChargingPlayer(boolean chargingPlayer) {
        this.chargingPlayer = chargingPlayer;
    }

    public boolean propulsionSprintingMode() {
        return this.propulsionSprintingMode;
    }

    public void setPropulsionSprintingMode(boolean propulsionSprintingMode) {
        this.propulsionSprintingMode = propulsionSprintingMode;
    }

    public int propulsionTicksUntilBurst() {
        return this.propulsionTicksUntilBurst;
    }

    public void setPropulsionTicksUntilBurst(int propulsionTicksUntilBurst) {
        this.propulsionTicksUntilBurst = propulsionTicksUntilBurst;
    }

    public int propulsionBurstTicksRemaining() {
        return this.propulsionBurstTicksRemaining;
    }

    public void setPropulsionBurstTicksRemaining(int propulsionBurstTicksRemaining) {
        this.propulsionBurstTicksRemaining = propulsionBurstTicksRemaining;
    }

    public int propulsionBurstDurationTicks() {
        return this.propulsionBurstDurationTicks;
    }

    public void setPropulsionBurstDurationTicks(int propulsionBurstDurationTicks) {
        this.propulsionBurstDurationTicks = propulsionBurstDurationTicks;
    }

    public int collisionTurnCooldown() {
        return this.collisionTurnCooldown;
    }

    public void setCollisionTurnCooldown(int collisionTurnCooldown) {
        this.collisionTurnCooldown = collisionTurnCooldown;
    }

    public int steeringLockTicks() {
        return this.steeringLockTicks;
    }

    public void setSteeringLockTicks(int steeringLockTicks) {
        this.steeringLockTicks = steeringLockTicks;
    }

    public int cruiseYawDecisionCooldown() {
        return this.cruiseYawDecisionCooldown;
    }

    public void setCruiseYawDecisionCooldown(int cruiseYawDecisionCooldown) {
        this.cruiseYawDecisionCooldown = cruiseYawDecisionCooldown;
    }

    public int cruisePitchDecisionCooldown() {
        return this.cruisePitchDecisionCooldown;
    }

    public void setCruisePitchDecisionCooldown(int cruisePitchDecisionCooldown) {
        this.cruisePitchDecisionCooldown = cruisePitchDecisionCooldown;
    }

    public int cruiseDepthDecisionCooldown() {
        return this.cruiseDepthDecisionCooldown;
    }

    public void setCruiseDepthDecisionCooldown(int cruiseDepthDecisionCooldown) {
        this.cruiseDepthDecisionCooldown = cruiseDepthDecisionCooldown;
    }

    public float cruiseYawTarget() {
        return this.cruiseYawTarget;
    }

    public void setCruiseYawTarget(float cruiseYawTarget) {
        this.cruiseYawTarget = cruiseYawTarget;
    }

    public float cruisePitchTarget() {
        return this.cruisePitchTarget;
    }

    public void setCruisePitchTarget(float cruisePitchTarget) {
        this.cruisePitchTarget = cruisePitchTarget;
    }

    public double cruiseTargetY() {
        return this.cruiseTargetY;
    }

    public void setCruiseTargetY(double cruiseTargetY) {
        this.cruiseTargetY = cruiseTargetY;
    }

    public void reset(float currentYaw, double currentY) {
        this.sprintingAway = false;
        this.chargingPlayer = false;
        this.propulsionSprintingMode = false;
        this.propulsionTicksUntilBurst = 0;
        this.propulsionBurstTicksRemaining = 0;
        this.propulsionBurstDurationTicks = 0;
        this.collisionTurnCooldown = 0;
        this.steeringLockTicks = 0;
        this.cruiseYawDecisionCooldown = 0;
        this.cruisePitchDecisionCooldown = 0;
        this.cruiseDepthDecisionCooldown = 0;
        this.cruiseYawTarget = currentYaw;
        this.cruisePitchTarget = 0.0F;
        this.cruiseTargetY = currentY;
    }
}
