package com.dexer.aquanaut.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parent for thin-panel enclosure blocks (FishingNetBlock, PlexiglassBlock).
 * <p>
 * Each panel face is an independent 1px-thick slab. Faces on sides that connect
 * to a same-type neighbour are removed (connection visible). Faces where the
 * interior cannot reach the outside become solid — forming an enclosure.
 * <p>
 * Subclasses decide whether the block can be "collected" by overriding
 * {@link #useWithoutItem}.
 */
public abstract class AbstractPanelBlock extends AbstractPipeBlock {

    protected static final Map<Direction, VoxelShape> PANELS = new EnumMap<>(Direction.class);

    static {
        PANELS.put(Direction.NORTH, box(0, 0, 0, 16, 16, 1));
        PANELS.put(Direction.SOUTH, box(0, 0, 15, 16, 16, 16));
        PANELS.put(Direction.EAST, box(15, 0, 0, 16, 16, 16));
        PANELS.put(Direction.WEST, box(0, 0, 0, 1, 16, 16));
        PANELS.put(Direction.UP, box(0, 15, 0, 16, 16, 16));
        PANELS.put(Direction.DOWN, box(0, 0, 0, 16, 1, 16));
    }

    protected AbstractPanelBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    // ── Type guard ─────────────────────────────────────────────────

    /** Only {@code true} for blocks whose runtime class equals ours. */
    private boolean isOwnType(Block block) {
        return block.getClass() == this.getClass();
    }

    // ── Placement & shape ──────────────────────────────────────────

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        BlockState state = defaultBlockState()
                .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
        return computeRaw(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        VoxelShape shape = Shapes.empty();
        for (Direction dir : Direction.values()) {
            if (!state.getValue(property(dir))) {
                shape = Shapes.or(shape, PANELS.get(dir));
            }
        }
        return shape;
    }

    // ── Connection ─────────────────────────────────────────────────

    @Override
    protected boolean canConnectTo(BlockState state, BlockState neighborState,
            Direction direction) {
        return isOwnType(neighborState.getBlock());
    }

    // ── Tick scheduling ────────────────────────────────────────────

    @Override
    protected BlockState updateShape(BlockState state, Direction direction,
            BlockState neighborState, LevelAccessor level, BlockPos pos,
            BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, 1);
        }
        return state;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
            boolean movedByPiston) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
            boolean movedByPiston) {
        if (!level.isClientSide && !isOwnType(newState.getBlock())) {
            for (Direction dir : Direction.values()) {
                level.scheduleTick(pos.relative(dir), this, 1);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos,
            RandomSource random) {
        BlockState current = level.getBlockState(pos);
        if (!isOwnType(current.getBlock())) {
            return;
        }
        refreshComponent(level, pos);
    }

    // ── Component refresh ──────────────────────────────────────────

    private void refreshComponent(ServerLevel level, BlockPos start) {
        Set<BlockPos> component = findComponent(level, start);

        Map<BlockPos, BlockState> raw = new HashMap<>();
        for (BlockPos p : component) {
            BlockState s = level.getBlockState(p);
            if (isOwnType(s.getBlock())) {
                raw.put(p, computeRaw(s, level, p));
            }
        }

        List<BlockPos> changed = new ArrayList<>();
        for (BlockPos p : component) {
            BlockState old = level.getBlockState(p);
            if (!isOwnType(old.getBlock()))
                continue;
            BlockState cur = computeFinal(old, level, p, raw);
            if (cur != old) {
                level.setBlock(p, cur, Block.UPDATE_CLIENTS);
                changed.add(p);
            }
        }
        for (BlockPos p : changed) {
            checkAutoBreak(level, p, level.getBlockState(p));
        }
    }

    private Set<BlockPos> findComponent(LevelAccessor level, BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        if (isOwnType(level.getBlockState(start).getBlock())) {
            visited.add(start);
            queue.add(start);
        }
        while (!queue.isEmpty()) {
            BlockPos c = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0)
                            continue;
                        BlockPos n = c.offset(dx, dy, dz);
                        if (!visited.contains(n)
                                && isOwnType(level.getBlockState(n).getBlock())) {
                            visited.add(n);
                            queue.add(n);
                        }
                    }
                }
            }
        }
        return visited;
    }

    // ── Connection logic ───────────────────────────────────────────

    private BlockState computeRaw(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockState r = state;
        for (Direction d : Direction.values()) {
            r = r.setValue(property(d), isAdjacentToOwn(level, pos, d));
        }
        return r;
    }

    private BlockState computeFinal(BlockState state, LevelAccessor level, BlockPos pos,
            Map<BlockPos, BlockState> raw) {
        BlockState r = state;
        for (Direction d : Direction.values()) {
            boolean connects = isAdjacentToOwn(level, pos, d)
                    || !canReachOutside(level, pos.relative(d), raw);
            r = r.setValue(property(d), connects);
        }
        return r;
    }

    private boolean isAdjacentToOwn(LevelAccessor level, BlockPos pos, Direction dir) {
        return isOwnType(level.getBlockState(pos.relative(dir)).getBlock());
    }

    // ── Enclosure BFS ──────────────────────────────────────────────

    private boolean canReachOutside(LevelAccessor level, BlockPos start,
            Map<BlockPos, BlockState> raw) {
        BlockState st = level.getBlockState(start);
        if (isOwnType(st.getBlock()))
            return false;
        if (!canPassThrough(st))
            return true;

        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> q = new ArrayDeque<>();
        seen.add(start);
        q.add(start);

        while (!q.isEmpty()) {
            BlockPos c = q.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = c.relative(d);
                if (seen.contains(n))
                    continue;
                if (panelAt(level, c, d, raw) || panelAt(level, n, d.getOpposite(), raw))
                    continue;
                if (isOpen(level, n))
                    return true;
                if (canPassThrough(level.getBlockState(n))) {
                    seen.add(n);
                    q.add(n);
                }
            }
        }
        return false;
    }

    private boolean panelAt(LevelAccessor level, BlockPos pos, Direction dir,
            Map<BlockPos, BlockState> raw) {
        BlockState st = raw.getOrDefault(pos, level.getBlockState(pos));
        if (!isOwnType(st.getBlock()))
            return false;
        return !st.getValue(property(dir));
    }

    // ── Helpers ────────────────────────────────────────────────────

    /** Default: panels cannot be collected. Subclasses may override. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    protected void checkAutoBreak(ServerLevel level, BlockPos pos, BlockState state) {
        for (Direction d : Direction.values()) {
            if (!state.getValue(property(d)))
                return;
        }
        level.destroyBlock(pos, false);
        level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5,
                pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(this)));
    }

    private boolean isOpen(LevelAccessor level, BlockPos pos) {
        if (pos.getY() < level.getMinBuildHeight()
                || pos.getY() > level.getMaxBuildHeight())
            return true;
        BlockState st = level.getBlockState(pos);
        if (isOwnType(st.getBlock()))
            return false;
        return !st.isAir() && st.getBlock() != Blocks.WATER
                && st.getFluidState().isEmpty();
    }

    private static boolean canPassThrough(BlockState state) {
        return state.isAir() || state.getBlock() == Blocks.WATER
                || !state.getFluidState().isEmpty();
    }

    public static BooleanProperty property(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    // ── Shared component access (for subclass collection logic) ────

    protected Set<BlockPos> getComponent(LevelAccessor level, BlockPos start) {
        return findComponent(level, start);
    }

    protected Map<BlockPos, BlockState> getRawStates(LevelAccessor level,
            Set<BlockPos> component) {
        Map<BlockPos, BlockState> raw = new HashMap<>();
        for (BlockPos p : component) {
            BlockState s = level.getBlockState(p);
            if (isOwnType(s.getBlock())) {
                raw.put(p, computeRaw(s, level, p));
            }
        }
        return raw;
    }

    protected boolean canReachOutsideFrom(LevelAccessor level, BlockPos start,
            Map<BlockPos, BlockState> raw) {
        return canReachOutside(level, start, raw);
    }

    protected boolean isOwnBlock(BlockState state) {
        return isOwnType(state.getBlock());
    }

    protected Set<BlockPos> collectInterior(LevelAccessor level, BlockPos start,
            Map<BlockPos, BlockState> raw) {
        Set<BlockPos> interior = new HashSet<>();
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        BlockState st = level.getBlockState(start);
        if (!canPassThrough(st))
            return interior;
        visited.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            BlockPos c = queue.poll();
            interior.add(c);
            for (Direction d : Direction.values()) {
                BlockPos n = c.relative(d);
                if (visited.contains(n))
                    continue;
                if (panelAt(level, c, d, raw)
                        || panelAt(level, n, d.getOpposite(), raw))
                    continue;
                if (!canPassThrough(level.getBlockState(n)))
                    continue;
                visited.add(n);
                queue.add(n);
            }
        }
        return interior;
    }
}
