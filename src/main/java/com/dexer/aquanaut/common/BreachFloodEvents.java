package com.dexer.aquanaut.common;

import com.dexer.aquanaut.Aquanaut;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Detects underwater breaches and gradually floods the connected air space.
 *
 * <h3>Trigger sources</h3>
 * <ul>
 * <li>{@link BlockEvent.BreakEvent} — player breaks a wall block adjacent to
 * water.</li>
 * <li>{@link BlockEvent.EntityPlaceEvent} — sponge placed; water may drain and
 * expose air.</li>
 * <li>{@link BlockEvent.FluidPlaceBlockEvent} — flowing water fills an air cell
 * at the boundary.</li>
 * <li>{@link BlockEvent.NeighborNotifyEvent} — catch-all for every other
 * block-update source:
 * piston push/retract, TNT, falling blocks, command blocks, modded machinery,
 * etc.
 * Filtered to positions adjacent to water so it does not react to leaf decay or
 * crop growth.</li>
 * </ul>
 *
 * <p>
 * All handlers defer detection to the next server tick via
 * {@link #DEFERRED_SCANS} so
 * vanilla block-update propagation and fluid scheduling can complete first.
 */
@EventBusSubscriber(modid = Aquanaut.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class BreachFloodEvents {
    private static final int MAX_VOLUME = 2000;
    private static final int MIN_FILL_PER_TICK = 1;
    private static final int MAX_FILL_PER_TICK = 4;
    private static final int DEFAULT_SCAN_RADIUS = 2;
    private static final int SPONGE_SCAN_RADIUS = 7;
    private static final int MAX_SCAN_ATTEMPTS = 20;
    private static final int MIN_DEPTH = 10;
    private static final double SUCTION_RADIUS = 6.0D;
    private static final double SUCTION_STRENGTH = 0.06D;
    private static final Direction[] DIRECTIONS = Direction.values();

    private static final Map<DeferredScanKey, DeferredScan> DEFERRED_SCANS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Deque<FloodTask>> ACTIVE_TASKS = new HashMap<>();
    private static final Set<FloodKey> ACTIVE_KEYS = new HashSet<>();
    /**
     * Per-dimension set of block positions currently owned by an active flood task
     * (entry points + frontier positions added as the BFS expands). Any position in
     * this set is excluded from new flood-task entry-point searches, preventing
     * duplicate tasks from being spawned by {@link BlockEvent.FluidPlaceBlockEvent}
     * when vanilla water spreads from source blocks that the flood task itself
     * placed.
     */
    private static final Map<ResourceKey<Level>, Set<BlockPos>> ACTIVE_FLOOD_ZONES = new HashMap<>();

    private BreachFloodEvents() {
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (hasAdjacentWater(level, event.getPos())) {
            // Evict cached connectivity for nearby blocks immediately — the break
            // may open a new path between previously-isolated water and the ocean,
            // or vice versa. Without this, callers would see the pre-break result
            // for up to BLOCK_CACHE_TTL ticks.
            PressureHelper.invalidateAround(level, event.getPos(), DEFAULT_SCAN_RADIUS + 2);
            queueDeferredScan(level, event.getPos(), DEFAULT_SCAN_RADIUS);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockState placedBlock = event.getPlacedBlock();
        if (placedBlock.is(Blocks.SPONGE) || placedBlock.is(Blocks.WET_SPONGE)) {
            // Sponge will absorb water in the next ticks — nearby water blocks that
            // were ocean-connected will become isolated. Evict their cache entries
            // so the next connectivity query re-runs the BFS on the updated world.
            PressureHelper.invalidateAround(level, event.getPos(), SPONGE_SCAN_RADIUS + 2);
            queueDeferredScan(level, event.getPos(), SPONGE_SCAN_RADIUS);
        }
    }

    @SubscribeEvent
    public static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level))
            return;
        if (!event.getOriginalState().isAir()
                || !event.getNewState().getFluidState().is(FluidTags.WATER))
            return;
        // Flowing water just filled an air cell — connectivity around here changed.
        PressureHelper.invalidateAround(level, event.getPos(), DEFAULT_SCAN_RADIUS + 2);
        queueDeferredScan(level, event.getPos(), DEFAULT_SCAN_RADIUS);
    }

    /**
     * Catch-all for every block-state change not covered by the specific handlers
     * above.
     * Fires for piston-moved blocks, TNT debris, falling blocks, structure
     * placements,
     * command-block changes, and any modded block updates.
     *
     * <p>
     * We filter aggressively: only proceed when the changed position is water or
     * directly
     * adjacent to water. This excludes the vast majority of unrelated block updates
     * (leaf
     * decay, crop growth, redstone dust, etc.) at essentially zero cost.
     *
     * <p>
     * Detection is deferred to the next tick so the full chain of piston-pushed
     * block
     * updates and fluid-flow rescheduling can complete before we scan for entry
     * points.
     */
    /**
     * Catch-all for piston push/retract, TNT, falling blocks, command fills, and
     * any other block-state change not covered by the specific handlers above.
     *
     * <p>
     * We only react when the block at {@code pos} is now <b>AIR</b> and is
     * directly adjacent to water. This intentionally skips positions where a block
     * was <em>placed</em> (e.g. water blocks placed by our own flood task), which
     * would otherwise cause a feedback loop that spawns new flood tasks for every
     * block the active task fills — the root cause of uncontrolled upward flooding.
     */
    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level))
            return;
        BlockPos pos = event.getPos();
        // CRITICAL: only react to newly-exposed AIR. Skipping water/solid prevents
        // the feedback loop where our own setBlock() calls spawn more flood tasks.
        if (!level.getBlockState(pos).isAir())
            return;
        if (!hasAdjacentWater(level, pos))
            return;
        PressureHelper.invalidateAround(level, pos, DEFAULT_SCAN_RADIUS + 2);
        queueDeferredScan(level, pos, DEFAULT_SCAN_RADIUS);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        processDeferredScans(server);
        processFloodTasks(server);
    }

    private static void queueDeferredScan(ServerLevel level, BlockPos pos, int radius) {
        DeferredScanKey key = new DeferredScanKey(level.dimension(), new BlockPos(pos));
        DeferredScan scan = DEFERRED_SCANS.get(key);
        if (scan == null) {
            DEFERRED_SCANS.put(key, new DeferredScan(radius));
            return;
        }

        scan.radius = Math.max(scan.radius, radius);
    }

    private static void processDeferredScans(MinecraftServer server) {
        if (DEFERRED_SCANS.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<DeferredScanKey, DeferredScan>> iterator = DEFERRED_SCANS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<DeferredScanKey, DeferredScan> entry = iterator.next();
            ServerLevel level = server.getLevel(entry.getKey().dimension());
            if (level == null) {
                iterator.remove();
                continue;
            }

            DeferredScan scan = entry.getValue();
            List<BlockPos> entryPoints = findEntryPoints(level, entry.getKey().pos(), scan.radius);
            if (!entryPoints.isEmpty()) {
                startFloodTask(level, entry.getKey().pos(), entryPoints);
                iterator.remove();
                continue;
            }

            scan.attempts++;
            if (scan.attempts >= MAX_SCAN_ATTEMPTS) {
                iterator.remove();
            }
        }
    }

    private static void startFloodTask(ServerLevel level, BlockPos breachPos, List<BlockPos> entryPoints) {
        if (entryPoints.isEmpty())
            return;

        BlockPos keyPos = new BlockPos(breachPos);
        FloodKey key = new FloodKey(level.dimension(), keyPos);
        if (!ACTIVE_KEYS.add(key))
            return;

        int waterlineY = computeWaterlineY(level, entryPoints);
        if (waterlineY - breachPos.getY() < MIN_DEPTH) {
            ACTIVE_KEYS.remove(key);
            return;
        }
        float pressure = PressureHelper.getPressure(level, (double) breachPos.getY());
        FloodTask task = new FloodTask(level, keyPos, entryPoints, pressure, key, waterlineY);
        if (task.isEmpty()) {
            ACTIVE_KEYS.remove(key);
            return;
        }
        ACTIVE_TASKS.computeIfAbsent(level.dimension(), d -> new ArrayDeque<>()).add(task);
    }

    private static void processFloodTasks(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            Deque<FloodTask> tasks = ACTIVE_TASKS.get(level.dimension());
            if (tasks == null || tasks.isEmpty()) {
                continue;
            }

            Iterator<FloodTask> iterator = tasks.iterator();
            while (iterator.hasNext()) {
                FloodTask task = iterator.next();
                if (task.tick()) {
                    iterator.remove();
                }
            }

            if (tasks.isEmpty()) {
                ACTIVE_TASKS.remove(level.dimension());
            }
        }
    }

    private static List<BlockPos> findEntryPoints(ServerLevel level, BlockPos center, int radius) {
        List<BlockPos> entryPoints = new ArrayList<>();
        Set<BlockPos> zone = ACTIVE_FLOOD_ZONES.get(level.dimension());
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int y = center.getY() - radius; y <= center.getY() + radius; y++) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                    mutablePos.set(x, y, z);
                    if (!level.getBlockState(mutablePos).isAir())
                        continue;
                    // Skip positions already owned by a running flood task; they are
                    // at the frontier of that task and will be filled in due course.
                    if (zone != null && zone.contains(mutablePos))
                        continue;
                    if (hasAdjacentOpenOcean(level, mutablePos)) {
                        entryPoints.add(new BlockPos(mutablePos));
                    }
                }
            }
        }

        return entryPoints;
    }

    /**
     * Returns true if any of the six direct neighbours of {@code pos} is water
     * that is connected to the open ocean surface (verified via
     * {@link PressureHelper#isConnectedToSurface}).
     */
    private static boolean hasAdjacentOpenOcean(ServerLevel level, BlockPos pos) {
        for (Direction dir : DIRECTIONS) {
            BlockPos nb = pos.relative(dir);
            if (level.getFluidState(nb).is(FluidTags.WATER)
                    && PressureHelper.isConnectedToSurface(level, nb)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves the waterline Y from the ocean-connected water adjacent to one of
     * the provided entry points. This is the Y at which water meets open air in the
     * ocean column — the flood must never fill any block at or above this level.
     * Falls back to {@code level.getSeaLevel()} when no adjacent connected water is
     * found (should never happen in normal circumstances).
     */
    private static int computeWaterlineY(ServerLevel level, List<BlockPos> entryPoints) {
        for (BlockPos ep : entryPoints) {
            for (Direction dir : DIRECTIONS) {
                BlockPos nb = ep.relative(dir);
                if (level.getFluidState(nb).is(FluidTags.WATER)) {
                    int sy = PressureHelper.getConnectedSurfaceY(level, nb);
                    if (sy != Integer.MIN_VALUE)
                        return sy;
                }
            }
        }
        return level.getSeaLevel();
    }

    private static boolean hasAdjacentWater(ServerLevel level, BlockPos pos) {
        for (Direction dir : DIRECTIONS) {
            if (level.getBlockState(pos.relative(dir)).getFluidState().is(FluidTags.WATER))
                return true;
        }
        return false;
    }

    private record DeferredScanKey(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private static final class DeferredScan {
        private int radius;
        private int attempts;

        private DeferredScan(int radius) {
            this.radius = radius;
        }
    }

    private record FloodKey(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private static final class FloodTask {
        private final ServerLevel level;
        private final BlockPos breachPos;
        private final FloodKey key;
        private final int batchSize;
        /**
         * All reachable air blocks in the cavity, sorted by Y ascending so water
         * fills from the floor upward — physically correct and concise. Computed
         * once at construction via a single bounded BFS; each tick just advances an
         * index into this list.
         */
        private final List<BlockPos> fillPlan;
        private int fillIndex;

        FloodTask(ServerLevel level, BlockPos breachPos, List<BlockPos> entryPoints,
                float pressure, FloodKey key, int waterlineY) {
            this.level = level;
            this.breachPos = breachPos;
            this.key = key;
            this.batchSize = Mth.clamp(
                    Math.round(Mth.lerp(pressure, MIN_FILL_PER_TICK, MAX_FILL_PER_TICK)),
                    MIN_FILL_PER_TICK, MAX_FILL_PER_TICK);

            // ── One-shot cavity BFS ───────────────────────────────────────────
            // Explores the full connected air volume reachable from the entry
            // points, bounded by waterlineY and MAX_VOLUME. Blocks already owned
            // by another active flood task are not crossed, so two simultaneous
            // breaches in the same room each fill only their own partition.
            Set<BlockPos> zone = ACTIVE_FLOOD_ZONES.computeIfAbsent(
                    level.dimension(), k -> new HashSet<>());
            Set<BlockPos> cavity = new HashSet<>();
            Deque<BlockPos> queue = new ArrayDeque<>();

            for (BlockPos ep : entryPoints) {
                if (ep.getY() < waterlineY
                        && level.getBlockState(ep).isAir()
                        && !zone.contains(ep)
                        && cavity.add(ep)) {
                    queue.add(ep);
                }
            }
            while (!queue.isEmpty() && cavity.size() < MAX_VOLUME) {
                BlockPos pos = queue.poll();
                for (Direction dir : DIRECTIONS) {
                    BlockPos nb = pos.relative(dir);
                    if (nb.getY() >= waterlineY)
                        continue;
                    if (zone.contains(nb))
                        continue; // another task owns this cell
                    if (!cavity.add(nb))
                        continue; // already in our cavity
                    if (level.getBlockState(nb).isAir())
                        queue.add(nb);
                }
            }

            // Claim the whole discovered cavity atomically.
            zone.addAll(cavity);

            // Sort by (Y ascending, horizontal distance from breach ascending).
            // This makes water fall to the floor directly below the breach first,
            // then ripple outward along the floor, then rise at the hole and
            // ripple outward again — matching real flooding behaviour.
            int bx = breachPos.getX(), bz = breachPos.getZ();
            List<BlockPos> plan = new ArrayList<>(cavity);
            plan.sort((a, b) -> {
                int dy = Integer.compare(a.getY(), b.getY());
                if (dy != 0)
                    return dy;
                int da = (a.getX() - bx) * (a.getX() - bx) + (a.getZ() - bz) * (a.getZ() - bz);
                int db = (b.getX() - bx) * (b.getX() - bx) + (b.getZ() - bz) * (b.getZ() - bz);
                return Integer.compare(da, db);
            });
            this.fillPlan = plan;
        }

        boolean isEmpty() {
            return fillPlan.isEmpty();
        }

        /**
         * Advances the fill plan by up to {@link #batchSize} positions per tick.
         * Positions that are no longer air (world changed since the scan) are
         * skipped silently — the index still advances so the task terminates.
         *
         * @return {@code true} when the fill is complete.
         */
        boolean tick() {
            if (fillIndex >= fillPlan.size()) {
                release();
                return true;
            }

            int filled = 0;
            while (fillIndex < fillPlan.size() && filled < batchSize) {
                BlockPos pos = fillPlan.get(fillIndex++);
                if (!level.getBlockState(pos).isAir())
                    continue;
                // Flag 2: client update only — no server neighbour-update chain and no
                // vanilla fluid tick scheduling, so no re-entry into the breach detector.
                level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
                filled++;
            }

            if (filled > 0) {
                level.sendParticles(ParticleTypes.BUBBLE,
                        breachPos.getX() + 0.5D, breachPos.getY() + 0.5D, breachPos.getZ() + 0.5D,
                        8 + filled, 0.45D, 0.25D, 0.45D, 0.015D);
                level.sendParticles(ParticleTypes.SPLASH,
                        breachPos.getX() + 0.5D, breachPos.getY() + 0.5D, breachPos.getZ() + 0.5D,
                        4 + filled / 2, 0.4D, 0.2D, 0.4D, 0.03D);
                applySuction();
            }

            if (fillIndex >= fillPlan.size()) {
                release();
                return true;
            }
            return false;
        }

        private void release() {
            ACTIVE_KEYS.remove(key);
            Set<BlockPos> zone = ACTIVE_FLOOD_ZONES.get(level.dimension());
            if (zone != null) {
                fillPlan.forEach(zone::remove);
                if (zone.isEmpty())
                    ACTIVE_FLOOD_ZONES.remove(level.dimension());
            }
        }

        private void applySuction() {
            Vec3 holeCenter = Vec3.atCenterOf(breachPos);
            AABB suctionBox = new AABB(breachPos).inflate(SUCTION_RADIUS, SUCTION_RADIUS, SUCTION_RADIUS);
            for (Player player : level.getEntitiesOfClass(Player.class, suctionBox,
                    p -> p.isAlive() && !p.isSpectator())) {
                Vec3 pullVector = holeCenter.subtract(player.getEyePosition());
                double distSqr = pullVector.lengthSqr();
                if (distSqr < 1.0E-6D || distSqr > SUCTION_RADIUS * SUCTION_RADIUS)
                    continue;
                double dist = Math.sqrt(distSqr);
                double pressure = PressureHelper.getPressure(level, (double) breachPos.getY());
                double strength = Mth.clamp(
                        pressure * SUCTION_STRENGTH * (1.0D - dist / SUCTION_RADIUS),
                        0.0D, SUCTION_STRENGTH);
                if (strength <= 0.0D)
                    continue;
                Vec3 pull = pullVector.normalize().scale(strength);
                player.push(pull.x, pull.y, pull.z);
            }
        }
    }
}