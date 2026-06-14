package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.core.BiomeRegistry;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class JellyJungleStemForestFeature extends Feature<NoneFeatureConfiguration> {
    public JellyJungleStemForestFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        if (!level.getBiome(origin).is(BiomeRegistry.JELLY_JUNGLE)) {
            return false;
        }

        boolean placedAny = false;
        int clusterCount = 2 + random.nextInt(3);
        for (int i = 0; i < clusterCount; i++) {
            BlockPos floor = sampleFloor(level, origin, random);
            BlockPos root = floor.above();
            JellyJungleStemForestPlanner.Plan plan = JellyJungleStemForestPlanner.plan(random.nextLong());
            if (!canPlacePlan(level, root, plan)) {
                continue;
            }

            materializePlan(level, root, plan, random);
            placedAny = true;
        }
        return placedAny;
    }

    private static BlockPos sampleFloor(WorldGenLevel level, BlockPos origin, RandomSource random) {
        int x = origin.getX() + random.nextInt(13) - 6;
        int z = origin.getZ() + random.nextInt(13) - 6;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(x, origin.getY() + 12, z);
        while (mutable.getY() > origin.getY() - 12 && level.getFluidState(mutable).is(FluidTags.WATER)) {
            mutable.move(Direction.DOWN);
        }
        return mutable.immutable();
    }

    private static boolean canPlacePlan(WorldGenLevel level, BlockPos root, JellyJungleStemForestPlanner.Plan plan) {
        if (!level.getFluidState(root).is(FluidTags.WATER)) {
            return false;
        }

        for (JellyJungleStemForestPlanner.Pos planned : plan.stemPositions()) {
            BlockPos target = root.offset(planned.x(), planned.y(), planned.z());
            if (!canReplaceStem(level, target)) {
                return false;
            }
        }
        return true;
    }

    private static void materializePlan(WorldGenLevel level,
                                        BlockPos root,
                                        JellyJungleStemForestPlanner.Plan plan,
                                        RandomSource random) {
        JellyJungleStemForestConnections.ConnectionMap connections = JellyJungleStemForestConnections.forPlan(plan);
        for (JellyJungleStemForestPlanner.Pos planned : plan.stemPositions()) {
            level.setBlock(root.offset(planned.x(), planned.y(), planned.z()),
                    stemState(connections.states().get(planned)), 3);
        }

        for (JellyJungleStemForestPlanner.Pos planned : plan.foliageTargets()) {
            BlockPos target = root.offset(planned.x(), planned.y(), planned.z());
            if (!canReplaceFoliage(level, target)) {
                continue;
            }

            Block foliage = random.nextFloat() < 0.14F
                    ? BlockRegistry.SEAWEED_FRUIT.get()
                    : BlockRegistry.SEAWEED.get();
            level.setBlock(target, foliage.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true), 3);
        }

        for (JellyJungleStemForestPlanner.Pos planned : plan.stemPositions()) {
            BlockPos target = root.offset(planned.x(), planned.y(), planned.z());
            level.setBlock(target, resolvedStemState(level, target), 3);
        }
    }

    private static boolean canReplaceStem(WorldGenLevel level, BlockPos pos) {
        if (pos.getY() <= level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        return state.getFluidState().is(FluidTags.WATER)
                || state.is(BlockRegistry.SEAWEED.get())
                || state.is(BlockRegistry.SEAWEED_FRUIT.get())
                || state.is(BlockRegistry.DROOPING_SEAWEED.get())
                || state.is(Blocks.KELP)
                || state.is(Blocks.KELP_PLANT);
    }

    private static boolean canReplaceFoliage(WorldGenLevel level, BlockPos pos) {
        if (pos.getY() <= level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
            return false;
        }
        return level.getFluidState(pos).is(FluidTags.WATER)
                || level.getBlockState(pos).is(Blocks.KELP)
                || level.getBlockState(pos).is(Blocks.KELP_PLANT);
    }

    private static BlockState stemState(JellyJungleStemForestConnections.ConnectionState state) {
        return BlockRegistry.SEAWEED_STEM.get().defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, true)
                .setValue(BlockStateProperties.NORTH, state.north())
                .setValue(BlockStateProperties.SOUTH, state.south())
                .setValue(BlockStateProperties.EAST, state.east())
                .setValue(BlockStateProperties.WEST, state.west())
                .setValue(BlockStateProperties.UP, state.up())
                .setValue(BlockStateProperties.DOWN, state.down());
    }

    private static BlockState resolvedStemState(WorldGenLevel level, BlockPos pos) {
        return BlockRegistry.SEAWEED_STEM.get().defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, true)
                .setValue(BlockStateProperties.NORTH, connectsAt(level, pos.north()))
                .setValue(BlockStateProperties.SOUTH, connectsAt(level, pos.south()))
                .setValue(BlockStateProperties.EAST, connectsAt(level, pos.east()))
                .setValue(BlockStateProperties.WEST, connectsAt(level, pos.west()))
                .setValue(BlockStateProperties.UP, connectsAt(level, pos.above()))
                .setValue(BlockStateProperties.DOWN, connectsAt(level, pos.below()));
    }

    private static boolean connectsAt(WorldGenLevel level, BlockPos pos) {
        BlockState neighbor = level.getBlockState(pos);
        return neighbor.is(BlockRegistry.SEAWEED_STEM.get())
                || neighbor.is(BlockRegistry.SEAWEED.get())
                || neighbor.is(BlockRegistry.SEAWEED_FRUIT.get())
                || neighbor.is(BlockRegistry.DROOPING_SEAWEED.get());
    }
}
