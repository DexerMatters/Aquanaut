package com.dexer.aquanaut.common.item;

import java.util.List;

import com.dexer.aquanaut.common.FlotationHelper.FlotationDevice;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * The handheld air bladder: an air supply, and a flotation device.
 *
 * <p>
 * Holding one in either hand holds the holder's depth -- see
 * {@code HandheldAirBladderEvents} for the tick rule and
 * {@link LargeHandheldAirBladderItem} for the variant that climbs instead.
 */
public class HandheldAirBladderItem extends AirSupplyItem {

    public HandheldAirBladderItem(Properties properties, int bubbleCount) {
        super(properties, bubbleCount);
    }

    /** What this bladder does for whoever holds it. */
    public FlotationDevice flotationDevice() {
        return FlotationDevice.HOLD_DEPTH;
    }

    /** The strongest flotation either hand is holding, or {@code NONE}. */
    public static FlotationDevice deviceInHand(LivingEntity entity) {
        return deviceOf(entity.getMainHandItem()).stronger(deviceOf(entity.getOffhandItem()));
    }

    private static FlotationDevice deviceOf(ItemStack stack) {
        return stack.getItem() instanceof HandheldAirBladderItem bladder
                ? bladder.flotationDevice()
                : FlotationDevice.NONE;
    }

    /** Translation key describing what the held bladder does. */
    protected String flotationTooltipKey() {
        return "tooltip.aquanaut.handheld_air_bladder.floats";
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(flotationTooltipKey()).withStyle(ChatFormatting.AQUA));
    }
}
