package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.core.BiomeRegistry;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A low fossil outcrop: a dome of siltstone with soft mud at its skirt, capped by a single
 * coherent fossil bed. The mud zone's landmark — layered, never a per-block speckle, and rooted
 * on the real floor so it can never float.
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
        BlockPos floor = MudZoneFloor.find(level, origin);
        if (floor == null) {
            return false;
        }

        int radius = 2 + random.nextInt(3);
        int peak = 2 + random.nextInt(2);
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
                    if (!existing.canBeReplaced()) {
                        break;
                    }
                    level.setBlock(cursor, outcropState(dy, height), 2);
                    placed = true;
                }
            }
        }
        return placed;
    }

    /** Banded by height: mud at the skirt, siltstone in the body, one fossil bed on the cap. */
    private static BlockState outcropState(int dy, int height) {
        if (dy >= height) {
            return BlockRegistry.FOSSIL_BED.get().defaultBlockState();
        }
        if (dy == 1) {
            return BlockRegistry.MUD.get().defaultBlockState();
        }
        return BlockRegistry.SILTSTONE.get().defaultBlockState();
    }
}
