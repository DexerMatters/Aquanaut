package com.dexer.aquanaut.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

import com.dexer.aquanaut.core.AttachmentRegistry;

public class AirSupplyItem extends Item {

    private final int airSupply;

    public AirSupplyItem(Properties properties, int airSupply) {
        super(properties);
        this.airSupply = airSupply;
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
        int bubbles = Math.max(1, airSupply / 20);
        tooltip.add(Component.translatable("tooltip.aquanaut.air_supply", bubbles).withStyle(ChatFormatting.AQUA));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player && !level.isClientSide) {
            applyAirSupply((ServerPlayer) player);
        }
        return super.finishUsingItem(stack, level, entity);
    }

    private void applyAirSupply(ServerPlayer player) {
        int currentExtra = player.getData(AttachmentRegistry.EXTRA_AIR_SUPPLY.get());
        int maxExtra = player.getData(AttachmentRegistry.MAX_EXTRA_AIR_SUPPLY.get());
        int vanillaMax = player.getMaxAirSupply();

        if (maxExtra <= 0) {
            maxExtra = vanillaMax;
            player.setData(AttachmentRegistry.MAX_EXTRA_AIR_SUPPLY.get(), maxExtra);
        }

        int newExtra = Math.min(currentExtra + airSupply, maxExtra);
        player.setData(AttachmentRegistry.EXTRA_AIR_SUPPLY.get(), newExtra);
    }
}
