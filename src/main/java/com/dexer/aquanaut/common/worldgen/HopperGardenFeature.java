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
 * Hopper gardens (漏斗晶园): fields of nested hopper halite cubes stepping upward like
 * salt ziggurats, their terraces dusted with mirror flakes where the brine dried.
 */
public final class HopperGardenFeature extends Feature<NoneFeatureConfiguration> {
    public HopperGardenFeature() {
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
        int clusters = 2 + random.nextInt(4);
        for (int i = 0; i < clusters; i++) {
            int ox = random.nextInt(11) - 5;
            int oz = random.nextInt(11) - 5;
            BlockPos floor = findFloor(level, origin.offset(ox, 0, oz));
            placed |= placeZiggurat(level, floor, random);
        }
        return placed;
    }

    private static boolean placeZiggurat(WorldGenLevel level, BlockPos floor, RandomSource random) {
        BlockState below = level.getBlockState(floor);
        if (!below.isFaceSturdy(level, floor, Direction.UP)) {
            return false;
        }
        int levels = 2 + random.nextInt(3);
        int footprint = 1 + random.nextInt(2);
        boolean placed = false;

        // Salt crust apron with mirror flake dusting.
        for (int dx = -footprint - 1; dx <= footprint + 1; dx++) {
            for (int dz = -footprint - 1; dz <= footprint + 1; dz++) {
                BlockPos ground = supportTopAt(level, floor.offset(dx, 2, dz), 4);
                if (ground == null || random.nextInt(3) == 0) {
                    continue;
                }
                BlockPos crown = ground.above();
                if (level.getFluidState(crown).is(FluidTags.WATER)) {
                    level.setBlock(crown, random.nextInt(4) == 0
                            ? BlockRegistry.MIRROR_FLAKE.get().defaultBlockState()
                                    .setValue(BlockStateProperties.WATERLOGGED, true)
                            : BlockRegistry.HALITE_CRUST.get().defaultBlockState(), 2);
                    placed = true;
                }
            }
        }

        // Nested hopper cubes rising in steps.
        BlockPos.MutableBlockPos cursor = floor.mutable();
        for (int step = 0; step < levels; step++) {
            int radius = Math.max(0, footprint - step / 2);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(floor.getX() + dx, floor.getY() + 1 + step, floor.getZ() + dz);
                    if (!canReplace(level, cursor)) {
                        continue;
                    }
                    level.setBlock(cursor, BlockRegistry.HOPPER_HALITE.get().defaultBlockState(), 2);
                    placed = true;
                }
            }
        }
        return placed;
    }

    private static boolean canReplace(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getFluidState().is(FluidTags.WATER) || state.canBeReplaced();
    }

    private static BlockPos supportTopAt(WorldGenLevel level, BlockPos pos, int range) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        for (int i = 0; i < range; i++) {
            BlockState state = level.getBlockState(mutable);
            // Only full-height tops count as ground: thin covers would leave the crust
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
