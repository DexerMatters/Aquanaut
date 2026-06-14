package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.core.BiomeRegistry;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;

public final class JellyJungleSurfaceRules {
    private JellyJungleSurfaceRules() {
    }

    public static SurfaceRules.RuleSource create() {
        return SurfaceRules.ifTrue(
                SurfaceRules.isBiome(BiomeRegistry.JELLY_JUNGLE),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,
                                SurfaceRules.state(Blocks.CLAY.defaultBlockState())),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR,
                                SurfaceRules.state(BlockRegistry.NUTRIENT_RICH_MUD.get().defaultBlockState())),
                        SurfaceRules.ifTrue(SurfaceRules.DEEP_UNDER_FLOOR,
                                SurfaceRules.state(BlockRegistry.SHALE.get().defaultBlockState()))
                ));
    }
}
