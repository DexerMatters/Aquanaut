package com.dexer.aquanaut.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A cresset of glowing mud sitting on a mud-brick footing. It is deliberately not a full cube:
 * a full 16-cube of glow read as a glowing box rather than a lamp, and the shape here matches
 * both the model and the light it casts.
 */
public final class MudLampBlock extends Block {
    public static final MapCodec<MudLampBlock> CODEC = simpleCodec(MudLampBlock::new);
    /** The footing and the glowing core together: 14 wide, 13 tall. */
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 13.0D, 15.0D);

    public MudLampBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<MudLampBlock> codec() {
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
