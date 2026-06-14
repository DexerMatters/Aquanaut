package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.common.block.DroopingSeaweedBlock;
import com.dexer.aquanaut.core.BiomeRegistry;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.KelpPlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class JellyJungleVegetationFeature extends Feature<NoneFeatureConfiguration> {
    public JellyJungleVegetationFeature() {
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
        for (int i = 0; i < 4 + random.nextInt(4); i++) {
            placedAny |= placeKelp(level, sampleFloor(level, origin, random), random);
        }
        for (int i = 0; i < 3 + random.nextInt(4); i++) {
            placedAny |= placeFloatingDroopingSeaweed(level, sampleFloor(level, origin, random), random);
        }
        for (int i = 0; i < 5 + random.nextInt(5); i++) {
            placedAny |= placeSeaweedCluster(level, sampleFloor(level, origin, random), random);
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

    private static boolean placeKelp(WorldGenLevel level, BlockPos floor, RandomSource random) {
        if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
            return false;
        }

        int height = 3 + random.nextInt(8);
        BlockPos.MutableBlockPos mutable = floor.mutable();
        for (int i = 1; i <= height; i++) {
            mutable.set(floor.getX(), floor.getY() + i, floor.getZ());
            if (!level.getFluidState(mutable).is(FluidTags.WATER)) {
                return i > 1;
            }
        }

        for (int i = 1; i < height; i++) {
            mutable.set(floor.getX(), floor.getY() + i, floor.getZ());
            level.setBlock(mutable, Blocks.KELP_PLANT.defaultBlockState(), 2);
        }

        mutable.set(floor.getX(), floor.getY() + height, floor.getZ());
        level.setBlock(mutable, Blocks.KELP.defaultBlockState()
                .setValue(KelpBlock.AGE, random.nextInt(24)), 2);
        return true;
    }

    private static boolean placeFloatingDroopingSeaweed(WorldGenLevel level, BlockPos floor, RandomSource random) {
        BlockPos top = floor.above(4 + random.nextInt(8));
        int height = 2 + random.nextInt(6);
        for (int offset = 0; offset < height; offset++) {
            if (!level.getFluidState(top.below(offset)).is(FluidTags.WATER)) {
                return false;
            }
        }

        for (int offset = 0; offset < height; offset++) {
            BlockPos current = top.below(offset);
            BlockState state = BlockRegistry.DROOPING_SEAWEED.get().defaultBlockState()
                    .setValue(BlockStateProperties.WATERLOGGED, true);
            if (offset == 0) {
                state = state.setValue(DroopingSeaweedBlock.PART, DroopingSeaweedBlock.SeaweedPart.TOP);
            } else if (offset == height - 1) {
                state = state.setValue(DroopingSeaweedBlock.PART, DroopingSeaweedBlock.SeaweedPart.TAIL);
            } else {
                state = state.setValue(DroopingSeaweedBlock.PART, DroopingSeaweedBlock.SeaweedPart.BODY);
            }
            level.setBlock(current, state, 2);
        }
        return true;
    }

    private static boolean placeSeaweedCluster(WorldGenLevel level, BlockPos floor, RandomSource random) {
        BlockPos center = floor.above();
        if (!level.getFluidState(center).is(FluidTags.WATER)) {
            return false;
        }

        boolean placedAny = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (random.nextFloat() < 0.35F) {
                    continue;
                }
                BlockPos target = center.offset(dx, random.nextInt(3), dz);
                if (!level.getFluidState(target).is(FluidTags.WATER)) {
                    continue;
                }

                Block block = random.nextFloat() < 0.12F
                        ? BlockRegistry.SEAWEED_FRUIT.get()
                        : BlockRegistry.SEAWEED.get();
                level.setBlock(target, block.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true), 2);
                placedAny = true;
            }
        }
        return placedAny;
    }
}
