package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.core.BiomeRegistry;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Rim colonies of calcite quills and occasional halite rosettes.
 */
public final class CalciteQuillFeature extends Feature<NoneFeatureConfiguration> {
    public CalciteQuillFeature() {
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
        int clusters = 3 + random.nextInt(4);
        for (int i = 0; i < clusters; i++) {
            int ox = random.nextInt(11) - 5;
            int oz = random.nextInt(11) - 5;
            BlockPos floor = findFloor(level, origin.offset(ox, 0, oz));
            BlockState below = level.getBlockState(floor);
            if (!below.isFaceSturdy(level, floor, Direction.UP)
                    && !below.is(BlockRegistry.HALITE_CRUST.get())
                    && !below.is(BlockRegistry.VARVE_SHALE.get())
                    && !below.is(BlockRegistry.HALITE_PIPE.get())) {
                continue;
            }
            BlockPos above = floor.above();
            if (!level.getFluidState(above).is(FluidTags.WATER)) {
                continue;
            }
            if (random.nextInt(10) < 3) {
                level.setBlock(above, BlockRegistry.HALITE_ROSETTE.get().defaultBlockState()
                        .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), 2);
            } else {
                level.setBlock(above, BlockRegistry.CALCITE_QUILL.get().defaultBlockState()
                        .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), 2);
            }
            placed = true;
        }
        return placed;
    }

    private static BlockPos findFloor(WorldGenLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(origin.getX(), origin.getY() + 10,
                origin.getZ());
        while (mutable.getY() > origin.getY() - 12 && level.getFluidState(mutable).is(FluidTags.WATER)) {
            mutable.move(Direction.DOWN);
        }
        return mutable.immutable();
    }
}
