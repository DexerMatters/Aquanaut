package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.core.BiomeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Fissures through the reef cap of the jelly jungle.
 *
 * <p>The crack is a meandering walk, not a straight axis-aligned cut: the heading drifts
 * with every step, and both width and depth taper to zero at the two ends, so the fissure
 * is a lens that pinches out instead of a machined slot with blunt ends.</p>
 */
public final class JellyJungleCrackFeature extends Feature<NoneFeatureConfiguration> {
    /** Per-step heading jitter, radians. */
    private static final double WANDER = 0.9D;

    public JellyJungleCrackFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        if (!level.getBiome(origin).is(BiomeRegistry.JELLY_JUNGLE)) {
            return false;
        }

        double heading = random.nextDouble() * Math.PI * 2.0D;
        int length = 8 + random.nextInt(8);
        double halfWidth = 1.0D + random.nextInt(2);
        int maxDepth = 8 + random.nextInt(8);

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        boolean placedAny = false;
        // Two arms walk away from the origin in opposite directions and meet there, so
        // the fissure is one continuous curve with tapering ends.
        placedAny |= carveArm(level, random, mutable, origin, heading, length, halfWidth, maxDepth, false);
        placedAny |= carveArm(level, random, mutable, origin, heading + Math.PI,
                length, halfWidth, maxDepth, true);
        return placedAny;
    }

    private static boolean carveArm(WorldGenLevel level, RandomSource random,
                                    BlockPos.MutableBlockPos mutable, BlockPos origin,
                                    double startHeading, int length, double halfWidth,
                                    int maxDepth, boolean skipFirst) {
        double heading = startHeading;
        double centerX = origin.getX() + 0.5D;
        double centerZ = origin.getZ() + 0.5D;
        boolean placedAny = false;
        for (int step = 0; step <= length; step++) {
            if (step > 0 || !skipFirst) {
                // Lens taper: full width at the meeting point, pinched shut at the tip.
                double taper = Mth.sin((float) (Math.PI * 0.5D * (1.0D - (double) step / length)));
                double width = halfWidth * taper;
                int depth = 3 + Mth.floor(taper * maxDepth);
                if (width >= 0.35D) {
                    placedAny |= carveCrossSection(level, mutable, centerX, centerZ,
                            origin.getY(), heading, width, depth);
                }
            }
            heading += (random.nextDouble() - 0.5D) * WANDER * 2.0D;
            centerX += Math.cos(heading);
            centerZ += Math.sin(heading);
        }
        return placedAny;
    }

    private static boolean carveCrossSection(WorldGenLevel level, BlockPos.MutableBlockPos mutable,
                                             double centerX, double centerZ, int originY,
                                             double heading, double width, int depth) {
        boolean placedAny = false;
        // Lateral direction perpendicular to the heading; fractional coverage keeps the
        // verge ragged instead of stair-stepping in whole blocks.
        double perpX = -Math.sin(heading);
        double perpZ = Math.cos(heading);
        int reach = (int) Math.ceil(width);
        for (int lateral = -reach; lateral <= reach; lateral++) {
            if (Math.abs(lateral) > width + 0.15D) {
                continue;
            }
            int x = (int) Math.floor(centerX + perpX * lateral);
            int z = (int) Math.floor(centerZ + perpZ * lateral);
            for (int dy = 1; dy >= -depth; dy--) {
                int y = originY + dy;
                if (y <= level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
                    continue;
                }
                mutable.set(x, y, z);
                BlockState current = level.getBlockState(mutable);
                if (current.is(Blocks.BEDROCK)) {
                    continue;
                }
                level.setBlock(mutable, Blocks.WATER.defaultBlockState(), 2);
                placedAny = true;
            }
        }
        return placedAny;
    }
}
