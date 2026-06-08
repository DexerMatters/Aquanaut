package com.dexer.aquanaut.common.ai;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.function.ToDoubleFunction;

final class FishSteeringMath {
    private static final double EPSILON = 1.0E-6D;
    private static final float[] YAW_SEARCH_OFFSETS = { 0.0F, 18.0F, -18.0F, 36.0F, -36.0F, 60.0F, -60.0F, 90.0F,
            -90.0F };
    private static final float[] PITCH_SEARCH_OFFSETS = { 0.0F, 10.0F, -10.0F, 22.0F, -22.0F, 35.0F, -35.0F };

    private FishSteeringMath() {
    }

    static Direction direction(double x, double y, double z) {
        return new Direction(x, y, z);
    }

    static Direction fromVec3(Vec3 vector) {
        return new Direction(vector.x, vector.y, vector.z);
    }

    static Vec3 toVec3(Direction direction) {
        return new Vec3(direction.x(), direction.y(), direction.z());
    }

    static Direction normalizeOrFallback(Direction vector, Direction fallback) {
        if (vector.lengthSqr() < EPSILON) {
            return fallback;
        }

        return vector.normalize();
    }

    static Direction applyVerticalBias(Direction direction, boolean nearSurface, boolean nearFloor, boolean hasWaterAbove,
            boolean hasWaterBelow) {
        if (direction.lengthSqr() < EPSILON) {
            return direction;
        }

        Direction adjusted = direction;
        if (adjusted.y() > 0.0D) {
            double upwardScale = !hasWaterAbove ? 0.08D : nearSurface ? 0.35D : 0.84D;
            adjusted = new Direction(adjusted.x(), adjusted.y() * upwardScale, adjusted.z());
        } else if (adjusted.y() < 0.0D) {
            double downwardScale = !hasWaterBelow ? 0.10D : nearFloor ? 0.82D : 1.05D;
            adjusted = new Direction(adjusted.x(), adjusted.y() * downwardScale, adjusted.z());
        }

        return normalizeOrFallback(adjusted, direction);
    }

    static Direction chooseBarrierAwareDirection(Direction desiredDirection, boolean allowVerticalSearch,
            ToDoubleFunction<Direction> clearanceScore) {
        if (desiredDirection.lengthSqr() < EPSILON) {
            return desiredDirection;
        }

        Direction normalized = desiredDirection.normalize();
        Direction bestDirection = normalized;
        double bestScore = clearanceScore.applyAsDouble(normalized) + 1.0D;
        float baseYaw = yawFromDirection(normalized);
        float basePitch = pitchFromDirection(normalized);

        float[] yawOffsets = allowVerticalSearch ? YAW_SEARCH_OFFSETS
                : new float[] { 0.0F, 18.0F, -18.0F, 36.0F, -36.0F, 60.0F, -60.0F };
        float[] pitchOffsets = allowVerticalSearch ? PITCH_SEARCH_OFFSETS : new float[] { 0.0F };

        for (float yawOffset : yawOffsets) {
            for (float pitchOffset : pitchOffsets) {
                if (yawOffset == 0.0F && pitchOffset == 0.0F) {
                    continue;
                }

                Direction candidate = directionFromRotation(basePitch + pitchOffset, baseYaw + yawOffset).normalize();
                double score = clearanceScore.applyAsDouble(candidate);
                score += normalized.dot(candidate) * 1.15D;
                score -= Math.abs(candidate.y() - normalized.y()) * 0.06D;

                if (score > bestScore + 1.0E-6D) {
                    bestDirection = candidate;
                    bestScore = score;
                }
            }
        }

        return bestDirection.normalize();
    }

    static float yawFromDirection(Direction direction) {
        return (float) (Mth.atan2(direction.z(), direction.x()) * Mth.RAD_TO_DEG) - 90.0F;
    }

    static float pitchFromDirection(Direction direction) {
        return (float) (-(Mth.atan2(direction.y(), direction.horizontalDistance()) * Mth.RAD_TO_DEG));
    }

    static Direction directionFromRotation(float pitch, float yaw) {
        double pitchRad = pitch * Mth.DEG_TO_RAD;
        double yawRad = yaw * Mth.DEG_TO_RAD;
        double horizontal = Math.cos(pitchRad);
        return new Direction(-Math.sin(yawRad) * horizontal, -Math.sin(pitchRad), Math.cos(yawRad) * horizontal);
    }

    record Direction(double x, double y, double z) {
        double lengthSqr() {
            return this.x * this.x + this.y * this.y + this.z * this.z;
        }

        double horizontalDistance() {
            return Math.sqrt(this.x * this.x + this.z * this.z);
        }

        Direction normalize() {
            double length = Math.sqrt(this.lengthSqr());
            if (length < EPSILON) {
                return this;
            }
            return new Direction(this.x / length, this.y / length, this.z / length);
        }

        Direction scale(double factor) {
            return new Direction(this.x * factor, this.y * factor, this.z * factor);
        }

        Direction add(Direction other) {
            return new Direction(this.x + other.x, this.y + other.y, this.z + other.z);
        }

        double dot(Direction other) {
            return this.x * other.x + this.y * other.y + this.z * other.z;
        }
    }
}
