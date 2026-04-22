package com.dexer.aquanaut.common.ai;

import com.dexer.aquanaut.common.entity.BaseFishEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class ThrowAwayAvoidanceDirectionStrategy extends AvoidanceDirectionStrategy {
    @Override
    public Vec3 computeEscapeDirection(BaseFishEntity fish, Player threat, PlayerAvoidanceLogic.EvasionState state) {
        this.updateEvasionBias(fish, state);

        Vec3 awayFromThreat = fish.position().subtract(threat.position());
        Vec3 baseAwayDirection = awayFromThreat.lengthSqr() < 1.0E-6D
                ? Vec3.directionFromRotation(fish.getXRot(), fish.getYRot())
                : awayFromThreat.normalize();

        Vec3 lateralDirection = baseAwayDirection.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (lateralDirection.lengthSqr() < 1.0E-6D) {
            lateralDirection = Vec3.directionFromRotation(0.0F, fish.getYRot() + 90.0F);
        }
        lateralDirection = lateralDirection.normalize().scale(state.evasiveSideSign());

        double verticalThrow = Mth.clamp((fish.getY() - threat.getY()) * 0.2D + state.evasiveVerticalBias(), -0.35D, 0.35D);
        return baseAwayDirection.scale(0.8D)
                .add(lateralDirection.scale(0.45D))
                .add(0.0D, verticalThrow, 0.0D);
    }

    private void updateEvasionBias(BaseFishEntity fish, PlayerAvoidanceLogic.EvasionState state) {
        state.decrementDirectionCooldown();
        if (state.directionCooldown() > 0) {
            return;
        }

        state.setDirectionCooldown(20 + fish.getRandom().nextInt(26));
        state.setEvasiveSideSign(fish.getRandom().nextBoolean() ? 1 : -1);
        state.setEvasiveVerticalBias((fish.getRandom().nextFloat() - 0.5F) * 0.3F);
    }
}
