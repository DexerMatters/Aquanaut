package com.dexer.aquanaut.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Low shell pile used as a hermit crab shell-upgrade landmark. */
public final class ShellPileBlock extends Block {
    public static final MapCodec<ShellPileBlock> CODEC = simpleCodec(ShellPileBlock::new);
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 8.0D, 15.0D);

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
}
