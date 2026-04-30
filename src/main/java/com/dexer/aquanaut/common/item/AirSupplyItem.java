package com.dexer.aquanaut.common.item;

import com.dexer.aquanaut.common.AirSupplyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

public class AirSupplyItem extends Item {

    private final int bubbleCount;

    public AirSupplyItem(Properties properties, int bubbleCount) {
        super(properties);
        this.bubbleCount = Math.max(1, bubbleCount);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getFoodProperties(player) != null) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.aquanaut.air_supply", bubbleCount).withStyle(ChatFormatting.AQUA));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player && !level.isClientSide) {
            applyAirSupply(player);
        }
        return super.finishUsingItem(stack, level, entity);
    }

    private void applyAirSupply(Player player) {
        // Air food never increases capacity. It only refills within the current max.
        int airTicks = AirSupplyHelper.bubblesToAirTicks(bubbleCount);
        AirSupplyHelper.addAir(player, airTicks);
    }
}
