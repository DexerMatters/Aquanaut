package com.dexer.aquanaut.common.block;

import com.dexer.aquanaut.core.ParticleRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A fumarole mouth: a mineral-rimmed vent that fumes continuously. Visually a squat vent
 * mound; behaviorally a particle source — hot steam (热泉 vapor) and the occasional sour
 * puff of sulfur gas hissing into the surrounding sea.
 */
public final class FumaroleBlock extends Block implements SimpleWaterloggedBlock {
    public static final MapCodec<FumaroleBlock> CODEC = simpleCodec(FumaroleBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2.0D, 0.0D, 2.0D, 14.0D, 6.0D, 14.0D),
            Block.box(5.0D, 0.0D, 5.0D, 11.0D, 10.0D, 11.0D));

    public FumaroleBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
    }

    @Override
    protected MapCodec<FumaroleBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        if (!fluidState.is(Fluids.WATER)) {
            return null;
        }
        return defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.WATERLOGGED);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.4D;
        double y = pos.getY() + 0.75D;
        double z = pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.4D;
        if (random.nextInt(4) == 0) {
            // Sour sulfur gas hissing out between the steam.
            level.addParticle(ParticleRegistry.SULFUR_GAS.get(), x, y, z,
                    0.0D, 0.015D + random.nextDouble() * 0.01D, 0.0D);
        } else {
            level.addParticle(ParticleRegistry.VENT_STEAM.get(), x, y, z,
                    (random.nextDouble() - 0.5D) * 0.01D,
                    0.02D + random.nextDouble() * 0.015D,
                    (random.nextDouble() - 0.5D) * 0.01D);
        }
    }
}
