package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.common.block.AshLayerBlock;
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
 * Ash drifts (尘): the fallout of the caldera settling in soft dunes over the floor,
 * burying the rock under fine volcanic dust and burying pockets of loose ash beneath.
 */
public final class AshDriftFeature extends Feature<NoneFeatureConfiguration> {
    public AshDriftFeature() {
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
        int radius = 5 + random.nextInt(4);
        boolean placed = false;
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
                double edge = 1.0D - Math.sqrt(dist2) / (radius + 1.0D);

                // Loose ash pockets where the drift presses into the old rock.
                if (random.nextDouble() < edge * 0.35D) {
                    level.setBlock(pos, BlockRegistry.VOLCANIC_ASH.get().defaultBlockState(), 2);
                    placed = true;
                }

                BlockPos dust = pos.above();
                if (!level.getFluidState(dust).is(FluidTags.WATER)) {
                    continue;
                }
                double roll = random.nextDouble();
                if (roll < edge * 0.85D) {
                    int layers = 1 + Math.min(3, (int) (edge * 4.0D * random.nextDouble() * 1.5D));
                    level.setBlock(dust, BlockRegistry.ASH_LAYER.get().defaultBlockState()
                            .setValue(AshLayerBlock.LAYERS, layers)
                            .setValue(BlockStateProperties.WATERLOGGED, true), 2);
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
