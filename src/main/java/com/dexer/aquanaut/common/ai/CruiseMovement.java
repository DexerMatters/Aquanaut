package com.dexer.aquanaut.common.ai;

import net.minecraft.util.Mth;

public class CruiseMovement extends Movement {
    @Override
    public void apply(MovementContext context) {
        if (context.state().steeringLockTicks() > 0) {
            context.setMotion(context.fish().cruiseAcceleration(), context.fish().cruiseMaxSpeed());
            return;
        }

        this.updateCruiseYawTarget(context);
        this.applyCruiseYawTarget(context);
        this.updateCruiseDepthTarget(context);
        this.updateCruisePitchTarget(context);
        this.applyCruisePitchTarget(context);
        context.setMotion(context.fish().cruiseAcceleration(), context.fish().cruiseMaxSpeed());
    }

    private void updateCruiseYawTarget(MovementContext context) {
        MovementState state = context.state();

        if (context.fish().horizontalCollision) {
            state.setCruiseYawTarget(context.fish().getYRot() + 180.0F + (context.fish().getRandom().nextFloat() - 0.5F) * 40.0F);
            state.setCruiseYawDecisionCooldown(context.fish().cruiseYawDecisionMinTicks());
            return;
        }

        state.setCruiseYawDecisionCooldown(state.cruiseYawDecisionCooldown() - 1);
        if (state.cruiseYawDecisionCooldown() <= 0) {
            state.setCruiseYawDecisionCooldown(context.fish().cruiseYawDecisionMinTicks() + context.fish().getRandom().nextInt(context.fish().cruiseYawDecisionRandomTicks()));
            if (context.fish().getRandom().nextFloat() < context.fish().cruiseTurnChance()) {
                float yawOffset = (context.fish().getRandom().nextFloat() - 0.5F) * context.fish().cruiseTurnRangeDegrees();
                state.setCruiseYawTarget(context.fish().getYRot() + yawOffset);
            } else {
                state.setCruiseYawTarget(context.fish().getYRot());
            }
        }
    }

    private void applyCruiseYawTarget(MovementContext context) {
        context.fish().setYRot(this.rotateTowards(context.fish().getYRot(), context.state().cruiseYawTarget(), context.fish().cruiseYawTurnRateDegrees()));
    }

    private void updateCruiseDepthTarget(MovementContext context) {
        MovementState state = context.state();
        state.setCruiseDepthDecisionCooldown(state.cruiseDepthDecisionCooldown() - 1);

        if (state.cruiseDepthDecisionCooldown() <= 0) {
            state.setCruiseDepthDecisionCooldown(context.fish().cruisePitchDecisionMinTicks() + context.fish().getRandom().nextInt(context.fish().cruisePitchDecisionRandomTicks()));
            double offsetMagnitude = 1.5D + context.fish().getRandom().nextDouble() * Math.max(0.1D, context.fish().cruiseDepthRange() - 1.5D);
            double offset = context.fish().getRandom().nextBoolean() ? offsetMagnitude : -offsetMagnitude;
            state.setCruiseTargetY(context.fish().getY() + offset);
        }

        if (!this.hasWaterAbove(context)) {
            state.setCruiseTargetY(context.fish().getY() - context.fish().cruiseDepthEmergencyOffset());
        } else if (!this.hasWaterBelow(context)) {
            state.setCruiseTargetY(context.fish().getY() + context.fish().cruiseDepthEmergencyOffset());
        }
    }

    private void updateCruisePitchTarget(MovementContext context) {
        MovementState state = context.state();
        state.setCruisePitchDecisionCooldown(state.cruisePitchDecisionCooldown() - 1);

        if (state.cruisePitchDecisionCooldown() <= 0) {
            state.setCruisePitchDecisionCooldown(context.fish().cruisePitchDecisionMinTicks() + context.fish().getRandom().nextInt(context.fish().cruisePitchDecisionRandomTicks()));
            state.setCruiseTargetY(state.cruiseTargetY() + (context.fish().getRandom().nextFloat() - 0.5F) * 1.5F);
        }

        double depthDelta = state.cruiseTargetY() - context.fish().getY();
        float depthPitchTarget = (float) (-(Mth.atan2(depthDelta, context.fish().cruiseDepthPitchDistance()) * Mth.RAD_TO_DEG));
        state.setCruisePitchTarget(Mth.clamp(depthPitchTarget, -context.fish().maxTiltDegrees(), context.fish().maxTiltDegrees()));
    }

    private void applyCruisePitchTarget(MovementContext context) {
        float clampedTarget = Mth.clamp(context.state().cruisePitchTarget(), -context.fish().maxTiltDegrees(), context.fish().maxTiltDegrees());
        context.fish().setXRot(this.rotateTowards(context.fish().getXRot(), clampedTarget, context.fish().cruisePitchTurnRateDegrees()));
    }
}
