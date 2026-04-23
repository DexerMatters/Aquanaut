package com.dexer.aquanaut.common.ai;

import com.dexer.aquanaut.common.entity.BaseFishEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class FishSchoolingAI {
    private static final double SEARCH_RADIUS = 8.0D;
    private static final double SEPARATION_RADIUS = 1.15D;
    private static final double FOLLOW_DISTANCE = 2.15D;
    private static final int LEADER_THRESHOLD = 4;

    public SchoolingDecision resolve(BaseFishEntity fish) {
        if (!fish.schoolingEnabled()) {
            return SchoolingDecision.inactive();
        }

        List<BaseFishEntity> schoolmates = this.findSchoolmates(fish);
        if (schoolmates.isEmpty()) {
            return SchoolingDecision.inactive();
        }

        Vec3 selfCenter = this.centerOf(fish);
        Vec3 schoolCenter = selfCenter;
        Vec3 headingSum = this.forwardVector(fish).scale(1.0D);
        Vec3 separationSum = new Vec3(0.0D, 0.0D, 0.0D);
        int memberCount = 1;

        for (BaseFishEntity schoolmate : schoolmates) {
            Vec3 mateCenter = this.centerOf(schoolmate);
            Vec3 offset = mateCenter.subtract(selfCenter);
            double distance = offset.length();

            if (distance > 1.0E-6D) {
                double proximityWeight = 1.0D / (1.0D + distance);
                headingSum = headingSum.add(this.forwardVector(schoolmate).scale(proximityWeight));
                schoolCenter = schoolCenter.add(mateCenter);
                memberCount++;

                if (distance < SEPARATION_RADIUS) {
                    double separationWeight = (SEPARATION_RADIUS - distance) / SEPARATION_RADIUS;
                    separationSum = separationSum
                            .add(selfCenter.subtract(mateCenter).normalize().scale(separationWeight));
                }
            }
        }

        if (memberCount <= 1) {
            return SchoolingDecision.inactive();
        }

        schoolCenter = schoolCenter.scale(1.0D / memberCount);
        Vec3 schoolHeading = this.normalizeOrFallback(headingSum, this.forwardVector(fish));
        BaseFishEntity leader = this.electLeader(fish, schoolmates, schoolCenter, schoolHeading);

        Vec3 desiredDirection;
        double speedMultiplier;
        if (leader == fish) {
            Vec3 cohesion = schoolCenter.subtract(selfCenter);
            desiredDirection = schoolHeading.scale(1.4D)
                    .add(cohesion.scale(0.55D))
                    .add(separationSum.scale(2.1D));
            speedMultiplier = memberCount >= LEADER_THRESHOLD ? 1.04D : 1.0D;
        } else {
            Vec3 leaderCenter = this.centerOf(leader);
            Vec3 leaderHeading = this.forwardVector(leader);
            Vec3 leaderRight = this.horizontalRightVector(leaderHeading);
            double followDistance = FOLLOW_DISTANCE + Math.min(0.75D, (memberCount - 1) * 0.12D);
            double sideBias = this.lateralSlotBias(fish, leader);
            Vec3 followPoint = leaderCenter.subtract(leaderHeading.scale(followDistance))
                    .add(leaderRight.scale(sideBias * 0.45D))
                    .add(0.0D, (schoolCenter.y - selfCenter.y) * 0.25D, 0.0D);

            Vec3 followVector = followPoint.subtract(selfCenter);
            desiredDirection = followVector.scale(1.55D)
                    .add(schoolHeading.scale(0.9D))
                    .add(separationSum.scale(2.35D));

            double distanceToLeader = leaderCenter.distanceTo(selfCenter);
            double distanceRatio = Mth.clamp(distanceToLeader / Math.max(1.0D, followDistance), 0.0D, 1.6D);
            speedMultiplier = Mth.clamp(0.94D + distanceRatio * 0.05D, 0.92D, 1.03D);
        }

        desiredDirection = new Vec3(desiredDirection.x, 0.0D, desiredDirection.z);
        desiredDirection = this.normalizeOrFallback(desiredDirection, new Vec3(schoolHeading.x, 0.0D, schoolHeading.z));
        return new SchoolingDecision(true, leader == fish, desiredDirection, speedMultiplier, schoolCenter.y,
                memberCount);
    }

    private List<BaseFishEntity> findSchoolmates(BaseFishEntity fish) {
        AABB searchBox = fish.getBoundingBox().inflate(SEARCH_RADIUS, SEARCH_RADIUS * 0.6D, SEARCH_RADIUS);
        return fish.level().getEntitiesOfClass(Entity.class, searchBox, entity -> entity instanceof BaseFishEntity other
                && other != fish && other.isAlive() && other.isInWater() && other.getClass() == fish.getClass())
                .stream()
                .map(entity -> (BaseFishEntity) entity)
                .toList();
    }

    private BaseFishEntity electLeader(BaseFishEntity fish, List<BaseFishEntity> schoolmates, Vec3 schoolCenter,
            Vec3 schoolHeading) {
        BaseFishEntity leader = fish;
        double bestScore = this.leaderScore(fish, schoolCenter, schoolHeading);

        for (BaseFishEntity schoolmate : schoolmates) {
            double score = this.leaderScore(schoolmate, schoolCenter, schoolHeading);
            if (score > bestScore + 1.0E-4D) {
                leader = schoolmate;
                bestScore = score;
            }
        }

        return leader;
    }

    private double leaderScore(BaseFishEntity fish, Vec3 schoolCenter, Vec3 schoolHeading) {
        Vec3 fishCenter = this.centerOf(fish);
        Vec3 offset = fishCenter.subtract(schoolCenter);
        double forwardness = offset.dot(schoolHeading);
        double speedBias = fish.getDeltaMovement().length() * 0.2D;
        double stableBias = (fish.getUUID().hashCode() & 255) / 255.0D * 0.18D;
        return forwardness * 1.8D + speedBias + stableBias;
    }

    private Vec3 centerOf(BaseFishEntity fish) {
        return fish.position().add(0.0D, fish.getBbHeight() * 0.45D, 0.0D);
    }

    private Vec3 forwardVector(BaseFishEntity fish) {
        Vec3 forward = Vec3.directionFromRotation(fish.getXRot(), fish.getYRot());
        return this.normalizeOrFallback(forward, new Vec3(0.0D, 0.0D, 1.0D));
    }

    private Vec3 horizontalRightVector(Vec3 forward) {
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        return this.normalizeOrFallback(right, new Vec3(1.0D, 0.0D, 0.0D));
    }

    private double lateralSlotBias(BaseFishEntity fish, BaseFishEntity leader) {
        int bits = fish.getUUID().hashCode() ^ leader.getUUID().hashCode();
        return (bits & 1) == 0 ? 1.0D : -1.0D;
    }

    private Vec3 normalizeOrFallback(Vec3 vector, Vec3 fallback) {
        if (vector.lengthSqr() < 1.0E-6D) {
            return fallback;
        }

        return vector.normalize();
    }

    public record SchoolingDecision(boolean active, boolean leader, Vec3 desiredDirection, double speedMultiplier,
            double schoolCenterY, int memberCount) {
        public static SchoolingDecision inactive() {
            return new SchoolingDecision(false, false, new Vec3(0.0D, 0.0D, 0.0D), 1.0D, 0.0D, 0);
        }
    }
}