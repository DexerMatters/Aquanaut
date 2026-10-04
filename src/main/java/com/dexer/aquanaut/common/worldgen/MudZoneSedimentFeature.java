package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.core.BiomeRegistry;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Converts the mud-zone floor's sediment into a thick, coherent mud bed. */
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

        int radius = 3 + random.nextInt(4);
        BlockPos floor = findFloor(level, origin);
        boolean placed = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius || random.nextFloat() < 0.08F) {
                    continue;
                }
                int depth = 2 + random.nextInt(2);
                for (int dy = 0; dy < depth; dy++) {
                    cursor.set(floor.getX() + dx, floor.getY() - dy, floor.getZ() + dz);
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
                || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(BlockRegistry.CORAL_SAND.get());
    }

    /** Drops from above the column to the first solid sediment block so the bed is not offset. */
    private static BlockPos findFloor(WorldGenLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(origin.getX(), origin.getY() + 16,
                origin.getZ());
        while (mutable.getY() > origin.getY() - 16 && level.getFluidState(mutable).is(FluidTags.WATER)) {
            mutable.move(Direction.DOWN);
        }
        return mutable.immutable();
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
