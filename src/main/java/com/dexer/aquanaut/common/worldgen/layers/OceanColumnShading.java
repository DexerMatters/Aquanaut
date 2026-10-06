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
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private OceanColumnShading() {
    }

    public static BlockState stateForY(OceanColumnPlanner.ColumnPlan plan, int blockY) {
        int blockX = plan.blockX();
        int blockZ = plan.blockZ();
        // The water-world spawn island claims its columns outright: the seamount plugs the
        // chamber, so there is no cavity, reef slab or abyss left to shade — solid land up to
        // the plateau floor, water above it.
        if (plan.island()) {
            return islandStateFor(plan, blockY);
        }
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
            return capStateFor(blockX, blockY, blockZ, capTop, capBottom);
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

    /**
     * The water-world spawn island. The plateau core ({@code mask >= 1}) is vanilla land:
     * grass over three layers of dirt over limestone and the regional lithology, flat for
     * building and able to hold trees. The beach flanks ({@code mask < 1}) keep the coral-sand
     * shore. Below the surface, seeded cave voids open inside the solid seamount — strictly
     * dry by construction (see {@link com.dexer.aquanaut.common.worldgen.blend.SpawnIslandCaves}).
     */
    private static BlockState islandStateFor(OceanColumnPlanner.ColumnPlan plan, int blockY) {
        int floorY = plan.cavityFloorY();
        if (blockY > floorY) {
            return WATER;
        }
        if (plan.islandMask() >= 1.0D
                && blockY <= floorY - com.dexer.aquanaut.common.worldgen.blend.SpawnIslandCaves.SURFACE_PROTECT_MARGIN
                && com.dexer.aquanaut.common.worldgen.blend.SpawnIslandCaves.isCave(
                        plan.islandSeed(), plan.blockX(), blockY, plan.blockZ())) {
            return AIR;
        }
        int depth = floorY - blockY;
        if (plan.islandMask() >= 1.0D) {
            // The stony-shore region: bare rock with exposed low-tier ore veins and small
            // lava ponds; the vanilla surface rule (stony_shore branch) lays the gravel.
            if (com.dexer.aquanaut.common.worldgen.blend.SpawnIslandMask.stoneShoreWeight(
                    plan.islandSeed(), plan.blockX(), plan.blockZ()) >= 0.5D) {
                if (com.dexer.aquanaut.common.worldgen.blend.SpawnIslandMask.lavaPoolAt(
                        plan.islandSeed(), plan.blockX(), plan.blockZ()) && depth <= 1) {
                    return Blocks.LAVA.defaultBlockState();
                }
                if (depth == 0) {
                    double ore = com.dexer.aquanaut.common.worldgen.blend.SpawnIslandMask.oreSpeckleAt(
                            plan.islandSeed(), plan.blockX(), plan.blockZ());
                    if (ore > 0.62D) {
                        return Blocks.COAL_ORE.defaultBlockState();
                    }
                    if (ore > 0.42D) {
                        return Blocks.IRON_ORE.defaultBlockState();
                    }
                    if (ore > 0.26D) {
                        return Blocks.COPPER_ORE.defaultBlockState();
                    }
                    return Blocks.STONE.defaultBlockState();
                }
                if (depth <= 3) {
                    return Blocks.STONE.defaultBlockState();
                }
                return floorStateFor(plan.blockX(), blockY, plan.blockZ(), floorY);
            }
            // The broad building plateau: vanilla-style grassland profile, trees take root
            // here; seeded sand patches speckle the grass so the ground is not a perfect disc.
            if (depth == 0) {
                if (com.dexer.aquanaut.common.worldgen.blend.SpawnIslandMask.sandPatchAt(
                        plan.islandSeed(), plan.blockX(), plan.blockZ())) {
                    return BlockRegistry.CORAL_SAND.get().defaultBlockState();
                }
                return Blocks.GRASS_BLOCK.defaultBlockState();
            }
            if (depth <= 3) {
                return Blocks.DIRT.defaultBlockState();
            }
            if (depth <= 4) {
                return BlockRegistry.LIMESTONE.get().defaultBlockState();
            }
            return floorStateFor(plan.blockX(), blockY, plan.blockZ(), floorY);
        }
        // The beach flanks keep the emerged-reef identity: coral sand over limestone.
        if (depth == 0) {
            double mud = SoftMixNoise.valueNoise(plan.blockX(), plan.blockZ(), 12, 0x5A4DL);
            if (mud > 0.72D) {
                return BlockRegistry.NUTRIENT_RICH_MUD.get().defaultBlockState();
            }
            return BlockRegistry.CORAL_SAND.get().defaultBlockState();
        }
        if (depth <= 2) {
            return BlockRegistry.CORAL_SAND.get().defaultBlockState();
        }
        if (depth <= 4) {
            return BlockRegistry.LIMESTONE.get().defaultBlockState();
        }
        return floorStateFor(plan.blockX(), blockY, plan.blockZ(), floorY);
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
        ReefDescriptor.Family family = ReefDescriptor.Family.VALUES[
                plan.contact().pickFamily(plan.reef().familyWeights(),
                        blockX, blockY, blockZ, OceanColumnPlanner.REEF_DITHER_SEED)];
        return switch (family) {
            // Volcanic ground: the monolithic concrete apron.
            case VOLCANIC -> BlockRegistry.VOLCANIC_AGGLOMERATE.get().defaultBlockState();
            // The brine gorge: varve shale with halite-crust lenses.
            case HALITE -> brineFloorStateFor(blockX, blockY, blockZ, plan.cavityFloorY());
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

    private static BlockState capStateFor(int blockX, int blockY, int blockZ, int capTop, int capBottom) {
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
