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
 * Flat ink-black brine mirrors inset on salt terrace tops.
 */
public final class BrineMirrorFeature extends Feature<NoneFeatureConfiguration> {
    public BrineMirrorFeature() {
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

        BlockPos floor = findFloor(level, origin);
        if (!level.getBlockState(floor).is(BlockRegistry.HALITE_CRUST.get())
                && !level.getBlockState(floor).is(BlockRegistry.VARVE_SHALE.get())) {
            return false;
        }

        int width = 1 + random.nextInt(3);
        int depth = 1 + random.nextInt(2);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int dx = -width; dx <= width; dx++) {
            for (int dz = -depth; dz <= depth; dz++) {
                if (random.nextInt(5) == 0) {
                    continue;
                }
                mutable.set(floor.getX() + dx, floor.getY(), floor.getZ() + dz);
                BlockState at = level.getBlockState(mutable);
                if (!at.is(BlockRegistry.HALITE_CRUST.get()) && !at.is(BlockRegistry.VARVE_SHALE.get())) {
                    continue;
                }
                level.setBlock(mutable, BlockRegistry.BRINE_MIRROR.get().defaultBlockState(), 2);
                placed = true;
            }
        }
        return placed;
    }

    private static BlockPos findFloor(WorldGenLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(origin.getX(), origin.getY() + 12,
                origin.getZ());
        while (mutable.getY() > origin.getY() - 16 && level.getFluidState(mutable).is(FluidTags.WATER)) {
            mutable.move(Direction.DOWN);
        }
        return mutable.immutable();
    }
}
