package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.core.BiomeRegistry;
import com.dexer.aquanaut.core.BlockRegistry;
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
 * A shallow meandering mud crack: the soft floor slumps along a fissure, exposing the fossil bed
 * underneath. The bed runs unbroken along the crack floor, so a crack reads as one drained seam
 * through the mud instead of a scatter of stray bones.
 */
public final class MudCrackFeature extends Feature<NoneFeatureConfiguration> {
    public MudCrackFeature() {
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
        if (floor == null || !isSoftFloor(level.getBlockState(floor))) {
            return false;
        }

        int length = 6 + random.nextInt(7);
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double dirX = Math.cos(angle);
        double dirZ = Math.sin(angle);
        int depth = 2 + random.nextInt(2);
        int sideX = dirZ > 0.0D ? 1 : -1;
        int sideZ = dirX > 0.0D ? -1 : 1;

        boolean placed = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int step = 0; step < length; step++) {
            int cx = floor.getX() + Mth.floor(dirX * step);
            int cz = floor.getZ() + Mth.floor(dirZ * step);
            // The crack meanders a little as it runs.
            cx += random.nextInt(3) - 1;
            cz += random.nextInt(3) - 1;
            for (int w = -1; w <= 1; w++) {
                int px = cx + w * sideX;
                int pz = cz + w * sideZ;
                for (int d = 0; d < depth; d++) {
                    cursor.set(px, floor.getY() - d, pz);
                    if (!isSoftFloor(level.getBlockState(cursor))) {
                        break;
                    }
                    level.setBlock(cursor,
                            d == depth - 1 ? BlockRegistry.FOSSIL_BED.get().defaultBlockState()
                                    : Blocks.WATER.defaultBlockState(),
                            2);
                    placed = true;
                }
            }
        }
        return placed;
    }

    private static boolean isSoftFloor(BlockState state) {
        return state.is(BlockRegistry.MUD.get())
                || state.is(BlockRegistry.NUTRIENT_RICH_MUD.get())
                || state.is(BlockRegistry.PARASITIC_MUD.get())
                || state.is(BlockRegistry.FOSSIL_BED.get());
    }
}
