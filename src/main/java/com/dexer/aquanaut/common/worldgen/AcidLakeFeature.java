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
 * 硫酸湖 — a sulfuric acid lake: a shallow bowl eaten into the volcanic floor, its walls
 * bleached and etched by acid, filled with heavy olive-green leachate that exhales mist.
 */
public final class AcidLakeFeature extends Feature<NoneFeatureConfiguration> {
    public AcidLakeFeature() {
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
        int radius = 4 + random.nextInt(5);
        int depth = 2 + random.nextInt(3);
        BlockState acid = BlockRegistry.SULFURIC_ACID.get().defaultBlockState();
        boolean placed = false;

        int liquidTopY = floor.getY() - 1;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > radius) {
                    continue;
                }
                double bowl = 1.0D - distance / (radius + 0.5D);
                int columnDepth = 1 + (int) Math.round(bowl * depth);
                // Each column finds its own ground so the etched bowl hugs slopes
                // instead of hanging walls over open water.
                BlockPos ground = supportTopAt(level, floor.offset(dx, 2, dz), radius + 4);
                if (ground == null) {
                    continue;
                }
                int groundY = ground.getY();
                int bowlFloorY = groundY - columnDepth;

                // Etched bowl: the acid has eaten the rock pale wherever it touches.
                for (int y = groundY; y > bowlFloorY; y--) {
                    level.setBlock(new BlockPos(ground.getX(), y, ground.getZ()),
                            BlockRegistry.ACID_ETCHED_BASALT.get().defaultBlockState(), 2);
                    placed = true;
                }

                // Heavy liquid pools flat up to just under the rim.
                for (int y = bowlFloorY + 2; y <= Math.min(liquidTopY, groundY); y++) {
                    BlockPos fill = new BlockPos(ground.getX(), y, ground.getZ());
                    if (level.getFluidState(fill).is(FluidTags.WATER)) {
                        level.setBlock(fill, acid, 2);
                        placed = true;
                    }
                }

                // Rim fringe: pumice rafts and sulfur frosting around the shore.
                if (distance > radius - 1.2D && random.nextInt(3) == 0) {
                    BlockPos shore = ground.above();
                    if (level.getFluidState(shore).is(FluidTags.WATER)) {
                        level.setBlock(shore, random.nextBoolean()
                                ? BlockRegistry.PUMICE.get().defaultBlockState()
                                : BlockRegistry.SULFUR_CRUST.get().defaultBlockState(), 2);
                    }
                }
            }
        }
        return placed;
    }

    /**
     * The topmost ground block within {@code range} below {@code pos}, or {@code null}
     * over open water.
     */
    private static BlockPos supportTopAt(WorldGenLevel level, BlockPos pos, int range) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        for (int i = 0; i < range; i++) {
            BlockState state = level.getBlockState(mutable);
            // Only full-height tops count as ground so the bowl never floats over
            // thin covers like mats or ash drifts.
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
