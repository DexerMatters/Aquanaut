package com.dexer.aquanaut.common.ai;

import com.dexer.aquanaut.common.entity.BaseFishEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class PlayerAvoidanceLogic {
    private final AvoidanceDirectionStrategy directionStrategy;
    private final EvasionState evasionState = new EvasionState();

    public PlayerAvoidanceLogic() {
        this(new ThrowAwayAvoidanceDirectionStrategy());
    }

    public PlayerAvoidanceLogic(AvoidanceDirectionStrategy directionStrategy) {
        this.directionStrategy = directionStrategy;
    }

    public AvoidanceResult compute(BaseFishEntity fish, double detectionRange) {
        Player threat = this.findThreatPlayer(fish, detectionRange);
        return this.computeAgainst(fish, threat);
    }

    public AvoidanceResult computeAgainst(BaseFishEntity fish, Player threat) {
        if (threat == null || !this.isThreatPlayerValid(fish, threat)) {
            this.reset();
            return null;
        }

        Vec3 thrownAwayDirection = this.directionStrategy.computeEscapeDirection(fish, threat, this.evasionState);
        if (thrownAwayDirection.lengthSqr() < 1.0E-6D) {
            thrownAwayDirection = Vec3.directionFromRotation(fish.getXRot(), fish.getYRot());
        }

        return new AvoidanceResult(threat, thrownAwayDirection.normalize());
    }

    public void reset() {
        this.evasionState.reset();
    }

    private Player findThreatPlayer(BaseFishEntity fish, double detectionRange) {
        Player nearestPlayer = fish.level().getNearestPlayer(fish, detectionRange);
        if (nearestPlayer == null || !this.isThreatPlayerValid(fish, nearestPlayer)) {
            return null;
        }

        return nearestPlayer;
    }

    private boolean isThreatPlayerValid(BaseFishEntity fish, Player player) {
        if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
            return false;
        }

        return fish.hasLineOfSight(player);
    }

    public record AvoidanceResult(Player target, Vec3 escapeDirection) {
    }

    public static final class EvasionState {
        private int directionCooldown;
        private int evasiveSideSign = 1;
        private float evasiveVerticalBias;

        public void reset() {
            this.directionCooldown = 0;
            this.evasiveVerticalBias = 0.0F;
        }

        public int directionCooldown() {
            return this.directionCooldown;
        }

        public void decrementDirectionCooldown() {
            this.directionCooldown--;
        }

        public void setDirectionCooldown(int directionCooldown) {
            this.directionCooldown = directionCooldown;
        }

        public int evasiveSideSign() {
            return this.evasiveSideSign;
        }

        public void setEvasiveSideSign(int evasiveSideSign) {
            this.evasiveSideSign = evasiveSideSign;
        }

        public float evasiveVerticalBias() {
            return this.evasiveVerticalBias;
        }

        public void setEvasiveVerticalBias(float evasiveVerticalBias) {
            this.evasiveVerticalBias = evasiveVerticalBias;
        }
    }
}
