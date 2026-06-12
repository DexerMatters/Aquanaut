package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

public final class MiddleLevelOceanTerrainShaper {
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final int TOP_WATER_Y = 62;
    private static final double CRACK_OPEN_THRESHOLD = 0.32D;
    private static final double PILLAR_THRESHOLD = 0.45D;
    private static final double CORAL_TREE_THRESHOLD = 0.58D;

    private MiddleLevelOceanTerrainShaper() {
    }

    public static int topWaterY() {
        return TOP_WATER_Y;
    }

    public static ColumnData[] precomputeColumns(ChunkAccess chunk, MiddleLevelOceanTransitionField transitionField) {
        int minBuildHeight = chunk.getMinBuildHeight();
        ChunkPos chunkPos = chunk.getPos();

        ColumnData[] columns = new ColumnData[256];

        for (int localX = 0; localX < 16; localX++) {
            int blockX = chunkPos.getBlockX(localX);
            int quartLocalX = localX >> 2;

            for (int localZ = 0; localZ < 16; localZ++) {
                int blockZ = chunkPos.getBlockZ(localZ);
                int quartLocalZ = localZ >> 2;

                if (!transitionField.isCurrentChunkQuartCellSupported(quartLocalX, quartLocalZ)) {
                    continue;
                }

                MiddleLevelOceanTerrainProfile.ColumnProfile profile =
                        MiddleLevelOceanTerrainProfile.profileFor(blockX, blockZ, minBuildHeight);
                double edgeStrength = Math.min(
                        transitionField.edgeStrengthAtBlock(localX, localZ),
                        MiddleLevelOceanTerrainProfile.chamberWallFade(blockX, blockZ));
                int fadedFloorY = lerpFloor(profile.capBottomY(), profile.cavityFloorY(), edgeStrength);

                columns[localX * 16 + localZ] = new ColumnData(
                        blockX, blockZ, TOP_WATER_Y, fadedFloorY, fadedFloorY - 3, edgeStrength, profile);
            }
        }

        return columns;
    }

    private static final int PILLAR_BASE_EXTRA = 4;
    private static final double CORAL_TREE_CHANCE = 0.06D;

    public record ColumnData(int blockX, int blockZ, int topCarveY, int cavityFloorY, int bottomY,
                             double edgeStrength,
                             MiddleLevelOceanTerrainProfile.ColumnProfile profile) {

        public BlockState stateForY(int blockY) {
            int capTop = profile.capTopY();
            int capBottom = profile.capBottomY();
            boolean openCrack = profile.crack() && edgeStrength >= CRACK_OPEN_THRESHOLD;

            if (!openCrack && edgeStrength >= CORAL_TREE_THRESHOLD && blockY > capTop && blockY <= capTop + 3) {
                return coralTreeStateFor(blockX, blockY, blockZ, capTop);
            }

            if (!openCrack && blockY <= capTop && blockY >= capBottom) {
                return capStateFor(blockX, blockY, blockZ, profile);
            }

            if (blockY <= cavityFloorY) {
                return floorStateFor(blockX, blockY, blockZ);
            }

            if (edgeStrength >= PILLAR_THRESHOLD && profile.pillarTopY() > 0 && blockY <= profile.pillarTopY()) {
                return pillarStateFor(blockX, blockY, blockZ);
            }

            if (edgeStrength >= PILLAR_THRESHOLD
                    && blockY <= cavityFloorY + PILLAR_BASE_EXTRA
                    && isAdjacentToPillar(blockX, blockZ)) {
                return pillarStateFor(blockX, blockY, blockZ);
            }

            return WATER;
        }

        private static boolean isAdjacentToPillar(int bx, int bz) {
            return MiddleLevelOceanTerrainProfile.isPillarAt(bx - 1, bz)
                    || MiddleLevelOceanTerrainProfile.isPillarAt(bx + 1, bz)
                    || MiddleLevelOceanTerrainProfile.isPillarAt(bx, bz - 1)
                    || MiddleLevelOceanTerrainProfile.isPillarAt(bx, bz + 1);
        }
    }

    private static BlockState coralTreeStateFor(int blockX, int blockY, int blockZ, int capTop) {
        if (isCoralTrunk(blockX, blockZ) && blockY <= capTop + trunkHeight(blockX, blockZ)) {
            return ringedCoralState(blockX, blockZ, Direction.Axis.Y);
        }

        for (int dir = 0; dir < 4; dir++) {
            int nx = blockX + ((dir == 0) ? -1 : (dir == 1) ? 1 : 0);
            int nz = blockZ + ((dir == 2) ? -1 : (dir == 3) ? 1 : 0);
            if (!isCoralTrunk(nx, nz)) continue;
            int tHeight = trunkHeight(nx, nz);
            if (blockY > capTop + tHeight) continue;
            int yOffset = blockY - capTop;
            if (yOffset < 1) continue;
            long branchHash = mix(0xB2A4C3L ^ ((long) nx * 0x9E3779B97F4A7C15L)
                    ^ ((long) nz * 0x632BE59BD9B4E019L) ^ ((long) yOffset * 0x94D049BB133111EBL));
            int branchDir = (int) (branchHash & 0x3);
            if (branchDir != dir) continue;
            if ((yOffset & 1) == 0) continue;
            Direction.Axis axis = (dir <= 1) ? Direction.Axis.X : Direction.Axis.Z;
            return ringedCoralState(nx, nz, axis);
        }

        return WATER;
    }

    private static boolean isCoralTrunk(int bx, int bz) {
        long h = mix(0xC02A100DL ^ ((long) bx * 0x632BE59BD9B4E019L) ^ ((long) bz * 0x9E3779B97F4A7C15L));
        double chance = ((h >>> 11) & ((1L << 53) - 1)) / (double) (1L << 53);
        return chance < CORAL_TREE_CHANCE;
    }

    private static int trunkHeight(int bx, int bz) {
        long h = mix(0xC02A100DL ^ ((long) bx * 0x632BE59BD9B4E019L) ^ ((long) bz * 0x9E3779B97F4A7C15L));
        return 2 + (int) ((h >>> 5) & 0x3);
    }

    private static BlockState ringedCoralState(int bx, int bz, Direction.Axis axis) {
        long h = mix(0xD1E2F3L ^ ((long) bx * 0x94D049BB133111EBL) ^ ((long) bz * 0x9E3779B97F4A7C15L));
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

    private static BlockState capStateFor(int blockX,
                                          int blockY,
                                          int blockZ,
                                          MiddleLevelOceanTerrainProfile.ColumnProfile profile) {
        int capTop = profile.capTopY();
        int capBottom = profile.capBottomY();
        int depth = capTop - blockY;

        if (depth == 0) {
            long h = mix(0x5A4DL ^ ((long) blockX * 0x632BE59BD9B4E019L) ^ ((long) blockZ * 0x9E3779B97F4A7C15L));
            double mudChance = ((h >>> 11) & ((1L << 53) - 1)) / (double) (1L << 53);
            if (mudChance < 0.15D) {
                return BlockRegistry.NUTRIENT_RICH_MUD.get().defaultBlockState();
            }
            return BlockRegistry.CORAL_SAND.get().defaultBlockState();
        }

        if (depth == 1) {
            return BlockRegistry.LIMESTONE.get().defaultBlockState();
        }

        int midpoint = (capTop + capBottom) / 2;
        if (blockY >= midpoint) {
            return switch ((int) Math.floorMod(hash(blockX, blockY, blockZ, 0xC0A1F00DL), 4L)) {
                case 0 -> BlockRegistry.LIMESTONE.get().defaultBlockState();
                case 1 -> Blocks.CALCITE.defaultBlockState();
                case 2 -> Blocks.TUFF.defaultBlockState();
                default -> BlockRegistry.LIMESTONE.get().defaultBlockState();
            };
        }

        return switch ((int) Math.floorMod(hash(blockX, blockY, blockZ, 0xA3B4C5D6L), 4L)) {
            case 0 -> BlockRegistry.SHALE.get().defaultBlockState();
            case 1 -> Blocks.STONE.defaultBlockState();
            case 2 -> Blocks.TUFF.defaultBlockState();
            default -> BlockRegistry.SHALE.get().defaultBlockState();
        };
    }

    private static BlockState pillarStateFor(int blockX, int blockY, int blockZ) {
        return switch ((int) Math.floorMod(hash(blockX, blockY, blockZ, 0x7A3F9E2DL), 5L)) {
            case 0 -> BlockRegistry.SHALE.get().defaultBlockState();
            case 1 -> BlockRegistry.SHALE.get().defaultBlockState();
            case 2 -> BlockRegistry.LIMESTONE.get().defaultBlockState();
            case 3 -> Blocks.STONE.defaultBlockState();
            default -> Blocks.TUFF.defaultBlockState();
        };
    }

    private static BlockState floorStateFor(int blockX, int blockY, int blockZ) {
        return switch ((int) Math.floorMod(hash(blockX, blockY, blockZ, 0x51EA5EEDL), 5L)) {
            case 0 -> BlockRegistry.SHALE.get().defaultBlockState();
            case 1 -> Blocks.STONE.defaultBlockState();
            case 2 -> Blocks.TUFF.defaultBlockState();
            case 3 -> Blocks.DEEPSLATE.defaultBlockState();
            default -> BlockRegistry.LIMESTONE.get().defaultBlockState();
        };
    }

    private static int lerpFloor(int capBottomY, int cavityFloorY, double edgeStrength) {
        return (int) Math.round(capBottomY - (capBottomY - cavityFloorY) * edgeStrength);
    }

    private static long hash(int x, int y, int z, long seed) {
        return mix(seed ^ ((long) x * 0x632BE59BD9B4E019L)
                ^ ((long) y * 0x94D049BB133111EBL)
                ^ ((long) z * 0x9E3779B97F4A7C15L));
    }

    private static long mix(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return value;
    }
}
