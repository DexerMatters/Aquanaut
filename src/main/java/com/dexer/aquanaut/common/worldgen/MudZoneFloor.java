package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Locates the real sediment floor beneath a water column. Only terrain counts: plants, corals and
 * the biome's own mushrooms are skipped, so a feature is never planted on top of an earlier
 * structure — the fossil beds used to end up sitting on a glow mushroom's cap for want of this
 * check. Air and water are skipped for the same reason as before, so nothing floats.
 */
public final class MudZoneFloor {
    private static final int ABOVE = 16;
    private static final int BELOW = 40;

    private MudZoneFloor() {
    }

    /** The first terrain block under the column, or {@code null} when the column is open. */
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
                && !state.getFluidState().is(FluidTags.WATER)
                && isTerrain(state);
    }

    /** The materials a feature may stand on: the biome's sediment, its shelf skin and rock. */
    private static boolean isTerrain(BlockState state) {
        return state.is(BlockRegistry.MUD.get())
                || state.is(BlockRegistry.NUTRIENT_RICH_MUD.get())
                || state.is(BlockRegistry.PARASITIC_MUD.get())
                || state.is(BlockRegistry.PACKED_MUD.get())
                || state.is(BlockRegistry.CRACKED_MUD.get())
                || state.is(BlockRegistry.SILTSTONE.get())
                || state.is(BlockRegistry.FOSSIL_BED.get())
                || state.is(BlockRegistry.CORAL_SAND.get())
                || state.is(BlockRegistry.LIMESTONE.get())
                || state.is(BlockRegistry.SHALE.get())
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SAND)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.STONE)
                || state.is(Blocks.DEEPSLATE);
    }
}
