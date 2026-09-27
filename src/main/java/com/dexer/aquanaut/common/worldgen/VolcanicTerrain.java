package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.common.block.AshLayerBlock;
import com.dexer.aquanaut.common.worldgen.layers.MixEntry;
import com.dexer.aquanaut.common.worldgen.layers.OceanLayer;
import com.dexer.aquanaut.common.worldgen.layers.OceanLayerStack;
import com.dexer.aquanaut.common.worldgen.layers.SoftMixNoise;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Binds {@link VolcanoGeometry} to real block states: volcanic strata, fluted lava domes
 * and resurgent necks, crater lakes, sinter spring pools, sulfur pans,
 * the satellite scoria cones of the plain, breach rubble and the ash dusting. One
 * {@code Column} is prepared per block column during the terrain fill and answers
 * {@code stateForY} queries; anything it does not claim falls through to the regular
 * middle-sea column planner.
 */
public final class VolcanicTerrain {
    private static final long DUST_SEED = 0x41534821L; // "ASH!"
    private static final long VEIN_SEED = 0x53554C46L; // "SULF"
    private static final long RUBBLE_SEED = 0x52554242L; // "RUBB"
    private static final long DOME_SEED = 0x444F4D45L; // "DOME"

    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState BASALT = BlockRegistry.VOLCANIC_BASALT.get().defaultBlockState()
            .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
    private static final BlockState SCORIA = BlockRegistry.SCORIA.get().defaultBlockState();
    private static final BlockState AGGLOMERATE = BlockRegistry.VOLCANIC_AGGLOMERATE.get().defaultBlockState();
    private static final BlockState PUMICE = BlockRegistry.PUMICE.get().defaultBlockState();
    // Fine ash as settled dust layers: a falling block here would slide off the cones and
    // drift down through the water column the moment anything updates it.
    private static final BlockState ASH = BlockRegistry.ASH_LAYER.get().defaultBlockState()
            .setValue(AshLayerBlock.LAYERS, 8)
            .setValue(BlockStateProperties.WATERLOGGED, true);
    private static final BlockState SULFUR_CRUST = BlockRegistry.SULFUR_CRUST.get().defaultBlockState();
    private static final BlockState SINTER = BlockRegistry.SINTER.get().defaultBlockState();
    private static final BlockState ACID_ETCHED = BlockRegistry.ACID_ETCHED_BASALT.get().defaultBlockState();
    private static final BlockState OBSIDIAN_GLASS = BlockRegistry.OBSIDIAN_GLASS.get().defaultBlockState();
    private static final BlockState FUMAROLE = BlockRegistry.FUMAROLE.get().defaultBlockState()
            .setValue(BlockStateProperties.WATERLOGGED, true);

    private VolcanicTerrain() {
    }

    /**
     * Continuous weight of Brimstone Caldera in the middle-sea biome mix, or 0 when the
     * active layer stack does not carry the biome.
     */
    public static double brimstoneWeight(OceanLayerStack stack, int quartX, int quartZ) {
        for (OceanLayer layer : stack.layers()) {
            int index = 0;
            for (MixEntry entry : layer.mix().entries()) {
                if (entry.biome().equals(BrimstoneCalderaPlacement.location())) {
                    return layer.mix().weightsAt(quartX, quartZ)[index];
                }
                index++;
            }
        }
        return 0.0D;
    }

    /** The per-column shading plan, or {@code null} on columns outside the volcanic biome. */
    public static Column columnAt(OceanLayerStack stack, int blockX, int blockZ,
                                  int floorY, double edgeFade) {
        return columnAt(stack, blockX, blockZ, floorY, edgeFade, Double.POSITIVE_INFINITY);
    }

    /**
     * The per-column shading plan, or {@code null} on columns outside the volcanic biome.
     * {@code maxRelief} caps how far cones may rise above the floor so summits keep clear
     * water below the reef overhead.
     */
    public static Column columnAt(OceanLayerStack stack, int blockX, int blockZ,
                                  int floorY, double edgeFade, double maxRelief) {
        double weight = brimstoneWeight(stack, blockX >> 2, blockZ >> 2);
        double strength = VolcanoGeometry.strength(weight, edgeFade);
        if (strength <= 0.0D) {
            return null;
        }
        VolcanoGeometry.ColumnShape shape = VolcanoGeometry.shapeAt(blockX, blockZ, floorY, strength, maxRelief);
        // Satellite vents only erupt between the giants, never on an edifice's own flanks.
        VolcanoGeometry.StackShape satellite = shape.partOfEdifice()
                ? VolcanoGeometry.StackShape.NONE
                : VolcanoGeometry.stackShapeAt(blockX, blockZ, shape.surfaceY(), strength);
        return new Column(blockX, blockZ, floorY, strength, shape,
                satellite.topY(), satellite.axisFraction());
    }

    /**
     * One column of volcanic geology. {@code stateForY} returns {@code null} whenever the
     * regular middle-sea planner should keep authority (open water, cave ceiling, floor
     * below the dusting).
     */
    public record Column(int blockX, int blockZ, int floorY, double strength,
                         VolcanoGeometry.ColumnShape shape,
                         double stackTopY, double stackAxis) {

        public BlockState stateForY(int blockY) {
            VolcanoGeometry.Volcano volcano = shape.volcano();
            boolean vent = volcano != null && shape.parasite() == null
                    && shape.distance() <= VolcanoGeometry.domeRadius(volcano, strength);

            if (blockY <= Math.ceil(shape.surfaceY())) {
                if (blockY == (int) Math.ceil(shape.surfaceY())) {
                    return surfaceState(vent);
                }
                return subSurfaceState(blockY, vent);
            }

            // Satellite scoria cones and spatter ridges of the plain.
            if (stackAxis <= 1.0D && blockY <= Math.ceil(stackTopY)) {
                return stackState(blockY);
            }

            double rubbleTop = volcano == null ? 0.0D
                    : VolcanoGeometry.breachRubbleTop(volcano, shape.distance(),
                    VolcanoGeometry.plugTopY(volcano, floorY, strength), strength);
            if (rubbleTop > shape.surfaceY() && blockY <= rubbleTop) {
                long rubble = SoftMixNoise.mix(blockX ^ blockY, blockZ, RUBBLE_SEED);
                return (rubble & 0x7L) == 0L ? WATER : AGGLOMERATE;
            }

            if (shape.craterInterior()) {
                if (blockY <= Math.ceil(shape.fillY())) {
                    return craterFill();
                }
                if (blockY <= Math.ceil(shape.openTopY())) {
                    return WATER;
                }
            }
            return null;
        }

        /** Thin dusting of the plains and distal aprons: drifting ash on the sea floor. */
        public BlockState dustingState() {
            if (shape.partOfEdifice() && shape.distance() < shape.volcano().baseRadius() * 1.05D) {
                return null;
            }
            long dust = SoftMixNoise.mix(blockX, blockZ, DUST_SEED);
            double patch = ((dust >>> 11) & ((1L << 53) - 1)) / (double) (1L << 53);
            if (patch > 0.72D - strength * 0.18D) {
                int layers = 1 + (int) ((dust >>> 4) & 0x3L);
                return BlockRegistry.ASH_LAYER.get().defaultBlockState()
                        .setValue(AshLayerBlock.LAYERS, Math.min(layers, 8))
                        .setValue(BlockStateProperties.WATERLOGGED, true);
            }
            return null;
        }

        private BlockState surfaceState(boolean vent) {
            VolcanoGeometry.Volcano volcano = shape.volcano();
            if (shape.channelStrength() > 0.35D && volcano != null) {
                // The drained spill chute of a breached volcano: rock scoured and stained
                // by everything the caldera has been spilling down it.
                long chute = SoftMixNoise.mix(blockX, blockZ, VEIN_SEED ^ 0x5C17E5L);
                if ((chute & 0x3L) == 0L) {
                    return SULFUR_CRUST;
                }
                return switch (volcano.craterType()) {
                    case CRATER_LAKE -> ACID_ETCHED;
                    case HOT_SPRING -> SINTER;
                    case SULFUR_PAN -> SULFUR_CRUST;
                };
            }
            if (vent) {
                // A glassy, sulfur-seamed lava dome: obsidian crusts, sulfur fumaroles.
                long crust = SoftMixNoise.mix(blockX, blockZ, DOME_SEED);
                if ((crust & 0x7L) == 0L) {
                    return SULFUR_CRUST;
                }
                if ((crust & 0xFL) == 1L) {
                    return OBSIDIAN_GLASS;
                }
                return BASALT;
            }
            if (shape.craterInterior() && volcano != null) {
                return switch (volcano.craterType()) {
                    case HOT_SPRING -> SINTER;
                    case CRATER_LAKE -> ACID_ETCHED;
                    case SULFUR_PAN -> SULFUR_CRUST;
                };
            }
            double fumarole = VolcanoGeometry.fumaroleChance(blockX, blockZ, volcano, shape.distance());
            long roll = SoftMixNoise.mix(blockX, blockZ, VEIN_SEED ^ 0x1111L);
            double unit = ((roll >>> 11) & ((1L << 53) - 1)) / (double) (1L << 53);
            if (unit < fumarole * 0.6D) {
                return FUMAROLE;
            }
            int lithology = VolcanoGeometry.surfaceLithology(blockX, blockZ,
                    shape.distance(), volcano);
            return switch (lithology) {
                case 1 -> ASH;
                case 2 -> SULFUR_CRUST;
                case 3 -> AGGLOMERATE;
                default -> SCORIA;
            };
        }

        private BlockState subSurfaceState(int blockY, boolean vent) {
            double depth = Math.max(0.0D, Math.ceil(shape.surfaceY()) - blockY);
            long seam = SoftMixNoise.mix(blockX + blockY, blockZ, VEIN_SEED);
            if (vent) {
                // Columnar jointing in the conduit, shot through with sulfur and glassy bands.
                if (depth < 2.0D) {
                    return (seam & 0x3L) == 0L ? OBSIDIAN_GLASS : BASALT;
                }
                if ((seam & 0x1FL) == 0L) {
                    return SULFUR_CRUST;
                }
                return (seam & 0x3FL) == 1L ? OBSIDIAN_GLASS : BASALT;
            }
            VolcanoGeometry.Volcano volcano = shape.volcano();
            if (depth <= 3.0D) {
                return depth < 2.0D ? SCORIA : AGGLOMERATE;
            }
            if (volcano == null) {
                return depth < 9.0D ? SCORIA : BASALT;
            }
            if (depth < 16.0D && (seam & 0x2FL) == 0L) {
                return SULFUR_CRUST;
            }
            int band = VolcanoGeometry.strataIndex(volcano, shape.distance(), depth);
            return switch (band) {
                case 0 -> BASALT;
                case 1 -> SCORIA;
                case 2 -> AGGLOMERATE;
                default -> PUMICE;
            };
        }

        /** A satellite scoria cone: agglutinated core, bomb-laden shoulders, sulfur crown. */
        private BlockState stackState(int blockY) {
            long seam = SoftMixNoise.mix(blockX + blockY * 3, blockZ - blockY, VEIN_SEED ^ 0x51AC0DEL);
            if (blockY == (int) Math.ceil(stackTopY)) {
                return (seam & 0x3L) == 0L ? SULFUR_CRUST : SCORIA;
            }
            if ((seam & 0x1FL) == 0L) {
                return SULFUR_CRUST;
            }
            if (stackAxis < 0.42D) {
                return (seam & 0x7L) == 0L ? PUMICE : BASALT;
            }
            if (stackAxis < 0.72D) {
                return (seam & 0x3L) == 0L ? AGGLOMERATE : SCORIA;
            }
            return (seam & 0x7L) == 0L ? AGGLOMERATE : SCORIA;
        }

        /** Every pooled crater — lake or hot spring — holds the caldera's own water. */
        private BlockState craterFill() {
            return WATER;
        }
    }
}
