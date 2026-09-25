package com.dexer.aquanaut.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Thin waterlogged mineral flora (calcite quills, halite rosettes).
 *
 * <p>When a {@code supportBelow} tag is given the plant may only root on that rock — this
 * is how 晶芽 obeys the "crystals grow only on 晶巢岩" rule while the other mineral flora
 * keep their own substrates. Without a tag the plant is plain waterlogged vegetation.</p>
 */
public final class CrystalPlantBlock extends Block implements SimpleWaterloggedBlock {
    public static final MapCodec<CrystalPlantBlock> CODEC = simpleCodec(props -> new CrystalPlantBlock(props));
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0D),
            Block.box(4.0D, 2.0D, 4.0D, 12.0D, 14.0D, 12.0D));

    @Nullable
    private final TagKey<Block> supportBelow;

    public CrystalPlantBlock(BlockBehaviour.Properties properties) {
        this(properties, null);
    }

    public CrystalPlantBlock(BlockBehaviour.Properties properties, @Nullable TagKey<Block> supportBelow) {
        super(properties);
        this.supportBelow = supportBelow;
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false));
    }

    @Override
    protected MapCodec<CrystalPlantBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        if (!fluidState.is(Fluids.WATER)) {
            return null;
        }
        BlockState state = defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return state.canSurvive(level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!state.getValue(BlockStateProperties.WATERLOGGED)) {
            return false;
        }
        return supportBelow == null || level.getBlockState(pos.below()).is(supportBelow);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return Shapes.empty();
    }
}
