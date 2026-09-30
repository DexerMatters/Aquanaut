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
 * the satellite scoria cones of the plain, breach rubble and the ash dusting.
 *
 * <p>Planning and shading are deliberately separate: {@link VolcanoGeometry#columnPlan}
 * composes the pure per-column {@link VolcanoGeometry.VolcanicColumnPlan} (testable
 * without a Minecraft runtime), and this class only answers {@code stateForY} queries
 * against such a plan during the chunk fill. Anything the plan does not claim falls
 * through to the regular middle-sea column planner.</p>
 */
public final class VolcanicTerrain {
    private static final long DUST_SEED = 0x41534821L; // "ASH!"
    private static final long VEIN_SEED = 0x53554C46L; // "SULF"
    private static final long RUBBLE_SEED = 0x52554242L; // "RUBB"
    private static final long DOME_SEED = 0x444F4D45L; // "DOME"

    /**
     * Lazy block-state holder: the planning half of the volcanic pipeline must stay
     * loadable in a bare unit-test JVM without Minecraft's block classes, so no
     * {@code BlockState} is resolved until a state query actually runs.
     */
    private static final class States {
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
    }

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

    /**
     * The shaded state of one height in one volcanic column, or {@code null} whenever the
     * regular middle-sea planner should keep authority (open water, cave ceiling, floor
     * below the dusting).
     */
    public static BlockState stateForY(VolcanoGeometry.VolcanicColumnPlan plan, int blockY) {
        VolcanoGeometry.ColumnShape shape = plan.shape();
        VolcanoGeometry.Volcano volcano = shape.volcano();
        boolean vent = volcano != null && shape.parasite() == null
                && shape.distance() <= VolcanoGeometry.domeRadius(volcano, plan.strength());

        if (blockY <= Math.ceil(shape.surfaceY())) {
            if (blockY == (int) Math.ceil(shape.surfaceY())) {
                return surfaceState(plan, vent);
            }
            return subSurfaceState(plan, blockY, vent);
        }

        // Satellite scoria cones and spatter ridges of the plain.
        if (plan.hasStack() && blockY <= Math.ceil(plan.stackTopY())) {
            return stackState(plan, blockY);
        }

        double rubbleTop = volcano == null ? 0.0D
                : VolcanoGeometry.breachRubbleTop(volcano, shape.distance(),
                        VolcanoGeometry.plugTopY(volcano, plan.floorY(), plan.strength()),
                        plan.strength());
        if (rubbleTop > shape.surfaceY() && blockY <= rubbleTop) {
            long rubble = SoftMixNoise.mix(plan.blockX() ^ blockY, plan.blockZ(), RUBBLE_SEED);
            return (rubble & 0x7L) == 0L ? States.WATER : States.AGGLOMERATE;
        }

        if (shape.craterInterior()) {
            if (blockY <= Math.ceil(shape.fillY())) {
                // Every pooled crater — lake or hot spring — holds the caldera's own water.
                return States.WATER;
            }
            if (blockY <= Math.ceil(shape.openTopY())) {
                return States.WATER;
            }
        }
        return null;
    }

    /** Thin dusting of the plains and distal aprons: drifting ash on the sea floor. */
    public static BlockState dustingState(VolcanoGeometry.VolcanicColumnPlan plan) {
        VolcanoGeometry.ColumnShape shape = plan.shape();
        if (shape.partOfEdifice() && shape.distance() < shape.volcano().baseRadius() * 1.05D) {
            return null;
        }
        long dust = SoftMixNoise.mix(plan.blockX(), plan.blockZ(), DUST_SEED);
        double patch = ((dust >>> 11) & ((1L << 53) - 1)) / (double) (1L << 53);
        if (patch > 0.72D - plan.strength() * 0.18D) {
            int layers = 1 + (int) ((dust >>> 4) & 0x3L);
            return BlockRegistry.ASH_LAYER.get().defaultBlockState()
                    .setValue(AshLayerBlock.LAYERS, Math.min(layers, 8))
                    .setValue(BlockStateProperties.WATERLOGGED, true);
        }
        return null;
    }

    private static BlockState surfaceState(VolcanoGeometry.VolcanicColumnPlan plan, boolean vent) {
        VolcanoGeometry.ColumnShape shape = plan.shape();
        VolcanoGeometry.Volcano volcano = shape.volcano();
        if (shape.channelStrength() > 0.35D && volcano != null) {
            // The drained spill chute of a breached volcano: rock scoured and stained
            // by everything the caldera has been spilling down it.
            long chute = SoftMixNoise.mix(plan.blockX(), plan.blockZ(), VEIN_SEED ^ 0x5C17E5L);
            if ((chute & 0x3L) == 0L) {
                return States.SULFUR_CRUST;
            }
            return switch (volcano.craterType()) {
                case CRATER_LAKE -> States.ACID_ETCHED;
                case HOT_SPRING -> States.SINTER;
                case SULFUR_PAN -> States.SULFUR_CRUST;
            };
        }
        if (vent) {
            // A glassy, sulfur-seamed lava dome: obsidian crusts, sulfur fumaroles.
            long crust = SoftMixNoise.mix(plan.blockX(), plan.blockZ(), DOME_SEED);
            if ((crust & 0x7L) == 0L) {
                return States.SULFUR_CRUST;
            }
            if ((crust & 0xFL) == 1L) {
                return States.OBSIDIAN_GLASS;
            }
            return States.BASALT;
        }
        if (shape.craterInterior() && volcano != null) {
            return switch (volcano.craterType()) {
                case HOT_SPRING -> States.SINTER;
                case CRATER_LAKE -> States.ACID_ETCHED;
                case SULFUR_PAN -> States.SULFUR_CRUST;
            };
        }
        double fumarole = VolcanoGeometry.fumaroleChance(plan.blockX(), plan.blockZ(), volcano,
                shape.distance());
        long roll = SoftMixNoise.mix(plan.blockX(), plan.blockZ(), VEIN_SEED ^ 0x1111L);
        double unit = ((roll >>> 11) & ((1L << 53) - 1)) / (double) (1L << 53);
        if (unit < fumarole * 0.6D) {
            return States.FUMAROLE;
        }
        int lithology = VolcanoGeometry.surfaceLithology(plan.blockX(), plan.blockZ(),
                shape.distance(), volcano);
        return switch (lithology) {
            case 1 -> States.ASH;
            case 2 -> States.SULFUR_CRUST;
            case 3 -> States.AGGLOMERATE;
            default -> States.SCORIA;
        };
    }

    private static BlockState subSurfaceState(VolcanoGeometry.VolcanicColumnPlan plan, int blockY,
                                              boolean vent) {
        VolcanoGeometry.ColumnShape shape = plan.shape();
        double depth = Math.max(0.0D, Math.ceil(shape.surfaceY()) - blockY);
        long seam = SoftMixNoise.mix(plan.blockX() + blockY, plan.blockZ(), VEIN_SEED);
        if (vent) {
            // Columnar jointing in the conduit, shot through with sulfur and glassy bands.
            if (depth < 2.0D) {
                return (seam & 0x3L) == 0L ? States.OBSIDIAN_GLASS : States.BASALT;
            }
            if ((seam & 0x1FL) == 0L) {
                return States.SULFUR_CRUST;
            }
            return (seam & 0x3FL) == 1L ? States.OBSIDIAN_GLASS : States.BASALT;
        }
        VolcanoGeometry.Volcano volcano = shape.volcano();
        if (depth <= 3.0D) {
            return depth < 2.0D ? States.SCORIA : States.AGGLOMERATE;
        }
        if (volcano == null) {
            return depth < 9.0D ? States.SCORIA : States.BASALT;
        }
        if (depth < 16.0D && (seam & 0x2FL) == 0L) {
            return States.SULFUR_CRUST;
        }
        int band = VolcanoGeometry.strataIndex(volcano, shape.distance(), depth);
        return switch (band) {
            case 0 -> States.BASALT;
            case 1 -> States.SCORIA;
            case 2 -> States.AGGLOMERATE;
            default -> States.PUMICE;
        };
    }

    /** A satellite scoria cone: agglutinated core, bomb-laden shoulders, sulfur crown. */
    private static BlockState stackState(VolcanoGeometry.VolcanicColumnPlan plan, int blockY) {
        long seam = SoftMixNoise.mix(plan.blockX() + blockY * 3, plan.blockZ() - blockY,
                VEIN_SEED ^ 0x51AC0DEL);
        if (blockY == (int) Math.ceil(plan.stackTopY())) {
            return (seam & 0x3L) == 0L ? States.SULFUR_CRUST : States.SCORIA;
        }
        if ((seam & 0x1FL) == 0L) {
            return States.SULFUR_CRUST;
        }
        if (plan.stackAxis() < 0.42D) {
            return (seam & 0x7L) == 0L ? States.PUMICE : States.BASALT;
        }
        if (plan.stackAxis() < 0.72D) {
            return (seam & 0x3L) == 0L ? States.AGGLOMERATE : States.SCORIA;
        }
        return (seam & 0x7L) == 0L ? States.AGGLOMERATE : States.SCORIA;
    }
}
