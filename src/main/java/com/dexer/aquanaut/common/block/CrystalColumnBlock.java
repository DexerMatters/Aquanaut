package com.dexer.aquanaut.common.block;

import com.dexer.aquanaut.core.TagRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * A crystal column spire (晶柱) — the hanging shards of the Crystal Nest. Crystals grow
 * <b>only on 晶巢岩</b>: a spire hangs from the nest stone ceiling either directly or
 * through the spire chain above it ({@code aquanaut:crystal_nest_stone} via
 * {@link TagRegistry#CRYSTAL_GROWTH_SUPPORT}), and when the stone above goes, the whole
 * chain pops downward link by link. Horizontally laid pillars must touch nest stone (or
 * fellow column) at one end.
 */
public final class CrystalColumnBlock extends RotatedPillarBlock {
    public static final MapCodec<CrystalColumnBlock> CODEC = simpleCodec(CrystalColumnBlock::new);

    public CrystalColumnBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<CrystalColumnBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null || !state.canSurvive(context.getLevel(), context.getClickedPos())) {
            return null;
        }
        return state;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.canSurvive(level, pos) ? state : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(BlockStateProperties.AXIS) == Direction.Axis.Y) {
            // Spire chains hang: the link above must be nest stone or another spire, so a
            // broken anchor cascades down the whole chain instead of leaving it floating.
            return anchored(level.getBlockState(pos.above()));
        }
        Direction.Axis axis = state.getValue(BlockStateProperties.AXIS);
        Direction positive = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
        return anchored(level.getBlockState(pos.relative(positive)))
                || anchored(level.getBlockState(pos.relative(positive.getOpposite())));
    }

    private boolean anchored(BlockState state) {
        return state.is(TagRegistry.CRYSTAL_GROWTH_SUPPORT) || state.getBlock() instanceof CrystalColumnBlock;
    }
}
