package com.dexer.aquanaut.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * 嗜热菌样本 — a sealed swatch of a thermophilic microbial mat. Pressed onto warm rock it
 * seeds the mat there.
 *
 * <p>The mats grow in the world and are kept out of the creative inventory, so a sample is
 * how a player plants one by hand. It needs the same footing the mat itself needs (rock
 * with a sturdy top, snow, or more mat) and takes on the waterlogging of whatever it is
 * seeded into.</p>
 */
public final class ThermophilicSampleItem extends Item {
    private final Block mat;

    public ThermophilicSampleItem(Block mat, Item.Properties properties) {
        super(properties);
        this.mat = mat;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos target = context.getClickedPos().relative(context.getClickedFace());
        BlockPos basePos = target.below();
        BlockState ground = level.getBlockState(basePos);
        BlockState current = level.getBlockState(target);
        boolean supported = ground.isFaceSturdy(level, basePos, Direction.UP)
                || ground.is(mat) || ground.is(Blocks.SNOW);
        boolean open = current.canBeReplaced() || current.getFluidState().is(FluidTags.WATER);
        if (!supported || !open) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            BlockState seeded = mat.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,
                    current.getFluidState().is(FluidTags.WATER));
            level.setBlock(target, seeded, 3);
            Player player = context.getPlayer();
            if (player == null || !player.isCreative()) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
