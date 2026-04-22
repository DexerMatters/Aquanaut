package com.dexer.aquanaut.common.ai;

import com.dexer.aquanaut.common.entity.BaseFishEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public abstract class AvoidanceDirectionStrategy {
    public abstract Vec3 computeEscapeDirection(BaseFishEntity fish, Player threat, PlayerAvoidanceLogic.EvasionState state);
}
