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
 * Native sulfur (硫磺): vein fills bleeding out of fractured volcanic rock, capped by
 * crusty sulfur frosting and shot through with small crystal blades.
 */
public final class SulfurVeinFeature extends Feature<NoneFeatureConfiguration> {
    public SulfurVeinFeature() {
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
        boolean placed = false;
        int radius = 3 + random.nextInt(4);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int dist2 = dx * dx + dz * dz;
                if (dist2 > radius * radius) {
                    continue;
                }
                BlockPos pos = floor.offset(dx, 0, dz);
                BlockState ground = level.getBlockState(pos);
                if (!ground.isFaceSturdy(level, pos, Direction.UP)) {
                    continue;
                }
                double edge = 1.0D - dist2 / (double) (radius * radius + 1);

                // Crust frosting near the outcrop center, veins under the skin.
                if (random.nextDouble() < edge * 0.75D) {
                    level.setBlock(pos, BlockRegistry.SULFUR_CRUST.get().defaultBlockState(), 2);
                    placed = true;
                    BlockPos bloom = pos.above();
                    if (random.nextInt(5) == 0 && level.getFluidState(bloom).is(FluidTags.WATER)
                            && level.getBlockState(bloom).getShape(level, bloom).isEmpty()) {
                        level.setBlock(bloom, BlockRegistry.SULFUR_CRYSTAL.get().defaultBlockState()
                                .setValue(BlockStateProperties.WATERLOGGED, true), 2);
                    }
                }
                int depth = 1 + random.nextInt(3);
                for (int down = 1; down <= depth; down++) {
                    BlockPos vein = pos.below(down);
                    if (random.nextDouble() < edge * 0.55D) {
                        level.setBlock(vein, BlockRegistry.SULFUR_CRUST.get().defaultBlockState(), 2);
                        placed = true;
                    }
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
