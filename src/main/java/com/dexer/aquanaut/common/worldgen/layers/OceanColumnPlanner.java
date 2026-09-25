package com.dexer.aquanaut.common.worldgen.layers;

import com.dexer.aquanaut.common.worldgen.BrimstoneCalderaPlacement;
import com.dexer.aquanaut.common.worldgen.BrineMirrorGorgePlacement;
import com.dexer.aquanaut.common.worldgen.CrystalNestPlacement;
import com.dexer.aquanaut.common.worldgen.MiddleLevelOceanPlacement;
import com.dexer.aquanaut.common.worldgen.MiddleLevelOceanTerrainProfile;
import com.dexer.aquanaut.common.worldgen.VolcanicTerrain;
import com.dexer.aquanaut.common.worldgen.VolcanoGeometry;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Precomputes per-column carve geometry and a geological material plan so fill-time
 * state selection is O(1) and rocks form strata/patches instead of per-block confetti.
 */
public final class OceanColumnPlanner {
    private static final long LITH_REGION_SEED = 0x71A7B0C5L;
    private static final long LITH_STRATA_SEED = 0x57A7A0C3L;
    private static final long REEF_THICKNESS_SEED = 0x5EEDFACE1L;
    private static final long REEF_CRACK_SEED = 0xC4ACCA7AL;
    private static final long DEEP_FLOOR_SEED = 0xA8155A11L;

    private OceanColumnPlanner() {
    }

    public static ColumnPlan[] planColumns(ChunkAccess chunk, OceanGenSampler sampler, TerrainModule terrain) {
        ColumnPlan[] plans = new ColumnPlan[256];
        if (!sampler.anySupported() || !terrain.enabled()) {
            return plans;
        }
        int minBuildHeight = chunk.getMinBuildHeight();
        ChunkPos chunkPos = chunk.getPos();

        for (int localX = 0; localX < 16; localX++) {
            int quartLocalX = localX >> 2;
            for (int localZ = 0; localZ < 16; localZ++) {
                if (!sampler.supportsCurrentChunkCell(quartLocalX, localZ >> 2)) {
                    continue;
                }
                plans[localX * 16 + localZ] = planColumnAt(sampler, terrain, minBuildHeight,
                        chunkPos.getBlockX(localX), chunkPos.getBlockZ(localZ));
            }
        }
        return plans;
    }

    /**
     * One fully analytic column plan: every input is a pure function of world position.
     * Halo columns — the crystal-nest decoration pass plans a ring around the chunk to see
     * real faces at the border — therefore come out exactly like the chunk that fills them.
     */
    public static ColumnPlan planColumnAt(OceanGenSampler sampler, TerrainModule terrain,
                                          int minBuildHeight, int blockX, int blockZ) {
        double edge = columnEdge(sampler, terrain, blockX, blockZ);
        MiddleLevelOceanTerrainProfile.ColumnProfile profile =
                MiddleLevelOceanTerrainProfile.profileFor(blockX, blockZ, minBuildHeight, terrain);
        // Floor smoothing runs before volcanic relief so cones, crater lakes and ash dusting
        // sit on the final geological floor.
        int floorY = smoothedFloorY(sampler, terrain, minBuildHeight, blockX, blockZ);
        boolean pillar = profile.hasOutcrop();
        int mountainTopY = MiddleLevelOceanTerrainProfile.mountainTopLimit(
                profile.capBottomY(), profile.cavityFloorY());
        VolcanicTerrain.Column volcanic = volcanicStrength(sampler, blockX, blockZ, edge) > 0.0D
                ? VolcanicTerrain.columnAt(sampler.stack(), blockX, blockZ, floorY, edge,
                        Math.max(6.0D, mountainTopY - floorY))
                : null;
        ReefKind reefKind = reefKindAt(sampler.stack(), blockX, blockZ);
        int reefBottomY = floorY - reefThickness(reefKind, blockX, blockZ);
        int deepFloorY = deepFloorY(edge, reefBottomY, minBuildHeight, blockX, blockZ);
        return new ColumnPlan(blockX, blockZ, terrain.topWaterY(), floorY, minBuildHeight - 1,
                edge, profile, pillar, terrain, volcanic,
                reefKind, reefBottomY, deepFloorY, mountainTopY);
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
        return lerpFloor(profile.capBottomY(), profile.cavityFloorY(), edge);
    }

    /** The underside of the reef cap: where the middle sea ends and the ceiling begins. */
    public static int ceilingY(TerrainModule terrain, int minBuildHeight, int blockX, int blockZ) {
        return MiddleLevelOceanTerrainProfile.profileFor(blockX, blockZ, minBuildHeight, terrain)
                .capBottomY() - 1;
    }

    private static double columnEdge(OceanGenSampler sampler, TerrainModule terrain, int blockX, int blockZ) {
        int localX = blockX - (sampler.baseQuartX() << 2);
        int localZ = blockZ - (sampler.baseQuartZ() << 2);
        return Math.min(sampler.edgeStrengthAtHaloBlock(localX, localZ),
                MiddleLevelOceanTerrainProfile.chamberWallFade(blockX, blockZ, terrain));
    }

    private static int rawFloorY(OceanGenSampler sampler, TerrainModule terrain, int minBuildHeight,
                                 int blockX, int blockZ) {
        MiddleLevelOceanTerrainProfile.ColumnProfile profile =
                MiddleLevelOceanTerrainProfile.profileFor(blockX, blockZ, minBuildHeight, terrain);
        double edge = columnEdge(sampler, terrain, blockX, blockZ);
        // Floor Y only uses smooth edge (no high-freq); cavity depth is already broad-scale.
        int floorY = lerpFloor(profile.capBottomY(), profile.cavityFloorY(), edge);
        // Brimstone Caldera's volcanic plains: swells and rifts ride the same fade.
        double volcanicStrength = volcanicStrength(sampler, blockX, blockZ, edge);
        if (volcanicStrength > 0.0D) {
            floorY += (int) Math.round(VolcanoGeometry.floorOffset(blockX, blockZ) * volcanicStrength);
        }
        return floorY;
    }

    /**
     * 3x3 average of the raw floor Y to kill single-column cliffs. Neighbors are planned
     * analytically (including halo columns), so the smoothing is identical no matter which
     * chunk asks — chunk borders cannot step.
     */
    private static int smoothedFloorY(OceanGenSampler sampler, TerrainModule terrain, int minBuildHeight,
                                      int blockX, int blockZ) {
        int raw = rawFloorY(sampler, terrain, minBuildHeight, blockX, blockZ);
        int sum = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                sum += rawFloorY(sampler, terrain, minBuildHeight, blockX + dx, blockZ + dz);
            }
        }
        int smoothed = (int) Math.round(sum / 9.0D);
        // Never move more than 2 blocks from the geological floor.
        return Math.max(raw - 2, Math.min(raw + 2, smoothed));
    }

    private static double volcanicStrength(OceanGenSampler sampler, int blockX, int blockZ, double edge) {
        double weight = VolcanicTerrain.brimstoneWeight(sampler.stack(), blockX >> 2, blockZ >> 2);
        return VolcanoGeometry.strength(weight, edge);
    }

    private static int lerpFloor(int capBottomY, int cavityFloorY, double edgeStrength) {
        // Smoothstep the openness so floor does not tear at mid-strength edges.
        double t = SoftMixNoise.smoothstep(edgeStrength);
        return (int) Math.round(capBottomY - (capBottomY - cavityFloorY) * t);
    }

    /**
     * The reef geology between the two seas is keyed to the middle-sea district overhead:
     * volcanic ground gets a monolithic concrete apron, the crystal nest keeps no floor at
     * all so both seas run into one another, the brine gorge's floor is fissured, and the
     * quiet middle-level ocean keeps solid strata.
     */
    private static ReefKind reefKindAt(OceanLayerStack stack, int blockX, int blockZ) {
        int quartX = blockX >> 2;
        int quartZ = blockZ >> 2;
        double best = biomeWeight(stack, MiddleLevelOceanPlacement.location(), quartX, quartZ);
        ReefKind kind = ReefKind.SOLID;
        double brine = biomeWeight(stack, BrineMirrorGorgePlacement.location(), quartX, quartZ);
        if (brine > best) {
            best = brine;
            kind = ReefKind.CRACKED;
        }
        double caldera = biomeWeight(stack, BrimstoneCalderaPlacement.location(), quartX, quartZ);
        if (caldera > best) {
            best = caldera;
            kind = ReefKind.CONCRETE;
        }
        if (biomeWeight(stack, CrystalNestPlacement.location(), quartX, quartZ) > best) {
            kind = ReefKind.OPEN;
        }
        return kind;
    }

    private static double biomeWeight(OceanLayerStack stack, ResourceLocation biome, int quartX, int quartZ) {
        for (OceanLayer layer : stack.layers()) {
            int index = 0;
            for (MixEntry entry : layer.mix().entries()) {
                if (entry.biome().equals(biome)) {
                    return layer.mix().weightsAt(quartX, quartZ)[index];
                }
                index++;
            }
        }
        return 0.0D;
    }

    /**
     * Real rock, not a shell: the reef between the two seas carries 9-15 blocks of strata,
     * and the volcanic apron is a thicker monolith.
     */
    private static int reefThickness(ReefKind kind, int blockX, int blockZ) {
        return switch (kind) {
            case OPEN -> 0;
            case CONCRETE -> 15 + (int) Math.round(
                    SoftMixNoise.valueNoise(blockX, blockZ, 48, REEF_THICKNESS_SEED) * 2.0D);
            default -> 12 + (int) Math.round(
                    SoftMixNoise.valueNoise(blockX, blockZ, 32, REEF_THICKNESS_SEED) * 3.0D);
        };
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

    /** How the reef between the two seas breaks — or does not. */
    public enum ReefKind {
        /** Crystal Nest: no floor at all, the deep sea runs open into the middle sea. */
        OPEN,
        /** Brimstone Caldera: a monolithic concrete apron, thick and unbroken. */
        CONCRETE,
        /** Brine Mirror Gorge: solid strata fissured by cracks that widen downward. */
        CRACKED,
        /** Middle-Level Ocean: solid strata. */
        SOLID
    }

    public record ColumnPlan(int blockX, int blockZ, int topCarveY, int cavityFloorY, int bottomY,
                             double edgeStrength,
                             MiddleLevelOceanTerrainProfile.ColumnProfile profile,
                             boolean pillar,
                             TerrainModule terrain,
                             VolcanicTerrain.Column volcanic,
                             ReefKind reefKind,
                             int reefBottomY,
                             int deepFloorY,
                             int mountainTopY) {

        private static final BlockState WATER = Blocks.WATER.defaultBlockState();

        public BlockState stateForY(int blockY) {
            // The reef between the two seas claims its full depth first: a real rock layer
            // with geology and thickness, then the deep sea and its abyssal floor below.
            if (blockY <= cavityFloorY) {
                return belowFloorStateFor(blockX, blockY, blockZ);
            }
            // Nothing settles or grows over open water: on rift floors (cracked brine tops,
            // the crystal nest's open shafts) drifting ash, volcanic dressing and outcrops
            // would hang detached over the void. Rock only grows from solid ground.
            boolean grounded = belowFloorStateFor(blockX, cavityFloorY, blockZ).canOcclude();
            if (volcanic != null) {
                BlockState volcanicState = volcanic.stateForY(blockY);
                if (volcanicState == null && blockY == cavityFloorY + 1) {
                    // Drifting ash over the plains and distal aprons.
                    volcanicState = volcanic.dustingState();
                }
                if (volcanicState != null) {
                    return grounded ? clampBelowMountainTop(volcanicState, blockY) : WATER;
                }
            }
            int capTop = profile.capTopY();
            int capBottom = profile.capBottomY();
            boolean openCrack = profile.crack() && edgeStrength >= terrain.crackOpenEdge();

            if (!openCrack && edgeStrength >= terrain.coralTreeEdge() && blockY > capTop && blockY <= capTop + 3) {
                return coralTreeStateFor(blockX, blockY, blockZ, capTop, terrain);
            }

            if (!openCrack && blockY <= capTop && blockY >= capBottom) {
                return capStateFor(blockX, blockY, blockZ, profile);
            }

            // Sedimentary outcrops belong to the quiet middle sea. Volcanic ground grows its
            // own satellite scoria cones and spatter ridges instead of stone pillars.
            if (volcanic == null && edgeStrength >= terrain.pillarEdge()
                    && profile.hasOutcrop() && blockY <= profile.outcropTopY()) {
                return grounded
                        ? clampBelowMountainTop(pillarStateFor(blockX, blockY, blockZ), blockY)
                        : WATER;
            }

            if (volcanic == null && edgeStrength >= terrain.pillarEdge()
                    && blockY <= cavityFloorY + terrain.pillarBaseExtra()
                    && pillar) {
                return grounded
                        ? clampBelowMountainTop(pillarStateFor(blockX, blockY, blockZ), blockY)
                        : WATER;
            }

            return WATER;
        }

        /**
         * Middle-sea relief never reaches the reef overhead: solid material tops out at
         * {@link #mountainTopY} with clear water between the summits and the ceiling.
         */
        private BlockState clampBelowMountainTop(BlockState state, int blockY) {
            if (blockY > mountainTopY && blockY < profile.capBottomY() && state.canOcclude()) {
                return WATER;
            }
            return state;
        }

        private BlockState belowFloorStateFor(int blockX, int blockY, int blockZ) {
            if (blockY > reefBottomY) {
                return reefStateFor(blockX, blockY, blockZ);
            }
            if (blockY > deepFloorY) {
                return WATER;
            }
            return deepFloorStateFor(blockX, blockY, blockZ);
        }

        /** The reef slab between the two seas, per district geology. */
        private BlockState reefStateFor(int blockX, int blockY, int blockZ) {
            return switch (reefKind) {
                // The crystal nest keeps no floor: both seas run into one another and the
                // lattice hangs between them.
                case OPEN -> WATER;
                // Volcanic ground: a monolithic concrete apron, thick and unbroken.
                case CONCRETE -> BlockRegistry.VOLCANIC_AGGLOMERATE.get().defaultBlockState();
                // The brine gorge's floor is fissured: cracks pinch shut at the middle-sea
                // floor and widen downward into the deep sea.
                case CRACKED -> reefCrackAt(blockX, blockY, blockZ)
                        ? WATER
                        : floorStateFor(blockX, blockY, blockZ, cavityFloorY);
                case SOLID -> floorStateFor(blockX, blockY, blockZ, cavityFloorY);
            };
        }

        private boolean reefCrackAt(int blockX, int blockY, int blockZ) {
            double thickness = Math.max(1, cavityFloorY - reefBottomY);
            double depth = (cavityFloorY - blockY) / thickness;
            // Three octaves of outline (22/7/3-block cells), with the finest breaking the
            // contour at chip scale and the threshold jittered per stratum band, so the
            // verges read rough-hewn and coarse instead of machined.
            double broad = SoftMixNoise.valueNoise(blockX, blockZ, 22, REEF_CRACK_SEED) * 0.55D;
            double mid = SoftMixNoise.valueNoise(blockX, blockZ, 7, REEF_CRACK_SEED ^ 0x51L) * 0.30D;
            double chips = SoftMixNoise.valueNoise(blockX, blockZ, 3, REEF_CRACK_SEED ^ 0xA2L) * 0.15D;
            double field = broad + mid + chips;
            double rough = SoftMixNoise.valueNoise(blockX ^ (blockY * 31), blockZ, 3,
                    REEF_CRACK_SEED ^ 0x77L);
            return field > 0.58D - depth * 0.45D + rough * 0.18D;
        }

        /** The abyssal floor of the deep sea: pelagic sediment over geological strata. */
        private BlockState deepFloorStateFor(int blockX, int blockY, int blockZ) {
            int depth = deepFloorY - blockY;
            if (depth == 0) {
                // Mud swales between silty shale drifts.
                double silt = SoftMixNoise.valueNoise(blockX, blockZ, 10, DEEP_FLOOR_SEED ^ 0x77L);
                return silt > 0.25D
                        ? BlockRegistry.NUTRIENT_RICH_MUD.get().defaultBlockState()
                        : BlockRegistry.VARVE_SHALE.get().defaultBlockState();
            }
            if (depth <= 2) {
                return BlockRegistry.VARVE_SHALE.get().defaultBlockState();
            }
            return floorStateFor(blockX, blockY, blockZ, deepFloorY);
        }
    }

    private static BlockState coralTreeStateFor(int blockX, int blockY, int blockZ, int capTop, TerrainModule terrain) {
        if (isCoralTrunk(blockX, blockZ, terrain) && blockY <= capTop + trunkHeight(blockX, blockZ)) {
            return ringedCoralState(blockX, blockZ, Direction.Axis.Y);
        }

        for (int dir = 0; dir < 4; dir++) {
            int nx = blockX + ((dir == 0) ? -1 : (dir == 1) ? 1 : 0);
            int nz = blockZ + ((dir == 2) ? -1 : (dir == 3) ? 1 : 0);
            if (!isCoralTrunk(nx, nz, terrain)) continue;
            int tHeight = trunkHeight(nx, nz);
            if (blockY > capTop + tHeight) continue;
            int yOffset = blockY - capTop;
            if (yOffset < 1) continue;
            long branchHash = SoftMixNoise.mix(nx, nz, 0xB2A4C3L ^ ((long) yOffset * 0x94D049BB133111EBL));
            int branchDir = (int) (branchHash & 0x3);
            if (branchDir != dir) continue;
            if ((yOffset & 1) == 0) continue;
            Direction.Axis axis = (dir <= 1) ? Direction.Axis.X : Direction.Axis.Z;
            return ringedCoralState(nx, nz, axis);
        }

        return Blocks.WATER.defaultBlockState();
    }

    private static boolean isCoralTrunk(int bx, int bz, TerrainModule terrain) {
        long h = SoftMixNoise.mix(bx >> 1, bz >> 1, 0xC02A100DL);
        double chance = ((h >>> 11) & ((1L << 53) - 1)) / (double) (1L << 53);
        return chance < terrain.coralTreeChance();
    }

    private static int trunkHeight(int bx, int bz) {
        long h = SoftMixNoise.mix(bx >> 1, bz >> 1, 0xC02A100DL);
        return 2 + (int) ((h >>> 5) & 0x3);
    }

    private static BlockState ringedCoralState(int bx, int bz, Direction.Axis axis) {
        long h = SoftMixNoise.mix(bx, bz, 0xD1E2F3L);
        return switch ((int) (h & 0x4L) != 0 ? (int) (h & 0x3) : (int) ((h >>> 2) & 0x3) + 2) {
            case 0 -> BlockRegistry.RINGED_BLUE_CORAL_BLOCK.get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, axis);
            case 1 -> BlockRegistry.RINGED_GREEN_CORAL_BLOCK.get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, axis);
            case 2 -> BlockRegistry.RINGED_PURPLE_CORAL_BLOCK.get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, axis);
            case 3 -> BlockRegistry.RINGED_RED_CORAL_BLOCK.get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, axis);
            default -> BlockRegistry.RINGED_FLUORASCENT_BLUE_CORAL_BLOCK.get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, axis);
        };
    }

    private static BlockState capStateFor(int blockX, int blockY, int blockZ,
                                          MiddleLevelOceanTerrainProfile.ColumnProfile profile) {
        int capTop = profile.capTopY();
        int capBottom = profile.capBottomY();
        int depth = capTop - blockY;

        if (depth == 0) {
            double mud = SoftMixNoise.valueNoise(blockX, blockZ, 12, 0x5A4DL);
            if (mud > 0.72D) {
                return BlockRegistry.NUTRIENT_RICH_MUD.get().defaultBlockState();
            }
            return BlockRegistry.CORAL_SAND.get().defaultBlockState();
        }

        if (depth == 1) {
            return BlockRegistry.LIMESTONE.get().defaultBlockState();
        }

        int midpoint = (capTop + capBottom) / 2;
        if (blockY >= midpoint) {
            return switch (strataIndex(blockX, blockZ, blockY, 3, 0xC0A1F00DL)) {
                case 0 -> BlockRegistry.LIMESTONE.get().defaultBlockState();
                case 1 -> Blocks.CALCITE.defaultBlockState();
                default -> Blocks.TUFF.defaultBlockState();
            };
        }

        return switch (strataIndex(blockX, blockZ, blockY, 3, 0xA3B4C5D6L)) {
            case 0 -> BlockRegistry.SHALE.get().defaultBlockState();
            case 1 -> Blocks.STONE.defaultBlockState();
            default -> Blocks.TUFF.defaultBlockState();
        };
    }

    private static BlockState pillarStateFor(int blockX, int blockY, int blockZ) {
        return switch (strataIndex(blockX, blockZ, blockY, 3, 0x7A3F9E2DL)) {
            case 0 -> BlockRegistry.SHALE.get().defaultBlockState();
            case 1 -> BlockRegistry.LIMESTONE.get().defaultBlockState();
            default -> Blocks.STONE.defaultBlockState();
        };
    }

    /**
     * Geological floor: a regional lithology band plus horizontal strata, not per-block RNG.
     */
    private static BlockState floorStateFor(int blockX, int blockY, int blockZ, int floorY) {
        int depth = Math.max(0, floorY - blockY);
        int band = depth / 4;
        double region = SoftMixNoise.valueNoise(blockX, blockZ, 48, LITH_REGION_SEED);
        double strata = SoftMixNoise.valueNoise(blockX, blockZ, 16, LITH_STRATA_SEED);

        // Regional field picks a dominant rock family over tens of blocks.
        if (region < 0.30D) {
            return band % 2 == 0 ? BlockRegistry.SHALE.get().defaultBlockState() : Blocks.STONE.defaultBlockState();
        }
        if (region < 0.55D) {
            return switch (band % 3) {
                case 0 -> BlockRegistry.LIMESTONE.get().defaultBlockState();
                case 1 -> Blocks.TUFF.defaultBlockState();
                default -> Blocks.STONE.defaultBlockState();
            };
        }
        if (region < 0.78D) {
            return strata > 0.55D
                    ? BlockRegistry.LIMESTONE.get().defaultBlockState()
                    : BlockRegistry.SHALE.get().defaultBlockState();
        }
        // Deep pockets: deeper bands darken toward deepslate.
        if (depth > 8) {
            return Blocks.DEEPSLATE.defaultBlockState();
        }
        return band % 2 == 0 ? Blocks.TUFF.defaultBlockState() : BlockRegistry.SHALE.get().defaultBlockState();
    }

    /**
     * Smooth banded lithology index (0..variants-1) along Y with regional XZ drift.
     */
    private static int strataIndex(int blockX, int blockZ, int blockY, int variants, long seed) {
        double drift = SoftMixNoise.valueNoise(blockX, blockZ, 24, LITH_STRATA_SEED ^ seed);
        double phase = blockY * 0.35D + drift * 3.0D;
        // floorMod(long, int) already returns int, and band is floored, so no further cast is needed.
        long band = (long) Math.floor(phase);
        return Math.floorMod(band, variants);
    }
}
