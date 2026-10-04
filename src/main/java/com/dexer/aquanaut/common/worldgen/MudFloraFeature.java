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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The mud zone's growth layer: seaweed beds, kelp stands, shell litter and drooping seaweed
 * hanging off the reef above. The murk calls for a soft, low canopy rather than a forest,
 * but it is dense enough that the biome reads as alive the moment a diver drops in. Reuses
 * the mod's seaweed blocks and the shell pile; no new flora material.
 */
public final class MudFloraFeature extends Feature<NoneFeatureConfiguration> {
    public MudFloraFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        if (!level.getBiome(origin).is(BiomeRegistry.MUD_ZONE)) {
            return false;
        }

        boolean placedAny = false;
        for (int i = 0; i < 5 + random.nextInt(4); i++) {
            placedAny |= placeSeaweedCluster(level, sampleFloor(level, origin, random), random);
        }
        for (int i = 0; i < 4 + random.nextInt(3); i++) {
            placedAny |= placeShortKelp(level, sampleFloor(level, origin, random), random);
        }
        for (int i = 0; i < 3 + random.nextInt(3); i++) {
            placedAny |= placeShellDebris(level, sampleFloor(level, origin, random), random);
        }
        for (int i = 0; i < 3 + random.nextInt(3); i++) {
            placedAny |= placeFloatingDroopingSeaweed(level, sampleFloor(level, origin, random), random);
        }
        for (int i = 0; i < 4 + random.nextInt(4); i++) {
            placedAny |= placeLeaf(level, sampleFloor(level, origin, random), BlockRegistry.MUD_BLOOM.get());
        }
        for (int i = 0; i < 3 + random.nextInt(3); i++) {
            placedAny |= placeLeaf(level, sampleFloor(level, origin, random), BlockRegistry.BEAN_KELP.get());
        }
        for (int i = 0; i < 2 + random.nextInt(3); i++) {
            placedAny |= placeLeaf(level, sampleFloor(level, origin, random), BlockRegistry.GLOW_FUNGUS.get());
        }
        return placedAny;
    }

    /** One waterlogged plant on the sediment, reusing the mod's plant blocks. */
    private static boolean placeLeaf(WorldGenLevel level, BlockPos floor, Block block) {
        BlockPos target = floor.above();
        if (!level.getFluidState(target).is(FluidTags.WATER)) {
            return false;
        }
        level.setBlock(target, block.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true), 2);
        return true;
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

    private static boolean placeSeaweedCluster(WorldGenLevel level, BlockPos floor, RandomSource random) {
        BlockPos center = floor.above();
        if (!level.getFluidState(center).is(FluidTags.WATER)) {
            return false;
        }
        boolean placedAny = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (random.nextFloat() < 0.45F) {
                    continue;
                }
                BlockPos target = center.offset(dx, random.nextInt(3), dz);
                if (!level.getFluidState(target).is(FluidTags.WATER)) {
                    continue;
                }
                Block block = random.nextFloat() < 0.14F
                        ? BlockRegistry.SEAWEED_FRUIT.get()
                        : BlockRegistry.SEAWEED.get();
                level.setBlock(target, block.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true), 2);
                placedAny = true;
            }
        }
        return placedAny;
    }

    private static boolean placeShortKelp(WorldGenLevel level, BlockPos floor, RandomSource random) {
        if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
            return false;
        }
        int maxHeight = 2 + random.nextInt(5);
        int height = 0;
        for (int i = 1; i <= maxHeight; i++) {
            if (!level.getFluidState(floor.above(i)).is(FluidTags.WATER)) {
                break;
            }
            height = i;
        }
        if (height < 1) {
            return false;
        }
        for (int i = 1; i < height; i++) {
            level.setBlock(floor.above(i), Blocks.KELP_PLANT.defaultBlockState(), 2);
        }
        level.setBlock(floor.above(height),
                Blocks.KELP.defaultBlockState().setValue(KelpBlock.AGE, random.nextInt(24)), 2);
        return true;
    }

    private static boolean placeShellDebris(WorldGenLevel level, BlockPos floor, RandomSource random) {
        boolean placedAny = false;
        int extent = random.nextInt(3);
        for (int dx = -extent; dx <= extent; dx++) {
            for (int dz = -extent; dz <= extent; dz++) {
                BlockPos target = floor.offset(dx, 1, dz);
                if (!level.getFluidState(target).is(FluidTags.WATER)) {
                    continue;
                }
                level.setBlock(target, BlockRegistry.SHELL_PILE.get().defaultBlockState(), 2);
                placedAny = true;
            }
        }
        return placedAny;
    }

    /** A curtain of seaweed hanging off the reef above, like the jelly jungle's. */
    private static boolean placeFloatingDroopingSeaweed(WorldGenLevel level, BlockPos floor, RandomSource random) {
        BlockPos top = floor.above(4 + random.nextInt(10));
        int height = 2 + random.nextInt(7);
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
}
