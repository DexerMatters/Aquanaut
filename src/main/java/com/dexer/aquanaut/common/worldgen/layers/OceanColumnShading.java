package com.dexer.aquanaut.common.worldgen.layers;

import com.dexer.aquanaut.common.worldgen.VolcanicTerrain;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fill-time block shading of a planned column: turns the pure
 * {@link OceanColumnPlanner.ColumnPlan} into concrete block states. Kept apart from the
 * planner so the whole planning/blend pipeline stays loadable (and unit-testable) in a
 * bare JVM without Minecraft's block classes.
 *
 * <p>The reef slab between the two seas is shaded from the continuous composition: karst
 * dissolution and brine fissures decide water vs rock at every height, and the rock that
 * survives is drawn from the district families through the contact dither, so contacts
 * interbed instead of butting against each other.</p>
 */
public final class OceanColumnShading {
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();

    private OceanColumnShading() {
    }

    public static BlockState stateForY(OceanColumnPlanner.ColumnPlan plan, int blockY) {
        int blockX = plan.blockX();
        int blockZ = plan.blockZ();
        // The reef between the two seas claims its full depth first: a real rock layer
        // with geology and thickness, then the deep sea and its abyssal floor below.
        if (blockY <= plan.cavityFloorY()) {
            return belowFloorStateFor(plan, blockX, blockY, blockZ);
        }
        // Nothing settles or grows over open water: on rift floors (dissolved brine tops,
        // the crystal nest's open shafts) drifting ash, volcanic dressing and outcrops
        // would hang detached over the void. Rock only grows from solid ground.
        if (plan.volcanic() != null) {
            BlockState volcanicState = VolcanicTerrain.stateForY(plan.volcanic(), blockY);
            if (volcanicState == null && blockY == plan.cavityFloorY() + 1) {
                // Drifting ash over the plains and distal aprons.
                volcanicState = VolcanicTerrain.dustingState(plan.volcanic());
            }
            if (volcanicState != null) {
                return plan.grounded()
                        ? clampBelowMountainTop(plan, volcanicState, blockY)
                        : WATER;
            }
        }
        int capTop = plan.capBand().topY();
        int capBottom = plan.capBand().bottomY();

        if (plan.capOpenness() < 0.25D && plan.edgeStrength() >= plan.terrain().coralTreeEdge()
                && blockY > capTop && blockY <= capTop + 3) {
            return coralTreeStateFor(blockX, blockY, blockZ, capTop, plan.terrain());
        }

        if (blockY <= capTop && blockY >= capBottom) {
            return capStateFor(plan, blockX, blockY, blockZ, capTop, capBottom);
        }

        // Sedimentary outcrops belong to the quiet middle sea. Volcanic ground grows its
        // own satellite scoria cones and spatter ridges instead of stone pillars.
        if (plan.volcanic() == null && plan.edgeStrength() >= plan.terrain().pillarEdge()
                && blockY <= plan.outcropTopY()) {
            return plan.grounded()
                    ? clampBelowMountainTop(plan, pillarStateFor(blockX, blockY, blockZ), blockY)
                    : WATER;
        }

        // The plinth fades with the massif relief itself, so footprint borders are
        // gentle aprons instead of a uniform four-block cliff ring.
        int skirt = plan.skirtBlocks();
        if (plan.volcanic() == null && skirt > 0
                && plan.edgeStrength() >= plan.terrain().pillarEdge()
                && blockY <= plan.cavityFloorY() + skirt) {
            return plan.grounded()
                    ? clampBelowMountainTop(plan, pillarStateFor(blockX, blockY, blockZ), blockY)
                    : WATER;
        }

        return WATER;
    }

    /**
     * Middle-sea relief never reaches the reef overhead: solid material tops out at the
     * plan's mountain line with clear water between the summits and the ceiling.
     */
    private static BlockState clampBelowMountainTop(OceanColumnPlanner.ColumnPlan plan,
                                                    BlockState state, int blockY) {
        if (blockY > plan.mountainTopY() && blockY < plan.profile().capBottomY() && state.canOcclude()) {
            return WATER;
        }
        return state;
    }

    private static BlockState belowFloorStateFor(OceanColumnPlanner.ColumnPlan plan,
                                                 int blockX, int blockY, int blockZ) {
        if (blockY > plan.reefBottomY()) {
            return reefStateFor(plan, blockX, blockY, blockZ);
        }
        if (blockY > plan.deepFloorY()) {
            return WATER;
        }
        return deepFloorStateFor(plan, blockX, blockY, blockZ);
    }

    /**
     * The reef family of one column. Mud is claimed only where it is a genuine majority of the
     * composed weights; elsewhere it is dropped before the contact dither, so the other
     * districts never sprout stray mud. The remaining rock families still interbed.
     */
    private static ReefDescriptor.Family familyFor(OceanColumnPlanner.ColumnPlan plan,
                                                   int blockX, int blockY, int blockZ) {
        double[] weights = plan.reef().familyWeights();
        int mud = ReefDescriptor.Family.MUD.ordinal();
        if (weights[mud] >= 0.5D) {
            return ReefDescriptor.Family.MUD;
        }
        double[] dither = weights;
        if (weights[mud] > 0.0D) {
            dither = weights.clone();
            dither[mud] = 0.0D;
        }
        return ReefDescriptor.Family.VALUES[
                plan.contact().pickFamily(dither, blockX, blockY, blockZ,
                        OceanColumnPlanner.REEF_DITHER_SEED)];
    }

    /** The reef slab between the two seas: dissolved by voids, fissured by brine, dithered by contact. */
    private static BlockState reefStateFor(OceanColumnPlanner.ColumnPlan plan,
                                           int blockX, int blockY, int blockZ) {
        double open = OceanColumnPlanner.reefOpenness(plan.reef(), plan.dissolution(),
                plan.karstNoise(), plan.brineField(),
                blockX, blockY, blockZ, plan.cavityFloorY(), plan.reefBottomY());
        if (open >= 0.5D) {
            // The crystal nest keeps no floor: both seas run into one another and the
            // lattice hangs between them. Brine fissures widen downward the same way.
            return WATER;
        }
        ReefDescriptor.Family family = familyFor(plan, blockX, blockY, blockZ);
        return switch (family) {
            // Volcanic ground: the monolithic concrete apron.
            case VOLCANIC -> BlockRegistry.VOLCANIC_AGGLOMERATE.get().defaultBlockState();
            // The brine gorge: varve shale with halite-crust lenses.
            case HALITE -> brineFloorStateFor(blockX, blockY, blockZ, plan.cavityFloorY());
            case MUD -> mudFloorStateFor(blockX, blockY, blockZ, plan.cavityFloorY());
            case SEDIMENTARY -> floorStateFor(blockX, blockY, blockZ, plan.cavityFloorY());
        };
    }

    /** The abyssal floor of the deep sea: pelagic sediment over geological strata. */
    private static BlockState deepFloorStateFor(OceanColumnPlanner.ColumnPlan plan,
                                                int blockX, int blockY, int blockZ) {
        int depth = plan.deepFloorY() - blockY;
        if (depth == 0) {
            // Mud swales between silty shale drifts.
            double silt = SoftMixNoise.valueNoise(blockX, blockZ, 10,
                    OceanColumnPlanner.DEEP_FLOOR_SEED ^ 0x77L);
            return silt > 0.25D
                    ? BlockRegistry.NUTRIENT_RICH_MUD.get().defaultBlockState()
                    : BlockRegistry.VARVE_SHALE.get().defaultBlockState();
        }
        if (depth <= 2) {
            return BlockRegistry.VARVE_SHALE.get().defaultBlockState();
        }
        return floorStateFor(blockX, blockY, blockZ, plan.deepFloorY());
    }

    /** Brine-impregnated strata: varve shale with halite crusts at the floor contact. */
    private static BlockState brineFloorStateFor(int blockX, int blockY, int blockZ, int cavityFloorY) {
        int depth = Math.max(0, cavityFloorY - blockY);
        if (depth == 0) {
            double crust = SoftMixNoise.valueNoise(blockX, blockZ, 10,
                    OceanColumnPlanner.BRINE_STRATA_SEED);
            return crust > 0.10D
                    ? BlockRegistry.HALITE_CRUST.get().defaultBlockState()
                    : BlockRegistry.VARVE_SHALE.get().defaultBlockState();
        }
        return switch (strataIndex(blockX, blockZ, blockY, 3,
                OceanColumnPlanner.BRINE_STRATA_SEED ^ 0x2BL)) {
            case 0 -> BlockRegistry.VARVE_SHALE.get().defaultBlockState();
            case 1 -> BlockRegistry.SHALE.get().defaultBlockState();
            default -> BlockRegistry.HALITE_CRUST.get().defaultBlockState();
        };
    }

    /**
     * Mud-zone ground: a waterlogged mud veneer over banded mudstone with fossil beds.
     * Nutrient-rich mud forms the humus patches; parasitic mud stays rare so it reads as
     * an anomaly rather than the default.
     */
    private static BlockState mudFloorStateFor(int blockX, int blockY, int blockZ, int cavityFloorY) {
        int depth = Math.max(0, cavityFloorY - blockY);
        if (depth == 0) {
            int patch = strataIndex(blockX, blockZ, blockY, 16,
                    OceanColumnPlanner.LITH_STRATA_SEED ^ 0x3DL);
            if (patch == 0) {
                return BlockRegistry.PARASITIC_MUD.get().defaultBlockState();
            }
            if (patch <= 4) {
                return BlockRegistry.NUTRIENT_RICH_MUD.get().defaultBlockState();
            }
            return BlockRegistry.MUD.get().defaultBlockState();
        }
        if (depth <= 2) {
            double silt = SoftMixNoise.valueNoise(blockX, blockZ, 12,
                    OceanColumnPlanner.LITH_REGION_SEED ^ 0x3DL);
            return silt > 0.62D
                    ? BlockRegistry.NUTRIENT_RICH_MUD.get().defaultBlockState()
                    : BlockRegistry.MUD.get().defaultBlockState();
        }
        return switch (strataIndex(blockX, blockZ, blockY, 16,
                OceanColumnPlanner.LITH_STRATA_SEED ^ 0x59L)) {
            case 0 -> BlockRegistry.NUTRIENT_RICH_MUD.get().defaultBlockState();
            case 1, 2 -> BlockRegistry.PACKED_MUD.get().defaultBlockState();
            case 3 -> BlockRegistry.SILTSTONE.get().defaultBlockState();
            case 15 -> BlockRegistry.FOSSIL_BED.get().defaultBlockState();
            default -> BlockRegistry.MUD.get().defaultBlockState();
        };
    }

    private static BlockState coralTreeStateFor(int blockX, int blockY, int blockZ, int capTop,
                                                TerrainModule terrain) {
        if (isCoralTrunk(blockX, blockZ, terrain) && blockY <= capTop + trunkHeight(blockX, blockZ)) {
            return ringedCoralState(blockX, blockZ, Direction.Axis.Y);
        }

        for (int dir = 0; dir < 4; dir++) {
            int nx = blockX + ((dir == 0) ? -1 : (dir == 1) ? 1 : 0);
            int nz = blockZ + ((dir == 2) ? -1 : (dir == 3) ? 1 : 0);
            if (!isCoralTrunk(nx, nz, terrain)) {
                continue;
            }
            int tHeight = trunkHeight(nx, nz);
            if (blockY > capTop + tHeight) {
                continue;
            }
            int yOffset = blockY - capTop;
            if (yOffset < 1) {
                continue;
            }
            long branchHash = SoftMixNoise.mix(nx, nz, 0xB2A4C3L ^ ((long) yOffset * 0x94D049BB133111EBL));
            int branchDir = (int) (branchHash & 0x3);
            if (branchDir != dir) {
                continue;
            }
            if ((yOffset & 1) == 0) {
                continue;
            }
            Direction.Axis axis = (dir <= 1) ? Direction.Axis.X : Direction.Axis.Z;
            return ringedCoralState(nx, nz, axis);
        }

        return WATER;
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

    private static BlockState capStateFor(OceanColumnPlanner.ColumnPlan plan,
                                          int blockX, int blockY, int blockZ, int capTop, int capBottom) {
        // A mud district turns its stretch of the reef shelf into a mud flat instead of a
        // sandy reef; the coral and jelly provinces keep their sand and limestone.
        if (familyFor(plan, blockX, blockY, blockZ) == ReefDescriptor.Family.MUD) {
            return mudFloorStateFor(blockX, blockY, blockZ, capTop);
        }
        int depth = capTop - blockY;

        if (depth == 0) {
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
        double region = SoftMixNoise.valueNoise(blockX, blockZ, 48, OceanColumnPlanner.LITH_REGION_SEED);
        double strata = SoftMixNoise.valueNoise(blockX, blockZ, 16, OceanColumnPlanner.LITH_STRATA_SEED);

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
        double drift = SoftMixNoise.valueNoise(blockX, blockZ, 24,
                OceanColumnPlanner.LITH_STRATA_SEED ^ seed);
        double phase = blockY * 0.35D + drift * 3.0D;
        long band = (long) Math.floor(phase);
        return Math.floorMod(band, variants);
    }
}
