package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.core.BiomeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class JellyJungleCrackFeature extends Feature<NoneFeatureConfiguration> {
    public JellyJungleCrackFeature() {
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

        Direction axis = random.nextBoolean() ? Direction.NORTH : Direction.EAST;
        int length = 8 + random.nextInt(8);
        int halfWidth = 1 + random.nextInt(2);
        int maxDepth = 8 + random.nextInt(8);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        boolean placedAny = false;

        for (int step = -length; step <= length; step++) {
            int depth = 3 + Mth.floor((1.0F - Math.abs((float) step / (float) Math.max(1, length))) * maxDepth);
            int centerX = origin.getX() + axis.getStepX() * step;
            int centerZ = origin.getZ() + axis.getStepZ() * step;
            for (int lateral = -halfWidth; lateral <= halfWidth; lateral++) {
                int x = centerX + axis.getClockWise().getStepX() * lateral;
                int z = centerZ + axis.getClockWise().getStepZ() * lateral;
                for (int dy = 1; dy >= -depth; dy--) {
                    mutable.set(x, origin.getY() + dy, z);
                    if (mutable.getY() <= level.getMinBuildHeight() || mutable.getY() >= level.getMaxBuildHeight()) {
                        continue;
                    }

                    BlockState current = level.getBlockState(mutable);
                    if (current.is(Blocks.BEDROCK)) {
                        continue;
                    }

                    level.setBlock(mutable, Blocks.WATER.defaultBlockState(), 2);
                    placedAny = true;
                }
            }
        }

        return placedAny;
    }
}
