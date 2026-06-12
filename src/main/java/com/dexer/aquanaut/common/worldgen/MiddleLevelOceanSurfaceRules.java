package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.core.BiomeRegistry;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;

public final class MiddleLevelOceanSurfaceRules {
    private MiddleLevelOceanSurfaceRules() {
    }

    public static SurfaceRules.RuleSource create() {
        return SurfaceRules.sequence(
                CoralForestSurfaceRules.create(),
                SurfaceRules.ifTrue(
                        SurfaceRules.isBiome(BiomeRegistry.MIDDLE_LEVEL_OCEAN),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,
                                        SurfaceRules.state(Blocks.CLAY.defaultBlockState())),
                                SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR,
                                        SurfaceRules.state(Blocks.GRAVEL.defaultBlockState())),
                                SurfaceRules.ifTrue(SurfaceRules.DEEP_UNDER_FLOOR,
                                        SurfaceRules.state(Blocks.TUFF.defaultBlockState()))
                        )));
    }
}
