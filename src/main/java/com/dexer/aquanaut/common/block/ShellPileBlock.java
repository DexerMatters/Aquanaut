package com.dexer.aquanaut.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Low shell pile used as a hermit crab shell-upgrade landmark. */
public final class ShellPileBlock extends Block {
    public static final MapCodec<ShellPileBlock> CODEC = simpleCodec(ShellPileBlock::new);
    /**
     * Occluding full shape, so adjacent water is culled rather than drawn as flowing water
     * over the pile's sides; the model is a full cube and matches it. The collision matches it
     * too: with a short collision the visual upper half was solid-looking but passable, so
     * creatures swam straight through the pile.
     */
    private static final VoxelShape SHAPE = Shapes.block();

    public ShellPileBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<ShellPileBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return SHAPE;
    }
}
