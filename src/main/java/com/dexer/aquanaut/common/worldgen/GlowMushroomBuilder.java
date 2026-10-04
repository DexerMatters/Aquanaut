package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Builds the huge glow mushroom: a glowing stalk under a domed cap, with a dark inner layer on
 * the underside. Shared by the worldgen feature and the bone-meal growth so both make the same
 * shape. Fails (and changes nothing) when the space is blocked.
 */
public final class GlowMushroomBuilder {
    private GlowMushroomBuilder() {
    }

    public static boolean grow(LevelAccessor level, BlockPos base, RandomSource random) {
        int height = 4 + random.nextInt(3);
        for (int i = 1; i <= height; i++) {
            if (!level.getBlockState(base.above(i)).canBeReplaced()) {
                return false;
            }
        }
        for (int i = 1; i <= height; i++) {
            level.setBlock(base.above(i), BlockRegistry.GLOW_MUSHROOM_STEM.get().defaultBlockState(), 2);
        }
        int radius = 2 + random.nextInt(2);
        for (int dy = 0; dy <= 1; dy++) {
            int r = radius - dy;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz > r * r + 1) {
                        continue;
                    }
                    if (dy == 0 && dx == 0 && dz == 0) {
                        continue; // keep the stalk top
                    }
                    BlockPos p = base.offset(dx, height + dy, dz);
                    BlockState existing = level.getBlockState(p);
                    if (!existing.canBeReplaced() && !existing.is(BlockRegistry.GLOW_MUSHROOM_STEM.get())) {
                        continue;
                    }
                    boolean underside = dy == 0;
                    level.setBlock(p, (underside ? BlockRegistry.GLOW_MUSHROOM_INSIDE
                            : BlockRegistry.GLOW_MUSHROOM_CAP).get().defaultBlockState(), 2);
                }
            }
        }
        return true;
    }
}
