package com.dexer.aquanaut.common.mud;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A ball of mud enriched with organic matter. Scattering it on a plant pushes its growth the way
 * bone meal does, and four balls can be packed back into a bed of planting soil.
 */
public final class NutrientMudBallItem extends Item {
    public NutrientMudBallItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BonemealableBlock bonemeal)
                || !bonemeal.isValidBonemealTarget(level, pos, state)) {
            return super.useOn(context);
        }
        if (level instanceof ServerLevel server) {
            if (bonemeal.isBonemealSuccess(level, level.random, pos, state)) {
                bonemeal.performBonemeal(server, level.random, pos, state);
            }
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
