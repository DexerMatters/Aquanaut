package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.core.BlockRegistry;
import com.dexer.aquanaut.core.BiomeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

public final class CoralForestPillarFeature extends Feature<NoneFeatureConfiguration> {
    private static final Direction[] HORIZONTALS = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    public CoralForestPillarFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        if (!level.getBiome(origin).is(BiomeRegistry.CORAL_FOREST)) {
            return false;
        }

        int height = 10 + random.nextInt(8);
        float endRadius = 2.75F + random.nextFloat() * 1.75F;
        float waistRadius = 0.80F + random.nextFloat() * 0.90F;
        int radiusLimit = Mth.ceil(endRadius) + 1;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        List<BlockPos> outerSkinPositions = new ArrayList<>();
        boolean placedAny = false;

        for (int y = 0; y < height; y++) {
            float progress = height == 1 ? 0.0F : (float) y / (height - 1);
            float bell = 1.0F - Math.abs(progress * 2.0F - 1.0F);
            bell *= bell;
            float radius = Mth.lerp(bell, endRadius, waistRadius);

            for (int dx = -radiusLimit; dx <= radiusLimit; dx++) {
                for (int dz = -radiusLimit; dz <= radiusLimit; dz++) {
                    double distance = Math.sqrt((double) dx * dx + (double) dz * dz);
                    if (distance > radius + 0.35D) {
                        continue;
                    }
                    if (distance < radius - 1.10D && random.nextFloat() < 0.06F + bell * 0.08F) {
                        continue;
                    }

                    mutable.set(origin.getX() + dx, origin.getY() + y, origin.getZ() + dz);
                    if (mutable.getY() < level.getMinBuildHeight() || mutable.getY() >= level.getMaxBuildHeight()) {
                        continue;
                    }

                    boolean outerSkin = distance > radius - 0.50D;
                    level.setBlock(mutable, stoneState(random), 2);
                    if (outerSkin) {
                        outerSkinPositions.add(mutable.immutable());
                    }
                    placedAny = true;
                }
            }
        }

        growCoralSlabs(level, random, outerSkinPositions);
        return placedAny;
    }

    private static void growCoralSlabs(WorldGenLevel level, RandomSource random, List<BlockPos> outerSkin) {
        for (BlockPos skinPos : outerSkin) {
            if (random.nextFloat() >= 0.12F) {
                continue;
            }

            Direction facing = HORIZONTALS[random.nextInt(4)];
            BlockPos slabPos = skinPos.relative(facing);

            if (!level.getBlockState(slabPos).is(Blocks.WATER)) {
                continue;
            }

            BlockState slab = coralSlabState(random);
            level.setBlock(slabPos, slab, 2);

            int cluster = random.nextInt(4);
            if (cluster >= 2) {
                Direction side = HORIZONTALS[random.nextInt(4)];
                BlockPos neighbor = slabPos.relative(side);
                if (level.getBlockState(neighbor).is(Blocks.WATER)) {
                    level.setBlock(neighbor, sameCoralSlab(slab, random), 2);

                    if (cluster >= 3) {
                        Direction side2 = HORIZONTALS[random.nextInt(4)];
                        BlockPos neighbor2 = neighbor.relative(side2);
                        if (level.getBlockState(neighbor2).is(Blocks.WATER)) {
                            level.setBlock(neighbor2, sameCoralSlab(slab, random), 2);
                        }
                    }
                }
            }
        }
    }

    private static BlockState sameCoralSlab(BlockState original, RandomSource random) {
        SlabType type = random.nextBoolean() ? SlabType.BOTTOM : SlabType.TOP;
        return original.setValue(SlabBlock.TYPE, type);
    }

    private static BlockState coralSlabState(RandomSource random) {
        SlabType type = random.nextBoolean() ? SlabType.BOTTOM : SlabType.TOP;
        BlockState state = switch (random.nextInt(5)) {
            case 0 -> BlockRegistry.TUBE_CORAL_SLAB.get().defaultBlockState();
            case 1 -> BlockRegistry.BRAIN_CORAL_SLAB.get().defaultBlockState();
            case 2 -> BlockRegistry.BUBBLE_CORAL_SLAB.get().defaultBlockState();
            case 3 -> BlockRegistry.FIRE_CORAL_SLAB.get().defaultBlockState();
            default -> BlockRegistry.HORN_CORAL_SLAB.get().defaultBlockState();
        };
        return state.setValue(SlabBlock.TYPE, type).setValue(SlabBlock.WATERLOGGED, true);
    }

    private static BlockState stoneState(RandomSource random) {
        return switch (random.nextInt(6)) {
            case 0 -> BlockRegistry.SHALE.get().defaultBlockState();
            case 1 -> BlockRegistry.SHALE.get().defaultBlockState();
            case 2 -> BlockRegistry.LIMESTONE.get().defaultBlockState();
            case 3 -> Blocks.STONE.defaultBlockState();
            case 4 -> Blocks.TUFF.defaultBlockState();
            default -> Blocks.CALCITE.defaultBlockState();
        };
    }
}
