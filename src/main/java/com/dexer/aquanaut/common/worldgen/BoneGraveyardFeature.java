package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.core.BiomeRegistry;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The mud zone's landmark: the half-buried ribcage of some ancient giant. A spine of fossil bed
 * runs across the sediment with paired ribs arcing up and out of the mud -- something a diver can
 * see from a distance and swim over to.
 */
public final class BoneGraveyardFeature extends Feature<NoneFeatureConfiguration> {
    public BoneGraveyardFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        if (!level.getBiome(origin).is(BiomeRegistry.MUD_ZONE)) {
            return false;
        }
        BlockPos floor = MudZoneFloor.find(level, origin);
        if (floor == null) {
            return false;
        }

        int ribs = 4 + random.nextInt(4);
        int spacing = 2;
        double theta = random.nextDouble() * Math.PI * 2.0D;
        double spineX = Math.cos(theta);
        double spineZ = Math.sin(theta);
        double sideX = -spineZ;
        double sideZ = spineX;

        boolean placed = false;
        for (int r = 0; r < ribs; r++) {
            int bx = floor.getX() + (int) Math.round(spineX * r * spacing);
            int bz = floor.getZ() + (int) Math.round(spineZ * r * spacing);
            placed |= setBone(level, new BlockPos(bx, floor.getY() + 1, bz), random, false);

            int ribHeight = 2 + random.nextInt(4);
            for (int side = -1; side <= 1; side += 2) {
                for (int h = 1; h <= ribHeight; h++) {
                    int out = (int) Math.round(h * 0.85D);
                    int rx = bx + (int) Math.round(sideX * side * out);
                    int rz = bz + (int) Math.round(sideZ * side * out);
                    placed |= setBone(level, new BlockPos(rx, floor.getY() + 1 + h, rz), random, true);
                }
            }
        }
        return placed;
    }

    private static boolean setBone(WorldGenLevel level, BlockPos pos, RandomSource random, boolean allowMudFill) {
        if (!level.getBlockState(pos).canBeReplaced()) {
            return false;
        }
        boolean bone = !allowMudFill || random.nextFloat() > 0.22F;
        level.setBlock(pos, (bone ? BlockRegistry.FOSSIL_BED : BlockRegistry.MUD).get().defaultBlockState(), 2);
        return true;
    }
}
