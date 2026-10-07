package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.core.BiomeRegistry;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * An exposed fossil bed: a flat patch of fossil bed let into the sediment surface, flush with the
 * floor around it. Nothing is stacked on top, so the fossil hugs the ground instead of riding a
 * mound the way the old domed outcrop did.
 */
public final class FossilOutcropFeature extends Feature<NoneFeatureConfiguration> {
    public FossilOutcropFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        if (!level.getBiome(origin).is(BiomeRegistry.MUD_ZONE)) {
            return false;
        }
        BlockPos floor = MudZoneFloor.find(level, origin);
        if (floor == null) {
            return false;
        }

        int radius = 2 + random.nextInt(3);
        boolean placed = false;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius || random.nextFloat() < 0.15F) {
                    continue;
                }
                BlockPos column = MudZoneFloor.find(level, floor.offset(dx, 1, dz));
                if (column == null || !isBedMaterial(level.getBlockState(column))) {
                    continue;
                }
                // Replace the surface block: the bed is let into the floor, not laid on top.
                level.setBlock(column, BlockRegistry.FOSSIL_BED.get().defaultBlockState(), 2);
                if (random.nextFloat() < 0.35F) {
                    level.setBlock(column.below(), BlockRegistry.FOSSIL_BED.get().defaultBlockState(), 2);
                }
                placed = true;
            }
        }
        return placed;
    }

    /** The sediment a fossil bed can show through: the flat's mud and its sandy shelf skin. */
    private static boolean isBedMaterial(BlockState state) {
        return state.is(BlockRegistry.MUD.get())
                || state.is(BlockRegistry.NUTRIENT_RICH_MUD.get())
                || state.is(BlockRegistry.PARASITIC_MUD.get())
                || state.is(BlockRegistry.PACKED_MUD.get())
                || state.is(BlockRegistry.CORAL_SAND.get())
                || state.is(Blocks.SAND)
                || state.is(Blocks.GRAVEL);
    }
}
