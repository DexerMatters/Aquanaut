package com.dexer.aquanaut.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Hydrostatic pressure utility.
 *
 * <p>
 * A position has non-zero pressure only when it is water that belongs to a body
 * connected to an open ocean surface — a water block exposed to air at or above
 * sea
 * level. Isolated water (buckets, sealed pools, disconnected underground lakes)
 * always returns 0.
 *
 * <h3>Algorithm</h3>
 * {@link #bfsFindSurface} runs in two phases:
 * <ol>
 * <li><b>Vertical fast path</b> — walks straight up through water with zero
 * heap
 * allocations. In open ocean this is all that runs: O(depth).</li>
 * <li><b>Y-descending priority BFS</b> — activated only when the vertical
 * column
 * is blocked (ceiling, overhang, cave). A max-heap by Y ensures the highest
 * reachable water blocks are explored first, so the ocean surface is found
 * after as few node visits as possible. Capped at {@value #MAX_BFS_NODES};
 * bodies exceeding the cap are conservatively treated as open ocean.</li>
 * </ol>
 *
 * <h3>Caching</h3>
 * <ul>
 * <li><b>Block cache</b> ({@value #BLOCK_CACHE_TTL} ticks) — per block
 * position,
 * short TTL so dynamic world changes (block breaks, flowing water, sponge)
 * are reflected quickly.</li>
 * <li><b>Entity cache</b> ({@value #ENTITY_CACHE_TTL} ticks) — per entity UUID,
 * longer TTL so fog and HUD code can call {@link #getPressure(Entity)} every
 * frame cheaply.</li>
 * </ul>
 * Call {@link #invalidateAround} immediately after any block change to evict
 * stale
 * entries in the affected region rather than waiting for the TTL.
 */
public final class PressureHelper {

    // ── Tunables ──────────────────────────────────────────────────────────

    /**
     * Max water nodes explored in the priority BFS before the body is declared
     * open ocean. Conservative: we prefer a flood trigger over missing one.
     */
    private static final int MAX_BFS_NODES = 2048;

    /**
     * Block cache TTL in ticks. Short so that dynamic changes (block breaks,
     * sponge, ice melt, fluid flow) are reflected within ~150 ms.
     */
    private static final int BLOCK_CACHE_TTL = 3;

    /** Entity cache TTL in ticks. Longer is fine for smooth fog rendering. */
    private static final int ENTITY_CACHE_TTL = 20;

    private static final int BLOCK_CACHE_MAX = 8192;
    private static final int ENTITY_CACHE_MAX = 256;

    // ── Caches ────────────────────────────────────────────────────────────

    private static final Map<BlockCacheKey, CacheEntry> BLOCK_CACHE = new HashMap<>();
    private static final Map<UUID, CacheEntry> ENTITY_CACHE = new HashMap<>();

    private PressureHelper() {
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Normalised [0, 1] hydrostatic pressure for a living entity.
     * Returns 0 if not submerged, or if the surrounding water is not connected
     * to the open ocean surface.
     *
     * <p>
     * Uses a per-entity cache — safe to call every rendered frame (fog, HUD).
     */
    public static float getPressure(Entity entity) {
        if (!(entity instanceof LivingEntity living) || !living.isInWater())
            return 0f;
        Level level = entity.level();
        long now = level.getGameTime();
        UUID id = entity.getUUID();
        CacheEntry ce = ENTITY_CACHE.get(id);
        if (ce != null && now - ce.timestamp <= ENTITY_CACHE_TTL) {
            return pressureFromSurface(ce.surfaceY, entity.blockPosition().getY(),
                    level.getMinBuildHeight());
        }
        int surfaceY = bfsFindSurface(level, entity.blockPosition());
        storeEntity(id, surfaceY, now);
        return pressureFromSurface(surfaceY, entity.blockPosition().getY(),
                level.getMinBuildHeight());
    }

    /**
     * Hydrostatic pressure at a specific block position.
     * Returns 0 if {@code pos} is not water, or the water body is isolated.
     *
     * <p>
     * Uses a 3-tick block-position cache so server-side calls remain cheap
     * while staying responsive to dynamic world changes.
     */
    public static float getPressure(Level level, BlockPos pos) {
        if (!level.getFluidState(pos).is(FluidTags.WATER))
            return 0f;
        int surfaceY = getConnectedSurfaceY(level, pos);
        return pressureFromSurface(surfaceY, pos.getY(), level.getMinBuildHeight());
    }

    /**
     * Depth-only pressure with no connectivity check.
     * Use when connectivity is already established (e.g., inside an active flood
     * task) or when only an approximate value is needed without a block position.
     */
    public static float getPressure(Level level, double y) {
        float seaLevel = level.getSeaLevel();
        float depthRange = seaLevel - level.getMinBuildHeight();
        if (depthRange <= 0f)
            return 0f;
        return Mth.clamp((seaLevel - (float) y) / depthRange, 0f, 1f);
    }

    /**
     * Returns true when the water at {@code waterPos} has a continuous path to
     * the open ocean surface. Isolated water bodies always return false.
     */
    public static boolean isConnectedToSurface(Level level, BlockPos waterPos) {
        return getConnectedSurfaceY(level, waterPos) != Integer.MIN_VALUE;
    }

    /**
     * Immediately evicts all block-cache entries within {@code radius} blocks of
     * {@code pos} in the same dimension. Call this from any block-change event
     * handler (block break, block place, sponge, fluid placement) so the next
     * connectivity query runs a fresh BFS instead of returning a stale cached
     * result.
     */
    public static void invalidateAround(Level level, BlockPos pos, int radius) {
        ResourceKey<Level> dim = level.dimension();
        int rSq = radius * radius;
        BLOCK_CACHE.keySet().removeIf(k -> k.dimension().equals(dim) && squareDist(k.x(), k.y(), k.z(), pos) <= rSq);
    }

    // ── Connectivity (cached) ──────────────────────────────────────────────

    /**
     * Returns the Y coordinate of the open ocean surface reachable from
     * {@code pos} through connected water, or {@link Integer#MIN_VALUE} if
     * the body is isolated (fully explored without finding an ocean-level surface).
     */
    public static int getConnectedSurfaceY(Level level, BlockPos pos) {
        if (!level.getFluidState(pos).is(FluidTags.WATER))
            return Integer.MIN_VALUE;
        long now = level.getGameTime();
        BlockCacheKey key = new BlockCacheKey(level.dimension(), pos.getX(), pos.getY(), pos.getZ());
        CacheEntry ce = BLOCK_CACHE.get(key);
        if (ce != null && now - ce.timestamp <= BLOCK_CACHE_TTL)
            return ce.surfaceY;
        int surfaceY = bfsFindSurface(level, pos);
        storeBlock(key, surfaceY, now);
        return surfaceY;
    }

    // ── BFS ───────────────────────────────────────────────────────────────

    /**
     * Finds the Y of the highest open-air water surface reachable from
     * {@code start} through connected water blocks.
     *
     * <h3>Phase 1 — vertical fast path</h3>
     * Walks straight up through water. In open ocean: hits air at sea level
     * immediately and returns — O(depth), zero heap allocations.
     *
     * <h3>Phase 2 — Y-descending priority BFS</h3>
     * Activated when the vertical column is obstructed. A {@link PriorityQueue}
     * ordered by Y descending ensures the highest reachable water blocks are
     * dequeued first, so the ocean surface (at or above sea level with open air
     * above it) is found after visiting as few nodes as possible.
     *
     * <p>
     * The surface condition is simply: water block whose block-above is air
     * AND that air is at or above sea level. No {@code seaLevel±2} fudge —
     * the ocean surface in any dimension is discovered naturally.
     *
     * <p>
     * If {@link #MAX_BFS_NODES} is exceeded the body is conservatively treated
     * as open ocean (returns {@code seaLevel}).
     *
     * @return Y of the air block directly above the surface water (≥ seaLevel),
     *         or {@link Integer#MIN_VALUE} if the water body is isolated.
     */
    private static int bfsFindSurface(Level level, BlockPos start) {
        int seaLevel = level.getSeaLevel();
        int maxHeight = level.getMaxBuildHeight() - 1;

        // ── Phase 1: vertical fast path ────────────────────────────────────
        BlockPos.MutableBlockPos cur = new BlockPos.MutableBlockPos(start.getX(), start.getY(), start.getZ());
        while (cur.getY() < maxHeight) {
            cur.move(Direction.UP);
            if (!level.getFluidState(cur).is(FluidTags.WATER)) {
                // Exited water — ocean surface if open air at or above sea level.
                if (level.getBlockState(cur).isAir() && cur.getY() >= seaLevel) {
                    return cur.getY();
                }
                // Blocked by solid, or air below sea level (underground). Fall to BFS.
                break;
            }
        }

        // ── Phase 2: Y-descending priority BFS ─────────────────────────────
        // Highest-Y blocks first so we reach the ocean surface with fewest visits.
        PriorityQueue<BlockPos> queue = new PriorityQueue<>((a, b) -> Integer.compare(b.getY(), a.getY()));
        Set<BlockPos> visited = new HashSet<>();
        BlockPos seed = start.immutable();
        queue.add(seed);
        visited.add(seed);
        int explored = 0;

        while (!queue.isEmpty()) {
            if (++explored > MAX_BFS_NODES) {
                // Body is large — conservatively declare it open ocean.
                return seaLevel;
            }

            BlockPos pos = queue.poll();

            // Surface check: air directly above this water block, at or above sea level.
            // No ±2 fudge — the surface is wherever open air meets water at ocean level.
            BlockPos above = pos.above();
            if (level.getBlockState(above).isAir()) {
                if (above.getY() >= seaLevel) {
                    // Found ocean-level surface — early exit.
                    return above.getY();
                }
                // Air is below sea level (underground lake surface). Keep searching
                // upward; there may still be a higher ocean-connected path.
            }

            // Expand all water neighbours.
            for (Direction dir : Direction.values()) {
                BlockPos nb = pos.relative(dir);
                if (visited.add(nb) && level.getFluidState(nb).is(FluidTags.WATER)) {
                    queue.add(nb);
                }
            }
        }

        // BFS exhausted — no ocean-level surface reachable. Isolated body.
        return Integer.MIN_VALUE;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static float pressureFromSurface(int surfaceY, int blockY, int minBuildHeight) {
        if (surfaceY == Integer.MIN_VALUE)
            return 0f;
        int depth = surfaceY - blockY;
        if (depth <= 0)
            return 0f;
        int maxDepth = surfaceY - minBuildHeight;
        return maxDepth > 0 ? Mth.clamp((float) depth / maxDepth, 0f, 1f) : 0f;
    }

    private static int squareDist(int x, int y, int z, BlockPos ref) {
        int dx = x - ref.getX(), dy = y - ref.getY(), dz = z - ref.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private static void storeBlock(BlockCacheKey key, int surfaceY, long now) {
        BLOCK_CACHE.put(key, new CacheEntry(surfaceY, now));
        if (BLOCK_CACHE.size() > BLOCK_CACHE_MAX) {
            BLOCK_CACHE.entrySet().removeIf(e -> now - e.getValue().timestamp > BLOCK_CACHE_TTL * 4L);
        }
    }

    private static void storeEntity(UUID id, int surfaceY, long now) {
        ENTITY_CACHE.put(id, new CacheEntry(surfaceY, now));
        if (ENTITY_CACHE.size() > ENTITY_CACHE_MAX) {
            ENTITY_CACHE.entrySet().removeIf(e -> now - e.getValue().timestamp > ENTITY_CACHE_TTL * 4L);
        }
    }

    // ── Types ─────────────────────────────────────────────────────────────

    private record BlockCacheKey(ResourceKey<Level> dimension, int x, int y, int z) {
    }

    private static final class CacheEntry {
        final int surfaceY;
        final long timestamp;

        CacheEntry(int surfaceY, long timestamp) {
            this.surfaceY = surfaceY;
            this.timestamp = timestamp;
        }
    }
}
