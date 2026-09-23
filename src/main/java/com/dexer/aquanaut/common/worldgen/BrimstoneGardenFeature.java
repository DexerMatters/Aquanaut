package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.common.block.DroopingSeaweedBlock;
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
 * Brimstone gardens: crystal-grown flowerbeds of native sulfur blades and glowing
 * fireblooms, hung about with dripping sulfur stalactites.
 */
public final class BrimstoneGardenFeature extends Feature<NoneFeatureConfiguration> {
    public BrimstoneGardenFeature() {
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

        boolean placed = false;
        int beds = 2 + random.nextInt(3);
        for (int bed = 0; bed < beds; bed++) {
            int ox = random.nextInt(11) - 5;
            int oz = random.nextInt(11) - 5;
            BlockPos floor = findFloor(level, origin.offset(ox, 0, oz));
            placed |= plantBed(level, floor, random);
        }

        int curtains = 1 + random.nextInt(2);
        for (int i = 0; i < curtains; i++) {
            placed |= hangStalactite(level,
                    origin.offset(random.nextInt(9) - 4, 0, random.nextInt(9) - 4), random);
        }
        return placed;
    }

    private static boolean plantBed(WorldGenLevel level, BlockPos floor, RandomSource random) {
        BlockState below = level.getBlockState(floor);
        if (!below.isFaceSturdy(level, floor, Direction.UP)) {
            return false;
        }
        if (random.nextInt(3) == 0) {
            level.setBlock(floor, BlockRegistry.SULFUR_CRUST.get().defaultBlockState(), 2);
        }
        boolean placed = false;
        int radius = 1 + random.nextInt(3);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                // Volcanic slopes: resolve ground per cell so nothing plants on open water.
                BlockPos ground = supportTopAt(level, floor.offset(dx, 2, dz), 5);
                if (ground == null) {
                    continue;
                }
                BlockPos spot = ground.above();
                // Flora roots into open water only: a mat or ash drift keeps its spot.
                if (!level.getFluidState(spot).is(FluidTags.WATER)
                        || !level.getBlockState(spot).getShape(level, spot).isEmpty()) {
                    continue;
                }
                BlockState plant;
                int roll = random.nextInt(10);
                if (roll < 5) {
                    plant = BlockRegistry.SULFUR_CRYSTAL.get().defaultBlockState();
                } else if (roll < 7) {
                    plant = BlockRegistry.FIREBLOOM.get().defaultBlockState();
                } else {
                    continue;
                }
                level.setBlock(spot, plant.setValue(BlockStateProperties.WATERLOGGED, true), 2);
                placed = true;
            }
        }
        return placed;
    }

    private static boolean hangStalactite(WorldGenLevel level, BlockPos origin, RandomSource random) {
        BlockPos ceiling = findCeiling(level, origin);
        if (!level.getBlockState(ceiling).isFaceSturdy(level, ceiling, Direction.DOWN)) {
            return false;
        }
        int height = 2 + random.nextInt(4);
        boolean placed = false;
        for (int offset = 1; offset <= height; offset++) {
            BlockPos pos = new BlockPos(ceiling.getX(), ceiling.getY() - offset, ceiling.getZ());
            if (!level.getFluidState(pos).is(FluidTags.WATER)) {
                break;
            }
            DroopingSeaweedBlock.SeaweedPart part =
                    offset == 1 ? DroopingSeaweedBlock.SeaweedPart.TOP
                            : offset == height ? DroopingSeaweedBlock.SeaweedPart.TAIL
                            : DroopingSeaweedBlock.SeaweedPart.BODY;
            level.setBlock(pos, BlockRegistry.SULFUR_STALACTITE.get().defaultBlockState()
                    .setValue(DroopingSeaweedBlock.PART, part)
                    .setValue(BlockStateProperties.WATERLOGGED, true), 2);
            placed = true;
        }
        return placed;
    }

    /**
     * The topmost ground block within {@code range} below {@code pos}, or {@code null}
     * over open water. Volcanic slopes demand this per-cell check: a patch plane measured
     * at one center would leave every neighbour floating.
     */
    private static BlockPos supportTopAt(WorldGenLevel level, BlockPos pos, int range) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        for (int i = 0; i < range; i++) {
            BlockState state = level.getBlockState(mutable);
            // Only full-height tops count as ground: thin covers (mats, ash drifts,
            // mirror flakes) would leave plants hovering above their surface.
            if (state.isFaceSturdy(level, mutable, Direction.UP)
                    && state.getShape(level, mutable).max(Direction.Axis.Y) >= 1.0D) {
                return mutable.immutable();
            }
            mutable.move(Direction.DOWN);
        }
        return null;
    }

    /** First overhang above the water column at {@code origin}: the ceiling flora hang from. */
    private static BlockPos findCeiling(WorldGenLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(origin.getX(), origin.getY(),
                origin.getZ());
        int top = origin.getY() + 24;
        while (mutable.getY() < top && !level.getFluidState(mutable).is(FluidTags.WATER)) {
            mutable.move(Direction.UP);
        }
        while (mutable.getY() < top && level.getFluidState(mutable).is(FluidTags.WATER)) {
            mutable.move(Direction.UP);
        }
        return mutable.immutable();
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
