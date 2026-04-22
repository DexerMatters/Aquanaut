package com.dexer.aquanaut.common.ai;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class PropulsionMovement extends Movement {
    @Override
    public void apply(MovementContext context) {
        double acceleration = context.acceleration();
        double maxSpeed = context.maxSpeed();
        if (acceleration <= 0.0D || maxSpeed <= 0.0D) {
            acceleration = context.fish().cruiseAcceleration();
            maxSpeed = context.fish().cruiseMaxSpeed();
        }

        double propulsionMultiplier = this.computePropulsionMultiplier(context);
        Vec3 movementDirection = Vec3.directionFromRotation(context.fish().getXRot(), context.fish().getYRot()).normalize();
        Vec3 currentVelocity = context.fish().getDeltaMovement();
        Vec3 nextVelocity = currentVelocity.scale(context.fish().waterDrag()).add(movementDirection.scale(acceleration * propulsionMultiplier));

        if (!context.state().isSprintingAway()) {
            double verticalAssist = net.minecraft.util.Mth.clamp(context.state().cruiseTargetY() - context.fish().getY(), -1.0D, 1.0D) * context.fish().cruiseVerticalAssist();
            nextVelocity = nextVelocity.add(0.0D, verticalAssist, 0.0D);
        }

        double maxSpeedSqr = maxSpeed * maxSpeed;
        if (nextVelocity.lengthSqr() > maxSpeedSqr) {
            nextVelocity = nextVelocity.normalize().scale(maxSpeed);
        }

        context.fish().setDeltaMovement(nextVelocity);
        context.fish().hasImpulse = true;

        float bodyTurnRate = context.fish().bodyTurnRateDegrees();
        context.fish().yBodyRot = this.rotateTowards(context.fish().yBodyRot, context.fish().getYRot(), bodyTurnRate);
        context.fish().yHeadRot = this.rotateTowards(context.fish().yHeadRot, context.fish().getYRot(), bodyTurnRate * 1.25F);
    }

    private double computePropulsionMultiplier(MovementContext context) {
        MovementState state = context.state();
        boolean sprinting = state.isSprintingAway();

        int intervalTicks = Math.max(1, sprinting ? context.fish().sprintPropulsionIntervalTicks() : context.fish().cruisePropulsionIntervalTicks());
        int burstTicks = Mth.clamp(sprinting ? context.fish().sprintPropulsionBurstTicks() : context.fish().cruisePropulsionBurstTicks(), 1, intervalTicks);

        if (state.propulsionSprintingMode() != sprinting || state.propulsionBurstDurationTicks() != burstTicks) {
            state.setPropulsionSprintingMode(sprinting);
            state.setPropulsionBurstDurationTicks(burstTicks);
            state.setPropulsionBurstTicksRemaining(0);
            state.setPropulsionTicksUntilBurst(0);
        }

        if (state.propulsionBurstTicksRemaining() <= 0) {
            if (state.propulsionTicksUntilBurst() > 0) {
                state.setPropulsionTicksUntilBurst(state.propulsionTicksUntilBurst() - 1);
                return sprinting ? context.fish().sprintPropulsionGlideAccelerationFactor() : context.fish().cruisePropulsionGlideAccelerationFactor();
            }

            state.setPropulsionBurstTicksRemaining(burstTicks);
            state.setPropulsionTicksUntilBurst(Math.max(0, intervalTicks - burstTicks));
        }

        int ticksRemaining = state.propulsionBurstTicksRemaining();
        int durationTicks = Math.max(1, state.propulsionBurstDurationTicks());
        float phase = 1.0F - (((float) ticksRemaining - 0.5F) / (float) durationTicks);
        phase = Mth.clamp(phase, 0.0F, 1.0F);
        double envelope = Mth.sin(phase * Mth.PI);

        state.setPropulsionBurstTicksRemaining(ticksRemaining - 1);

        double pulseFactor = sprinting ? context.fish().sprintPropulsionBurstAccelerationFactor() : context.fish().cruisePropulsionBurstAccelerationFactor();
        return pulseFactor * (0.72D + envelope * 0.28D);
    }
}
