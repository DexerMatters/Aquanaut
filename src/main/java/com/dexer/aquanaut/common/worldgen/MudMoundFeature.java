package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.core.BiomeRegistry;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Low, slumped mud mounds on the mud-zone floor. Soft sediment piles spread out instead of
 * standing up, so the profile is a rounded dome that flattens toward its rim — never the
 * vertical cliffs a rock feature would throw. Surfaces read as mud; the buried core banded
 * mudstone and varve shale.
 */
public final class MudMoundFeature extends Feature<NoneFeatureConfiguration> {
    public MudMoundFeature() {
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
        int radius = 3 + random.nextInt(4);
        int peak = 1 + random.nextInt(4);
        boolean placed = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > radius) {
                    continue;
                }
                int height = (int) Math.round(peak * (1.0D - (dist / radius) * (dist / radius)));
                if (height <= 0) {
                    continue;
                }
                int baseY = floor.getY();
                for (int dy = 1; dy <= height; dy++) {
                    int y = baseY + dy;
                    if (y >= level.getMaxBuildHeight()) {
                        break;
                    }
                    cursor.set(floor.getX() + dx, y, floor.getZ() + dz);
                    BlockState existing = level.getBlockState(cursor);
                    if (!existing.canBeReplaced() && !level.getFluidState(cursor).is(FluidTags.WATER)) {
                        break;
                    }
                    level.setBlock(cursor, moundState(level, cursor, random), 2);
                    placed = true;
                }
            }
        }
        return placed;
    }

    private static BlockState moundState(WorldGenLevel level, BlockPos pos, RandomSource random) {
        if (level.getFluidState(pos.above()).is(FluidTags.WATER)) {
            if (random.nextFloat() < 0.10F) {
                return BlockRegistry.NUTRIENT_RICH_MUD.get().defaultBlockState();
            }
            return BlockRegistry.MUD.get().defaultBlockState();
        }
        return random.nextFloat() < 0.35F
                ? BlockRegistry.VARVE_SHALE.get().defaultBlockState()
                : BlockRegistry.MUD.get().defaultBlockState();
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
