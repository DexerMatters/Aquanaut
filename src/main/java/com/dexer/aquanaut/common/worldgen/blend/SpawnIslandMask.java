package com.dexer.aquanaut.common.worldgen.blend;

import com.dexer.aquanaut.common.worldgen.layers.SoftMixNoise;

/**
 * Coast field of the water-world spawn island, with a meandering world-seeded coastline.
 *
 * <p>
 * The water world preset turns every column into open ocean, so a fresh spawn would drown
 * (design note {@code Aquanaut-Design/Changes.md}). This mask raises the planned geological
 * floor near the world origin to dry ground: the floor is pinned to at least
 * {@link #ISLAND_TOP_Y}, a few blocks above the preset's sea level 63, and the target fades
 * out over the fade width so the seamount flanks merge into the ordinary ocean floor. The
 * planner's cliff guard runs over the blended field like over any other raw floor, so chunk
 * borders and the analytic single-column path stay bit-identical.
 * </p>
 *
 * <p>
 * The coastline is <b>not</b> a radius: {@code coastFieldAt} evaluates a domain-warped,
 * four-octave value-noise field around the mean coast radius — broad lobes swing whole
 * capes and bays, swells and coves meander them, a fine crinkle textures the shoreline —
 * and 2-3 seed-placed bays cut smooth angular notches deep into it, so the silhouette reads
 * as capes, headlands and real water bays instead of a circle. The fade width itself drifts
 * per bearing, so neither the silhouette nor the shelf contours around the island read as a
 * circle. The blend only starts past the local coast field, so the whole interior keeps the
 * full plateau height - there is no moat between the building core and the shore. On top of
 * the plateau, {@link #reliefAt} raises seeded hills and sinks valleys (a few cluster-gated
 * masses rise roughly twice the base amplitude; suppressed inside the organic flat building
 * core and before the shoreline), {@link #duneLift} speckles one-block knolls, and
 * {@link #sandPatchAt} breaks the grass into an irregular speckle. All modulation is
 * scrambled from the world seed.
 * </p>
 */
public final class SpawnIslandMask {
    /** Plateau floor target; the water world preset's sea level is 63. */
    public static final int ISLAND_TOP_Y = 70;
    /**
     * Radius (blocks) of the dead-flat building core: dune relief is suppressed inside, so
     * the middle of the island stays a clean construction site. The boundary is noise-wobbled
     * per world so the flat zone reads as a natural clearing instead of a drawn circle.
     */
    public static final int FLAT_RADIUS = 24;
    /** Mean coastline radius (blocks): the meandering coast field oscillates around it. */
    public static final int COAST_MEAN_RADIUS = 112;
    /** Peak deviation (blocks) of the coastline from its mean radius. */
    public static final int COAST_AMPLITUDE = 56;
    /** Nominal fade width (blocks) from the local coastline down to untouched ocean floor. */
    public static final int FADE_WIDTH = 176;
    /** Narrowest and widest the fade actually gets, per bearing and world seed. */
    public static final double FADE_MIN = FADE_WIDTH * 0.85D;
    public static final double FADE_MAX = FADE_WIDTH * 1.15D;
    /** Nominal radius at which the lift reaches zero on the mean outline. */
    public static final int FADE_RADIUS = COAST_MEAN_RADIUS + FADE_WIDTH;
    /**
     * Smallest radius that is still fully emerged from the coast field alone; sea bays clamp
     * the coastline further in, down to {@link #BAY_FLOOR}.
     */
    /**
     * The harbour inlet: the first bay of every island is drawn as a narrow, deep notch — a
     * genuine gap in the coastline the sea pours through — instead of an ordinary broad bay.
     * Its target sits below {@link #BAY_FLOOR}, so its waterline cuts roughly seventy blocks
     * deeper into the island than the surrounding coast.
     */
    private static final double BAY_INLET_TARGET_MIN = 40.0D;
    private static final double BAY_INLET_TARGET_SPREAD = 8.0D;
    private static final double BAY_INLET_HALF_ANGLE_MIN = 9.0D;
    private static final double BAY_INLET_HALF_ANGLE_SPREAD = 6.0D;
    private static final double BAY_INLET_FALLOFF_MIN = 0.5D;
    private static final double BAY_INLET_FALLOFF_SPREAD = 0.4D;
    /**
     * Smallest radius that is still fully emerged from the coast field and its ordinary sea
     * bays; the harbour inlet clamps the coastline further in, down to
     * {@code BAY_INLET_TARGET_MIN}.
     */
    public static final double MIN_PLATEAU_RADIUS = Math.min(
            COAST_MEAN_RADIUS - COAST_AMPLITUDE, BAY_INLET_TARGET_MIN);
    /** Beyond this radius the lift is exactly zero for every seed and direction. */
    public static final double MAX_FADE_RADIUS = COAST_MEAN_RADIUS + COAST_AMPLITUDE + FADE_MAX;
    /**
     * Exponent on the mask inside {@link #blendFloor}: slightly above one, so the floor
     * leaves the plateau a touch faster than a linear blend and the waterline sits closer to
     * the coastline — a narrower sand apron, more green. The edge grade stays around one and
     * a quarter blocks per block, and the cliff guard eases the rest.
     */
    private static final double BLEND_SHAPE_EXPONENT = 1.3D;
    /**
     * Mask at which the blended shelf can still touch sea level at a structure anchor. Below
     * this the planned floor of every plausible profile stays under water: the highest shelf
     * a raw floor can carry is about 45 blocks (quarry cap 40 plus quarter-strength volcanic
     * swell), and even that blends to 62.2 at the threshold — under the sea level 63. The
     * structure guard refuses anchors above this mask, so no shipwreck can surface on land.
     */
    public static final double ISLAND_STRUCTURE_MASK = 0.75D;
    /** Chunk dilation of the structure claim, so wide structures cannot straddle the island. */
    private static final int STRUCTURE_CLAIM_DILATION_CHUNKS = 2;

    /** Salt of the coast field ("ISLA") and of its domain warp. */
    private static final long COAST_FIELD_SALT = 0x51A0C1A7L;
    private static final long COAST_WARP_SALT = 0x1B2CL;
    private static final long FADE_WIDTH_SALT = 0xFADE1L;
    /** Salt and geometry of the seed-placed sea bays carved into the coast field. */
    private static final long BAY_SALT = 0xBA7E1L;
    private static final int BAY_MIN_COUNT = 2;
    private static final int BAY_MAX_COUNT = 3;
    /**
     * Every bay clamps the coast field down to its own target depth ({@link #BAY_FLOOR}..
     * {@code BAY_FLOOR + BAY_TARGET_SPREAD}), so the waterline cut is guaranteed deep for
     * every seed no matter how high the coast octaves ride at that bearing.
     */
    private static final double BAY_FLOOR = 52.0D;
    private static final double BAY_TARGET_SPREAD = 20.0D;
    private static final double BAY_MIN_HALF_ANGLE = 18.0D;
    private static final double BAY_HALF_ANGLE_SPREAD = 26.0D;
    /**
     * Per-bay wall shape exponent applied to the falloff {@code (1 - smoothstep)}: below one
     * the deep water holds wide and the headland corners turn abrupt, above one the bay
     * tapers into a shallow throat with long shoulders. Drawn per bay, so neighbouring bays
     * get visibly different widths and wall steepness.
     */
    private static final double BAY_FALLOFF_MIN = 0.5D;
    private static final double BAY_FALLOFF_SPREAD = 0.85D;
    /**
     * Angular gaps between bays are drawn independently (each in
     * {@code BAY_GAP_MIN}..{@code BAY_GAP_MIN + BAY_GAP_SPREAD}) and normalized to the full
     * circle around a seed-chosen rotation. A layout that lands within
     * {@link #BAY_EVEN_TOLERANCE_DEG} of the even grid (180 degrees for two bays, 120 for
     * three) is redrawn deterministically, so bay bearings never read as a symmetric pair or
     * star. Every bay pair also keeps at least 60 degrees of separation.
     */
    private static final double BAY_GAP_MIN = 1.0D;
    private static final double BAY_GAP_SPREAD = 1.0D;
    private static final long BAY_GAP_SALT = 0x200L;
    private static final long BAY_ROTATION_SALT = 0x2FFL;
    private static final double BAY_EVEN_TOLERANCE_DEG = 30.0D;
    private static final double BAY_MIN_SEPARATION_DEG = 60.0D;
    private static final double BAY_QUARRY_CLEARANCE_DEG = 60.0D;
    /**
     * Draw budget for the bay layout. A draw must clear three rules at once (separation,
     * even-grid deviation, quarry clearance), so clean passes are a minority; the budget
     * covers the tail comfortably and, with the per-seed cache, costs nothing at runtime.
     */
    private static final int BAY_ANGLE_ATTEMPTS = 64;
    /** Salt of the seeded plateau/flat-core/beach modulation fields. */
    private static final long FLAT_EDGE_SALT = 0x7A31L;
    private static final long DUNE_SEED_SALT = 0xD0A7L;
    private static final long SAND_PATCH_SALT = 0x5A1DL;
    /** Salts of the three hill-field octaves. */
    private static final long HILL_BROAD_SALT = 0x11AAL;
    private static final long HILL_MID_SALT = 0x22BBL;
    private static final long HILL_FINE_SALT = 0x33CCL;
    /** Salt, cell and gate of the hill-cluster field that raises a few dome-shaped masses. */
    private static final long HILL_CLUSTER_SALT = 0x44DDL;
    private static final double HILL_CLUSTER_CELL = 80.0D;
    private static final double HILL_CLUSTER_GATE = 0.30D;
    private static final double HILL_CLUSTER_SPAN = 0.30D;
    public static final double HILL_CLUSTER_BOOST = 22.0D;
    /** Hard ceiling of the plateau relief, so hill tops stay inside the claimed biome band. */
    public static final double HILL_MAX_HEIGHT = 26.0D;
    /** Salt, geometry and reach of the guaranteed hill dome every island carries. */
    private static final long HILL_DOME_SALT = 0x48111L;
    private static final long HILL_DOME_SHAPE_SALT = 0x48113L;
    /**
     * The plateau pond: one small freshwater basin per island, sunk two blocks below the
     * plateau top on the ring between the bays and the quarry. Its pad flattens the ground
     * to a gentle bowl (valleys filled, hillocks capped), so the water surface always sits
     * above every neighbour the splash can reach — {@link #POND_WATER_Y} is strictly below
     * the padded rim's floor, so the pond can never leak.
     */
    public static final int POND_WATER_Y = ISLAND_TOP_Y - 2;
    private static final long POND_SALT = 0x904DL;
    private static final long POND_SHAPE_SALT = 0x904EL;
    private static final long POND_WOBBLE_SALT = 0x904FL;
    private static final double POND_DIST = 46.0D;
    private static final double POND_MIN_RADIUS = 7.5D;
    private static final double POND_RADIUS_SPREAD = 3.0D;
    private static final double POND_WOBBLE = 1.5D;
    private static final double POND_DEPTH = 5.0D;
    private static final double POND_PAD_WIDTH = 6.0D;
    private static final double POND_PAD_MIN = 0.0D;
    private static final double POND_PAD_MAX = 2.0D;
    private static final double POND_MIN_QUARRY_DIST = 45.0D;
    private static final double POND_MIN_DOME_DIST = 50.0D;
    /** Least plateau margin (coast exceed over the pond's distance) a site must show. */
    private static final double POND_MIN_PLATEAU_MARGIN = 12.0D;
    private static final double HILL_DOME_DIST = 46.0D;
    /** Per-seed dome skirt radius and top height, so no two worlds share one hill profile. */
    private static final double HILL_DOME_MIN_RADIUS = 34.0D;
    private static final double HILL_DOME_RADIUS_SPREAD = 12.0D;
    private static final double HILL_DOME_MIN_AMPLITUDE = 12.5D;
    private static final double HILL_DOME_AMPLITUDE_SPREAD = 3.0D;
    /** Noise-space cells of the four warped coastline octaves: lobes, swells, coves, crinkle. */
    private static final double COAST_BROAD_CELL = 260.0D;
    private static final double COAST_SWELL_CELL = 110.0D;
    private static final double COAST_COVE_CELL = 46.0D;
    private static final double COAST_CRINKLE_CELL = 20.0D;
    /** Peak amplitude (blocks) of the domain warp applied before sampling the coast field. */
    private static final double COAST_WARP_AMPLITUDE = 30.0D;
    /** Cell (blocks) of the per-bearing fade-width variation. */
    private static final double FADE_WIDTH_CELL = 190.0D;
    /** Cells (blocks) of the flat-core boundary wobble and of the dune/sand speckle. */
    private static final double FLAT_EDGE_CELL = 31.0D;
    private static final double DUNE_CELL = 9.0D;
    private static final double DUNE_THRESHOLD = 0.5D;
    public static final double SAND_PATCH_CELL = 22.0D;
    private static final double SAND_PATCH_THRESHOLD = 0.7D;
    /** Rocky outcrop field: stone knobs and low crags breaking the grassland texture. */
    private static final long CRAG_SALT = 0xC7A6L;
    private static final double CRAG_CELL = 15.0D;
    private static final double CRAG_GATE = 0.45D;
    private static final double CRAG_SPAN = 0.25D;
    private static final double CRAG_LIFT = 3.0D;
    /** Outcrops surface as bare rock above this lift weight. */
    public static final double CRAG_STONE_AT = 0.7D;
    /** Cells and amplitude of the seeded hill/valley relief on the outer plateau. */
    private static final double HILL_BROAD_CELL = 110.0D;
    private static final double HILL_MID_CELL = 44.0D;
    private static final double HILL_FINE_CELL = 17.0D;
    /** Base peak height (blocks) of a fully-ramped hill; cluster hills reach amplitude + boost. */
    public static final double HILL_AMPLITUDE = 12.0D;
    /** Deepest valley floor (blocks) below the plateau, so dips stay above sea level 63. */
    public static final double HILL_MAX_DIP = 6.0D;    /** Width (blocks) of the ramp that lifts hills out of the flat building core. */
    private static final double HILL_RISE_WIDTH = 22.0D;
    /** Width (blocks) over which hills sink back to shore level ahead of the coastline. */
    private static final double HILL_COAST_FADE = 24.0D;
    /** Plateau relief at or above this height claims the hill biome instead of plains. */
    public static final double HILL_BIOME_THRESHOLD = 4.5D;
    /** Salts of the stony-shore region ("STON"), its boundary wobble and its lava pond. */
    private static final long STONE_SHORE_SALT = 0x5700L;
    private static final long STONE_WOBBLE_SALT = 0x5701L;
    private static final long LAVA_POOL_SALT = 0x5702L;
    private static final long ORE_SPECKLE_SALT = 0x5703L;
    /** Distance (blocks) of the stony-shore centre from the spawn column. */
    private static final double STONE_SHORE_DISTANCE = 50.0D;
    /** Radius (blocks) of the stony-shore region and half-width of its boundary wobble. */
    private static final double STONE_SHORE_RADIUS = 22.0D;
    private static final double STONE_SHORE_WOBBLE = 4.0D;
    /** Radius (blocks) of the small lava pond and half-width of its ragged edge. */
    private static final double LAVA_POOL_RADIUS = 7.0D;
    private static final double LAVA_POOL_WOBBLE = 2.0D;
    /**
     * How far (radians) the pond centre may stray from the quarry centre: 0.02-0.06 rad is
     * 1-3 blocks at the 50-block quarry distance, so the whole pond always sits in the deep
     * stone core instead of poking out toward the grass fringe.
     */
    private static final double LAVA_POOL_AZIMUTH_MIN = 0.02D;
    private static final double LAVA_POOL_AZIMUTH_SPREAD = 0.04D;
    /** The pond may only open where the quarry is solid stone, so its basin is walled by rock. */
    private static final double LAVA_POOL_STONE_GATE = 0.85D;

    private SpawnIslandMask() {
    }

    /**
     * Centre of the stony-shore region: every island carries one, at a fixed distance from
     * the spawn column on a seed-chosen azimuth, so the mining spot is guaranteed to exist
     * while its bearing varies per world.
     */
    public static double[] stoneShoreCenter(long islandSeed) {
        double angle = unit01(scramble(islandSeed, STONE_SHORE_SALT)) * 2.0D * Math.PI;
        return new double[]{
                Math.cos(angle) * STONE_SHORE_DISTANCE,
                Math.sin(angle) * STONE_SHORE_DISTANCE};
    }

    /**
     * Blend weight of the stony-shore region at this column, in [0, 1]: 1 deep inside the
     * rocky ground, 0 outside its noise-wobbled boundary. The caller only shades where the
     * column is fully emerged plateau, so the region can never spill into water.
     */
    public static double stoneShoreWeight(long islandSeed, int blockX, int blockZ) {
        double[] center = stoneShoreCenter(islandSeed);
        double dx = blockX - center[0];
        double dz = blockZ - center[1];
        double dist = Math.sqrt(dx * dx + dz * dz);
        double wobble = SoftMixNoise.valueNoise(blockX, blockZ, 19.0D,
                scramble(islandSeed, STONE_WOBBLE_SALT)) * STONE_SHORE_WOBBLE;
        return SoftMixNoise.smoothstep((STONE_SHORE_RADIUS + wobble - dist) / 6.0D);
    }

    /**
     * Whether this column is one of the small lava ponds sunken into the deep-stone core of
     * the stony shore. The pond centre stays within 1-3 blocks of the quarry centre and the
     * waterline only opens where the quarry is solid stone ({@code weight >= 0.85}), so the
     * lava is always walled by rock: no grass — and nothing flammable — can touch it, and
     * the quarry's own wobble can no longer raise grass islands inside the pool.
     */
    public static boolean lavaPoolAt(long islandSeed, int blockX, int blockZ) {
        double theta = unit01(scramble(islandSeed, STONE_SHORE_SALT)) * 2.0D * Math.PI;
        double poolAngle = theta + LAVA_POOL_AZIMUTH_MIN
                + unit01(scramble(islandSeed, LAVA_POOL_SALT)) * LAVA_POOL_AZIMUTH_SPREAD;
        double px = Math.cos(poolAngle) * STONE_SHORE_DISTANCE;
        double pz = Math.sin(poolAngle) * STONE_SHORE_DISTANCE;
        double dx = blockX - px;
        double dz = blockZ - pz;
        double dist = Math.sqrt(dx * dx + dz * dz);
        double wobble = SoftMixNoise.valueNoise(blockX, blockZ, 11.0D,
                scramble(islandSeed, LAVA_POOL_SALT)) * LAVA_POOL_WOBBLE;
        double pool = SoftMixNoise.smoothstep((LAVA_POOL_RADIUS + wobble - dist) / 3.0D);
        return pool >= 0.6D
                && stoneShoreWeight(islandSeed, blockX, blockZ) >= LAVA_POOL_STONE_GATE;
    }

    /**
     * High-frequency speckle field of the stony-shore surface: the caller maps its value
     * onto exposed low-tier ores (coal, iron, copper) so the rock shows visible veins.
     */
    public static double oreSpeckleAt(long islandSeed, int blockX, int blockZ) {
        return SoftMixNoise.valueNoise(blockX, blockZ, 3.0D, scramble(islandSeed, ORE_SPECKLE_SALT));
    }

    /** Seedless overload: the fixed mid-amplitude silhouette (tests, seedless fallbacks). */
    public static double maskAt(int blockX, int blockZ) {
        return maskAt(0L, blockX, blockZ);
    }

    /** Continuous blend weight in [0, 1]: 1 on the plateau, 0 at and beyond the coastline fade. */
    public static double maskAt(long islandSeed, int blockX, int blockZ) {
        double distSq = (double) blockX * blockX + (double) blockZ * blockZ;
        if (distSq >= MAX_FADE_RADIUS * MAX_FADE_RADIUS) {
            return 0.0D;
        }
        double dist = Math.sqrt(distSq);
        if (dist < 1.0e-9D) {
            return 1.0D;
        }
        double coastField = coastFieldAt(islandSeed, blockX, blockZ);
        double fade = fadeWidthAt(islandSeed, blockX, blockZ);
        return SoftMixNoise.smoothstep((coastField + fade - dist) / fade);
    }

    /**
     * The local coastline radius: mean radius plus a domain-warped, four-octave excursion of
     * up to {@link #COAST_AMPLITUDE} blocks, minus 2-3 seed-placed bay notches that cut real
     * water bays between headlands. The broad lobe octave (260-block cells) swings whole
     * capes and bays, the swell and cove octaves meander them, and the crinkle octave
     * textures the shoreline — together with the warp this keeps the silhouette and every
     * shelf contour off any circle. Pure in (seed, x, z); the blend starts only past this
     * field, so the whole interior holds the full plateau height.
     */
    public static double coastFieldAt(long islandSeed, int blockX, int blockZ) {
        BoundaryWarp warp = new BoundaryWarp.Fbm(COAST_WARP_AMPLITUDE, 120.0D, 40.0D,
                scramble(islandSeed, COAST_WARP_SALT));
        double[] warped = warp.warp(blockX, blockZ, new double[2]);
        long fieldSeed = scramble(islandSeed, COAST_FIELD_SALT);
        double lobes = SoftMixNoise.valueNoise(warped[0], warped[1], COAST_BROAD_CELL, fieldSeed);
        double swells = SoftMixNoise.valueNoise(warped[0], warped[1], COAST_SWELL_CELL, fieldSeed ^ 0x9E3L);
        double coves = SoftMixNoise.valueNoise(warped[0], warped[1], COAST_COVE_CELL, fieldSeed ^ 0x51DL);
        double crinkle = SoftMixNoise.valueNoise(warped[0], warped[1], COAST_CRINKLE_CELL, fieldSeed ^ 0x77L);
        double field = COAST_MEAN_RADIUS
                + COAST_AMPLITUDE * (0.42D * lobes + 0.30D * swells + 0.20D * coves + 0.08D * crinkle);
        return applyBays(islandSeed, blockX, blockZ, field);
    }

    /**
     * Bearings (radians) of the island's 2-3 sea bays. The angular gaps are drawn
     * independently from {@code BAY_GAP_MIN}..{@code BAY_GAP_MIN + BAY_GAP_SPREAD} and
     * normalized to the full circle around a seed-chosen rotation. Every draw is scored
     * against the three layout rules — bay separation, deviation from the even grid, and
     * clearance from the quarry — and the best-scoring draw wins: a clean layout stops the
     * search immediately, and even a seed whose draws never pass cleanly gets the most
     * irregular layout the budget found instead of the last one drawn. Fully deterministic.
     * The result is cached per seed because this runs from the per-block mask evaluation.
     * {@link #applyBays} and {@link #hillDomeCenter} share it; do not mutate the result.
     */
    public static double[] bayCenterAngles(long islandSeed) {
        if (bayCacheAngles != null && bayCacheSeed == islandSeed) {
            return bayCacheAngles;
        }
        double[] angles = computeBayCenterAngles(islandSeed);
        bayCacheAngles = angles;
        bayCacheSeed = islandSeed;
        return angles;
    }

    /** Single-entry memo of {@link #bayCenterAngles}: the seed is constant for a whole world. */
    private static long bayCacheSeed;
    private static double[] bayCacheAngles;

    private static double[] computeBayCenterAngles(long islandSeed) {
        long hash = scramble(islandSeed, BAY_SALT);
        int count = BAY_MIN_COUNT + (int) ((hash >>> 33) % (BAY_MAX_COUNT - BAY_MIN_COUNT + 1));
        double[] angles = new double[count];
        double[] best = new double[count];
        double bestScore = -Double.MAX_VALUE;
        for (int attempt = 0; attempt < BAY_ANGLE_ATTEMPTS; attempt++) {
            // The rotation is redrawn per attempt: the first bay always sits exactly on it,
            // so a fixed rotation would pin bay0 and veto every attempt whenever the quarry
            // happens to sit near that one bearing.
            double rotation = unit01(scramble(hash, BAY_ROTATION_SALT + attempt)) * 2.0D * Math.PI;
            double[] gaps = new double[count];
            double total = 0.0D;
            for (int i = 0; i < count; i++) {
                gaps[i] = BAY_GAP_MIN + unit01(
                        scramble(hash, BAY_GAP_SALT + attempt * 16L + i)) * BAY_GAP_SPREAD;
                total += gaps[i];
            }
            double accumulated = rotation;
            for (int i = 0; i < count; i++) {
                angles[i] = accumulated;
                accumulated += gaps[i] / total * 2.0D * Math.PI;
            }
            double score = bayLayoutScore(islandSeed, angles);
            if (score > bestScore) {
                bestScore = score;
                System.arraycopy(angles, 0, best, 0, count);
            }
            if (score >= 0.0D) {
                break;
            }
        }
        return best;
    }

    /**
     * The tightest margin (degrees) of a bay layout against the three rules: neighbouring
     * bays at least {@link #BAY_MIN_SEPARATION_DEG} apart, some gap at least
     * {@link #BAY_EVEN_TOLERANCE_DEG} off the even grid (so the layout never reads as a
     * symmetric pair or star), and every bay centre at least {@link #BAY_QUARRY_CLEARANCE_DEG}
     * from the quarry (a bay aimed at the mining region would clamp the coastline through it
     * and drown its outer rock). Non-negative means the layout passes. The angles must be
     * ascending around the circle.
     */
    private static double bayLayoutScore(long islandSeed, double[] ascendingAngles) {
        double even = 2.0D * Math.PI / ascendingAngles.length;
        double quarryAngle = unit01(scramble(islandSeed, STONE_SHORE_SALT)) * 2.0D * Math.PI;
        double minSeparation = Double.MAX_VALUE;
        double maxDeviation = 0.0D;
        double minClearance = Double.MAX_VALUE;
        for (int i = 0; i < ascendingAngles.length; i++) {
            double gap = i + 1 < ascendingAngles.length
                    ? ascendingAngles[i + 1] - ascendingAngles[i]
                    : ascendingAngles[0] + 2.0D * Math.PI - ascendingAngles[i];
            minSeparation = Math.min(minSeparation, gap);
            maxDeviation = Math.max(maxDeviation, Math.abs(gap - even));
            minClearance = Math.min(minClearance,
                    Math.abs(angleDelta(ascendingAngles[i], quarryAngle)));
        }
        return Math.min(Math.min(
                        Math.toDegrees(minSeparation) - BAY_MIN_SEPARATION_DEG,
                        Math.toDegrees(maxDeviation) - BAY_EVEN_TOLERANCE_DEG),
                Math.toDegrees(minClearance) - BAY_QUARRY_CLEARANCE_DEG);
    }

    /**
     * Clamps the seed-placed sea bays into the coast field. Each bay owns an angular window
     * with a smooth falloff and pulls the field toward its own target depth
     * ({@link #BAY_FLOOR}..+spread), so its waterline pushes a guaranteed ~60-100 blocks
     * inland between two headlands for every seed. Width, target depth and wall shape are
     * drawn per bay (see {@link #bayProfiles}); the notch factor depends only on the
     * bearing, so the bay walls add no radial slope — the flank gradient stays the blend's
     * own.
     */
    private static double applyBays(long islandSeed, int blockX, int blockZ, double field) {
        double[] centers = bayCenterAngles(islandSeed);
        double[][] profiles = bayProfiles(islandSeed);
        double angle = Math.atan2(blockZ, blockX);
        for (int i = 0; i < centers.length; i++) {
            double delta = Math.abs(angleDelta(angle, centers[i]));
            double halfAngle = profiles[i][1];
            if (delta < halfAngle) {
                double falloff = Math.pow(
                        1.0D - SoftMixNoise.smoothstep(delta / halfAngle), profiles[i][2]);
                field -= (field - profiles[i][0]) * falloff;
            }
        }
        return field;
    }

    /**
     * Per-bay profile of every sea bay, in bay order: {@code [target depth, half angle
     * (radians), wall shape exponent]}. The draws are independent per bay and seed, so one
     * island gets a broad lagoon beside a narrow fjord notch, each with its own wall
     * steepness. Package-private for the width/shape variety test.
     */
    static double[][] bayProfiles(long islandSeed) {
        long hash = scramble(islandSeed, BAY_SALT);
        double[][] profiles = new double[bayCenterAngles(islandSeed).length][3];
        for (int i = 0; i < profiles.length; i++) {
            long slot = scramble(hash, 0x100L + i);
            profiles[i][0] = BAY_FLOOR + unit01(slot) * BAY_TARGET_SPREAD;
            profiles[i][1] = Math.toRadians(BAY_MIN_HALF_ANGLE
                    + unit01(scramble(slot, 0x101L)) * BAY_HALF_ANGLE_SPREAD);
            profiles[i][2] = BAY_FALLOFF_MIN
                    + unit01(scramble(slot, 0x2DDL)) * BAY_FALLOFF_SPREAD;
        }
        // The first bay is the island's harbour inlet: narrow, deep, and cut through the
        // coastline so the open sea reaches far into the land.
        long inletSlot = scramble(hash, 0x200L);
        profiles[0][0] = BAY_INLET_TARGET_MIN + unit01(inletSlot) * BAY_INLET_TARGET_SPREAD;
        profiles[0][1] = Math.toRadians(BAY_INLET_HALF_ANGLE_MIN
                + unit01(scramble(inletSlot, 0x201L)) * BAY_INLET_HALF_ANGLE_SPREAD);
        profiles[0][2] = BAY_INLET_FALLOFF_MIN
                + unit01(scramble(inletSlot, 0x202L)) * BAY_INLET_FALLOFF_SPREAD;
        return profiles;
    }

    /** Signed angular distance from {@code a} to {@code b}, wrapped into [-π, π]. */
    private static double angleDelta(double a, double b) {
        double delta = a - b;
        while (delta > Math.PI) {
            delta -= 2.0D * Math.PI;
        }
        while (delta < -Math.PI) {
            delta += 2.0D * Math.PI;
        }
        return delta;
    }

    /**
     * The local fade width in blocks: a seeded, slowly drifting fraction of
     * {@link #FADE_WIDTH} ({@link #FADE_MIN}..{@link #FADE_MAX}), so the shelf ring around
     * the island is a wide beach plain on one bearing and a narrow fringe on the next
     * instead of an even, concentric band.
     */
    private static double fadeWidthAt(long islandSeed, int blockX, int blockZ) {
        double v = (SoftMixNoise.valueNoise(blockX, blockZ, FADE_WIDTH_CELL,
                scramble(islandSeed, FADE_WIDTH_SALT)) + 1.0D) * 0.5D;
        return FADE_WIDTH * (0.85D + 0.30D * v);
    }

    /**
     * Centre of the guaranteed hill dome: every island carries one tall hill. Candidates are
     * the midpoints between consecutive sea bays and their antipodes — the bearings with the
     * largest clearance from every bay — filtered to stay clear of bay mouths, then scored
     * against the stony-shore quarry so the dome never collapses into it. The fixed radius
     * 46 sits just outside the widest flat-core wobble and well inside the tightest
     * coastline, so the dome needs no coast fade.
     */
    public static double[] hillDomeCenter(long islandSeed) {
        double[] bays = bayCenterAngles(islandSeed);
        int count = bays.length;
        long slot = scramble(islandSeed, HILL_DOME_SALT);
        double jitter = (unit01(slot) - 0.5D) * 0.35D;
        double[] shore = stoneShoreCenter(islandSeed);
        double[] best = null;
        double bestScore = -Double.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            double mid = bays[i] + 0.5D * angleDelta(bays[(i + 1) % count], bays[i]);
            for (int j = 0; j < 2; j++) {
                double angle = mid + (j == 0 ? jitter : Math.PI + jitter);
                double clearance = Double.MAX_VALUE;
                for (double bay : bays) {
                    clearance = Math.min(clearance, Math.abs(angleDelta(angle, bay)));
                }
                if (clearance < Math.toRadians(BAY_MIN_HALF_ANGLE) + 0.12D) {
                    continue;
                }
                int x = (int) Math.round(Math.cos(angle) * HILL_DOME_DIST);
                int z = (int) Math.round(Math.sin(angle) * HILL_DOME_DIST);
                double shoreDx = x - shore[0];
                double shoreDz = z - shore[1];
                // The dome's skirt must not reach the quarry, whose lava ponds need flat ground.
                if (shoreDx * shoreDx + shoreDz * shoreDz < 45.0D * 45.0D) {
                    continue;
                }
                double score = Math.min(clearance, 1.0D)
                        - stoneShoreWeight(islandSeed, x, z);
                if (score > bestScore) {
                    bestScore = score;
                    best = new double[]{x, z};
                }
            }
        }
        if (best != null) {
            return best;
        }
        // Unreachable while bay separations stay above ~100 degrees; fall back to the first
        // bay midpoint so the dome always has a deterministic centre.
        double mid = bays[0] + 0.5D * angleDelta(bays[1 % count], bays[0]);
        return new double[]{Math.cos(mid) * HILL_DOME_DIST, Math.sin(mid) * HILL_DOME_DIST};
    }

    /** Single-entry memo of {@link #hillDomeCenter}: the seed is constant for a whole world. */
    private static long domeCacheSeed;
    private static double[] domeCacheProfile;

    /** Cached per-seed dome profile: centre x, centre z, skirt radius, top height. */
    private static double[] hillDomeProfileCached(long islandSeed) {
        if (domeCacheProfile == null || domeCacheSeed != islandSeed) {
            // Benign race: the computation is pure, threads may just redo it.
            double[] center = hillDomeCenter(islandSeed);
            long shape = scramble(islandSeed, HILL_DOME_SHAPE_SALT);
            domeCacheProfile = new double[]{
                    center[0], center[1],
                    HILL_DOME_MIN_RADIUS + unit01(shape) * HILL_DOME_RADIUS_SPREAD,
                    HILL_DOME_MIN_AMPLITUDE
                            + unit01(scramble(shape, 0x33L)) * HILL_DOME_AMPLITUDE_SPREAD};
            domeCacheSeed = islandSeed;
        }
        return domeCacheProfile;
    }

    /**
     * Centre of the island's plateau pond: drawn from quarter-points of every gap between
     * sea bays and their antipodes — the bearings with the most land — keeping clear of the
     * quarry and preferring distance from the dome, and scored by the plateau margin at the
     * bearing so the basin and its padded rim always sit on fully emerged ground.
     */
    public static double[] pondCenter(long islandSeed) {
        double[] bays = bayCenterAngles(islandSeed);
        double[] dome = hillDomeCenter(islandSeed);
        double[] shore = stoneShoreCenter(islandSeed);
        double jitter = (unit01(scramble(islandSeed, POND_SALT)) - 0.5D) * 0.25D;
        double[] best = null;
        double bestScore = -Double.MAX_VALUE;
        for (int i = 0; i < bays.length; i++) {
            double gap = angleDelta(bays[(i + 1) % bays.length], bays[i]);
            for (int k = 1; k <= 3; k++) {
                double bearing = bays[i] + gap * k / 4.0D + jitter;
                for (int j = 0; j < 2; j++) {
                    double angle = bearing + (j == 0 ? 0.0D : Math.PI);
                    int x = (int) Math.round(Math.cos(angle) * POND_DIST);
                    int z = (int) Math.round(Math.sin(angle) * POND_DIST);
                    double margin = coastFieldAt(islandSeed, x, z) - POND_DIST;
                    if (Math.hypot(x - shore[0], z - shore[1]) < POND_MIN_QUARRY_DIST
                            || margin < POND_MIN_PLATEAU_MARGIN) {
                        continue;
                    }
                    double domeDist = Math.hypot(x - dome[0], z - dome[1]);
                    double score = margin
                            - 2.0D * Math.max(0.0D, POND_MIN_DOME_DIST - domeDist);
                    if (score > bestScore) {
                        bestScore = score;
                        best = new double[]{x, z};
                    }
                }
            }
        }
        if (best != null) {
            return best;
        }
        // Unreachable for realistic bay layouts: a dozen candidate bearings, a quarry four
        // fifths of the ring away and one narrow coastal dip cannot exclude them all. The
        // dome's antipode keeps a deterministic centre regardless; the fill's water gate
        // still walls any basin the ground cannot hold.
        return new double[]{-dome[0], -dome[1]};
    }

    private static long pondCacheSeed;
    private static double[] pondCacheProfile;

    /** Cached per-seed pond profile: centre x, centre z, basin radius. */
    private static double[] pondProfileCached(long islandSeed) {
        if (pondCacheProfile == null || pondCacheSeed != islandSeed) {
            double[] center = pondCenter(islandSeed);
            long shape = scramble(islandSeed, POND_SHAPE_SALT);
            pondCacheProfile = new double[]{
                    center[0], center[1],
                    POND_MIN_RADIUS + unit01(shape) * POND_RADIUS_SPREAD};
            pondCacheSeed = islandSeed;
        }
        return pondCacheProfile;
    }

    /**
     * Smooth strength of the pond basin at this column, in [0, 1]: 1 in the deep centre
     * (the fill lays water here when the floor is below {@link #POND_WATER_Y}), fading to 0
     * at the basin's wobbled rim. Pure and deterministic.
     */
    public static double pondBasinAt(long islandSeed, int blockX, int blockZ) {
        double[] pond = pondProfileCached(islandSeed);
        double dist = Math.hypot(blockX - pond[0], blockZ - pond[1]);
        if (dist >= pond[2] + POND_WOBBLE) {
            return 0.0D;
        }
        double wobble = SoftMixNoise.valueNoise(blockX, blockZ, 13.0D,
                scramble(islandSeed, POND_WOBBLE_SALT)) * POND_WOBBLE;
        return SoftMixNoise.smoothstep((pond[2] + wobble - dist) / 3.0D);
    }

    /**
     * Seeded relief of the plateau: real hills and valleys outside the organic flat building
     * core, from three octaves (a few broad hill masses, mid valley structure, fine texture).
     * A broad cluster field gates a boost where its noise peaks, and every island is also
     * guaranteed one tall dome-shaped hill between its sea bays (see
     * {@link #hillDomeCenter}); the total is capped at {@link #HILL_MAX_HEIGHT} so hill tops
     * stay inside the claimed biome band. Valley floors are clamped at {@link #HILL_MAX_DIP}
     * below the plateau, so no dip can fall under sea level 63. The relief ramps up over
     * {@link #HILL_RISE_WIDTH} out of the flat core, is damped inside the stony-shore quarry
     * (its lava ponds need near-flat ground), and sinks back to shore level over
     * {@link #HILL_COAST_FADE} ahead of the local coastline so no hill is sheared off at the
     * beach. Zero on the beach flanks; the cliff guard downstream relaxes slopes.
     */
    public static double reliefAt(long islandSeed, int blockX, int blockZ, double mask) {
        if (mask < 1.0D) {
            return 0.0D;
        }
        double dist = Math.sqrt((double) blockX * blockX + (double) blockZ * blockZ);
        double wobble = (SoftMixNoise.valueNoise(blockX, blockZ, FLAT_EDGE_CELL,
                scramble(islandSeed, FLAT_EDGE_SALT)) + 1.0D) * 0.5D;
        double flatRadius = FLAT_RADIUS * (0.75D + 0.5D * wobble);
        double ramp = SoftMixNoise.smoothstep((dist - flatRadius) / HILL_RISE_WIDTH);
        if (ramp <= 0.0D) {
            return 0.0D;
        }
        double broad = SoftMixNoise.valueNoise(blockX, blockZ, HILL_BROAD_CELL,
                scramble(islandSeed, HILL_BROAD_SALT));
        double mid = SoftMixNoise.valueNoise(blockX, blockZ, HILL_MID_CELL,
                scramble(islandSeed, HILL_MID_SALT));
        double fine = SoftMixNoise.valueNoise(blockX, blockZ, HILL_FINE_CELL,
                scramble(islandSeed, HILL_FINE_SALT));
        double shaped = 0.55D * broad + 0.30D * mid + 0.15D * fine;
        double cluster = SoftMixNoise.valueNoise(blockX, blockZ, HILL_CLUSTER_CELL,
                scramble(islandSeed, HILL_CLUSTER_SALT));
        double gate = SoftMixNoise.smoothstep((cluster - HILL_CLUSTER_GATE) / HILL_CLUSTER_SPAN);
        double shoreWeight = stoneShoreWeight(islandSeed, blockX, blockZ);
        double relief = Math.max(shaped * HILL_AMPLITUDE, -HILL_MAX_DIP);
        // The cluster boost must not pile hills into the stony-shore quarry: its lava ponds
        // need near-flat working ground, so the boost dies out with the shore weight while
        // the base relief keeps its usual light damping.
        relief += gate * cluster * HILL_CLUSTER_BOOST * (1.0D - shoreWeight);
        // Rocky outcrops ride the same protections as every other relief term: damped in
        // the quarry, faded at the coast, ramped off the flat core.
        relief += cragWeightAt(islandSeed, blockX, blockZ) * CRAG_LIFT;
        relief *= 1.0D - 0.75D * shoreWeight;
        double coastMargin = coastFieldAt(islandSeed, blockX, blockZ) - dist;
        relief *= SoftMixNoise.smoothstep(coastMargin / HILL_COAST_FADE);
        relief *= ramp;
        // The guaranteed hill dome rides on top. Its centre is pinned outside the flat core
        // and clear of every bay and the quarry, and the mask gate sinks everything past the
        // shoreline, so the dome needs neither coast fade nor extra shaping.
        double[] dome = hillDomeProfileCached(islandSeed);
        double domeDist = Math.hypot(blockX - dome[0], blockZ - dome[1]);
        double domeRise = SoftMixNoise.smoothstep((dome[2] - domeDist) / dome[2]);
        relief += domeRise * dome[3] * ramp;
        // The pond rides on top of everything else: first its pad flattens the ring into a
        // gentle bowl (valleys filled up to the pad floor, hillocks capped to the pad
        // ceiling), then the basin dips the centre. The pad guarantees the rim stands at or
        // above the plateau top, so the water line below it is walled wherever it reaches —
        // the pond can never spill down the flank.
        double[] pond = pondProfileCached(islandSeed);
        double pondDist = Math.hypot(blockX - pond[0], blockZ - pond[1]);
        double pondZone = SoftMixNoise.smoothstep(
                (pond[2] + POND_WOBBLE + POND_PAD_WIDTH - pondDist) / 4.0D);
        if (pondZone > 0.0D) {
            double padded = Math.max(POND_PAD_MIN, Math.min(POND_PAD_MAX, relief));
            relief = SoftMixNoise.lerp(pondZone, relief, padded);
        }
        relief -= POND_DEPTH * pondBasinAt(islandSeed, blockX, blockZ);
        relief = Math.max(relief, -HILL_MAX_DIP);
        return Math.min(relief, HILL_MAX_HEIGHT);
    }

    /**
     * Seeded micro relief of the plateau: 1 extra block on roughly a quarter of the outer
     * ground as low grassy knolls, and nothing on the beach flanks (any {@code mask < 1}) or
     * inside the organic flat building core. The cliff guard downstream relaxes the steps.
     */
    public static int duneLift(long islandSeed, int blockX, int blockZ, double mask) {
        if (mask < 1.0D) {
            return 0;
        }
        double dist = Math.sqrt((double) blockX * blockX + (double) blockZ * blockZ);
        double wobble = (SoftMixNoise.valueNoise(blockX, blockZ, FLAT_EDGE_CELL,
                scramble(islandSeed, FLAT_EDGE_SALT)) + 1.0D) * 0.5D;
        double flatRadius = FLAT_RADIUS * (0.75D + 0.5D * wobble);
        if (dist <= flatRadius) {
            return 0;
        }
        double dune = SoftMixNoise.valueNoise(blockX, blockZ, DUNE_CELL,
                scramble(islandSeed, DUNE_SEED_SALT));
        return dune > DUNE_THRESHOLD ? 1 : 0;
    }

    /**
     * Whether the plateau grassland at this column is a speckled vanilla-sand patch: the sand
     * spots are scattered by seeded noise instead of forming a ring, so the plateau reads as
     * irregular ground. Trees skip sand on their own (would_survive fails there).
     */
    public static boolean sandPatchAt(long islandSeed, int blockX, int blockZ) {
        return SoftMixNoise.valueNoise(blockX, blockZ, SAND_PATCH_CELL,
                scramble(islandSeed, SAND_PATCH_SALT)) > SAND_PATCH_THRESHOLD;
    }

    /** Smooth weight of the rocky outcrop field, in [0, 1]. */
    private static double cragWeightAt(long islandSeed, int blockX, int blockZ) {
        double noise = SoftMixNoise.valueNoise(blockX, blockZ, CRAG_CELL,
                scramble(islandSeed, CRAG_SALT));
        return SoftMixNoise.smoothstep((noise - CRAG_GATE) / CRAG_SPAN);
    }

    /**
     * Whether this plateau column surfaces as bare outcrop rock: the crag field is strong,
     * the column lies outside the flat building core (matching the boundary the relief ramp
     * uses), and it is not part of the pond bowl — the pond bed keeps its sand.
     */
    public static boolean cragStoneAt(long islandSeed, int blockX, int blockZ) {
        if (cragWeightAt(islandSeed, blockX, blockZ) < CRAG_STONE_AT
                || pondBasinAt(islandSeed, blockX, blockZ) > 0.0D) {
            return false;
        }
        double dist = Math.sqrt((double) blockX * blockX + (double) blockZ * blockZ);
        double wobble = (SoftMixNoise.valueNoise(blockX, blockZ, FLAT_EDGE_CELL,
                scramble(islandSeed, FLAT_EDGE_SALT)) + 1.0D) * 0.5D;
        return dist > FLAT_RADIUS * (0.75D + 0.5D * wobble);
    }

    /**
     * Blends the raw geological floor toward the island plateau. The target is
     * {@code max(rawFloor, ISLAND_TOP_Y)} so the mask never lowers ground that already rises
     * above the plateau on its own (e.g. volcanic relief riding the same field). The blend
     * runs on the mask raised to {@link #BLEND_SHAPE_EXPONENT}: the smoothstep mask's flat
     * shoulders neutralize the low kink, and the mild concavity pulls the sea a few blocks
     * closer to the coastline — a narrower sand apron and more green — while the flank keeps
     * one gentle, even grade instead of the wall the squared mask used to pile up. The cliff
     * guard downstream evens out the last steps.
     */
    public static double blendFloor(double rawFloor, double mask) {
        if (mask <= 0.0D) {
            return rawFloor;
        }
        double target = Math.max(rawFloor, ISLAND_TOP_Y);
        double shaped = Math.pow(mask, BLEND_SHAPE_EXPONENT);
        return SoftMixNoise.lerp(shaped, rawFloor, target);
    }

    /**
     * Whether this chunk is within the island's structure-exclusion zone. Structures anchor at
     * their chunk centre, so the claim is sampled on a grid of chunk centres around this one;
     * the ring beyond the zone keeps its wrecks, which are guaranteed to anchor under water.
     */
    public static boolean islandClaimsChunk(long islandSeed, int chunkMinBlockX, int chunkMinBlockZ) {
        for (int dx = -STRUCTURE_CLAIM_DILATION_CHUNKS; dx <= STRUCTURE_CLAIM_DILATION_CHUNKS; dx++) {
            for (int dz = -STRUCTURE_CLAIM_DILATION_CHUNKS; dz <= STRUCTURE_CLAIM_DILATION_CHUNKS; dz++) {
                if (maskAt(islandSeed, chunkMinBlockX + 8 + (dx << 4),
                        chunkMinBlockZ + 8 + (dz << 4)) > ISLAND_STRUCTURE_MASK) {
                    return true;
                }
            }
        }
        return false;
    }

    /** SplitMix64-style scramble so unrelated world seeds give unrelated island characters. */
    static long scramble(long worldSeed, long salt) {
        long z = (worldSeed ^ salt) * 0x9E3779B97F4A7C15L;
        z ^= z >>> 30;
        z *= 0xBF58476D1CE4E5B9L;
        z ^= z >>> 27;
        z *= 0x94D049BB133111EBL;
        z ^= z >>> 31;
        return z;
    }

    static double unit01(long scrambled) {
        return (scrambled >>> 11) / (double) (1L << 53);
    }
}
