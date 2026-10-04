package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.core.BiomeRegistry;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A low fossil-bearing outcrop: varve shale eroded out of the mud, banded with fossil beds
 * and crowned with an exposed fossil node. The mud zone's resource landmark — rarer and
 * more deliberate than the drifting sediment patches.
 */
public final class FossilOutcropFeature extends Feature<NoneFeatureConfiguration> {
    public FossilOutcropFeature() {
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

        BlockPos floor = findFloor(level, origin);
        int radius = 2 + random.nextInt(3);
        int peak = 1 + random.nextInt(3);
        boolean placed = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > radius) {
                    continue;
                }
                int height = (int) Math.round(peak * (1.0D - (dist / radius) * (dist / radius)));
                if (height <= 0) {
                    continue;
                }
                for (int dy = 1; dy <= height; dy++) {
                    int y = floor.getY() + dy;
                    if (y >= level.getMaxBuildHeight()) {
                        break;
                    }
                    cursor.set(floor.getX() + dx, y, floor.getZ() + dz);
                    BlockState existing = level.getBlockState(cursor);
                    if (!existing.canBeReplaced() && !level.getFluidState(cursor).is(FluidTags.WATER)) {
                        break;
                    }
                    level.setBlock(cursor, outcropState(random), 2);
                    placed = true;
                }
            }
        }
        if (placed && random.nextFloat() < 0.6F) {
            // Expose one fossil node at the crest.
            cursor.set(floor.getX(), floor.getY() + 1, floor.getZ());
            level.setBlock(cursor, BlockRegistry.FOSSIL_BED.get().defaultBlockState(), 2);
        }
        return placed;
    }

    private static BlockState outcropState(RandomSource random) {
        if (random.nextFloat() < 0.22F) {
            return BlockRegistry.FOSSIL_BED.get().defaultBlockState();
        }
        return random.nextFloat() < 0.6F
                ? BlockRegistry.VARVE_SHALE.get().defaultBlockState()
                : BlockRegistry.MUD.get().defaultBlockState();
    }

    private static BlockPos findFloor(WorldGenLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(origin.getX(), origin.getY() + 16,
                origin.getZ());
        while (mutable.getY() > origin.getY() - 16 && level.getFluidState(mutable).is(FluidTags.WATER)) {
            mutable.move(Direction.DOWN);
        }
        return mutable.immutable();
    }
}
