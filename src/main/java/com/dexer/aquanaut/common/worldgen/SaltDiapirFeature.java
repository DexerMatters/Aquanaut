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
 * Salt diapirs (盐丘): tall, gently leaning towers of pressurized halite that squeezed
 * upward through the gorge. Banded hopper growth rings terrace the flanks, halite pipe
 * forms the viscous core, and salt fringe drips from every overhanging step.
 */
public final class SaltDiapirFeature extends Feature<NoneFeatureConfiguration> {
    public SaltDiapirFeature() {
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
        int height = 12 + random.nextInt(15);
        int baseRadius = 3 + random.nextInt(4);
        double leanX = (random.nextDouble() - 0.5D) * 0.35D;
        double leanZ = (random.nextDouble() - 0.5D) * 0.35D;
        int band = 4 + random.nextInt(4);
        BlockState pipe = BlockRegistry.HALITE_PIPE.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        boolean placed = false;

        for (int y = 0; y <= height; y++) {
            double t = (double) y / height;
            int radius = Math.max(1, (int) Math.round(baseRadius * (1.0D - t * 0.7D)));
            // Terraced growth: hopper bands step the flank outward every few levels.
            if (y % band == band - 1) {
                radius += 1;
            }
            int cx = floor.getX() + (int) Math.round(leanX * y);
            int cz = floor.getZ() + (int) Math.round(leanZ * y);
            for (int dx = -radius - 1; dx <= radius + 1; dx++) {
                for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                    int dist2 = dx * dx + dz * dz;
                    if (dist2 > (radius + 1) * (radius + 1)) {
                        continue;
                    }
                    BlockPos pos = new BlockPos(cx + dx, floor.getY() + y, cz + dz);
                    if (!canReplace(level, pos)) {
                        continue;
                    }
                    BlockState state;
                    if (dist2 > (radius - 1) * (radius - 1)) {
                        state = y % band == band - 1
                                ? BlockRegistry.HOPPER_HALITE.get().defaultBlockState()
                                : BlockRegistry.HALITE_CRUST.get().defaultBlockState();
                    } else if (y % band == band - 1) {
                        state = BlockRegistry.HOPPER_HALITE.get().defaultBlockState();
                    } else {
                        state = pipe;
                    }
                    level.setBlock(pos, state, 2);
                    placed = true;
                }
            }

            // Salt fringe dripping from the overhanging step edges.
            if (y % band == band - 1 && y > 2 && random.nextInt(3) == 0) {
                BlockPos edge = new BlockPos(cx + radius * directionSign(random), floor.getY() + y - 1,
                        cz + radius * directionSign(random));
                hangFringe(level, edge, random);
            }
        }

        // Crown: hopper crystals over the squeezed summit.
        BlockPos crown = new BlockPos(floor.getX() + (int) Math.round(leanX * height), floor.getY() + height + 1,
                floor.getZ() + (int) Math.round(leanZ * height));
        if (level.getFluidState(crown).is(FluidTags.WATER)) {
            level.setBlock(crown, BlockRegistry.HOPPER_HALITE.get().defaultBlockState(), 2);
            placed = true;
        }
        for (int i = 0; i < 3; i++) {
            BlockPos spot = crown.offset(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
            BlockState below = level.getBlockState(spot.below());
            if (level.getFluidState(spot).is(FluidTags.WATER)
                    && level.getBlockState(spot).getShape(level, spot).isEmpty()
                    && below.isFaceSturdy(level, spot.below(), Direction.UP)
                    && below.getShape(level, spot.below()).max(Direction.Axis.Y) >= 1.0D
                    && random.nextBoolean()) {
                level.setBlock(spot, random.nextBoolean()
                        ? BlockRegistry.HALITE_ROSETTE.get().defaultBlockState()
                        .setValue(BlockStateProperties.WATERLOGGED, true)
                        : BlockRegistry.CALCITE_QUILL.get().defaultBlockState()
                        .setValue(BlockStateProperties.WATERLOGGED, true), 2);
                placed = true;
            }
        }
        return placed;
    }

    private static int directionSign(RandomSource random) {
        return random.nextBoolean() ? 1 : -1;
    }

    private static void hangFringe(WorldGenLevel level, BlockPos from, RandomSource random) {
        int height = 1 + random.nextInt(3);
        for (int offset = 1; offset <= height; offset++) {
            BlockPos pos = from.below(offset);
            if (!level.getFluidState(pos).is(FluidTags.WATER)) {
                return;
            }
            com.dexer.aquanaut.common.block.DroopingSeaweedBlock.SeaweedPart part =
                    offset == 1 ? com.dexer.aquanaut.common.block.DroopingSeaweedBlock.SeaweedPart.TOP
                            : offset == height ? com.dexer.aquanaut.common.block.DroopingSeaweedBlock.SeaweedPart.TAIL
                            : com.dexer.aquanaut.common.block.DroopingSeaweedBlock.SeaweedPart.BODY;
            level.setBlock(pos, BlockRegistry.SALT_FRINGE.get().defaultBlockState()
                    .setValue(com.dexer.aquanaut.common.block.DroopingSeaweedBlock.PART, part)
                    .setValue(BlockStateProperties.WATERLOGGED, true), 2);
        }
    }

    private static boolean canReplace(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getFluidState().is(FluidTags.WATER) || state.canBeReplaced();
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
