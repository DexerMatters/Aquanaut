package com.dexer.aquanaut.common.worldgen.layers;

import com.dexer.aquanaut.common.worldgen.MiddleLevelOceanTerrainProfile;
import com.dexer.aquanaut.common.worldgen.VolcanicTerrain;
import com.dexer.aquanaut.common.worldgen.VolcanoGeometry;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.Direction;
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
            int blockX = chunkPos.getBlockX(localX);
            int quartLocalX = localX >> 2;
            for (int localZ = 0; localZ < 16; localZ++) {
                if (!sampler.supportsCurrentChunkCell(quartLocalX, localZ >> 2)) {
                    continue;
                }
                int blockZ = chunkPos.getBlockZ(localZ);
                MiddleLevelOceanTerrainProfile.ColumnProfile profile =
                        MiddleLevelOceanTerrainProfile.profileFor(blockX, blockZ, minBuildHeight, terrain);
                double edge = Math.min(
                        sampler.edgeStrengthAtLocalBlock(localX, localZ),
                        MiddleLevelOceanTerrainProfile.chamberWallFade(blockX, blockZ, terrain));
                // Floor Y only uses smooth edge (no high-freq); cavity depth is already broad-scale.
                int fadedFloorY = lerpFloor(profile.capBottomY(), profile.cavityFloorY(), edge);
                // Brimstone Caldera's volcanic plains: swells and rifts ride the same fade.
                double volcanicStrength = volcanicStrength(sampler, blockX, blockZ, edge);
                if (volcanicStrength > 0.0D) {
                    fadedFloorY += (int) Math.round(VolcanoGeometry.floorOffset(blockX, blockZ)
                            * volcanicStrength);
                }
                boolean pillar = MiddleLevelOceanTerrainProfile.isPillarAt(blockX, blockZ, terrain);
                plans[localX * 16 + localZ] = new ColumnPlan(
                        blockX, blockZ, terrain.topWaterY(), fadedFloorY, fadedFloorY - 3,
                        edge, profile, pillar, terrain, null);
            }
        }

        // Soften isolated 1-block floor spikes so the lower sea bed stays walkable-looking.
        smoothFloorHeights(plans, chunkPos);
        planVolcanoes(plans, sampler);
        return plans;
    }

    /**
     * Second pass: volcanic columns are planned after floor smoothing so cones, crater
     * lakes and ash dusting sit on the final geological floor.
     */
    private static void planVolcanoes(ColumnPlan[] plans, OceanGenSampler sampler) {
        for (int i = 0; i < 256; i++) {
            ColumnPlan plan = plans[i];
            if (plan == null) {
                continue;
            }
            double strength = volcanicStrength(sampler, plan.blockX(), plan.blockZ(), plan.edgeStrength());
            if (strength <= 0.0D) {
                continue;
            }
            VolcanicTerrain.Column volcanic = VolcanicTerrain.columnAt(sampler.stack(), plan.blockX(),
                    plan.blockZ(), plan.cavityFloorY(), plan.edgeStrength());
            if (volcanic == null) {
                continue;
            }
            plans[i] = new ColumnPlan(plan.blockX(), plan.blockZ(), plan.topCarveY(), plan.cavityFloorY(),
                    plan.bottomY(), plan.edgeStrength(), plan.profile(), plan.pillar(), plan.terrain(), volcanic);
        }
    }

    private static double volcanicStrength(OceanGenSampler sampler, int blockX, int blockZ, double edge) {
        double weight = VolcanicTerrain.brimstoneWeight(sampler.stack(), blockX >> 2, blockZ >> 2);
        return VolcanoGeometry.strength(weight, edge);
    }

    /**
     * 3x3 average of faded floor Y (supported columns only) to kill single-column cliffs.
     */
    private static void smoothFloorHeights(ColumnPlan[] plans, ChunkPos chunkPos) {
        int[] raw = new int[256];
        boolean[] present = new boolean[256];
        for (int i = 0; i < 256; i++) {
            if (plans[i] != null) {
                raw[i] = plans[i].cavityFloorY();
                present[i] = true;
            }
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int idx = x * 16 + z;
                if (!present[idx]) {
                    continue;
                }
                int sum = 0;
                int count = 0;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int nx = x + dx;
                        int nz = z + dz;
                        if (nx < 0 || nz < 0 || nx > 15 || nz > 15) {
                            continue;
                        }
                        int nidx = nx * 16 + nz;
                        if (present[nidx]) {
                            sum += raw[nidx];
                            count++;
                        }
                    }
                }
                if (count == 0) {
                    continue;
                }
                int smoothed = (int) Math.round(sum / (double) count);
                // Never move more than 2 blocks from the geological floor.
                int clamped = Math.max(raw[idx] - 2, Math.min(raw[idx] + 2, smoothed));
                ColumnPlan old = plans[idx];
                plans[idx] = new ColumnPlan(old.blockX(), old.blockZ(), old.topCarveY(),
                        clamped, clamped - 3, old.edgeStrength(), old.profile(), old.pillar(), old.terrain(),
                        old.volcanic());
            }
        }
    }

    private static int lerpFloor(int capBottomY, int cavityFloorY, double edgeStrength) {
        // Smoothstep the openness so floor does not tear at mid-strength edges.
        double t = SoftMixNoise.smoothstep(edgeStrength);
        return (int) Math.round(capBottomY - (capBottomY - cavityFloorY) * t);
    }

    public record ColumnPlan(int blockX, int blockZ, int topCarveY, int cavityFloorY, int bottomY,
                             double edgeStrength,
                             MiddleLevelOceanTerrainProfile.ColumnProfile profile,
                             boolean pillar,
                             TerrainModule terrain,
                             VolcanicTerrain.Column volcanic) {

        public BlockState stateForY(int blockY) {
            if (volcanic != null) {
                BlockState volcanicState = volcanic.stateForY(blockY);
                if (volcanicState == null && blockY == cavityFloorY + 1) {
                    // Drifting ash over the plains and distal aprons.
                    volcanicState = volcanic.dustingState();
                }
                if (volcanicState != null) {
                    return volcanicState;
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

            if (blockY <= cavityFloorY) {
                return floorStateFor(blockX, blockY, blockZ, cavityFloorY);
            }

            // Sedimentary outcrops belong to the quiet middle sea. Volcanic ground grows its
            // own satellite scoria cones and spatter ridges instead of stone pillars.
            if (volcanic == null && edgeStrength >= terrain.pillarEdge()
                    && profile.pillarTopY() > 0 && blockY <= profile.pillarTopY()) {
                return pillarStateFor(blockX, blockY, blockZ);
            }

            if (volcanic == null && edgeStrength >= terrain.pillarEdge()
                    && blockY <= cavityFloorY + terrain.pillarBaseExtra()
                    && pillar) {
                return pillarStateFor(blockX, blockY, blockZ);
            }

            return Blocks.WATER.defaultBlockState();
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
