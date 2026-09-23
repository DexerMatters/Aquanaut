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
 * Salt karst arches (盐拱): hollow bridges left where brine dissolved the weaker beds
 * away. Varve legs carry a span of viscous halite pipe; fringe curtains drip from the
 * underside onto a floor dusted with mirror flakes.
 */
public final class SaltArchFeature extends Feature<NoneFeatureConfiguration> {
    public SaltArchFeature() {
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
        int span = 5 + random.nextInt(5);
        int legHeight = 3 + random.nextInt(5);
        Direction along = Direction.from2DDataValue(random.nextInt(4));
        boolean placed = false;

        // The two legs: stacked varve with halite crust shelves.
        for (int side = -1; side <= 1; side += 2) {
            BlockPos foot = floor.relative(along, side * (span / 2));
            BlockPos ground = supportTopAt(level, foot.above(2), 5);
            if (ground == null) {
                continue;
            }
            for (int up = 1; up <= legHeight; up++) {
                BlockPos pos = ground.above(up);
                if (!canReplace(level, pos)) {
                    break;
                }
                level.setBlock(pos, up % 3 == 0
                        ? BlockRegistry.HALITE_CRUST.get().defaultBlockState()
                        : BlockRegistry.VARVE_SHALE.get().defaultBlockState(), 2);
                placed = true;
            }
        }

        // The span: halite pipe laid along the bridge, arcing up at mid-span.
        BlockState pipe = BlockRegistry.HALITE_PIPE.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, along.getAxis());
        for (int i = -span / 2; i <= span / 2; i++) {
            double t = (double) i / (span / 2.0D);
            int arcY = legHeight + 1 + (int) Math.round((1.0D - t * t) * 1.5D);
            BlockPos top = floor.relative(along, i).above(arcY);
            BlockPos ground = supportTopAt(level, floor.relative(along, i).above(2), 6);
            int baseY = ground == null ? floor.getY() : ground.getY();
            for (int y = 0; y < 2; y++) {
                BlockPos pos = new BlockPos(top.getX(), baseY + arcY + y, top.getZ());
                if (canReplace(level, pos)) {
                    level.setBlock(pos, y == 0 && random.nextInt(4) == 0
                            ? BlockRegistry.HOPPER_HALITE.get().defaultBlockState()
                            : pipe, 2);
                    placed = true;
                }
            }
            // Fringe curtains and flakes below the span.
            BlockPos under = new BlockPos(top.getX(), baseY + arcY - 1, top.getZ());
            if (level.getFluidState(under).is(FluidTags.WATER)) {
                hangFringe(level, under, random);
            }
            BlockPos floorBelow = ground == null ? floor.relative(along, i) : ground;
            if (level.getFluidState(floorBelow.above()).is(FluidTags.WATER) && random.nextInt(3) == 0) {
                level.setBlock(floorBelow.above(), BlockRegistry.MIRROR_FLAKE.get().defaultBlockState()
                        .setValue(BlockStateProperties.WATERLOGGED, true), 2);
                placed = true;
            }
        }
        return placed;
    }

    private static void hangFringe(WorldGenLevel level, BlockPos under, RandomSource random) {
        if (random.nextInt(3) != 0) {
            return;
        }
        int height = 1 + random.nextInt(3);
        for (int offset = 1; offset <= height; offset++) {
            BlockPos pos = under.below(offset);
            if (!level.getFluidState(pos).is(FluidTags.WATER)) {
                return;
            }
            DroopingSeaweedBlock.SeaweedPart part =
                    offset == 1 ? DroopingSeaweedBlock.SeaweedPart.TOP
                            : offset == height ? DroopingSeaweedBlock.SeaweedPart.TAIL
                            : DroopingSeaweedBlock.SeaweedPart.BODY;
            level.setBlock(pos, BlockRegistry.SALT_FRINGE.get().defaultBlockState()
                    .setValue(DroopingSeaweedBlock.PART, part)
                    .setValue(BlockStateProperties.WATERLOGGED, true), 2);
        }
    }

    private static boolean canReplace(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getFluidState().is(FluidTags.WATER) || state.canBeReplaced();
    }

    private static BlockPos supportTopAt(WorldGenLevel level, BlockPos pos, int range) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        for (int i = 0; i < range; i++) {
            BlockState state = level.getBlockState(mutable);
            // Only full-height tops count as ground: thin covers would leave the arch
            // legs hovering above their surface.
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
