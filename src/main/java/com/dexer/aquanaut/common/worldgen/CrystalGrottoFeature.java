package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.common.block.DroopingSeaweedBlock;
import com.dexer.aquanaut.core.BiomeRegistry;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Crystal grottos (镜晶洞): a broken dome of varve and halite boulders leaning over a
 * still pool of brine mirror, its inward faces crusted with glittering halite druse,
 * the pool rim blooming with calcite quills and gypsum roses, salt fringe dripping
 * from the overhangs.
 */
public final class CrystalGrottoFeature extends Feature<NoneFeatureConfiguration> {
    public CrystalGrottoFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        if (!level.getBiome(origin).is(BiomeRegistry.BRINE_MIRROR_GORGE)) {
            return false;
        }

        BlockPos floor = findFloor(level, origin);
        int radius = 4 + random.nextInt(4);
        boolean placed = false;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > radius) {
                    continue;
                }
                double ring = distance / (radius + 0.5D);
                BlockPos ground = supportTopAt(level, floor.offset(dx, 2, dz), radius + 3);
                if (ground == null) {
                    continue;
                }

                if (ring < 0.35D) {
                    // The mirror pool: dead-still black brine at the grotto heart.
                    if (random.nextInt(5) != 0) {
                        level.setBlock(ground, BlockRegistry.BRINE_MIRROR.get().defaultBlockState(), 2);
                        placed = true;
                    }
                    if (ring < 0.2D && level.getFluidState(ground.above()).is(FluidTags.WATER)
                            && level.getBlockState(ground.above()).getShape(level, ground.above()).isEmpty()
                            && random.nextInt(4) == 0) {
                        level.setBlock(ground.above(), BlockRegistry.CALCITE_QUILL.get().defaultBlockState()
                                .setValue(BlockStateProperties.WATERLOGGED, true), 2);
                    }
                } else if (ring > 0.55D) {
                    // Boulder walls: varve and halite leaning inward into broken domes.
                    int mound = 1 + (int) Math.round((ring - 0.55D) / 0.45D * (2.0D + random.nextDouble() * 3.0D));
                    BlockPos.MutableBlockPos stack = ground.mutable();
                    for (int up = 1; up <= mound; up++) {
                        stack.set(ground.getX(), ground.getY() + up, ground.getZ());
                        if (!canReplace(level, stack)) {
                            break;
                        }
                        BlockState rock = switch (random.nextInt(4)) {
                            case 0 -> BlockRegistry.VARVE_SHALE.get().defaultBlockState();
                            case 1 -> BlockRegistry.HALITE_DRUSE.get().defaultBlockState();
                            case 2 -> BlockRegistry.HOPPER_HALITE.get().defaultBlockState();
                            default -> BlockRegistry.HALITE_CRUST.get().defaultBlockState();
                        };
                        level.setBlock(stack, rock, 2);
                        placed = true;
                        if (up == mound) {
                            hangFringe(level, stack.below(), random);
                        }
                    }
                    // Druse glazes the pool-facing side of the wall.
                    BlockPos face = new BlockPos(ground.getX() - (int) Math.signum(dx),
                            ground.getY() + 1, ground.getZ() - (int) Math.signum(dz));
                    if (random.nextInt(3) == 0 && canReplace(level, face)) {
                        level.setBlock(face, BlockRegistry.HALITE_DRUSE.get().defaultBlockState(), 2);
                    }
                } else {
                    // Rim flora where the pool meets the walls.
                    BlockPos bloom = ground.above();
                    if (level.getFluidState(bloom).is(FluidTags.WATER)
                            && level.getBlockState(bloom).getShape(level, bloom).isEmpty()
                            && random.nextInt(3) == 0) {
                        level.setBlock(bloom, random.nextBoolean()
                                ? BlockRegistry.GYPSUM_ROSE.get().defaultBlockState()
                                : BlockRegistry.HALITE_ROSETTE.get().defaultBlockState()
                                .setValue(BlockStateProperties.WATERLOGGED, true), 2);
                        placed = true;
                    }
                    if (random.nextInt(4) == 0) {
                        level.setBlock(ground, BlockRegistry.HALITE_DRUSE.get().defaultBlockState(), 2);
                    }
                }
            }
        }
        return placed;
    }

    private static void hangFringe(WorldGenLevel level, BlockPos under, RandomSource random) {
        if (random.nextInt(3) != 0) {
            return;
        }
        int height = 1 + random.nextInt(3);
        for (int offset = 1; offset <= height; offset++) {
            BlockPos pos = under.below(offset);
            if (!level.getFluidState(pos).is(FluidTags.WATER)) {
                return;
            }
            DroopingSeaweedBlock.SeaweedPart part =
                    offset == 1 ? DroopingSeaweedBlock.SeaweedPart.TOP
                            : offset == height ? DroopingSeaweedBlock.SeaweedPart.TAIL
                            : DroopingSeaweedBlock.SeaweedPart.BODY;
            level.setBlock(pos, BlockRegistry.SALT_FRINGE.get().defaultBlockState()
                    .setValue(DroopingSeaweedBlock.PART, part)
                    .setValue(BlockStateProperties.WATERLOGGED, true), 2);
        }
    }

    private static boolean canReplace(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getFluidState().is(FluidTags.WATER) || state.canBeReplaced();
    }

    private static BlockPos supportTopAt(WorldGenLevel level, BlockPos pos, int range) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        for (int i = 0; i < range; i++) {
            BlockState state = level.getBlockState(mutable);
            // Only full-height tops count as ground: thin covers would leave the flora
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
