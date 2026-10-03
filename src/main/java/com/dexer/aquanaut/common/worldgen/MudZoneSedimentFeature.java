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

/** Places shallow organic sediment patches on the middle-level ocean floor. */
public final class MudZoneSedimentFeature extends Feature<NoneFeatureConfiguration> {
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

        int radius = 2 + random.nextInt(3);
        boolean placed = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius || random.nextFloat() < 0.18F) {
                    continue;
                }
                int depth = 1 + random.nextInt(2);
                for (int dy = 0; dy < depth; dy++) {
                    cursor.set(origin.getX() + dx, origin.getY() - dy, origin.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (!isSediment(state)) {
                        break;
                    }
                    level.setBlock(cursor, mudState(level, cursor, random), 2);
                    placed = true;
                }
            }
        }
        return placed;
    }

    private static boolean isSediment(BlockState state) {
        return state.is(Blocks.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY)
                || state.is(BlockRegistry.CORAL_SAND.get());
    }

    private static BlockState mudState(WorldGenLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < 0.035F && level.getBlockState(pos.above()).is(Blocks.WATER)) {
            return BlockRegistry.FOSSIL_BED.get().defaultBlockState();
        }
        if (random.nextFloat() < 0.08F) {
            return BlockRegistry.PARASITIC_MUD.get().defaultBlockState();
        }
        if (random.nextFloat() < 0.18F) {
            return BlockRegistry.NUTRIENT_RICH_MUD.get().defaultBlockState();
        }
        return BlockRegistry.MUD.get().defaultBlockState();
    }
}
