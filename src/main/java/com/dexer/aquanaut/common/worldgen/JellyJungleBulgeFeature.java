package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.common.block.DroopingSeaweedBlock;
import com.dexer.aquanaut.core.BiomeRegistry;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

public final class JellyJungleBulgeFeature extends Feature<NoneFeatureConfiguration> {
    public JellyJungleBulgeFeature() {
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

        int radiusX = 3 + random.nextInt(3);
        int radiusZ = 3 + random.nextInt(3);
        int height = 2 + random.nextInt(3);
        BlockPos base = origin.below();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        List<BlockPos> topSurface = new ArrayList<>();
        boolean placedAny = false;

        for (int dx = -radiusX; dx <= radiusX; dx++) {
            for (int dz = -radiusZ; dz <= radiusZ; dz++) {
                double horizontal = normalized(dx, radiusX) + normalized(dz, radiusZ);
                if (horizontal > 1.0D) {
                    continue;
                }

                int columnHeight = 1 + Mth.floor((1.0D - horizontal) * height);
                for (int dy = 0; dy <= columnHeight; dy++) {
                    mutable.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
                    if (!canReplaceBulge(level, mutable)) {
                        continue;
                    }

                    boolean exposedSurface = dy == columnHeight;
                    level.setBlock(mutable, exposedSurface ? outerJellyState(random) : innerJellyState(random), 2);
                    if (exposedSurface) {
                        topSurface.add(mutable.immutable());
                    }
                    placedAny = true;
                }
            }
        }

        decorateSurface(level, random, topSurface);
        return placedAny;
    }

    private static void decorateSurface(WorldGenLevel level, RandomSource random, List<BlockPos> topSurface) {
        for (BlockPos surface : topSurface) {
            if (random.nextFloat() < 0.55F) {
                level.setBlock(surface, wrappedJellyState(random), 2);
            }

            BlockPos above = surface.above();
            if (!level.getFluidState(above).is(FluidTags.WATER)) {
                continue;
            }

            float roll = random.nextFloat();
            if (roll < 0.20F) {
                level.setBlock(above, seaweedCoverState(random), 2);
            } else if (roll < 0.28F) {
                int height = 2 + random.nextInt(4);
                placeDroopingColumn(level, above.offset(random.nextInt(3) - 1, 1 + random.nextInt(4),
                        random.nextInt(3) - 1), height);
            }
        }
    }

    private static void placeDroopingColumn(WorldGenLevel level, BlockPos topPos, int height) {
        for (int offset = 0; offset < height; offset++) {
            BlockPos current = topPos.below(offset);
            if (!level.getFluidState(current).is(FluidTags.WATER)) {
                return;
            }
        }

        for (int offset = 0; offset < height; offset++) {
            BlockPos current = topPos.below(offset);
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
    }

    private static BlockState seaweedCoverState(RandomSource random) {
        Block block = random.nextFloat() < 0.14F
                ? BlockRegistry.SEAWEED_FRUIT.get()
                : BlockRegistry.SEAWEED.get();
        return block.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
    }

    private static BlockState innerJellyState(RandomSource random) {
        return switch (random.nextInt(6)) {
            case 0 -> BlockRegistry.WHITE_JELLY_BLOCK.get().defaultBlockState();
            case 1 -> BlockRegistry.LIGHT_GOLDEN_JELLY_BLOCK.get().defaultBlockState();
            case 2, 3 -> BlockRegistry.LIGHT_CYAN_JELLY_BLOCK.get().defaultBlockState();
            default -> BlockRegistry.LIGHT_RED_JELLY_BLOCK.get().defaultBlockState();
        };
    }

    private static BlockState outerJellyState(RandomSource random) {
        return switch (random.nextInt(5)) {
            case 0 -> BlockRegistry.WHITE_JELLY_BLOCK.get().defaultBlockState();
            case 1 -> BlockRegistry.LIGHT_GOLDEN_JELLY_BLOCK.get().defaultBlockState();
            default -> BlockRegistry.LIGHT_CYAN_JELLY_BLOCK.get().defaultBlockState();
        };
    }

    private static BlockState wrappedJellyState(RandomSource random) {
        return switch (random.nextInt(5)) {
            case 0 -> BlockRegistry.WHITE_JELLY_BLOCK_SEAWEED.get().defaultBlockState();
            case 1 -> BlockRegistry.LIGHT_GOLDEN_JELLY_BLOCK_SEAWEED.get().defaultBlockState();
            default -> BlockRegistry.LIGHT_CYAN_JELLY_BLOCK_SEAWEED.get().defaultBlockState();
        };
    }

    private static boolean canReplaceBulge(WorldGenLevel level, BlockPos pos) {
        if (pos.getY() <= level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        return state.getFluidState().is(FluidTags.WATER)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.TUFF)
                || state.is(BlockRegistry.NUTRIENT_RICH_MUD.get())
                || state.is(BlockRegistry.SHALE.get())
                || state.is(BlockRegistry.LIMESTONE.get())
                || state.is(BlockRegistry.SEAWEED.get())
                || state.is(BlockRegistry.SEAWEED_FRUIT.get())
                || state.is(BlockRegistry.DROOPING_SEAWEED.get());
    }

    private static double normalized(int delta, int radius) {
        double scaled = (double) delta / (double) Math.max(1, radius);
        return scaled * scaled;
    }
}
