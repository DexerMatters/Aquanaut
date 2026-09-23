package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.common.block.DroopingSeaweedBlock;
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
 * Salt cascades (盐瀑): curtains of salt fringe pouring from overhangs and terrace
 * lips, some drips grown all the way down into halite pipe columns, the pool beneath
 * scattering calcite quills across mirror flakes.
 */
public final class SaltCascadeFeature extends Feature<NoneFeatureConfiguration> {
    public SaltCascadeFeature() {
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
        int drops = 3 + random.nextInt(6);
        for (int i = 0; i < drops; i++) {
            int ox = random.nextInt(9) - 4;
            int oz = random.nextInt(9) - 4;
            placed |= placeCascade(level, origin.offset(ox, 0, oz), random);
        }
        return placed;
    }

    private static boolean placeCascade(WorldGenLevel level, BlockPos origin, RandomSource random) {
        BlockPos ceiling = findCeiling(level, origin);
        BlockState above = level.getBlockState(ceiling);
        if (!above.isFaceSturdy(level, ceiling, Direction.DOWN)) {
            return false;
        }
        int height = 2 + random.nextInt(6);
        boolean grownToFloor = random.nextInt(3) == 0;
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
            level.setBlock(pos, BlockRegistry.SALT_FRINGE.get().defaultBlockState()
                    .setValue(DroopingSeaweedBlock.PART, part)
                    .setValue(BlockStateProperties.WATERLOGGED, true), 2);
            placed = true;
        }

        // Some drips met the floor long ago and thickened into halite columns.
        BlockPos tip = new BlockPos(ceiling.getX(), ceiling.getY() - height - 1, ceiling.getZ());
        if (grownToFloor && placed) {
            BlockPos ground = supportTopAt(level, tip, height + 3);
            if (ground != null) {
                BlockState column = BlockRegistry.HALITE_PIPE.get().defaultBlockState()
                        .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
                BlockPos.MutableBlockPos riser = ground.mutable();
                for (int up = 1; up <= height + 3; up++) {
                    riser.set(ground.getX(), ground.getY() + up, ground.getZ());
                    if (!level.getFluidState(riser).is(FluidTags.WATER)
                            || riser.getY() >= ceiling.getY()) {
                        break;
                    }
                    level.setBlock(riser, column, 2);
                    placed = true;
                }
            }
        }

        // The splash pool: quills and mirror flakes on the floor beneath.
        BlockPos ground = supportTopAt(level, tip, height + 4);
        if (ground != null) {
            BlockPos pool = ground.above();
            // Splash decor roots into open water only: covers keep their spot.
            if (level.getFluidState(pool).is(FluidTags.WATER)
                    && level.getBlockState(pool).getShape(level, pool).isEmpty()) {
                if (random.nextInt(3) == 0) {
                    level.setBlock(pool, BlockRegistry.MIRROR_FLAKE.get().defaultBlockState()
                            .setValue(BlockStateProperties.WATERLOGGED, true), 2);
                    placed = true;
                } else if (random.nextInt(4) == 0) {
                    level.setBlock(pool, BlockRegistry.CALCITE_QUILL.get().defaultBlockState()
                            .setValue(BlockStateProperties.WATERLOGGED, true), 2);
                    placed = true;
                }
            }
        }
        return placed;
    }

    /** First overhang above the water column at {@code origin}: the cascade lip. */
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

    private static BlockPos supportTopAt(WorldGenLevel level, BlockPos pos, int range) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        for (int i = 0; i < range; i++) {
            BlockState state = level.getBlockState(mutable);
            // Only full-height tops count as ground: thin covers would leave the splash
            // decor hovering above their surface.
            if (state.isFaceSturdy(level, mutable, Direction.UP)
                    && state.getShape(level, mutable).max(Direction.Axis.Y) >= 1.0D) {
                return mutable.immutable();
            }
            mutable.move(Direction.DOWN);
        }
        return null;
    }
}
