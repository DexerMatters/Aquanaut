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
 * Hanging mineral fringe from ceilings and overhangs.
 */
public final class SaltFringeFeature extends Feature<NoneFeatureConfiguration> {
    public SaltFringeFeature() {
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
        int drops = 2 + random.nextInt(4);
        for (int i = 0; i < drops; i++) {
            int ox = random.nextInt(9) - 4;
            int oz = random.nextInt(9) - 4;
            BlockPos ceiling = findCeiling(level, origin.offset(ox, 0, oz));
            placed |= placeDrop(level, ceiling, random);
        }
        return placed;
    }

    private static boolean placeDrop(WorldGenLevel level, BlockPos ceiling, RandomSource random) {
        BlockState above = level.getBlockState(ceiling);
        if (!above.isFaceSturdy(level, ceiling, Direction.DOWN)) {
            return false;
        }
        int height = 2 + random.nextInt(5);
        BlockPos.MutableBlockPos mutable = ceiling.mutable();
        for (int offset = 1; offset <= height; offset++) {
            mutable.set(ceiling.getX(), ceiling.getY() - offset, ceiling.getZ());
            if (!level.getFluidState(mutable).is(FluidTags.WATER)) {
                return offset > 1;
            }
        }
        for (int offset = 1; offset <= height; offset++) {
            mutable.set(ceiling.getX(), ceiling.getY() - offset, ceiling.getZ());
            DroopingSeaweedBlock.SeaweedPart part =
                    offset == 1 ? DroopingSeaweedBlock.SeaweedPart.TOP
                            : offset == height ? DroopingSeaweedBlock.SeaweedPart.TAIL
                            : DroopingSeaweedBlock.SeaweedPart.BODY;
            level.setBlock(mutable, BlockRegistry.SALT_FRINGE.get().defaultBlockState()
                    .setValue(DroopingSeaweedBlock.PART, part)
                    .setValue(BlockStateProperties.WATERLOGGED, true), 2);
        }
        return true;
    }

    private static BlockPos findCeiling(WorldGenLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(origin.getX(), origin.getY() - 4,
                origin.getZ());
        int top = origin.getY() + 20;
        while (mutable.getY() < top && level.getFluidState(mutable).is(FluidTags.WATER)) {
            mutable.move(Direction.UP);
        }
        return mutable.immutable();
    }
}
