package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.core.BiomeRegistry;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Black smoker fields: honeycomb hydrothermal vent complexes rather than lone pillars.
 * Every complex is a rubbly sulfide mound crowned by a choir of fluted, tapering chimneys
 * with bulbous mineral flanges, sulfur-frosted shoulders and a live vent mouth on the
 * tallest stack — the beehive morphology of a real vent field.
 */
public final class SmokerClusterFeature extends Feature<NoneFeatureConfiguration> {
    public SmokerClusterFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        if (!level.getBiome(origin).is(BiomeRegistry.BRIMSTONE_CALDERA)) {
            return false;
        }

        boolean placed = false;
        int complexes = 2 + random.nextInt(3);
        for (int i = 0; i < complexes; i++) {
            int ox = random.nextInt(17) - 8;
            int oz = random.nextInt(17) - 8;
            BlockPos floor = findFloor(level, origin.offset(ox, 0, oz));
            placed |= placeComplex(level, floor, random);
        }
        return placed;
    }

    /** A sulfide mound with a choir of fluted chimneys growing off its crest. */
    private static boolean placeComplex(WorldGenLevel level, BlockPos floor, RandomSource random) {
        if (!solidGround(level, floor)) {
            return false;
        }
        int moundRadius = 3 + random.nextInt(3);
        int moundHeight = 1 + random.nextInt(2);
        buildMound(level, floor, random, moundRadius, moundHeight);

        boolean placed = false;
        int spires = 2 + random.nextInt(4);
        for (int i = 0; i < spires; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double dist = random.nextDouble() * moundRadius * 0.72D;
            BlockPos column = floor.offset((int) Math.round(Math.cos(angle) * dist), 0,
                    (int) Math.round(Math.sin(angle) * dist));
            BlockPos base = supportTopAt(level, column.offset(0, moundHeight + 2, 0), 6);
            if (base == null) {
                continue;
            }
            placed |= buildSpire(level, base.above(), random, i == 0);
        }
        return placed;
    }

    /**
     * Rubbly sulfide mound: each cell stands on its own resolved ground so the skirt hugs
     * the real floor instead of hovering beside it, and every block is laid in open water.
     */
    private static void buildMound(WorldGenLevel level, BlockPos center, RandomSource random,
                                   int radius, int height) {
        BlockState crust = BlockRegistry.SULFUR_CRUST.get().defaultBlockState();
        BlockState scoria = BlockRegistry.SCORIA.get().defaultBlockState();
        BlockState agglomerate = BlockRegistry.VOLCANIC_AGGLOMERATE.get().defaultBlockState();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > radius + 0.35D) {
                    continue;
                }
                BlockPos ground = supportTopAt(level, center.offset(dx, 2, dz), 5);
                if (ground == null) {
                    continue;
                }
                int layers = (int) Math.round(height * (1.0D - d / (radius + 0.8D)));
                for (int i = 1; i <= layers; i++) {
                    BlockPos pos = ground.offset(0, i, 0);
                    if (!openWater(level, pos)) {
                        break;
                    }
                    boolean frost = i == layers && random.nextInt(4) == 0;
                    level.setBlock(pos, frost ? crust
                            : (random.nextInt(3) == 0 ? scoria : agglomerate), 2);
                }
            }
        }
    }

    /**
     * One fluted chimney: a columnar shaft that tapers from a 3x3 foot to a single block,
     * with four-lobed fluting, bulbous mineral flanges every third course and a frosted rim
     * of sulfur crust. The lead stack of the choir is crowned by a live vent mouth.
     */
    private static boolean buildSpire(WorldGenLevel level, BlockPos base, RandomSource random,
                                      boolean fuming) {
        int height = 4 + random.nextInt(9);
        BlockState pipe = BlockRegistry.VENT_CHIMNEY.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState flange = BlockRegistry.VOLCANIC_AGGLOMERATE.get().defaultBlockState();
        BlockState frost = BlockRegistry.SULFUR_CRUST.get().defaultBlockState();
        int built = 0;
        for (int y = 0; y < height; y++) {
            double radius = 0.55D + 1.6D * Math.pow(1.0D - (double) y / height, 0.85D);
            boolean bulge = y > 0 && y % 3 == 0;
            double widened = bulge ? radius + 0.8D : radius;
            int laid = 0;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d > widened + 1.05D) {
                        continue;
                    }
                    // Fluting: the cross-section is a four-lobed star, not a tube.
                    double theta = Math.atan2(dz, dx);
                    double flute = 1.0D + 0.22D * Math.sin(4.0D * theta + y * 0.7D);
                    if (d > widened * flute) {
                        continue;
                    }
                    BlockPos pos = base.offset(dx, y, dz);
                    if (!openWater(level, pos)) {
                        continue;
                    }
                    BlockState state;
                    if (d <= 1.05D) {
                        state = pipe;
                    } else if (d <= widened * 0.78D) {
                        state = flange;
                    } else {
                        state = frost;
                    }
                    level.setBlock(pos, state, 2);
                    laid++;
                }
            }
            if (laid == 0) {
                break;
            }
            built++;
        }
        if (built > 0 && fuming) {
            BlockPos mouth = base.offset(0, built, 0);
            if (openWater(level, mouth)) {
                level.setBlock(mouth, BlockRegistry.FUMAROLE.get().defaultBlockState()
                        .setValue(BlockStateProperties.WATERLOGGED, true), 2);
            }
        }
        return built > 0;
    }

    /** Open water with no partial cover in it: the only space a vent may grow into. */
    private static boolean openWater(WorldGenLevel level, BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.WATER)
                && level.getBlockState(pos).getShape(level, pos).isEmpty();
    }

    private static boolean solidGround(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isFaceSturdy(level, pos, Direction.UP)
                && state.getShape(level, pos).max(Direction.Axis.Y) >= 1.0D;
    }

    /**
     * The topmost full-height ground block within {@code range} below {@code pos}, or
     * {@code null} over open water. Volcanic slopes demand this per-cell check so mounds
     * and frost hug the real floor instead of hovering beside it.
     */
    private static BlockPos supportTopAt(WorldGenLevel level, BlockPos pos, int range) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        for (int i = 0; i < range; i++) {
            BlockState state = level.getBlockState(mutable);
            // Only full-height tops count as ground: thin covers would leave the skirt
            // hovering above their surface.
            if (state.isFaceSturdy(level, mutable, Direction.UP)
                    && state.getShape(level, mutable).max(Direction.Axis.Y) >= 1.0D) {
                return mutable.immutable();
            }
            mutable.move(Direction.DOWN);
        }
        return null;
    }

    private static BlockPos findFloor(WorldGenLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(origin.getX(), origin.getY() + 16,
                origin.getZ());
        while (mutable.getY() > origin.getY() - 24 && level.getFluidState(mutable).is(FluidTags.WATER)) {
            mutable.move(Direction.DOWN);
        }
        return mutable.immutable();
    }
}
