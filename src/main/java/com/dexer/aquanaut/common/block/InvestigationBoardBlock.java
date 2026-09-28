package com.dexer.aquanaut.common.block;

import com.dexer.aquanaut.common.block.InvestigationBoardPlacement.Part;
import com.dexer.aquanaut.common.block.entity.InvestigationBoardBlockEntity;
import com.dexer.aquanaut.common.investigation.InvestigationBoardApi;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

/**
 * A wall-mounted investigation board.
 *
 * <p>
 * The board is a 2x2 run of cells on a wall, and it refuses to be placed anywhere else: every one of
 * its cells needs a wall with a sturdy face behind it and empty space in front of it, so the board
 * can never end up floating, half buried in a block, or hanging off a fence post. Each cell carries
 * the block so the whole board collides, and only the clicked cell (its
 * {@link Part#BOTTOM_LEFT bottom left}) draws the model.
 *
 * <p>
 * The geometry itself comes from the GeckoLib block-entity renderer, like the dissection table; the
 * block model is an empty shell that only supplies particles and the outline.
 */
public class InvestigationBoardBlock extends BaseEntityBlock {
    public static final MapCodec<InvestigationBoardBlock> CODEC = simpleCodec(InvestigationBoardBlock::new);

    /** The direction the board's front looks: away from the wall it hangs on. */
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /** Which of the board's four cells this block is. */
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    /** The board's thickness and the air in front of the wall, in sixteenths of a block. */
    private static final int THICKNESS = 2;
    private static final int WALL_GAP = 1;

    public InvestigationBoardBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, Part.BOTTOM_LEFT));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            InvestigationBoardApi.open(serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /**
     * Final because {@code Block}'s own constructor calls this while the block is still being built;
     * an overridable override would let a subclass run against a half-initialised block. This class
     * is a leaf, so nothing loses the ability to add properties.
     */
    @Override
    protected final void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    // -- placement -------------------------------------------------------

    /**
     * Whether the board fits: every cell needs empty space in front of it and a sturdy wall face
     * behind it. Called before the block exists, so it is the check a player actually feels.
     */
    public static boolean fits(LevelReader level, BlockPos anchor, Direction facing) {
        Direction wall = InvestigationBoardPlacement.wall(facing);
        for (Part part : Part.values()) {
            BlockPos cell = InvestigationBoardPlacement.cell(part, facing, anchor);
            if (!level.getBlockState(cell).canBeReplaced()) {
                return false;
            }
            BlockPos support = cell.relative(wall);
            if (!level.getBlockState(support).isFaceSturdy(level, support, facing)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos anchor = context.getClickedPos();
        if (!fits(context.getLevel(), anchor, facing)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing).setValue(PART, Part.BOTTOM_LEFT);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) {
            return;
        }
        Direction facing = state.getValue(FACING);
        for (Part part : Part.values()) {
            if (part.isAnchor()) {
                continue;
            }
            BlockPos cell = InvestigationBoardPlacement.cell(part, facing, pos);
            level.setBlock(cell, state.setValue(PART, part), Block.UPDATE_ALL);
        }
    }

    // -- survival --------------------------------------------------------

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        Direction wall = InvestigationBoardPlacement.wall(facing);
        BlockPos support = pos.relative(wall);
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState,
            LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        // Only the wall keeps the board up; a board that loses it comes down whole, because taking
        // one cell away runs onRemove, which clears the other three.
        return direction == InvestigationBoardPlacement.wall(state.getValue(FACING)) && !this.canSurvive(state, level, pos)
                ? Blocks.AIR.defaultBlockState()
                : state;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !movedByPiston) {
            // Whichever cell goes, the rest of the board goes with it, silently: the cell being
            // removed has already dropped the board, so its companions must not drop again.
            Direction facing = state.getValue(FACING);
            BlockPos anchor = InvestigationBoardPlacement.anchorFor(state.getValue(PART), facing, pos);
            for (Part part : Part.values()) {
                BlockPos cell = InvestigationBoardPlacement.cell(part, facing, anchor);
                if (!cell.equals(pos) && level.getBlockState(cell).is(this)) {
                    level.setBlock(cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    // -- rendering -------------------------------------------------------

    @Nullable
    @Override
    public InvestigationBoardBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InvestigationBoardBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // The board is drawn by the block-entity renderer; the block model only supplies particles.
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape(state.getValue(FACING), state.getValue(PART));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape(state.getValue(FACING), state.getValue(PART));
    }

    /**
     * The board's slice of this cell: a thin slab standing off the wall, a full block high on the
     * bottom row and half a block high on the top one.
     */
    private static VoxelShape shape(Direction facing, Part part) {
        int height = part.up() == 1 ? 8 : 16;
        int far = 16 - WALL_GAP;
        int near = far - THICKNESS;
        return switch (facing) {
            case NORTH -> Block.box(0, 0, near, 16, height, far);
            case SOUTH -> Block.box(0, 0, WALL_GAP, 16, height, WALL_GAP + THICKNESS);
            case EAST -> Block.box(WALL_GAP, 0, 0, WALL_GAP + THICKNESS, height, 16);
            case WEST -> Block.box(near, 0, 0, far, height, 16);
            default -> throw new IllegalArgumentException("investigation board cannot face " + facing);
        };
    }
}
