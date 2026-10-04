package com.dexer.aquanaut.common.worldgen.layers;

import com.dexer.aquanaut.common.worldgen.VolcanoGeometry;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end invariants of the district terrain blend on real chunk grids:
 * <ul>
 *   <li>no adjacent-column cliffs anywhere — including across district borders where the
 *       old argmax reef dropped 12-block walls;</li>
 *   <li>chunk seams are invisible: neighbouring grids agree on shared columns exactly;</li>
 *   <li>the analytic single-column reference path equals the production grid path;</li>
 *   <li>region-support gating still hands unsupported columns back to vanilla;</li>
 *   <li>and the whole pipeline stays inside a generous per-chunk time budget.</li>
 * </ul>
 */
public final class TerrainBlendContinuityTest {
    private static final int MIN_BUILD_HEIGHT = -64;
    private static final ResourceLocation DEEP_OCEAN =
            ResourceLocation.withDefaultNamespace("deep_ocean");

    /** A sampler whose whole support field (chunk + halo) qualifies as deep ocean. */
    private static OceanGenSampler supportedSampler(int minX, int minZ) {
        return supportedSampler(OceanLayerStacks.defaultStack(), minX, minZ);
    }

    private static OceanGenSampler supportedSampler(OceanLayerStack stack, int minX, int minZ) {
        boolean[][] halo = new boolean[OceanGenSampler.FIELD_SIZE][OceanGenSampler.FIELD_SIZE];
        for (boolean[] row : halo) {
            java.util.Arrays.fill(row, true);
        }
        return samplerWithHalo(stack, halo, minX, minZ);
    }

    /** A sampler with an explicit support halo, for exercising the region border. */
    private static OceanGenSampler samplerWithHalo(OceanLayerStack stack, boolean[][] halo,
                                                   int minX, int minZ) {
        ResourceLocation[][] surfaceBiomes = new ResourceLocation[4][4];
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                surfaceBiomes[x][z] = DEEP_OCEAN;
            }
        }
        boolean[] openWater = new boolean[16];
        java.util.Arrays.fill(openWater, true);
        return new OceanGenSampler(stack, surfaceBiomes, openWater, halo, minX >> 2, minZ >> 2);
    }

    private static ChunkTerrainBlend blend(int minX, int minZ) {
        return ChunkTerrainBlend.build(supportedSampler(minX, minZ), TerrainModule.reefCap(),
                MIN_BUILD_HEIGHT, minX, minZ);
    }

    @Test
    void floorsNeverStepLikeCliffsWithinAChunk() {
        ChunkTerrainBlend blend = blend(48, -32);
        for (int x = 0; x < 15; x++) {
            for (int z = 0; z < 16; z++) {
                OceanColumnPlanner.ColumnPlan a = blend.planAt(x, z);
                OceanColumnPlanner.ColumnPlan b = blend.planAt(x + 1, z);
                assertTrue(Math.abs(a.cavityFloorY() - b.cavityFloorY()) <= 2,
                        "floor cliff at (" + x + "," + z + "): " + a.cavityFloorY() + " -> " + b.cavityFloorY());
            }
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 15; z++) {
                OceanColumnPlanner.ColumnPlan a = blend.planAt(x, z);
                OceanColumnPlanner.ColumnPlan b = blend.planAt(x, z + 1);
                assertTrue(Math.abs(a.cavityFloorY() - b.cavityFloorY()) <= 2,
                        "floor cliff at (" + x + "," + z + "): " + a.cavityFloorY() + " -> " + b.cavityFloorY());
            }
        }
    }

    @Test
    void reefAndAbyssFollowTheFloorWithoutWalls() {
        ChunkTerrainBlend blend = blend(-112, 96);
        for (int x = 0; x < 15; x++) {
            for (int z = 0; z < 15; z++) {
                OceanColumnPlanner.ColumnPlan a = blend.planAt(x, z);
                OceanColumnPlanner.ColumnPlan b = blend.planAt(x + 1, z + 1);
                assertTrue(Math.abs(a.reefBottomY() - b.reefBottomY()) <= 3,
                        "reef underside steps like a wall at (" + x + "," + z + "): "
                                + a.reefBottomY() + " -> " + b.reefBottomY());
                assertTrue(Math.abs(a.deepFloorY() - b.deepFloorY()) <= 3,
                        "abyss floor steps like a wall at (" + x + "," + z + ")");
            }
        }
    }

    /**
     * A transect of successive chunk grids across more than a full district cell: every
     * world column is read from the chunk that owns it, exactly like the fill pass does,
     * so district borders (including the crystal nest's dissolving reef) are crossed for
     * real. No step may exceed the guard bound anywhere along the way.
     */
    @Test
    void districtBordersCarryNoCliffsAcrossAWorldTransect() {
        int worstFloorStep = 0;
        int worstReefStep = 0;
        Integer previousFloor = null;
        Integer previousReefBottom = null;
        for (int chunkX = -20; chunkX <= 20; chunkX++) {
            ChunkTerrainBlend blend = blend(chunkX * 16, 64);
            for (int local = 0; local < 16; local++) {
                OceanColumnPlanner.ColumnPlan plan = blend.planAt(local, 8);
                assertNotNull(plan);
                if (previousFloor != null) {
                    worstFloorStep = Math.max(worstFloorStep,
                            Math.abs(plan.cavityFloorY() - previousFloor));
                    worstReefStep = Math.max(worstReefStep,
                            Math.abs(plan.reefBottomY() - previousReefBottom));
                }
                previousFloor = plan.cavityFloorY();
                previousReefBottom = plan.reefBottomY();
            }
        }
        assertTrue(worstFloorStep <= 2,
                "the guarded floor never steps like a cliff across districts, worst " + worstFloorStep);
        assertTrue(worstReefStep <= 3,
                "the reef underside pinches out gradually across districts, worst " + worstReefStep);
    }

    /**
     * The reef slab must pinch out gradually where the crystal nest dissolves it: across a
     * transect long enough to span whole district cells the composition has to pass through
     * intermediate thicknesses and void appetites — never jump a 12-block wall at an argmax
     * contour — and adjacent columns may never differ by more than a block of slab.
     */
    @Test
    void reefSlabPinchesOutThroughIntermediateStates() {
        java.util.List<OceanColumnPlanner.ReefDriverRef> drivers =
                OceanColumnPlanner.reefDrivers(OceanLayerStacks.defaultStack());
        boolean sawIntermediateThickness = false;
        boolean sawIntermediateVoid = false;
        boolean sawFullVoid = false;
        boolean sawSolid = false;
        double previousThickness = Double.NaN;
        for (int x = -1600; x <= 1600; x += 4) {
            OceanColumnPlanner.ReefComposition reef = OceanColumnPlanner.composeReef(drivers, x, 91);
            double familySum = 0.0D;
            for (double w : reef.familyWeights()) {
                assertTrue(w >= 0.0D);
                familySum += w;
            }
            assertEquals(1.0D, familySum, 1e-9, "family weights stay normalised");
            assertTrue(reef.thickness() >= 0.0D && reef.thickness() <= 18.0D,
                    "reef thickness stays geological (" + reef.thickness() + ")");
            if (!Double.isNaN(previousThickness)) {
                assertTrue(Math.abs(reef.thickness() - previousThickness) <= 2.0D,
                        "reef thickness never walls at x=" + x + ": "
                                + previousThickness + " -> " + reef.thickness());
            }
            previousThickness = reef.thickness();
            if (reef.thickness() > 0.5D && reef.thickness() < 10.0D) {
                sawIntermediateThickness = true;
            }
            if (reef.voidAmount() > 0.05D && reef.voidAmount() < 0.95D) {
                sawIntermediateVoid = true;
            }
            if (reef.voidAmount() > 0.98D) {
                sawFullVoid = true;
            }
            if (reef.voidAmount() < 0.02D) {
                sawSolid = true;
            }
        }
        assertTrue(sawIntermediateThickness,
                "the district transition carries partially thinned reef, not a wall");
        assertTrue(sawIntermediateVoid,
                "the karst border phase exists between solid rock and open nest");
        assertTrue(sawFullVoid,
                "the crystal-nest core still opens both seas into one another");
        assertTrue(sawSolid, "solid districts keep their full slab");
    }

    @Test
    void neighbouringChunksAgreeOnSharedColumns() {        ChunkTerrainBlend west = blend(0, 0);
        ChunkTerrainBlend east = blend(16, 0);
        for (int shared = 0; shared < 2; shared++) {
            for (int z = 0; z < 16; z++) {
                OceanColumnPlanner.ColumnPlan a = west.planAt(14 + shared, z);
                OceanColumnPlanner.ColumnPlan b = east.planAt(-2 + shared, z);
                String at = "shared column world (" + (16 + shared - 2) + "," + z + ")";
                assertEquals(a.cavityFloorY(), b.cavityFloorY(), at + " floor");
                assertEquals(a.reefBottomY(), b.reefBottomY(), at + " reef bottom");
                assertEquals(a.deepFloorY(), b.deepFloorY(), at + " abyss floor");
                assertEquals(a.capOpenness(), b.capOpenness(), 1e-12, at + " cap openness");
                assertEquals(a.outcropTopY(), b.outcropTopY(), at + " outcrop");
                assertEquals(a.grounded(), b.grounded(), at + " grounded");
                assertEquals(a.reef().thickness(), b.reef().thickness(), 1e-12, at + " reef thickness");
            }
        }
    }

    @Test
    void analyticReferencePathMatchesTheChunkGrid() {
        OceanGenSampler sampler = supportedSampler(32, 32);
        TerrainModule terrain = TerrainModule.reefCap();
        ChunkTerrainBlend blend = ChunkTerrainBlend.build(sampler, terrain, MIN_BUILD_HEIGHT, 32, 32);
        for (int x = 2; x < 14; x += 3) {
            for (int z = 2; z < 14; z += 3) {
                OceanColumnPlanner.ColumnPlan grid = blend.planAt(x, z);
                OceanColumnPlanner.ColumnPlan analytic = OceanColumnPlanner.planColumnAt(
                        sampler, terrain, MIN_BUILD_HEIGHT, 32 + x, 32 + z);
                assertEquals(grid.cavityFloorY(), analytic.cavityFloorY(),
                        "floor mismatch at local (" + x + "," + z + ")");
                assertEquals(grid.reefBottomY(), analytic.reefBottomY(), "reef mismatch");
                assertEquals(grid.capOpenness(), analytic.capOpenness(), 1e-12, "openness mismatch");
                assertEquals(grid.grounded(), analytic.grounded(), "grounded mismatch");
            }
        }
    }

    @Test
    void unsupportedCellsFallBackToVanilla() {
        OceanLayerStack stack = OceanLayerStacks.defaultStack();
        ResourceLocation[][] surfaceBiomes = new ResourceLocation[4][4];
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                surfaceBiomes[x][z] = DEEP_OCEAN;
            }
        }
        // One quart cell is shallow coast: no open water, no mod terrain.
        surfaceBiomes[0][0] = ResourceLocation.withDefaultNamespace("beach");
        boolean[] openWater = new boolean[16];
        java.util.Arrays.fill(openWater, true);
        openWater[0] = false;
        boolean[][] halo = new boolean[OceanGenSampler.FIELD_SIZE][OceanGenSampler.FIELD_SIZE];
        for (boolean[] row : halo) {
            java.util.Arrays.fill(row, true);
        }
        OceanGenSampler sampler = new OceanGenSampler(stack, surfaceBiomes, openWater, halo, 0, 0);
        ChunkTerrainBlend blend = ChunkTerrainBlend.build(sampler, TerrainModule.reefCap(),
                MIN_BUILD_HEIGHT, 0, 0);
        OceanColumnPlanner.ColumnPlan[] plans = blend.chunkPlans();
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                assertNull(plans[x * 16 + z], "unsupported quart cell must stay vanilla");
            }
        }
        assertNotNull(plans[5 * 16 + 5], "supported cell keeps its plan");
    }

    @Test
    void volcanicReliefEmergesFromZeroWithoutPopping() {
        // Across the emergence window the edifice must leave the plain flush: zero relief
        // at zero strength, proportional growth afterwards, and no jump when the cheap
        // scan-skip threshold is crossed (the old hard MIN_STRENGTH gate popped whole
        // quarter-height cones into existence along a contour line).
        double[] strengths = {0.0D, 0.021D, 0.03D, 0.12D, 0.5D, 1.0D};
        int checked = 0;
        for (int cellX = -4; cellX <= 4; cellX++) {
            for (int cellZ = -4; cellZ <= 4; cellZ++) {
                VolcanoGeometry.Volcano volcano = VolcanoGeometry.volcanoAt(cellX, cellZ);
                if (volcano == null) {
                    continue;
                }
                checked++;
                int probeX = volcano.centerX() + (int) Math.round(volcano.baseRadius() * 0.7D);
                int probeZ = volcano.centerZ();
                double previous = -1.0D;
                for (double strength : strengths) {
                    VolcanoGeometry.ColumnShape shape = VolcanoGeometry.shapeAt(
                            probeX, probeZ, -20, strength);
                    double relief = shape.surfaceY() - (-20);
                    assertTrue(relief >= -1e-9,
                            "the plain baseline keeps relief non-negative (" + relief + ")");
                    assertTrue(relief <= 36.0D * strength + 1e-9,
                            "relief grows proportionally to strength (" + relief + " at " + strength + ")");
                    assertTrue(relief >= previous - 1e-9,
                            "relief is monotone in strength: " + previous + " -> " + relief);
                    if (strength <= 0.03D) {
                        assertTrue(relief <= 1.5D,
                                "at fringe strength the flank stays flush with the plain (" + relief + ")");
                    }
                    previous = relief;
                }
            }
        }
        assertTrue(checked > 0, "the probe region should contain volcanoes");
    }

    @Test
    void abyssalFloorLeavesTheAuthoredChamberUntouched() {
        // The deeper world only extends the Y range: the cap, the chamber floor, the reef slab and
        // the guarding must come out identical to the vanilla-height world. The one thing that
        // follows the new depth is the bottom-anchored abyssal sediment floor.
        int abyssal = -512;
        OceanLayerStack stack = OceanLayerStacks.defaultStack().deepenedFor(abyssal);
        ChunkTerrainBlend deepBlend = ChunkTerrainBlend.build(
                supportedSampler(stack, 48, -32), TerrainModule.reefCap(), abyssal, 48, -32);
        ChunkTerrainBlend referenceBlend = blend(48, -32);

        for (int x = 0; x < 16; x += 3) {
            for (int z = 0; z < 16; z += 3) {
                OceanColumnPlanner.ColumnPlan deepPlan = deepBlend.planAt(x, z);
                OceanColumnPlanner.ColumnPlan referencePlan = referenceBlend.planAt(x, z);
                assertEquals(referencePlan.profile().cavityFloorY(), deepPlan.profile().cavityFloorY(),
                        "the chamber floor must not move at (" + x + "," + z + ")");
                assertEquals(referencePlan.cavityFloorY(), deepPlan.cavityFloorY(),
                        "the blended chamber floor must not move at (" + x + "," + z + ")");
                assertEquals(referencePlan.profile().capBottomY(), deepPlan.profile().capBottomY(),
                        "the reef cap must not move at (" + x + "," + z + ")");
                assertEquals(referencePlan.reefBottomY(), deepPlan.reefBottomY(),
                        "the reef slab must not move at (" + x + "," + z + ")");
                assertEquals(referencePlan.reef().thickness(), deepPlan.reef().thickness(), 1e-9,
                        "reef geology is untouched");
                assertEquals(referencePlan.capOpenness(), deepPlan.capOpenness(), 1e-12,
                        "cap dissolution is untouched");
                assertTrue(deepPlan.deepFloorY() >= abyssal + 2,
                        "the abyss floor never leaves the world (" + deepPlan.deepFloorY() + ")");
                assertTrue(deepPlan.deepFloorY() <= referencePlan.deepFloorY(),
                        "the abyss floor follows the new world floor downward");
                assertTrue(deepPlan.deepFloorY() <= deepPlan.reefBottomY(),
                        "the abyss floor always sits below the reef slab");
            }
        }

        for (int x = 0; x < 15; x++) {
            for (int z = 0; z < 16; z++) {
                int left = deepBlend.planAt(x, z).cavityFloorY();
                int right = deepBlend.planAt(x + 1, z).cavityFloorY();
                assertTrue(Math.abs(left - right) <= 2,
                        "floor cliff at (" + x + "," + z + "): " + left + " -> " + right);
            }
        }
    }

    @Test
    void abyssalPlainStaysFlatAcrossTheBasin() {
        // The deep-sea floor is the placeholder reserved for future abyssal biomes: every column of
        // a fully supported chunk must sit on the same sediment level, at any world height, instead
        // of inheriting the middle sea's chamber-wall relief as a mountain range.
        for (int minBuildHeight : new int[] {MIN_BUILD_HEIGHT, -512}) {
            ChunkTerrainBlend blend = ChunkTerrainBlend.build(
                    supportedSampler(0, 0), TerrainModule.reefCap(), minBuildHeight, 0, 0);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    assertEquals(minBuildHeight + 4, blend.planAt(x, z).deepFloorY(),
                            "flat abyssal plain expected at (" + x + "," + z + ") in a "
                                    + minBuildHeight + " world");
                }
            }
        }
    }

    @Test
    void abyssalPlainClosesOnlyAtTheRegionBorder() {
        int abyssal = -512;
        int plain = abyssal + 4;
        OceanLayerStack stack = OceanLayerStacks.defaultStack().deepenedFor(abyssal);

        // Support stops right beside the chunk: the columns nearest the border climb out of the
        // plain, the columns furthest inside stay flat, and the climb is monotone and never rises
        // past the reef underside (which closes the basin against the surrounding crust).
        boolean[][] halo = new boolean[OceanGenSampler.FIELD_SIZE][OceanGenSampler.FIELD_SIZE];
        for (boolean[] row : halo) {
            java.util.Arrays.fill(row, true);
        }
        for (int qx = 0; qx < OceanGenSampler.HALO_QUART_RADIUS; qx++) {
            java.util.Arrays.fill(halo[qx], false);
        }
        ChunkTerrainBlend blend = ChunkTerrainBlend.build(
                samplerWithHalo(stack, halo, 0, 0), TerrainModule.reefCap(), abyssal, 0, 0);

        for (int z = 0; z < 16; z++) {
            assertEquals(plain, blend.planAt(15, z).deepFloorY(),
                    "the plain must survive away from the border");
            assertTrue(blend.planAt(0, z).deepFloorY() > plain + 300,
                    "the border must climb out of the plain, not stay flat into the crust");
            int previous = Integer.MAX_VALUE;
            for (int x = 0; x < 16; x++) {
                OceanColumnPlanner.ColumnPlan plan = blend.planAt(x, z);
                assertTrue(plan.deepFloorY() <= previous,
                        "the floor may only rise toward the border at (" + x + "," + z + ")");
                assertTrue(plan.deepFloorY() <= plan.reefBottomY(),
                        "the floor never rises past the reef underside at (" + x + "," + z + ")");
                previous = plan.deepFloorY();
            }
        }
    }

    @Test
    void chunkPlanningStaysInsideTheTimeBudget() {
        // Generous CI bound: the grid pipeline must stay far cheaper than the legacy
        // per-column re-planning it replaced (which re-derived every column up to ten times).
        long started = System.nanoTime();
        int chunks = 24;
        int plans = 0;
        for (int i = 0; i < chunks; i++) {
            ChunkTerrainBlend blend = blend((i % 6) * 16 - 48, (i / 6) * 16 - 32);
            for (int x = 0; x < 16; x += 2) {
                for (int z = 0; z < 16; z += 2) {
                    // Full plan assembly (weights, reef composition, volcanic shape scan,
                    // cap lens, karst fields) — the per-chunk cost of the blend pipeline.
                    assertNotNull(blend.planAt(x, z));
                    plans++;
                }
            }
        }
        long millis = (System.nanoTime() - started) / 1_000_000L;
        assertEquals(chunks * 64, plans);
        assertTrue(millis < 20_000L, chunks + " chunk blends took " + millis + "ms");
    }
}
