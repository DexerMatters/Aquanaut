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
 * Stepped salt terraces over banded varve - the signature architecture of Brine Mirror Gorge.
 */
public final class BrineTerraceFeature extends Feature<NoneFeatureConfiguration> {
    public BrineTerraceFeature() {
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
        int steps = 2 + random.nextInt(4);
        int radius = 3 + random.nextInt(5);
        boolean placed = false;
        BlockPos.MutableBlockPos cursor = floor.mutable();

        for (int step = 0; step < steps; step++) {
            int stepRadius = Math.max(1, radius - step);
            int y = floor.getY() + step;
            for (int dx = -stepRadius; dx <= stepRadius; dx++) {
                for (int dz = -stepRadius; dz <= stepRadius; dz++) {
                    if (dx * dx + dz * dz > stepRadius * stepRadius + 1) {
                        continue;
                    }
                    cursor.set(floor.getX() + dx, y, floor.getZ() + dz);
                    BlockState below = level.getBlockState(cursor.below());
                    if (!below.isFaceSturdy(level, cursor.below(), Direction.UP)
                            && !below.is(BlockRegistry.HALITE_CRUST.get())
                            && !below.is(BlockRegistry.VARVE_SHALE.get())) {
                        continue;
                    }
                    if (!level.getFluidState(cursor).is(FluidTags.WATER)
                            && !level.getBlockState(cursor).canBeReplaced()) {
                        continue;
                    }
                    BlockState top = step == steps - 1 || random.nextInt(3) == 0
                            ? BlockRegistry.HALITE_CRUST.get().defaultBlockState()
                            : BlockRegistry.VARVE_SHALE.get().defaultBlockState();
                    level.setBlock(cursor, top, 2);
                    cursor.move(Direction.DOWN);
                    for (int depth = 1; depth <= 2 + step; depth++) {
                        cursor.setY(y - depth);
                        if (cursor.getY() < floor.getY() - 8) {
                            break;
                        }
                        level.setBlock(cursor, BlockRegistry.VARVE_SHALE.get().defaultBlockState(), 2);
                    }
                    placed = true;
                }
            }
        }
        return placed;
    }

    private static BlockPos findFloor(WorldGenLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(origin.getX(), origin.getY() + 16,
                origin.getZ());
        while (mutable.getY() > origin.getY() - 24 && level.getFluidState(mutable).is(FluidTags.WATER)) {
            mutable.move(Direction.DOWN);
        }
        return mutable.immutable();
    }
}
