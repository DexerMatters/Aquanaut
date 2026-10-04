package com.dexer.aquanaut.common.worldgen.layers;

import com.dexer.aquanaut.common.worldgen.BrimstoneCalderaPlacement;
import com.dexer.aquanaut.common.worldgen.MiddleLevelOceanTerrainProfile;
import com.dexer.aquanaut.common.worldgen.OutcropGeometry;
import com.dexer.aquanaut.common.worldgen.VolcanoGeometry;
import com.dexer.aquanaut.common.worldgen.blend.CellSource;
import com.dexer.aquanaut.common.worldgen.blend.DistrictWeightField;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-chunk terrain blend fields: the production entry point of the generic district
 * mixing pipeline.
 *
 * <p>Everything the column planner needs at block resolution — district weights, the
 * geological floor, the reef composition, cap openness and the emergence-scaled volcanic
 * relief — is a pure function of world position. This class adds one thing on top: a
 * precomputed, halo-padded grid so each expensive field is evaluated <em>once per column
 * instead of once per consumer</em>. The old planner re-derived the raw floor nine times
 * per column for smoothing and re-planned every column a second time for the crystal-nest
 * skin; the grid evaluates it once per padded cell and shares the plan objects.</p>
 *
 * <p>The anti-cliff guard is a fixed symmetric-window convolution
 * ({@link com.dexer.aquanaut.common.worldgen.blend.CliffGuard}), so a guarded value only
 * depends on raw values within its radius. The raw grid extends {@link #RAW_HALO} blocks
 * beyond the chunk, which covers the guard radius plus the plan halo: shared columns come
 * out bit-identical no matter which neighbouring chunk computes them, and the analytic
 * single-column path in {@link OceanColumnPlanner#planColumnAt} agrees with the grid by
 * construction.</p>
 */
public final class ChunkTerrainBlend implements OceanColumnPlanner.PlanSource {
    /**
     * Blocks of raw-floor grid beyond the chunk border. The guard is a fixed-radius local
     * operator ({@code CliffGuard.radius()} = smoothing 2 + relaxation reach 12), and
     * guarded columns reach {@link #PLAN_HALO} beyond the chunk, so a halo of 16 keeps
     * every dependency window strictly inside the grid — the precondition for
     * bit-identical shared columns between neighbouring chunks.
     */
    public static final int RAW_HALO = 16;
    /** Blocks beyond the chunk border for which full column plans are available. */
    public static final int PLAN_HALO = 2;
    /** Side of the raw-floor grid. */
    public static final int EXTENT = 16 + RAW_HALO * 2;

    private final OceanGenSampler sampler;
    private final TerrainModule terrain;
    private final int minBuildHeight;
    private final int minX;
    private final int minZ;
    private final double[] guardedFloor;
    private final OceanColumnPlanner.ColumnPlan[][] plans;
    private final List<OceanColumnPlanner.ReefDriverRef> reefDrivers;
    private final Map<OceanLayer, DistrictWeightField> weightFields = new HashMap<>();
    private final CellSource<VolcanoGeometry.Volcano> volcanoSource =
            CellSource.<VolcanoGeometry.Volcano>memoized(VolcanoGeometry::volcanoAt);
    private final CellSource<VolcanoGeometry.Stack> stackSource =
            CellSource.<VolcanoGeometry.Stack>memoized(VolcanoGeometry::stackAt);
    private final CellSource<OutcropGeometry.Outcrop> outcropSource =
            CellSource.<OutcropGeometry.Outcrop>memoized(OutcropGeometry::outcropAt);

    private ChunkTerrainBlend(OceanGenSampler sampler, TerrainModule terrain, int minBuildHeight,
                              int minX, int minZ) {
        this.sampler = sampler;
        this.terrain = terrain;
        this.minBuildHeight = minBuildHeight;
        this.minX = minX;
        this.minZ = minZ;
        this.reefDrivers = OceanColumnPlanner.reefDrivers(sampler.stack());

        double[] raw = new double[EXTENT * EXTENT];
        for (int gx = 0; gx < EXTENT; gx++) {
            int blockX = minX - RAW_HALO + gx;
            for (int gz = 0; gz < EXTENT; gz++) {
                raw[gx * EXTENT + gz] = OceanColumnPlanner.rawFloorAt(this, blockX, minZ - RAW_HALO + gz);
            }
        }
        this.guardedFloor = terrain.blend().cliffGuard().guard(raw, EXTENT);
        int planExtent = 16 + PLAN_HALO * 2;
        this.plans = new OceanColumnPlanner.ColumnPlan[planExtent][planExtent];
    }

    public static ChunkTerrainBlend build(ChunkAccess chunk, OceanGenSampler sampler, TerrainModule terrain) {
        return new ChunkTerrainBlend(sampler, terrain, chunk.getMinBuildHeight(),
                chunk.getPos().getMinBlockX(), chunk.getPos().getMinBlockZ());
    }

    /** Grid around an arbitrary origin with a provided sampler — tests and analytics. */
    public static ChunkTerrainBlend build(OceanGenSampler sampler, TerrainModule terrain, int minBuildHeight,
                                          int minX, int minZ) {
        return new ChunkTerrainBlend(sampler, terrain, minBuildHeight, minX, minZ);
    }

    @Override
    public TerrainModule terrain() {
        return terrain;
    }

    @Override
    public int minBuildHeight() {
        return minBuildHeight;
    }

    public OceanGenSampler sampler() {
        return sampler;
    }

    public int minX() {
        return minX;
    }

    public int minZ() {
        return minZ;
    }

    @Override
    public CellSource<VolcanoGeometry.Volcano> volcanoSource() {
        return volcanoSource;
    }

    @Override
    public CellSource<VolcanoGeometry.Stack> stackSource() {
        return stackSource;
    }

    @Override
    public CellSource<OutcropGeometry.Outcrop> outcropSource() {
        return outcropSource;
    }

    /** Guarded geological floor of one chunk-local column; local range [-PLAN_HALO, 16+PLAN_HALO). */
    public double guardedFloorAt(int localX, int localZ) {
        int gx = localX + RAW_HALO;
        int gz = localZ + RAW_HALO;
        return guardedFloor[gx * EXTENT + gz];
    }

    /** The full column plan of one chunk-local column, built once and shared by all consumers. */
    public OceanColumnPlanner.ColumnPlan planAt(int localX, int localZ) {
        int px = localX + PLAN_HALO;
        int pz = localZ + PLAN_HALO;
        if (px < 0 || pz < 0 || px >= plans.length || pz >= plans.length) {
            throw new IndexOutOfBoundsException(
                    "plan halo exceeded: local (" + localX + ", " + localZ + ")");
        }
        OceanColumnPlanner.ColumnPlan plan = plans[px][pz];
        if (plan == null) {
            plan = OceanColumnPlanner.planColumn(this, minX + localX, minZ + localZ,
                    guardedFloorAt(localX, localZ));
            plans[px][pz] = plan;
        }
        return plan;
    }

    /**
     * The 16×16 chunk plans with the region-support gate applied, exactly like the legacy
     * {@code OceanColumnPlanner.planColumns}: unsupported quart cells stay {@code null} and
     * fall through to vanilla noise terrain.
     */
    public OceanColumnPlanner.ColumnPlan[] chunkPlans() {
        if (!sampler.anySupported() || !terrain.enabled()) {
            return new OceanColumnPlanner.ColumnPlan[256];
        }
        OceanColumnPlanner.ColumnPlan[] out = new OceanColumnPlanner.ColumnPlan[256];
        for (int localX = 0; localX < 16; localX++) {
            int quartLocalX = localX >> 2;
            for (int localZ = 0; localZ < 16; localZ++) {
                if (!sampler.supportsCurrentChunkCell(quartLocalX, localZ >> 2)) {
                    continue;
                }
                out[localX * 16 + localZ] = planAt(localX, localZ);
            }
        }
        return out;
    }

    /**
     * Continuous block-resolution weight of one biome across every layer that carries it.
     * Quart-staircase sampling ({@code block >> 2}) is what made strength-driven relief
     * step every four blocks; this reads the interpolated field instead.
     */
    public double biomeWeightAtBlock(ResourceLocation biome, int blockX, int blockZ) {
        double total = 0.0D;
        for (OceanLayer layer : sampler.stack().layers()) {
            int index = OceanColumnPlanner.indexOf(layer.mix(), biome);
            if (index < 0) {
                continue;
            }
            total += weightField(layer).weightAtBlock(index, blockX, blockZ);
        }
        return total;
    }

    @Override
    public double columnEdge(int blockX, int blockZ) {
        int localX = blockX - (sampler.baseQuartX() << 2);
        int localZ = blockZ - (sampler.baseQuartZ() << 2);
        return Math.min(sampler.edgeStrengthAtHaloBlock(localX, localZ),
                MiddleLevelOceanTerrainProfile.chamberWallFade(blockX, blockZ, terrain));
    }

    @Override
    public double regionEdge(int blockX, int blockZ) {
        int localX = blockX - (sampler.baseQuartX() << 2);
        int localZ = blockZ - (sampler.baseQuartZ() << 2);
        return sampler.edgeStrengthAtHaloBlock(localX, localZ);
    }

    @Override
    public double volcanicStrength(int blockX, int blockZ, double edge) {
        double weight = biomeWeightAtBlock(BrimstoneCalderaPlacement.location(), blockX, blockZ);
        return VolcanoGeometry.strength(weight, edge, terrain.blend().emergenceEdificeFull());
    }

    @Override
    public OceanColumnPlanner.ReefComposition reefCompositionAt(int blockX, int blockZ) {
        return OceanColumnPlanner.composeReef(reefDrivers, blockX, blockZ);
    }

    @Override
    public MiddleLevelOceanTerrainProfile.ColumnProfile profileAt(int blockX, int blockZ) {
        return MiddleLevelOceanTerrainProfile.profileFor(blockX, blockZ, minBuildHeight, terrain, outcropSource);
    }

    public DistrictWeightField weightField(OceanLayer layer) {
        return weightFields.computeIfAbsent(layer, l -> new DistrictWeightField(l.mix()));
    }
}
