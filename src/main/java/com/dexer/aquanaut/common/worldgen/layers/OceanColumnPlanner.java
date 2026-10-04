package com.dexer.aquanaut.common.worldgen.layers;

import com.dexer.aquanaut.common.worldgen.BrimstoneCalderaPlacement;
import com.dexer.aquanaut.common.worldgen.MiddleLevelOceanTerrainProfile;
import com.dexer.aquanaut.common.worldgen.OutcropGeometry;
import com.dexer.aquanaut.common.worldgen.VolcanoGeometry;
import com.dexer.aquanaut.common.worldgen.blend.BlendMath;
import com.dexer.aquanaut.common.worldgen.blend.BoundaryWarp;
import com.dexer.aquanaut.common.worldgen.blend.CellSource;
import com.dexer.aquanaut.common.worldgen.blend.ContactMaterial;
import com.dexer.aquanaut.common.worldgen.blend.DissolutionField;
import com.dexer.aquanaut.common.worldgen.blend.DistrictWeightField;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Precomputes per-column carve geometry and a geological material plan so fill-time
 * state selection is O(1) and rocks form strata/patches instead of per-block confetti.
 *
 * <p>District geology is composed, not selected: every described district contributes its
 * reef thickness, solidity, crack affinity and rock family proportionally to its
 * continuous block-resolution weight ({@link DistrictWeightField}), voids come from the
 * karst {@link DissolutionField} instead of hard OPEN/CRACKED booleans, and rock families
 * interdigitate through the {@link ContactMaterial} dither. No district border can end on
 * a thickness cliff, a severed crack field or a material wall anymore.</p>
 *
 * <p>This class is deliberately free of block states — planning stays loadable and
 * unit-testable without a Minecraft runtime. The block shading of a plan lives in
 * {@link OceanColumnShading}.</p>
 */
public final class OceanColumnPlanner {
    static final long LITH_REGION_SEED = 0x71A7B0C5L;
    static final long LITH_STRATA_SEED = 0x57A7A0C3L;
    static final long REEF_KARST_SEED = 0xC4ACCA7AL;
    static final long REEF_DITHER_SEED = 0x5EEDFACE1L;
    static final long BRINE_CRACK_SEED = 0xB414E5A1L;
    static final long BRINE_STRATA_SEED = 0x62D214E5L;
    static final long DEEP_FLOOR_SEED = 0xA8155A11L;
    /** Soft width (in field units) of the brine fissure ramp — replaces the hard threshold. */
    static final double BRINE_CRACK_WIDTH = 0.25D;
    /** Cell of the karst dissolution noise that modulates reef voids. */
    static final double KARST_CELL = 9.0D;

    private OceanColumnPlanner() {
    }

    public static ColumnPlan[] planColumns(ChunkAccess chunk, OceanGenSampler sampler, TerrainModule terrain) {
        return ChunkTerrainBlend.build(chunk, sampler, terrain).chunkPlans();
    }

    /**
     * One fully analytic column plan: every input is a pure function of world position.
     * This is the ground-truth reference path — the per-chunk grid in
     * {@link ChunkTerrainBlend} evaluates exactly the same functions and, because the
     * cliff guard is a fixed-radius local operator, agrees with this path block for block
     * wherever their windows overlap. Halo columns (the crystal-nest decoration pass)
     * therefore come out exactly like the chunk that fills them.
     */
    public static ColumnPlan planColumnAt(OceanGenSampler sampler, TerrainModule terrain,
                                          int minBuildHeight, int blockX, int blockZ) {
        AnalyticSource source = new AnalyticSource(sampler, terrain, minBuildHeight);
        double floor = terrain.blend().cliffGuard()
                .guardPoint((x, z) -> rawFloorAt(source, x, z), blockX, blockZ);
        return planColumn(source, blockX, blockZ, floor);
    }

    /** Shared column assembly used by the chunk grid and the analytic path alike. */
    public static ColumnPlan planColumn(PlanSource source, int blockX, int blockZ, double guardedFloor) {
        TerrainModule terrain = source.terrain();
        int minBuildHeight = source.minBuildHeight();
        int floorY = (int) Math.round(guardedFloor);
        MiddleLevelOceanTerrainProfile.ColumnProfile profile = source.profileAt(blockX, blockZ);
        double edge = source.columnEdge(blockX, blockZ);

        // Effective cap openness: shafts only dissolve well inside the region, and the
        // edge gate is a ramp, so the shaft outline never hard-stops at a contour.
        double edgeRamp = SoftMixNoise.smoothstep((edge - terrain.crackOpenEdge()) / 0.15D);
        double capOpenness = SoftMixNoise.clamp01(profile.capOpenness() * edgeRamp);
        MiddleLevelOceanTerrainProfile.LensBand capBand = MiddleLevelOceanTerrainProfile.lensBand(
                profile.capTopY(), profile.capBottomY(), capOpenness);

        // Massif relief fades with the dissolving cap so nothing towers under a shaft
        // and no massif ends abruptly on the old crack contour.
        double outcropRelief = profile.outcropRelief() * (1.0D - capOpenness);
        int outcropTopY = outcropRelief > 0.05D
                ? profile.cavityFloorY() + (int) Math.round(outcropRelief)
                : Integer.MIN_VALUE;

        ReefComposition reef = source.reefCompositionAt(blockX, blockZ);
        int reefBottomY = floorY - (int) Math.round(reef.thickness());
        int deepFloorY = deepFloorY(edge, reefBottomY, minBuildHeight, blockX, blockZ);
        int mountainTopY = MiddleLevelOceanTerrainProfile.mountainTopLimit(
                profile.capBottomY(), profile.cavityFloorY());

        double volcanicStrength = source.volcanicStrength(blockX, blockZ, edge);
        VolcanoGeometry.VolcanicColumnPlan volcanic = VolcanoGeometry.columnPlan(volcanicStrength,
                blockX, blockZ, floorY, Math.max(6.0D, mountainTopY - floorY),
                source.volcanoSource(), source.stackSource(), terrain.blend().satelliteEmergence());

        // Per-column constants of the fill-time openness fields: the karst noise depends
        // only on (x, z) and the brine fissure field likewise, so the per-Y query stays
        // allocation-free and cheap.
        DissolutionField dissolution = terrain.blend().dissolution();
        BoundaryWarp warp = terrain.blend().fieldWarp();
        double karstNoise = BlendMath.ridgedFbm(
                blockX + warp.warpX(blockX, blockZ),
                blockZ + warp.warpZ(blockX, blockZ),
                KARST_CELL, 2, 0.55D, 2.1D, REEF_KARST_SEED);
        double brineField = brineField(blockX, blockZ);
        boolean grounded = reefBottomY < floorY
                && reefOpenness(reef, dissolution, karstNoise, brineField,
                        blockX, floorY, blockZ, floorY, reefBottomY) < 0.5D;

        return new ColumnPlan(blockX, blockZ, terrain.topWaterY(), floorY, minBuildHeight - 1,
                edge, profile, terrain, volcanic, reef, reefBottomY, deepFloorY, mountainTopY,
                capOpenness, capBand, outcropRelief, outcropTopY, grounded,
                karstNoise, brineField, dissolution, terrain.blend().contactMaterial());
    }

    /**
     * Pure floor height with only the chamber-wall fade applied (no region-edge sampler).
     * The crystal-nest lattice uses this to root its pipes into the sea floor identically
     * in every chunk.
     */
    public static int pureFloorY(TerrainModule terrain, int minBuildHeight, int blockX, int blockZ) {
        MiddleLevelOceanTerrainProfile.ColumnProfile profile =
                MiddleLevelOceanTerrainProfile.profileFor(blockX, blockZ, minBuildHeight, terrain);
        double edge = MiddleLevelOceanTerrainProfile.chamberWallFade(blockX, blockZ, terrain);
        return (int) Math.round(lerpFloor(profile.capBottomY(), profile.cavityFloorY(), edge));
    }

    /** The underside of the reef cap: where the middle sea ends and the ceiling begins. */
    public static int ceilingY(TerrainModule terrain, int minBuildHeight, int blockX, int blockZ) {
        return MiddleLevelOceanTerrainProfile.profileFor(blockX, blockZ, minBuildHeight, terrain)
                .effCapBottomY() - 1;
    }

    /** Raw (unguarded) geological floor of one column — the cliff guard's input field. */
    public static double rawFloorAt(PlanSource source, int blockX, int blockZ) {
        MiddleLevelOceanTerrainProfile.ColumnProfile profile = source.profileAt(blockX, blockZ);
        double edge = source.columnEdge(blockX, blockZ);
        // Floor Y only uses smooth edge (no high-freq); cavity depth is already broad-scale.
        double floor = lerpFloor(profile.capBottomY(), profile.cavityFloorY(), edge);
        // Brimstone Caldera's volcanic plains: swells and rifts ride the same fade.
        double volcanicStrength = source.volcanicStrength(blockX, blockZ, edge);
        if (volcanicStrength > 0.0D) {
            floor += VolcanoGeometry.floorOffset(blockX, blockZ) * volcanicStrength;
        }
        return floor;
    }

    /** Smoothstep the openness so floor does not tear at mid-strength edges. */
    static double lerpFloor(int capBottomY, int cavityFloorY, double edgeStrength) {
        double t = SoftMixNoise.smoothstep(edgeStrength);
        return capBottomY - (capBottomY - cavityFloorY) * t;
    }

    /** One described biome entry that drives the reef composition, with its layer's weight field. */
    public record ReefDriverRef(DistrictWeightField field, int entryIndex, ReefDescriptor descriptor) {
    }

    /** Collect every reef-driving (described) entry across the stack's layers. */
    public static List<ReefDriverRef> reefDrivers(OceanLayerStack stack) {
        List<ReefDriverRef> drivers = new ArrayList<>();
        for (OceanLayer layer : stack.layers()) {
            DistrictWeightField field = null;
            int index = 0;
            for (MixEntry entry : layer.mix().entries()) {
                ReefDescriptor descriptor = ReefDescriptor.get(entry.biome());
                if (descriptor != null) {
                    if (field == null) {
                        field = new DistrictWeightField(layer.mix());
                    }
                    drivers.add(new ReefDriverRef(field, index, descriptor));
                }
                index++;
            }
        }
        return drivers;
    }

    /**
     * Compose the reef slab of one column from every district's continuous contribution:
     * linear thickness blend, weight-proportional solidity (the karst void appetite),
     * crack affinity and family weights for the contact dither.
     */
    public static ReefComposition composeReef(List<ReefDriverRef> drivers, int blockX, int blockZ) {
        double totalWeight = 0.0D;
        double thickness = 0.0D;
        double solidity = 0.0D;
        double crack = 0.0D;
        double[] families = new double[ReefDescriptor.Family.VALUES.length];
        for (ReefDriverRef driver : drivers) {
            double w = driver.field().weightAtBlock(driver.entryIndex(), blockX, blockZ);
            if (w <= 0.0D) {
                continue;
            }
            ReefDescriptor descriptor = driver.descriptor();
            totalWeight += w;
            thickness += w * descriptor.thicknessAt(blockX, blockZ);
            solidity += w * descriptor.solidity();
            crack += w * descriptor.crackAffinity();
            families[descriptor.family().ordinal()] += w;
        }
        if (totalWeight <= 0.0D) {
            ReefDescriptor fallback = ReefDescriptor.defaultSolid();
            families[fallback.family().ordinal()] = 1.0D;
            return new ReefComposition(fallback.thicknessAt(blockX, blockZ),
                    1.0D - fallback.solidity(), 0.0D, families);
        }
        for (int i = 0; i < families.length; i++) {
            families[i] /= totalWeight;
        }
        double voidAmount = shapedVoid(1.0D - solidity / totalWeight);
        return new ReefComposition(thickness / totalWeight, voidAmount,
                crack / totalWeight, families);
    }

    /**
     * The void-appetite ramp: normalized district weights saturate below 1 (the middle sea's
     * districts share the partition), so the raw "1 − solidity" of a crystal-nest core tops
     * out below 1 and the reef would never fully open. Reshaping through this window keeps the
     * karst border phase (partial, speckled dissolution) and guarantees the nest core opens
     * both seas into one another — continuously, with no threshold cliff: below
     * {@code VOID_RAMP_FROM} the slab stays sealed, above {@code VOID_RAMP_TO} it is gone.
     * The window tracks the district count: adding a fifth district to the middle sea lowered
     * the nest's peak normalized share from ~0.60 to ~0.52, so the ramp moved with it.
     */
    static final double VOID_RAMP_FROM = 0.24D;
    static final double VOID_RAMP_TO = 0.50D;

    static double shapedVoid(double rawVoid) {
        return SoftMixNoise.smoothstep((rawVoid - VOID_RAMP_FROM) / (VOID_RAMP_TO - VOID_RAMP_FROM));
    }

    /**
     * The composed reef slab of one column: continuous thickness, void appetite, brine
     * crack affinity and the dither family weights.
     */
    public record ReefComposition(double thickness, double voidAmount, double crackWeight,
                                  double[] familyWeights) {
    }

    /**
     * Continuous openness of the reef slab at one height: karst dissolution driven by the
     * district void appetite, screened with the brine fissures that pinch shut at the
     * middle-sea floor and widen downward. Both components ramp smoothly in every input,
     * so voids are lens-shaped and district borders dissolve instead of being severed.
     */
    static double reefOpenness(ReefComposition reef, DissolutionField dissolution,
                               double karstNoise, double brineField,
                               int blockX, int blockY, int blockZ,
                               int cavityFloorY, int reefBottomY) {
        double slab = Math.max(1.0D, cavityFloorY - reefBottomY);
        double depth = SoftMixNoise.clamp01((cavityFloorY - blockY) / slab);
        double open = dissolution.openness(reef.voidAmount(), karstNoise, depth * 0.10D);
        double brineRamp = SoftMixNoise.smoothstep((reef.crackWeight() - 0.12D) / 0.28D);
        if (brineRamp > 0.0D) {
            double rough = SoftMixNoise.valueNoise(blockX ^ (blockY * 31), blockZ, 3,
                    BRINE_CRACK_SEED ^ 0x77L);
            double margin = brineField - (0.58D - depth * 0.45D + rough * 0.18D);
            double brine = SoftMixNoise.smoothstep(margin / BRINE_CRACK_WIDTH) * brineRamp;
            open = 1.0D - (1.0D - open) * (1.0D - brine);
        }
        return open;
    }

    /** Three octaves of fissure outline (22/7/3-block cells), continuous in [−1, 1]. */
    static double brineField(int blockX, int blockZ) {
        double broad = SoftMixNoise.valueNoise(blockX, blockZ, 22, BRINE_CRACK_SEED) * 0.55D;
        double mid = SoftMixNoise.valueNoise(blockX, blockZ, 7, BRINE_CRACK_SEED ^ 0x51L) * 0.30D;
        double chips = SoftMixNoise.valueNoise(blockX, blockZ, 3, BRINE_CRACK_SEED ^ 0xA2L) * 0.15D;
        return broad + mid + chips;
    }

    /**
     * Top of the abyssal floor: broad sediment hills near the bottom of the world, which
     * takes the whole remaining depth below the reef. At the region edge the floor rises
     * to meet the reef underside and closes the deep chamber, so transition columns stay
     * solid all the way to bedrock.
     */
    private static int deepFloorY(double edgeStrength, int reefBottomY, int minBuildHeight,
                                  int blockX, int blockZ) {
        double hills = SoftMixNoise.valueNoise(blockX, blockZ, 96, DEEP_FLOOR_SEED);
        double drift = SoftMixNoise.valueNoise(blockX, blockZ, 26, DEEP_FLOOR_SEED ^ 0x5A5AL);
        int abyssY = minBuildHeight + 4 + (int) Math.round(hills * 3.5D + drift * 1.5D);
        abyssY = Math.max(minBuildHeight + 2, Math.min(abyssY, minBuildHeight + 12));
        return (int) Math.round(SoftMixNoise.lerp(SoftMixNoise.smoothstep(edgeStrength), reefBottomY, abyssY));
    }

    /**
     * Everything a column plan needs from its surroundings, so the chunk grid and the
     * analytic reference path share one assembly implementation.
     */
    public interface PlanSource {
        TerrainModule terrain();

        int minBuildHeight();

        double columnEdge(int blockX, int blockZ);

        double volcanicStrength(int blockX, int blockZ, double edge);

        ReefComposition reefCompositionAt(int blockX, int blockZ);

        MiddleLevelOceanTerrainProfile.ColumnProfile profileAt(int blockX, int blockZ);

        CellSource<VolcanoGeometry.Volcano> volcanoSource();

        CellSource<VolcanoGeometry.Stack> stackSource();

        CellSource<OutcropGeometry.Outcrop> outcropSource();
    }

    /**
     * Stateless single-column source: fresh memoized weight fields and cell caches around
     * one {@link OceanGenSampler}. Used by {@link #planColumnAt} — tests, halo fallbacks
     * and any caller without a chunk grid.
     */
    static final class AnalyticSource implements PlanSource {
        private final OceanGenSampler sampler;
        private final TerrainModule terrain;
        private final int minBuildHeight;
        private final List<ReefDriverRef> drivers;
        private final Map<OceanLayer, DistrictWeightField> fields = new HashMap<>();
        private final CellSource<VolcanoGeometry.Volcano> volcanoes =
                CellSource.<VolcanoGeometry.Volcano>memoized(VolcanoGeometry::volcanoAt);
        private final CellSource<VolcanoGeometry.Stack> stacks =
                CellSource.<VolcanoGeometry.Stack>memoized(VolcanoGeometry::stackAt);
        private final CellSource<OutcropGeometry.Outcrop> outcrops =
                CellSource.<OutcropGeometry.Outcrop>memoized(OutcropGeometry::outcropAt);

        AnalyticSource(OceanGenSampler sampler, TerrainModule terrain, int minBuildHeight) {
            this.sampler = sampler;
            this.terrain = terrain;
            this.minBuildHeight = minBuildHeight;
            this.drivers = reefDrivers(sampler.stack());
        }

        @Override
        public TerrainModule terrain() {
            return terrain;
        }

        @Override
        public int minBuildHeight() {
            return minBuildHeight;
        }

        @Override
        public double columnEdge(int blockX, int blockZ) {
            int localX = blockX - (sampler.baseQuartX() << 2);
            int localZ = blockZ - (sampler.baseQuartZ() << 2);
            return Math.min(sampler.edgeStrengthAtHaloBlock(localX, localZ),
                    MiddleLevelOceanTerrainProfile.chamberWallFade(blockX, blockZ, terrain));
        }

        @Override
        public double volcanicStrength(int blockX, int blockZ, double edge) {
            double weight = biomeWeightAtBlock(BrimstoneCalderaPlacement.location(), blockX, blockZ);
            return VolcanoGeometry.strength(weight, edge, terrain.blend().emergenceEdificeFull());
        }

        @Override
        public ReefComposition reefCompositionAt(int blockX, int blockZ) {
            return composeReef(drivers, blockX, blockZ);
        }

        @Override
        public MiddleLevelOceanTerrainProfile.ColumnProfile profileAt(int blockX, int blockZ) {
            return MiddleLevelOceanTerrainProfile.profileFor(blockX, blockZ, minBuildHeight, terrain, outcrops);
        }

        @Override
        public CellSource<VolcanoGeometry.Volcano> volcanoSource() {
            return volcanoes;
        }

        @Override
        public CellSource<VolcanoGeometry.Stack> stackSource() {
            return stacks;
        }

        @Override
        public CellSource<OutcropGeometry.Outcrop> outcropSource() {
            return outcrops;
        }

        double biomeWeightAtBlock(ResourceLocation biome, int blockX, int blockZ) {
            double total = 0.0D;
            for (OceanLayer layer : sampler.stack().layers()) {
                int index = indexOf(layer.mix(), biome);
                if (index < 0) {
                    continue;
                }
                total += field(layer).weightAtBlock(index, blockX, blockZ);
            }
            return total;
        }

        DistrictWeightField field(OceanLayer layer) {
            return fields.computeIfAbsent(layer, l -> new DistrictWeightField(l.mix()));
        }
    }

    static int indexOf(BiomeMix mix, ResourceLocation biome) {
        for (int i = 0; i < mix.entries().size(); i++) {
            if (mix.entries().get(i).biome().equals(biome)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * The complete pure plan of one block column: floor and abyss heights, the composed
     * reef slab, the cap dissolution lens, massif relief, the volcanic column plan and the
     * per-column noise constants the fill-time shading needs. Block states themselves are
     * resolved by {@link OceanColumnShading#stateForY(ColumnPlan, int)}.
     */
    public record ColumnPlan(int blockX, int blockZ, int topCarveY, int cavityFloorY, int bottomY,
                             double edgeStrength,
                             MiddleLevelOceanTerrainProfile.ColumnProfile profile,
                             TerrainModule terrain,
                             VolcanoGeometry.VolcanicColumnPlan volcanic,
                             ReefComposition reef,
                             int reefBottomY,
                             int deepFloorY,
                             int mountainTopY,
                             double capOpenness,
                             MiddleLevelOceanTerrainProfile.LensBand capBand,
                             double outcropRelief,
                             int outcropTopY,
                             boolean grounded,
                             double karstNoise,
                             double brineField,
                             DissolutionField dissolution,
                             ContactMaterial contact) {

        /** Height of the sedimentary plinth the massif footprint grows out of. */
        public int skirtBlocks() {
            if (outcropRelief <= 0.5D) {
                return 0;
            }
            return Math.min(terrain.pillarBaseExtra(), (int) Math.round(outcropRelief));
        }
    }
}
