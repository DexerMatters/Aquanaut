package com.dexer.aquanaut.common.ai;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class EscapeMovement extends Movement {
    private final PlayerAvoidanceLogic playerAvoidanceLogic = new PlayerAvoidanceLogic();

    @Override
    public void apply(MovementContext context) {
        PlayerAvoidanceLogic.AvoidanceResult avoidanceResult = this.playerAvoidanceLogic.compute(context.fish(), context.fish().playerDetectionRange());
        boolean escaping = avoidanceResult != null;
        context.state().setSprintingAway(escaping);

        if (!escaping) {
            return;
        }

        context.setMotion(context.fish().escapeAcceleration(), context.fish().escapeMaxSpeed());
        if (context.state().steeringLockTicks() > 0) {
            return;
        }

        Vec3 desiredDirection = avoidanceResult.escapeDirection();
        float targetYaw = this.yawFromDirection(desiredDirection);
        float targetPitch = Mth.clamp(this.pitchFromDirection(desiredDirection), -context.fish().maxTiltDegrees(), context.fish().maxTiltDegrees());
        context.fish().setYRot(this.rotateTowards(context.fish().getYRot(), targetYaw, context.fish().escapeTurnRateDegrees()));
        context.fish().setXRot(this.rotateTowards(context.fish().getXRot(), targetPitch, context.fish().escapeTurnRateDegrees() * 0.7F));

        context.state().setCruiseYawTarget(context.fish().getYRot());
    }

    public void reset() {
        this.playerAvoidanceLogic.reset();
    }
}
