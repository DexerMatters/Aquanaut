package com.dexer.aquanaut.common.block;

import com.dexer.aquanaut.core.TagRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A crystal cluster growing straight out of a rock face — the white, colored, resonant
 * and life-gem crystals of the Crystal Nest. {@code FACING} is the growth direction
 * (away from the face it is anchored to), so all six orientations place and render along
 * their support exactly like vanilla amethyst clusters.
 *
 * <p>Crystals grow <b>only on 晶巢岩</b> ({@code aquanaut:crystal_nest_stone}): the anchor
 * face must be a sturdy face of a block in {@link com.dexer.aquanaut.core.TagRegistry#CRYSTAL_GROWTH_SUPPORT},
 * anywhere — worldgen roots them the same way, so a cluster whose nest stone is taken away
 * pops off like a torch.</p>
 */
public final class CrystalClusterBlock extends Block implements SimpleWaterloggedBlock {
    public static final MapCodec<CrystalClusterBlock> CODEC = simpleCodec(CrystalClusterBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final VoxelShape[] SHAPE_BY_FACING = new VoxelShape[6];

    static {
        SHAPE_BY_FACING[Direction.DOWN.get3DDataValue()] = Block.box(2.0D, 9.0D, 2.0D, 14.0D, 16.0D, 14.0D);
        SHAPE_BY_FACING[Direction.UP.get3DDataValue()] = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 7.0D, 14.0D);
        SHAPE_BY_FACING[Direction.NORTH.get3DDataValue()] = Block.box(2.0D, 2.0D, 9.0D, 14.0D, 14.0D, 16.0D);
        SHAPE_BY_FACING[Direction.SOUTH.get3DDataValue()] = Block.box(2.0D, 2.0D, 0.0D, 14.0D, 14.0D, 7.0D);
        SHAPE_BY_FACING[Direction.WEST.get3DDataValue()] = Block.box(9.0D, 2.0D, 2.0D, 16.0D, 14.0D, 14.0D);
        SHAPE_BY_FACING[Direction.EAST.get3DDataValue()] = Block.box(0.0D, 2.0D, 2.0D, 7.0D, 14.0D, 14.0D);
    }

    public CrystalClusterBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.UP)
                .setValue(BlockStateProperties.WATERLOGGED, false));
    }

    @Override
    protected MapCodec<CrystalClusterBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState()
                .setValue(FACING, context.getClickedFace())
                .setValue(BlockStateProperties.WATERLOGGED, false);
        if (!state.canSurvive(context.getLevel(), context.getClickedPos())) {
            return null;
        }
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return state.setValue(BlockStateProperties.WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction growth = state.getValue(FACING);
        BlockPos support = pos.relative(growth.getOpposite());
        BlockState supportState = level.getBlockState(support);
        return supportState.is(TagRegistry.CRYSTAL_GROWTH_SUPPORT)
                && supportState.isFaceSturdy(level, support, growth);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BlockStateProperties.WATERLOGGED);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_BY_FACING[state.getValue(FACING).get3DDataValue()];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return Shapes.empty();
    }
}
