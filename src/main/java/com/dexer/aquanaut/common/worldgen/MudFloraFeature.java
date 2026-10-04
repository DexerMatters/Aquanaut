package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.core.BiomeRegistry;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The mud zone's growth layer: the biome's own plants only -- mud bloom, bean kelp and
 * glow fungus -- plus a rare, small shell scatter. It deliberately does NOT place seaweed,
 * seaweed fruit, kelp or drooping seaweed: those belong to the seaweed/jelly provinces,
 * and reusing them here made the mud flats read as a second jelly jungle instead of their
 * own biome.
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
        for (int i = 0; i < 6 + random.nextInt(5); i++) {
            placedAny |= placeLeaf(level, sampleFloor(level, origin, random), BlockRegistry.MUD_BLOOM.get());
        }
        for (int i = 0; i < 4 + random.nextInt(4); i++) {
            placedAny |= placeLeaf(level, sampleFloor(level, origin, random), BlockRegistry.BEAN_KELP.get());
        }
        for (int i = 0; i < 2 + random.nextInt(3); i++) {
            placedAny |= placeLeaf(level, sampleFloor(level, origin, random), BlockRegistry.GLOW_FUNGUS.get());
        }
        // Shells are a seasoning, not a bed: one small patch at most, and usually none.
        if (random.nextFloat() < 0.2F) {
            placedAny |= placeShellDebris(level, sampleFloor(level, origin, random), random);
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

    /** A small, rare scatter of shell litter. */
    private static boolean placeShellDebris(WorldGenLevel level, BlockPos floor, RandomSource random) {
        boolean placedAny = false;
        int extent = random.nextInt(2);
        for (int dx = -extent; dx <= extent; dx++) {
            for (int dz = -extent; dz <= extent; dz++) {
                if (random.nextFloat() < 0.35F) {
                    continue;
                }
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
}
