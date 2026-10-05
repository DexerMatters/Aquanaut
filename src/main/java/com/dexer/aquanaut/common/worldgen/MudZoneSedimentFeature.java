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
 * Lays a thick, coherent mud bed over the mud zone floor. The bed is deep enough to read as mud
 * rather than a veneer, and the nutrient and parasitic marshes are keyed to a coarse cell so
 * they pool in broad drifts instead of speckling the floor block by block.
 */
public final class MudZoneSedimentFeature extends Feature<NoneFeatureConfiguration> {
    private static final long NUTRIENT_SEED = 0x4E75744CL;
    private static final long PARASITIC_SEED = 0x50617261L;

    public MudZoneSedimentFeature() {
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
        if (floor == null || !isSediment(level.getBlockState(floor))) {
            return false;
        }

        int radius = 4 + random.nextInt(4);
        int depth = 3 + random.nextInt(3);
        boolean placed = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius || random.nextFloat() < 0.10F) {
                    continue;
                }
                for (int dy = 0; dy < depth; dy++) {
                    cursor.set(floor.getX() + dx, floor.getY() - dy, floor.getZ() + dz);
                    if (!isSediment(level.getBlockState(cursor))) {
                        break;
                    }
                    level.setBlock(cursor, mudState(cursor.getX(), cursor.getZ()), 2);
                    placed = true;
                }
            }
        }
        return placed;
    }

    /**
     * The sediment the bed may replace: the shelf skin, its limestone body, and the mud left by
     * an earlier pass over the same ground.
     */
    private static boolean isSediment(BlockState state) {
        return state.is(Blocks.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY)
                || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(BlockRegistry.CORAL_SAND.get())
                || state.is(BlockRegistry.LIMESTONE.get())
                || state.is(BlockRegistry.MUD.get())
                || state.is(BlockRegistry.NUTRIENT_RICH_MUD.get())
                || state.is(BlockRegistry.PARASITIC_MUD.get());
    }

    private static BlockState mudState(int x, int z) {
        if (patch01(x, z, 7, NUTRIENT_SEED) > 0.80D) {
            return BlockRegistry.NUTRIENT_RICH_MUD.get().defaultBlockState();
        }
        if (patch01(x, z, 5, PARASITIC_SEED) > 0.88D) {
            return BlockRegistry.PARASITIC_MUD.get().defaultBlockState();
        }
        return BlockRegistry.MUD.get().defaultBlockState();
    }

    /** Stable pseudo-random value in [0,1) for a coarse cell, so patches form blobs. */
    private static double patch01(int x, int z, int cell, long seed) {
        long cx = Math.floorDiv(x, cell);
        long cz = Math.floorDiv(z, cell);
        long h = cx * 374761393L + cz * 668265263L + seed;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h ^= h >>> 16;
        return (h >>> 11) / (double) (1L << 53);
    }
}
