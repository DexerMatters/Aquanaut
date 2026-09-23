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
 * Druse veins (晶皮脉): glittering halite druse seeping out of fractured rock, banded
 * with rose potash, its floor littered with mirror flakes and the odd gypsum rose.
 */
public final class DruseVeinFeature extends Feature<NoneFeatureConfiguration> {
    public DruseVeinFeature() {
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
        int radius = 3 + random.nextInt(4);
        boolean placed = false;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int dist2 = dx * dx + dz * dz;
                if (dist2 > radius * radius) {
                    continue;
                }
                BlockPos ground = supportTopAt(level, floor.offset(dx, 2, dz), 4);
                if (ground == null) {
                    continue;
                }
                double edge = 1.0D - dist2 / (double) (radius * radius + 1);

                if (random.nextDouble() < edge * 0.8D) {
                    level.setBlock(ground, BlockRegistry.HALITE_DRUSE.get().defaultBlockState(), 2);
                    placed = true;
                }
                // Potash banding under the druse skin.
                int depth = 1 + random.nextInt(2);
                for (int down = 1; down <= depth; down++) {
                    BlockPos vein = ground.below(down);
                    if (random.nextDouble() < edge * 0.5D) {
                        level.setBlock(vein, BlockRegistry.SYLVITE_CRUST.get().defaultBlockState(), 2);
                        placed = true;
                    }
                }

                BlockPos bloom = ground.above();
                // Flora and flakes root into open water only: covers keep their spot.
                if (!level.getFluidState(bloom).is(FluidTags.WATER)
                        || !level.getBlockState(bloom).getShape(level, bloom).isEmpty()) {
                    continue;
                }
                int roll = random.nextInt(10);
                if (roll < 3) {
                    level.setBlock(bloom, BlockRegistry.MIRROR_FLAKE.get().defaultBlockState()
                            .setValue(BlockStateProperties.WATERLOGGED, true), 2);
                    placed = true;
                } else if (roll < 4) {
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
            // Only full-height tops count as ground: thin covers would leave the veins
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
