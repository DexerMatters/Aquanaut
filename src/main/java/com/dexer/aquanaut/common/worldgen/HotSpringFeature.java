package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.core.BiomeRegistry;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 热泉 — a hot spring: stepped siliceous sinter terraces wrapped around a hissing vent
 * throat, aproned with warm thermophilic mats. The quiet heart of the caldera.
 */
public final class HotSpringFeature extends Feature<NoneFeatureConfiguration> {
    public HotSpringFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        if (!level.getBiome(origin).is(BiomeRegistry.BRIMSTONE_CALDERA)) {
            return false;
        }

        BlockPos floor = findFloor(level, origin);
        int steps = 3 + random.nextInt(3);
        int radius = 4 + random.nextInt(4);
        boolean placed = false;

        // Concentric sinter terraces, each raised lip catching the mineral rain.
        for (int step = 0; step < steps; step++) {
            int stepRadius = Math.max(1, radius - step);
            int y = floor.getY() + step;
            for (int dx = -stepRadius; dx <= stepRadius; dx++) {
                for (int dz = -stepRadius; dz <= stepRadius; dz++) {
                    int dist2 = dx * dx + dz * dz;
                    if (dist2 > stepRadius * stepRadius + 1) {
                        continue;
                    }
                    BlockPos pos = new BlockPos(floor.getX() + dx, y, floor.getZ() + dz);
                    BlockState below = level.getBlockState(pos.below());
                    if (!below.isFaceSturdy(level, pos.below(), Direction.UP)
                            && !below.is(BlockRegistry.SINTER.get())) {
                        continue;
                    }
                    if (!level.getFluidState(pos).is(FluidTags.WATER)
                            && !level.getBlockState(pos).canBeReplaced()) {
                        continue;
                    }
                    level.setBlock(pos, BlockRegistry.SINTER.get().defaultBlockState(), 2);
                    placed = true;

                    // Warm mats settle on the outer terraces where the water cools.
                    if (step >= steps - 2 && dist2 > (stepRadius - 1) * (stepRadius - 1)
                            && level.getBlockState(pos.above()).getShape(level, pos.above()).isEmpty()
                            && random.nextInt(4) == 0) {
                        level.setBlock(pos.above(), randomMat(random), 2);
                    }
                }
            }
        }

        // The vent throat: fuming mouths ringed by sulfur frost.
        int vents = 1 + random.nextInt(2);
        for (int i = 0; i < vents; i++) {
            BlockPos throat = floor.offset(random.nextInt(3) - 1, steps - 1, random.nextInt(3) - 1);
            BlockState throatBelow = level.getBlockState(throat.below());
            boolean supported = throatBelow.is(BlockRegistry.SINTER.get())
                    || (throatBelow.isFaceSturdy(level, throat.below(), Direction.UP)
                            && throatBelow.getShape(level, throat.below()).max(Direction.Axis.Y) >= 1.0D);
            if (!level.getFluidState(throat).is(FluidTags.WATER)
                    || !level.getBlockState(throat).getShape(level, throat).isEmpty()
                    || !supported) {
                continue;
            }
            level.setBlock(throat, BlockRegistry.FUMAROLE.get().defaultBlockState()
                    .setValue(BlockStateProperties.WATERLOGGED, true), 2);
            placed = true;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos rim = throat.relative(direction);
                if (level.getBlockState(rim).is(BlockRegistry.SINTER.get()) && random.nextBoolean()) {
                    level.setBlock(rim, BlockRegistry.SULFUR_CRUST.get().defaultBlockState(), 2);
                }
            }
        }
        return placed;
    }

    private static BlockState randomMat(RandomSource random) {
        BlockState mat = switch (random.nextInt(3)) {
            case 0 -> BlockRegistry.THERMOPHILIC_MAT_GOLD.get().defaultBlockState();
            case 1 -> BlockRegistry.THERMOPHILIC_MAT_RUST.get().defaultBlockState();
            default -> BlockRegistry.THERMOPHILIC_MAT_OLIVE.get().defaultBlockState();
        };
        return mat.setValue(BlockStateProperties.WATERLOGGED, true);
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
