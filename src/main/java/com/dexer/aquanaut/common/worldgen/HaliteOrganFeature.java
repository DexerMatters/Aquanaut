package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.core.BiomeRegistry;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Choirs of thin fluted halite organ pipes.
 */
public final class HaliteOrganFeature extends Feature<NoneFeatureConfiguration> {
    public HaliteOrganFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        if (!level.getBiome(origin).is(BiomeRegistry.BRINE_MIRROR_GORGE)) {
            return false;
        }

        boolean placed = false;
        int pipes = 3 + random.nextInt(7);
        for (int i = 0; i < pipes; i++) {
            int ox = random.nextInt(7) - 3;
            int oz = random.nextInt(7) - 3;
            BlockPos floor = findFloor(level, origin.offset(ox, 0, oz));
            placed |= placePipe(level, floor, random);
        }
        return placed;
    }

    private static boolean placePipe(WorldGenLevel level, BlockPos floor, RandomSource random) {
        BlockState below = level.getBlockState(floor);
        if (!below.isFaceSturdy(level, floor, Direction.UP)
                && !below.is(BlockRegistry.HALITE_CRUST.get())
                && !below.is(BlockRegistry.VARVE_SHALE.get())
                && !below.is(BlockRegistry.HALITE_PIPE.get())) {
            return false;
        }
        int height = 3 + random.nextInt(12);
        BlockPos.MutableBlockPos mutable = floor.mutable();
        BlockState pipe = BlockRegistry.HALITE_PIPE.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        for (int i = 1; i <= height; i++) {
            mutable.set(floor.getX(), floor.getY() + i, floor.getZ());
            if (!level.getFluidState(mutable).is(FluidTags.WATER)) {
                return i > 1;
            }
            level.setBlock(mutable, pipe, 2);
        }
        return true;
    }

    private static BlockPos findFloor(WorldGenLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(origin.getX(), origin.getY() + 16,
                origin.getZ());
        while (mutable.getY() > origin.getY() - 20 && level.getFluidState(mutable).is(FluidTags.WATER)) {
            mutable.move(Direction.DOWN);
        }
        return mutable.immutable();
    }
}
