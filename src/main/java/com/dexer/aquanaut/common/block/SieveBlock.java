package com.dexer.aquanaut.common.block;

import com.dexer.aquanaut.common.block.entity.SieveBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * The sifting sieve, exactly one block tall: a stone base, a tall woven net bag standing on it,
 * and a thin-framed screen tray perched on the bag's mouth, sliding left and right.
 *
 * <p>
 * The lowest face sits on the block's bottom plane, so the model fills the cell from {@code y = 0}
 * to {@code y = 16}. The geometry comes from the GeckoLib block-entity renderer like the other
 * fabricated stations; the vanilla block model is an empty shell that only supplies the breaking
 * particles and the outline.
 *
 * <p>
 * Sifting is not implemented yet: the block is a decorative placement, and the shaking loop is a
 * constant ambient animation.
 */
public class SieveBlock extends BaseEntityBlock {
    public static final MapCodec<SieveBlock> CODEC = simpleCodec(SieveBlock::new);

    /**
     * The outline follows the model, so no part of it hangs in mid-air: the stone base slab is the
     * widest piece at 14x14 and the woven bag carrying the screen tray is a 12x12 column standing on
     * it. The tray's sideways slide is animation only and deliberately gets no collision of its own,
     * otherwise the outline would frame the empty air above the base that the tray merely sweeps
     * through.
     */
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2.0, 1.0, 2.0, 14.0, 16.0, 14.0),
            Block.box(1.0, 0.0, 1.0, 15.0, 1.0, 15.0));

    public SieveBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public SieveBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SieveBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
