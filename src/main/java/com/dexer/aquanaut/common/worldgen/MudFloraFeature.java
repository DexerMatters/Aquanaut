package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.core.BiomeRegistry;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The mud zone's growth layer: the biome's own plants only -- mud bloom, bean kelp, glow fungus,
 * silt reed, pale puffball and the sea moss mat -- plus a rare, small shell scatter.
 *
 * <p>Every plant is rooted through {@link MudZoneFloor}, which only accepts real terrain, so a
 * plant can never be parked in open water (the old column scan stopped on water and called it
 * ground) and never lands on top of an earlier structure such as a glow mushroom.</p>
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
        placedAny |= scatter(level, origin, random, BlockRegistry.MUD_BLOOM.get(), 1 + random.nextInt(2));
        placedAny |= scatter(level, origin, random, BlockRegistry.BEAN_KELP.get(), 1 + random.nextInt(2));
        placedAny |= scatter(level, origin, random, BlockRegistry.SILT_REED.get(), 1 + random.nextInt(2));
        placedAny |= scatter(level, origin, random, BlockRegistry.GLOW_FUNGUS.get(), 1);
        if (random.nextBoolean()) {
            placedAny |= scatter(level, origin, random, BlockRegistry.GLOW_FUNGUS_AMBER.get(), 1);
        }
        if (random.nextBoolean()) {
            placedAny |= scatter(level, origin, random, BlockRegistry.GLOW_FUNGUS_VIOLET.get(), 1);
        }
        if (random.nextInt(3) == 0) {
            placedAny |= scatter(level, origin, random, BlockRegistry.PALE_PUFFBALL.get(), 1);
        }
        // Sea moss only takes hold on the nutrient-rich mud it feeds on.
        placedAny |= scatterMoss(level, origin, random, 3);
        // Shells are a seasoning, not a bed: one small patch at most, and usually none.
        if (random.nextFloat() < 0.12F) {
            BlockPos floor = sampleFloor(level, origin, random);
            if (floor != null) {
                placedAny |= placeShellDebris(level, floor, random);
            }
        }
        return placedAny;
    }

    /** Tries to plant one species on its own random column, a few times over. */
    private static boolean scatter(WorldGenLevel level, BlockPos origin, RandomSource random, Block block,
            int attempts) {
        boolean placed = false;
        for (int i = 0; i < attempts; i++) {
            BlockPos floor = sampleFloor(level, origin, random);
            if (floor != null) {
                placed |= placeLeaf(level, floor, block);
            }
        }
        return placed;
    }

    private static boolean scatterMoss(WorldGenLevel level, BlockPos origin, RandomSource random, int attempts) {
        boolean placed = false;
        for (int i = 0; i < attempts; i++) {
            BlockPos floor = sampleFloor(level, origin, random);
            if (floor != null) {
                placed |= placeMoss(level, floor);
            }
        }
        return placed;
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

    /** A sea moss mat on nutrient-rich mud, waterlogged like the rest of the biome. */
    private static boolean placeMoss(WorldGenLevel level, BlockPos floor) {
        if (!level.getBlockState(floor).is(BlockRegistry.NUTRIENT_RICH_MUD.get())) {
            return false;
        }
        BlockPos target = floor.above();
        if (!level.getFluidState(target).is(FluidTags.WATER)) {
            return false;
        }
        level.setBlock(target, BlockRegistry.SEA_MOSS.get().defaultBlockState(), 2);
        return true;
    }

    /** The sediment under a random column of the patch, or {@code null} when it has none. */
    private static BlockPos sampleFloor(WorldGenLevel level, BlockPos origin, RandomSource random) {
        int x = origin.getX() + random.nextInt(13) - 6;
        int z = origin.getZ() + random.nextInt(13) - 6;
        return MudZoneFloor.find(level, new BlockPos(x, origin.getY() + 12, z));
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
                if (!level.getFluidState(target).is(FluidTags.WATER)
                        || !isSediment(level.getBlockState(floor.offset(dx, 0, dz)))) {
                    continue;
                }
                level.setBlock(target, BlockRegistry.SHELL_PILE.get().defaultBlockState(), 2);
                placedAny = true;
            }
        }
        return placedAny;
    }

    private static boolean isSediment(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(BlockRegistry.MUD.get())
                || state.is(BlockRegistry.NUTRIENT_RICH_MUD.get())
                || state.is(BlockRegistry.PARASITIC_MUD.get())
                || state.is(BlockRegistry.PACKED_MUD.get())
                || state.is(BlockRegistry.CORAL_SAND.get());
    }
}
