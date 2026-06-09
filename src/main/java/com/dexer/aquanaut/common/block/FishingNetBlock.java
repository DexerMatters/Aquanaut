package com.dexer.aquanaut.common.block;

import com.dexer.aquanaut.common.inventory.aquarium.AquariumEntitySnapshot;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumInventoryHelper;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.WaterAnimal;
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
import net.minecraft.world.phys.AABB;
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

public final class FishingNetBlock extends AbstractPipeBlock {

    public static final MapCodec<FishingNetBlock> CODEC = simpleCodec(FishingNetBlock::new);

    private static final Map<Direction, VoxelShape> PANELS = new EnumMap<>(Direction.class);
    static {
        PANELS.put(Direction.NORTH, box(0, 0, 0, 16, 16, 1));
        PANELS.put(Direction.SOUTH, box(0, 0, 15, 16, 16, 16));
        PANELS.put(Direction.EAST, box(15, 0, 0, 16, 16, 16));
        PANELS.put(Direction.WEST, box(0, 0, 0, 1, 16, 16));
        PANELS.put(Direction.UP, box(0, 15, 0, 16, 16, 16));
        PANELS.put(Direction.DOWN, box(0, 0, 0, 16, 1, 16));
    }

    public FishingNetBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<FishingNetBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        BlockState state = defaultBlockState()
                .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
        return computeRaw(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = Shapes.empty();
        for (Direction dir : Direction.values()) {
            if (!state.getValue(property(dir))) {
                shape = Shapes.or(shape, PANELS.get(dir));
            }
        }
        return shape;
    }

    @Override
    protected boolean canConnectTo(BlockState state, BlockState neighborState, Direction direction) {
        return neighborState.getBlock() instanceof FishingNetBlock;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, 1);
        }
        return state;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !(newState.getBlock() instanceof FishingNetBlock)) {
            for (Direction dir : Direction.values()) {
                level.scheduleTick(pos.relative(dir), this, 1);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState current = level.getBlockState(pos);
        if (!(current.getBlock() instanceof FishingNetBlock)) {
            return;
        }
        refreshComponent(level, pos);
    }

    // ── Component refresh ──────────────────────────────────────────

    private void refreshComponent(ServerLevel level, BlockPos start) {
        Set<BlockPos> component = findComponent(level, start);

        // Pass 1: raw state (only adjacent-net connections)
        Map<BlockPos, BlockState> raw = new HashMap<>();
        for (BlockPos p : component) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof FishingNetBlock) {
                raw.put(p, computeRaw(s, level, p));
            }
        }

        // Pass 2: final state using raw states for BFS blocking
        List<BlockPos> changed = new ArrayList<>();
        for (BlockPos p : component) {
            BlockState old = level.getBlockState(p);
            if (!(old.getBlock() instanceof FishingNetBlock)) continue;
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
        if (level.getBlockState(start).getBlock() instanceof FishingNetBlock) {
            visited.add(start);
            queue.add(start);
        }
        while (!queue.isEmpty()) {
            BlockPos c = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos n = c.offset(dx, dy, dz);
                        if (!visited.contains(n)
                                && level.getBlockState(n).getBlock() instanceof FishingNetBlock) {
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
            r = r.setValue(property(d), isAdjacentToNet(level, pos, d));
        }
        return r;
    }

    private BlockState computeFinal(BlockState state, LevelAccessor level, BlockPos pos,
            Map<BlockPos, BlockState> raw) {
        BlockState r = state;
        for (Direction d : Direction.values()) {
            boolean connects = isAdjacentToNet(level, pos, d)
                    || !canReachOutside(level, pos.relative(d), raw);
            r = r.setValue(property(d), connects);
        }
        return r;
    }

    private static boolean isAdjacentToNet(LevelAccessor level, BlockPos pos, Direction dir) {
        return level.getBlockState(pos.relative(dir)).getBlock() instanceof FishingNetBlock;
    }

    // ── BFS for enclosure detection ────────────────────────────────

    private boolean canReachOutside(LevelAccessor level, BlockPos start,
            Map<BlockPos, BlockState> raw) {
        BlockState st = level.getBlockState(start);
        if (st.getBlock() instanceof FishingNetBlock) return false;
        if (!canPassThrough(st)) return true;

        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> q = new ArrayDeque<>();
        seen.add(start);
        q.add(start);

        while (!q.isEmpty()) {
            BlockPos c = q.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = c.relative(d);
                if (seen.contains(n)) continue;
                if (panelAt(level, c, d, raw) || panelAt(level, n, d.getOpposite(), raw)) continue;
                if (isOpen(level, n)) return true;
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
        if (!(st.getBlock() instanceof FishingNetBlock)) return false;
        return !st.getValue(property(dir));
    }

    // ── Helpers ────────────────────────────────────────────────────

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        Direction face = hit.getDirection();
        if (state.getValue(property(face))) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp && level instanceof ServerLevel sl) {
            if (collectStructure(sp, sl, pos, face)) {
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    public boolean collectStructure(ServerPlayer player, Level level, BlockPos pos, Direction face) {
        Set<BlockPos> component = findComponent(level, pos);
        Map<BlockPos, BlockState> raw = new HashMap<>();
        for (BlockPos p : component) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof FishingNetBlock) {
                raw.put(p, computeRaw(s, level, p));
            }
        }
        BlockPos interiorStart = pos.relative(face.getOpposite());
        if (level.getBlockState(interiorStart).getBlock() instanceof FishingNetBlock
                || canReachOutside(level, interiorStart, raw)) {
            return false;
        }

        Set<BlockPos> interior = collectInterior(level, interiorStart, raw);
        for (BlockPos ip : interior) {
            AABB box = new AABB(ip);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e instanceof WaterAnimal)) {
                AquariumEntitySnapshot.snapshot(e).ifPresent(snapshot -> {
                    AquariumInventoryHelper.addFishEntry(player, snapshot);
                });
                e.discard();
            }
        }

        int count = 0;
        for (BlockPos p : component) {
            if (level.getBlockState(p).getBlock() instanceof FishingNetBlock) {
                level.destroyBlock(p, false);
                count++;
            }
        }
        if (count > 0) {
            ItemStack drop = new ItemStack(this, count);
            if (!player.getInventory().add(drop)) {
                level.addFreshEntity(new ItemEntity(level, player.getX(),
                        player.getY() + 0.5, player.getZ(), drop));
            }
            level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_LEATHER.value(),
                    SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        return true;
    }

    private Set<BlockPos> collectInterior(LevelAccessor level, BlockPos start,
            Map<BlockPos, BlockState> raw) {
        Set<BlockPos> interior = new HashSet<>();
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        BlockState st = level.getBlockState(start);
        if (!canPassThrough(st)) return interior;
        visited.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            BlockPos c = queue.poll();
            interior.add(c);
            for (Direction d : Direction.values()) {
                BlockPos n = c.relative(d);
                if (visited.contains(n)) continue;
                if (panelAt(level, c, d, raw) || panelAt(level, n, d.getOpposite(), raw)) continue;
                if (!canPassThrough(level.getBlockState(n))) continue;
                visited.add(n);
                queue.add(n);
            }
        }
        return interior;
    }

    private void checkAutoBreak(ServerLevel level, BlockPos pos, BlockState state) {
        for (Direction d : Direction.values()) {
            if (!state.getValue(property(d))) return;
        }
        level.destroyBlock(pos, false);
        level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5,
                pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(this)));
    }

    private static boolean isOpen(LevelAccessor level, BlockPos pos) {
        if (pos.getY() < level.getMinBuildHeight() || pos.getY() > level.getMaxBuildHeight()) return true;
        BlockState st = level.getBlockState(pos);
        if (st.getBlock() instanceof FishingNetBlock) return false;
        return !st.isAir() && st.getBlock() != Blocks.WATER && st.getFluidState().isEmpty();
    }

    private static boolean canPassThrough(BlockState state) {
        return state.isAir() || state.getBlock() == Blocks.WATER || !state.getFluidState().isEmpty();
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
}
