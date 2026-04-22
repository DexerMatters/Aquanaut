package com.dexer.aquanaut.common.ai;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class CollisionRecoveryMovement extends Movement {
    @Override
    public void apply(MovementContext context) {
        MovementState state = context.state();

        if (state.collisionTurnCooldown() > 0) {
            state.setCollisionTurnCooldown(state.collisionTurnCooldown() - 1);
        }
        if (state.steeringLockTicks() > 0) {
            state.setSteeringLockTicks(state.steeringLockTicks() - 1);
        }

        if (context.fish().horizontalCollision && state.collisionTurnCooldown() <= 0) {
            float reboundYaw = context.fish().getYRot() + 150.0F + (context.fish().getRandom().nextFloat() - 0.5F) * 70.0F;
            context.fish().setYRot(reboundYaw);
            context.fish().yBodyRot = reboundYaw;
            context.fish().yHeadRot = reboundYaw;
            state.setCruiseYawTarget(reboundYaw);

            Vec3 reboundDir = Vec3.directionFromRotation(context.fish().getXRot(), reboundYaw).normalize();
            context.fish().setDeltaMovement(context.fish().getDeltaMovement().scale(0.45D).add(reboundDir.scale(0.09D)));
            context.fish().hasImpulse = true;
            state.setCollisionTurnCooldown(context.fish().collisionTurnCooldownTicks());
            state.setSteeringLockTicks(Math.max(state.steeringLockTicks(), 3));
        }

        if (context.fish().verticalCollision && state.collisionTurnCooldown() <= 0) {
            float reboundPitch = Mth.clamp(-context.fish().getXRot() * 0.8F, -context.fish().maxTiltDegrees(), context.fish().maxTiltDegrees());
            if (!hasWaterAbove(context)) {
                reboundPitch = Mth.clamp(Math.abs(reboundPitch) + 12.0F, 8.0F, context.fish().maxTiltDegrees());
            } else if (!hasWaterBelow(context)) {
                reboundPitch = Mth.clamp(-Math.abs(reboundPitch) - 12.0F, -context.fish().maxTiltDegrees(), -8.0F);
            }
            context.fish().setXRot(reboundPitch);
            state.setCollisionTurnCooldown(context.fish().collisionTurnCooldownTicks());
            state.setSteeringLockTicks(Math.max(state.steeringLockTicks(), 2));
        }
    }
}
