package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.common.block.CrystalClusterBlock;
import com.dexer.aquanaut.common.block.DroopingSeaweedBlock;
import com.dexer.aquanaut.common.worldgen.crystalnest.CrystalNestField;
import com.dexer.aquanaut.common.worldgen.crystalnest.CrystalNestLattice;
import com.dexer.aquanaut.common.worldgen.crystalnest.CrystalNestSkin;
import com.dexer.aquanaut.common.worldgen.layers.MixEntry;
import com.dexer.aquanaut.common.worldgen.layers.OceanColumnPlanner;
import com.dexer.aquanaut.common.worldgen.layers.OceanGenSampler;
import com.dexer.aquanaut.common.worldgen.layers.OceanLayer;
import com.dexer.aquanaut.common.worldgen.layers.OceanLayerStack;
import com.dexer.aquanaut.common.worldgen.layers.SoftMixNoise;
import com.dexer.aquanaut.common.worldgen.layers.TerrainModule;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.Fluids;

import java.util.HashMap;
import java.util.Map;

/**
 * Binds the pure crystal-nest skinning pipeline to real chunk fill. One {@link Chunk}
 * is prepared per chunk during the terrain fill and answers state queries for the open
 * middle-sea band; anything it does not claim falls through to the regular column
 * planner.
 *
 * <p>
 * The lattice only grows where the Crystal Nest biome mix wins, and only while the reef
 * cap really seals the sea overhead — columns with a crack in their ceiling are left
 * open, so the geode field fades into the neighboring biomes instead of butting against
 * them and never hangs in the air below an opening.
 */
public final class CrystalNestTerrain {
    /**
     * Biome-mix weight where the lattice reaches full size: only deep inside a nest
     * district, so the geodes read as coherent cores rather than an all-covering skin.
     */
    private static final double WEIGHT_FULL = 0.5D;
    /**
     * Biome-mix weight below which the lattice is gone entirely: roughly where the nest
     * wins the district argument, so neighbors keep their own floors.
     */
    private static final double WEIGHT_FLOOR = 0.26D;
    /** Chunks fainter than this skip the whole pipeline. */
    private static final double MIN_STRENGTH = 0.05D;
    private static final int SKIN_PAD = CrystalNestSkin.PAD;
    private static final int SKIN_SIZE = CrystalNestSkin.SIZE;

    private CrystalNestTerrain() {
    }

    /**
     * Continuous weight of the Crystal Nest in the middle-sea biome mix, or 0 when the
     * active layer stack does not carry the biome.
     */
    public static double crystalWeight(OceanLayerStack stack, int quartX, int quartZ) {
        for (OceanLayer layer : stack.layers()) {
            int index = 0;
            for (MixEntry entry : layer.mix().entries()) {
                if (entry.biome().equals(CrystalNestPlacement.location())) {
                    return layer.mix().weightsAt(quartX, quartZ)[index];
                }
                index++;
            }
        }
        return 0.0D;
    }

    /**
     * The lattice strength of one column: the soft biome-mix weight, faded out at the
     * region border and scaled back by the dissolving ceiling. This is the transition
     * field that melts the crystal nest into its neighboring biomes — every factor is
     * continuous, so the lattice thins out gradually instead of stopping on a contour.
     */
    public static double strengthFor(double weight, double edgeStrength, double capOpenness) {
        double ramp = SoftMixNoise.smoothstep((weight - WEIGHT_FLOOR) / (WEIGHT_FULL - WEIGHT_FLOOR));
        return Math.min(1.0D, ramp) * edgeStrength * (1.0D - SoftMixNoise.clamp01(capOpenness));
    }

    /** Skins the crystal nest for one chunk, or {@code null} where the lattice never reaches. */
    public static Chunk build(ChunkAccess chunk, OceanGenSampler sampler, TerrainModule terrain,
                              com.dexer.aquanaut.common.worldgen.layers.ChunkTerrainBlend blend) {
        if (!terrain.enabled() || !sampler.anySupported()) {
            return null;
        }
        int minBuildHeight = chunk.getMinBuildHeight();
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int padded = SKIN_SIZE + SKIN_PAD * 2;

        OceanColumnPlanner.ColumnPlan[] plans = new OceanColumnPlanner.ColumnPlan[padded * padded];
        double[] strength = new double[padded * padded];
        double maxStrength = 0.0D;
        // One shared plan per column over the chunk plus one block of halo: the skin needs
        // real face information across the border, and reusing the chunk blend's plans keeps
        // them bit-identical to the ones the fill pass writes — at a fraction of the cost of
        // re-planning every halo column analytically.
        for (int px = 0; px < padded; px++) {
            int localX = px - SKIN_PAD;
            for (int pz = 0; pz < padded; pz++) {
                int localZ = pz - SKIN_PAD;
                OceanColumnPlanner.ColumnPlan plan = blend.planAt(localX, localZ);
                plans[px * padded + pz] = plan;
                double weight = blend.biomeWeightAtBlock(CrystalNestPlacement.location(),
                        minX + localX, minZ + localZ);
                double strengthHere = strengthFor(weight, plan.edgeStrength(), plan.capOpenness());
                strength[px * padded + pz] = strengthHere;
                maxStrength = Math.max(maxStrength, strengthHere);
            }
        }
        if (maxStrength < MIN_STRENGTH) {
            return null;
        }

        int yLo = Integer.MAX_VALUE;
        int yHi = Integer.MIN_VALUE;
        for (OceanColumnPlanner.ColumnPlan plan : plans) {
            yLo = Math.min(yLo, plan.cavityFloorY());
            yHi = Math.max(yHi, plan.capBand().bottomY() - 1);
        }
        yLo = Math.max(yLo, minBuildHeight);
        yHi = Math.min(yHi, minBuildHeight + chunk.getHeight() - 1);
        if (yHi < yLo) {
            return null;
        }
        int ySize = yHi - yLo + 1;

        CrystalNestLattice.SpanQuery span = new CrystalNestLattice.SpanQuery() {
            @Override
            public int floorY(double x, double z) {
                return OceanColumnPlanner.pureFloorY(terrain, minBuildHeight,
                        (int) Math.floor(x), (int) Math.floor(z));
            }

            @Override
            public int ceilingY(double x, double z) {
                return OceanColumnPlanner.ceilingY(terrain, minBuildHeight,
                        (int) Math.floor(x), (int) Math.floor(z));
            }
        };
        CrystalNestField field = new CrystalNestField(new CrystalNestLattice(span));

        CrystalNestSkin.SolidQuery legacySolid = (x, y, z) -> {
            OceanColumnPlanner.ColumnPlan plan = plans[(x - minX + SKIN_PAD) * padded + (z - minZ + SKIN_PAD)];
            return plan != null && isWholeSolid(
                    com.dexer.aquanaut.common.worldgen.layers.OceanColumnShading.stateForY(plan, y),
                    chunk, x, y, z);
        };
        CrystalNestSkin.StrengthQuery strengthQuery = (x, z) ->
                strength[(x - minX + SKIN_PAD) * padded + (z - minZ + SKIN_PAD)];

        CrystalNestSkin.Cell[] cells = CrystalNestSkin.skin(minX, minZ, yLo, ySize,
                legacySolid, field, strengthQuery);
        return new Chunk(cells, minX, minZ, yLo, ySize);
    }

    /** The skinned, decorated state of one chunk's open middle-sea band. */
    public static final class Chunk {
        private final CrystalNestSkin.Cell[] cells;
        private final int minX;
        private final int minZ;
        private final int yLo;
        private final int ySize;
        private final Map<CrystalNestSkin.Cell, BlockState> states = new HashMap<>();

        Chunk(CrystalNestSkin.Cell[] cells, int minX, int minZ, int yLo, int ySize) {
            this.cells = cells;
            this.minX = minX;
            this.minZ = minZ;
            this.yLo = yLo;
            this.ySize = ySize;
        }

        /**
         * The block this cell wants, or {@code null} when the regular middle-sea column
         * planner should decide (its own floor, cap, pillars, and plain water).
         */
        public BlockState stateAt(int x, int y, int z) {
            if (x < minX || x >= minX + SKIN_SIZE || z < minZ || z >= minZ + SKIN_SIZE
                    || y < yLo || y >= yLo + ySize) {
                return null;
            }
            CrystalNestSkin.Cell cell = cells[((x - minX) * SKIN_SIZE + (z - minZ)) * ySize + (y - yLo)];
            if (cell.kind() == CrystalNestSkin.Kind.LEGACY || cell.kind() == CrystalNestSkin.Kind.WATER) {
                return null;
            }
            return states.computeIfAbsent(cell, CrystalNestTerrain::stateFor);
        }
    }

    /**
     * Whole-terrain solidity for the legacy floor, cap and pillars: the substrate of the
     * algae mats and their tufts. Crystal never roots here — crystals grow only on 晶巢岩
     * (the lattice rock), see {@link CrystalNestSkin}'s crystal support rule. Translucent
     * blocks (glass, jelly, ice …) and partial ones (mats, slabs, carpets …) are not
     * whole solids either.
     */
    private static boolean isWholeSolid(BlockState state, ChunkAccess chunk, int x, int y, int z) {
        if (state.is(Blocks.WATER) || state.getFluidState().is(Fluids.WATER) || !state.canOcclude()) {
            return false;
        }
        BlockPos pos = new BlockPos(x, y, z);
        return state.isCollisionShapeFullBlock(chunk, pos) && !state.propagatesSkylightDown(chunk, pos);
    }

    private static BlockState stateFor(CrystalNestSkin.Cell cell) {
        return switch (cell.kind()) {
            case ROCK -> BlockRegistry.CRYSTAL_NEST_STONE.get().defaultBlockState();
            case DRUSE -> BlockRegistry.CRYSTAL_DRUSE.get().defaultBlockState();
            case ALGAE -> BlockRegistry.ALGAE_MAT.get().defaultBlockState();
            case TUFT -> waterlogged(BlockRegistry.ALGAE_TUFT.get().defaultBlockState());
            case SPROUT -> waterlogged(BlockRegistry.CRYSTAL_SPROUT.get().defaultBlockState());
            case COLUMN -> BlockRegistry.CRYSTAL_COLUMN.get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            case CLUSTER -> waterlogged(clusterState(cell.variant()))
                    .setValue(CrystalClusterBlock.FACING, Direction.from3DDataValue(cell.facing()));
            case FRINGE -> waterlogged(BlockRegistry.CRYSTAL_FRINGE.get().defaultBlockState())
                    .setValue(DroopingSeaweedBlock.PART, switch (cell.variant()) {
                        case CrystalNestSkin.FRINGE_BODY -> DroopingSeaweedBlock.SeaweedPart.BODY;
                        case CrystalNestSkin.FRINGE_TAIL -> DroopingSeaweedBlock.SeaweedPart.TAIL;
                        default -> DroopingSeaweedBlock.SeaweedPart.TOP;
                    });
            default -> Blocks.WATER.defaultBlockState();
        };
    }

    private static BlockState clusterState(int material) {
        return switch (material) {
            case CrystalNestSkin.CLUSTER_ROSE -> BlockRegistry.ROSE_CRYSTAL_CLUSTER.get().defaultBlockState();
            case CrystalNestSkin.CLUSTER_AMETHYST -> BlockRegistry.AMETHYST_CRYSTAL_CLUSTER.get().defaultBlockState();
            case CrystalNestSkin.CLUSTER_AQUA -> BlockRegistry.AQUA_CRYSTAL_CLUSTER.get().defaultBlockState();
            case CrystalNestSkin.CLUSTER_SMOKY -> BlockRegistry.SMOKY_CRYSTAL_CLUSTER.get().defaultBlockState();
            case CrystalNestSkin.CLUSTER_RESONANT -> BlockRegistry.RESONANT_CRYSTAL_CLUSTER.get().defaultBlockState();
            case CrystalNestSkin.CLUSTER_LIFE -> BlockRegistry.LIFE_GEM_CLUSTER.get().defaultBlockState();
            default -> BlockRegistry.WHITE_CRYSTAL_CLUSTER.get().defaultBlockState();
        };
    }

    private static BlockState waterlogged(BlockState state) {
        return state.setValue(BlockStateProperties.WATERLOGGED, true);
    }
}
