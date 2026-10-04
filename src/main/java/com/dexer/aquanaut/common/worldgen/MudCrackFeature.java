package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.core.BiomeRegistry;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A shallow meandering mud crack: the soft floor slumps along a fissure, exposing the varve
 * shale and fossil beds underneath. Mud and nutrient-rich mud give way to water over the
 * crack, so the floor reads as broken and drained rather than solid.
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

        BlockPos floor = findFloor(level, origin);
        int length = 6 + random.nextInt(7);
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double dirX = Math.cos(angle);
        double dirZ = Math.sin(angle);
        int depth = 2 + random.nextInt(2);

        boolean placed = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int step = 0; step < length; step++) {
            int cx = floor.getX() + Mth.floor(dirX * step);
            int cz = floor.getZ() + Mth.floor(dirZ * step);
            // The crack meanders a little as it runs.
            cx += random.nextInt(3) - 1;
            cz += random.nextInt(3) - 1;
            for (int w = -1; w <= 1; w++) {
                int px = cx + (w == 0 ? 0 : (dirZ > 0 ? 1 : -1));
                int pz = cz + (w == 0 ? 0 : (dirX > 0 ? -1 : 1));
                for (int d = 0; d < depth; d++) {
                    cursor.set(px, floor.getY() - d, pz);
                    BlockState state = level.getBlockState(cursor);
                    if (!isSoftFloor(state)) {
                        break;
                    }
                    if (d == depth - 1 && random.nextFloat() < 0.25F) {
                        level.setBlock(cursor, BlockRegistry.FOSSIL_BED.get().defaultBlockState(), 2);
                    } else if (d == depth - 1) {
                        level.setBlock(cursor, BlockRegistry.VARVE_SHALE.get().defaultBlockState(), 2);
                    } else {
                        level.setBlock(cursor, Blocks.WATER.defaultBlockState(), 2);
                    }
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

    private static BlockPos findFloor(WorldGenLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(origin.getX(), origin.getY() + 16,
                origin.getZ());
        while (mutable.getY() > origin.getY() - 16 && level.getFluidState(mutable).is(FluidTags.WATER)) {
            mutable.move(Direction.DOWN);
        }
        return mutable.immutable();
    }
}
