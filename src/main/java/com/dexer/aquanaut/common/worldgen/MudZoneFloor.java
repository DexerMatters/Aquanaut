package com.dexer.aquanaut.common.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Locates the real sediment floor beneath a water column. The biome features used to stop at
 * the first non-water block, which is air whenever the column is broken, so a feature could be
 * planted in open water and leave floating blocks. This walks down to the first solid block
 * instead, skipping air and water plants.
 */
public final class MudZoneFloor {
    private static final int ABOVE = 16;
    private static final int BELOW = 40;

    private MudZoneFloor() {
    }

    /** The first solid block under the column, or {@code null} when the column is open. */
    public static BlockPos find(WorldGenLevel level, BlockPos origin) {
        int top = Math.min(origin.getY() + ABOVE, level.getMaxBuildHeight() - 1);
        int bottom = Math.max(level.getMinBuildHeight(), origin.getY() - BELOW);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(origin.getX(), top, origin.getZ());
        while (cursor.getY() > bottom) {
            if (isFloor(level.getBlockState(cursor))) {
                return cursor.immutable();
            }
            cursor.move(Direction.DOWN);
        }
        return null;
    }

    private static boolean isFloor(BlockState state) {
        return !state.isAir() && !state.canBeReplaced()
                && !state.getFluidState().is(FluidTags.WATER);
    }
}
