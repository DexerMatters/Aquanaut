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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Gypsum gardens (石膏晶园): bundles of bladed selenite standing among sandy gypsum
 * roses, rooted in rose-pink potash crust — the potash beds of the drying basin.
 */
public final class GypsumGardenFeature extends Feature<NoneFeatureConfiguration> {
    public GypsumGardenFeature() {
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
        int beds = 2 + random.nextInt(3);
        for (int i = 0; i < beds; i++) {
            int ox = random.nextInt(11) - 5;
            int oz = random.nextInt(11) - 5;
            BlockPos floor = findFloor(level, origin.offset(ox, 0, oz));
            placed |= plantBed(level, floor, random);
        }
        return placed;
    }

    private static boolean plantBed(WorldGenLevel level, BlockPos floor, RandomSource random) {
        BlockState below = level.getBlockState(floor);
        if (!below.isFaceSturdy(level, floor, Direction.UP)) {
            return false;
        }
        boolean placed = false;
        int radius = 2 + random.nextInt(3);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                BlockPos ground = supportTopAt(level, floor.offset(dx, 2, dz), 4);
                if (ground == null) {
                    continue;
                }
                // Potash crust underfoot wherever the potash brine concentrated.
                if (random.nextInt(3) == 0) {
                    level.setBlock(ground, BlockRegistry.SYLVITE_CRUST.get().defaultBlockState(), 2);
                    placed = true;
                }
                BlockPos bloom = ground.above();
                // Flora roots into open water only: a flake or crust cover keeps its spot.
                if (!level.getFluidState(bloom).is(FluidTags.WATER)
                        || !level.getBlockState(bloom).getShape(level, bloom).isEmpty()) {
                    continue;
                }
                int roll = random.nextInt(10);
                if (roll < 4) {
                    // Selenite blade bundles, one to four blades high.
                    int height = 1 + random.nextInt(4);
                    for (int up = 0; up < height; up++) {
                        BlockPos blade = bloom.above(up);
                        if (!level.getFluidState(blade).is(FluidTags.WATER)) {
                            break;
                        }
                        level.setBlock(blade, BlockRegistry.GYPSUM_BLADE.get().defaultBlockState()
                                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y), 2);
                        placed = true;
                    }
                } else if (roll < 7) {
                    level.setBlock(bloom, BlockRegistry.GYPSUM_ROSE.get().defaultBlockState()
                            .setValue(BlockStateProperties.WATERLOGGED, true), 2);
                    placed = true;
                }
            }
        }
        return placed;
    }

    private static BlockPos supportTopAt(WorldGenLevel level, BlockPos pos, int range) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        for (int i = 0; i < range; i++) {
            BlockState state = level.getBlockState(mutable);
            // Only full-height tops count as ground: thin covers would leave the blades
            // hovering above their surface.
            if (state.isFaceSturdy(level, mutable, Direction.UP)
                    && state.getShape(level, mutable).max(Direction.Axis.Y) >= 1.0D) {
                return mutable.immutable();
            }
            mutable.move(Direction.DOWN);
        }
        return null;
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
