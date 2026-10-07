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
 * Low, slumped mud mounds on the mud zone floor. Soft sediment piles spread out instead of
 * standing up, so the profile is a rounded dome that flattens toward its rim. The surface reads
 * as mud over a siltstone core — a coherent pile, not a per-block confetti of materials.
 */
public final class MudMoundFeature extends Feature<NoneFeatureConfiguration> {
    public MudMoundFeature() {
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

        int radius = 4 + random.nextInt(4);
        int peak = 2 + random.nextInt(4);
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
                    if (!level.getBlockState(cursor).canBeReplaced()) {
                        break;
                    }
                    level.setBlock(cursor, moundState(dy, height), 2);
                    placed = true;
                }
            }
        }
        return placed;
    }

    /** Mud skin over a siltstone core, banded by height so the mound reads as one pile. */
    private static BlockState moundState(int dy, int height) {
        if (dy >= height - 1) {
            return BlockRegistry.MUD.get().defaultBlockState();
        }
        return BlockRegistry.SILTSTONE.get().defaultBlockState();
    }
}
