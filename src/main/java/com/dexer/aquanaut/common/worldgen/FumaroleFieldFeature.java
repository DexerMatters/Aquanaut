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
 * Fumarole fields: sinter cones frosted with sulfur and crowned by a live vent mouth, with
 * sulfur stalactites dripping from every overhang the steam reaches.
 */
public final class FumaroleFieldFeature extends Feature<NoneFeatureConfiguration> {
    public FumaroleFieldFeature() {
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
        int vents = 4 + random.nextInt(6);
        for (int i = 0; i < vents; i++) {
            int ox = random.nextInt(13) - 6;
            int oz = random.nextInt(13) - 6;
            BlockPos floor = findFloor(level, origin.offset(ox, 0, oz));
            placed |= placeVent(level, floor, random);
        }

        int drips = 1 + random.nextInt(3);
        for (int i = 0; i < drips; i++) {
            placed |= placeStalactite(level,
                    origin.offset(random.nextInt(9) - 4, 0, random.nextInt(9) - 4), random);
        }
        return placed;
    }

    private static boolean placeVent(WorldGenLevel level, BlockPos floor, RandomSource random) {
        if (!solidGround(level, floor)) {
            return false;
        }
        BlockPos throat = floor.above();
        // The vent mouth needs open water and real ground: never perched on a cover.
        if (!openWater(level, throat)) {
            return false;
        }

        // A low siliceous sinter cone: hot-spring deposits build up around the mouth, so the
        // vent reads as a hydrothermal landform instead of a single block in the silt.
        BlockState sinter = BlockRegistry.SINTER.get().defaultBlockState();
        BlockState crust = BlockRegistry.SULFUR_CRUST.get().defaultBlockState();
        int radius = 1 + random.nextInt(2);
        int height = 1 + random.nextInt(2);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > radius + 0.35D) {
                    continue;
                }
                BlockPos ground = supportTopAt(level, floor.offset(dx, 2, dz), 4);
                if (ground == null) {
                    continue;
                }
                int layers = (int) Math.round(height * (1.0D - d / (radius + 0.8D)));
                for (int i = 1; i <= layers; i++) {
                    BlockPos pos = ground.offset(0, i, 0);
                    if (!openWater(level, pos)) {
                        break;
                    }
                    level.setBlock(pos, random.nextInt(3) == 0 ? crust : sinter, 2);
                }
            }
        }

        BlockPos crest = supportTopAt(level, floor.offset(0, height + 3, 0), 7);
        if (crest == null) {
            return false;
        }
        BlockPos mouth = crest.above();
        if (!openWater(level, mouth)) {
            return false;
        }
        level.setBlock(mouth, BlockRegistry.FUMAROLE.get().defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, true), 2);

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos rim = crest.relative(direction);
            if (random.nextInt(3) != 0 && solidGround(level, rim) && openWater(level, rim.above())) {
                level.setBlock(rim, crust, 2);
            }
            BlockPos blade = mouth.relative(direction);
            if (random.nextInt(4) == 0 && solidGround(level, blade.below())
                    && openWater(level, blade)) {
                level.setBlock(blade, BlockRegistry.SULFUR_CRYSTAL.get().defaultBlockState()
                        .setValue(BlockStateProperties.WATERLOGGED, true), 2);
            }
        }
        return true;
    }

    /** Open water with no partial cover in it: the only space a vent may grow into. */
    private static boolean openWater(WorldGenLevel level, BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.WATER)
                && level.getBlockState(pos).getShape(level, pos).isEmpty();
    }

    private static boolean solidGround(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isFaceSturdy(level, pos, Direction.UP)
                && state.getShape(level, pos).max(Direction.Axis.Y) >= 1.0D;
    }

    private static BlockPos supportTopAt(WorldGenLevel level, BlockPos pos, int range) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        for (int i = 0; i < range; i++) {
            BlockState state = level.getBlockState(mutable);
            if (state.isFaceSturdy(level, mutable, Direction.UP)
                    && state.getShape(level, mutable).max(Direction.Axis.Y) >= 1.0D) {
                return mutable.immutable();
            }
            mutable.move(Direction.DOWN);
        }
        return null;
    }

    private static boolean placeStalactite(WorldGenLevel level, BlockPos origin, RandomSource random) {
        BlockPos ceiling = findCeiling(level, origin);
        if (!level.getBlockState(ceiling).isFaceSturdy(level, ceiling, Direction.DOWN)) {
            return false;
        }
        int height = 1 + random.nextInt(3);
        for (int offset = 1; offset <= height; offset++) {
            BlockPos pos = new BlockPos(ceiling.getX(), ceiling.getY() - offset, ceiling.getZ());
            if (!level.getFluidState(pos).is(FluidTags.WATER)) {
                return offset > 1;
            }
            DroopingSeaweedBlock.SeaweedPart part =
                    offset == 1 ? DroopingSeaweedBlock.SeaweedPart.TOP
                            : offset == height ? DroopingSeaweedBlock.SeaweedPart.TAIL
                            : DroopingSeaweedBlock.SeaweedPart.BODY;
            level.setBlock(pos, BlockRegistry.SULFUR_STALACTITE.get().defaultBlockState()
                    .setValue(DroopingSeaweedBlock.PART, part)
                    .setValue(BlockStateProperties.WATERLOGGED, true), 2);
        }
        return true;
    }

    /** First overhang above the water column at {@code origin}: the ceiling drips hang from. */
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
