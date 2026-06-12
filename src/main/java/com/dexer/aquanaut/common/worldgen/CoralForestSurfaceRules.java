package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.core.BiomeRegistry;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.world.level.levelgen.SurfaceRules;

public final class CoralForestSurfaceRules {
    private CoralForestSurfaceRules() {
    }

    public static SurfaceRules.RuleSource create() {
        return SurfaceRules.ifTrue(
                SurfaceRules.isBiome(BiomeRegistry.CORAL_FOREST),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,
                                SurfaceRules.state(BlockRegistry.CORAL_SAND.get().defaultBlockState())),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR,
                                SurfaceRules.state(BlockRegistry.LIMESTONE.get().defaultBlockState())),
                        SurfaceRules.ifTrue(SurfaceRules.DEEP_UNDER_FLOOR,
                                SurfaceRules.state(BlockRegistry.SHALE.get().defaultBlockState()))
                ));
    }
}
