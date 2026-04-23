package com.dexer.aquanaut.common;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public final class PressureHelper {
    private PressureHelper() {
    }

    public static float getPressure(Entity entity) {
        if (!(entity instanceof LivingEntity living) || !living.isInWater()) {
            return 0.0F;
        }

        return getPressure(entity.level(), entity.getY());
    }

    public static float getPressure(Level level, BlockPos pos) {
        return getPressure(level, pos.getY());
    }

    public static float getPressure(Level level, double y) {
        float seaLevel = level.getSeaLevel();
        float maxDepth = level.getMinBuildHeight();
        float depthRange = seaLevel - maxDepth;
        if (depthRange <= 0.0F) {
            return 0.0F;
        }

        return Mth.clamp((seaLevel - (float) y) / depthRange, 0.0F, 1.0F);
    }
}