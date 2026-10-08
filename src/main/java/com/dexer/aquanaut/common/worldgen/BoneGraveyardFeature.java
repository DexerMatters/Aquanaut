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
 * The mud zone's landmark: the skeleton of some ancient giant, read as an imprint in the
 * sediment. Every bone is let INTO the floor surface and each rib stays joined to the spine, so
 * nothing rises into the water and no fossil block is ever left standing on its own.
 */
public final class BoneGraveyardFeature extends Feature<NoneFeatureConfiguration> {
    public BoneGraveyardFeature() {
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

        int vertebrae = 5 + random.nextInt(4);
        double theta = random.nextDouble() * Math.PI * 2.0D;
        double spineX = Math.cos(theta);
        double spineZ = Math.sin(theta);
        double sideX = -spineZ;
        double sideZ = spineX;

        boolean placed = false;
        for (int r = 0; r < vertebrae; r++) {
            int bx = floor.getX() + (int) Math.round(spineX * r);
            int bz = floor.getZ() + (int) Math.round(spineZ * r);
            placed |= layBone(level, floor, bx, bz);

            // A rib on each side, joined to the spine and lying flat on the floor.
            if (r % 2 == 1) {
                int ribLength = 2 + random.nextInt(2);
                for (int side = -1; side <= 1; side += 2) {
                    for (int out = 1; out <= ribLength; out++) {
                        int rx = bx + (int) Math.round(sideX * side * out);
                        int rz = bz + (int) Math.round(sideZ * side * out);
                        placed |= layBone(level, floor, rx, rz);
                    }
                }
            }
        }
        return placed;
    }

    /** Lets fossil bed into the sediment surface of one column, flush with the floor around it. */
    private static boolean layBone(WorldGenLevel level, BlockPos floor, int x, int z) {
        BlockPos column = MudZoneFloor.find(level, floor.offset(x - floor.getX(), 1, z - floor.getZ()));
        if (column == null || !isBedMaterial(level.getBlockState(column))) {
            return false;
        }
        level.setBlock(column, BlockRegistry.FOSSIL_BED.get().defaultBlockState(), 2);
        return true;
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
