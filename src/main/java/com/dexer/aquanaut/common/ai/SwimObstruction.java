package com.dexer.aquanaut.common.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * "Would this block stop a fish?" — the question the school and the movement controller keep asking.
 *
 * <p>
 * This used to be {@code BlockState#isSolid()}. That method, and its {@code blocksMotion()} sibling,
 * are deprecated with no direct replacement, so the predicate is spelled out here instead of being
 * inherited from whichever vanilla shortcut happens to survive. A block obstructs a swimmer when it
 * has a collision shape at all, except for the two shapes vanilla deliberately lets swimmers pass
 * through — cobwebs and bamboo saplings.
 */
final class SwimObstruction {

    private SwimObstruction() {
    }

    /** Whether {@code pos} holds a block that a fish should swim around rather than through. */
    static boolean blocks(BlockGetter level, BlockPos pos) {
        return blocks(level.getBlockState(pos), level, pos);
    }

    private static boolean blocks(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.is(Blocks.COBWEB) || state.is(Blocks.BAMBOO_SAPLING)) {
            return false;
        }
        return !state.getCollisionShape(level, pos).isEmpty();
    }
}