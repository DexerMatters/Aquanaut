package com.dexer.aquanaut.common.item;

import com.dexer.aquanaut.common.FlotationHelper.FlotationDevice;

/**
 * The large handheld air bladder: the same device, and enough of it to climb.
 *
 * <p>
 * Where the small bladder holds its holder's depth, this one drives them up at
 * {@link FlotationDevice#FAST_ASCENT}. It is the same item model scaled up in
 * the hand, so the size is visible where it matters.
 */
public class LargeHandheldAirBladderItem extends HandheldAirBladderItem {

    public LargeHandheldAirBladderItem(Properties properties, int bubbleCount) {
        super(properties, bubbleCount);
    }

    @Override
    public FlotationDevice flotationDevice() {
        return FlotationDevice.FAST_ASCENT;
    }

    @Override
    protected String flotationTooltipKey() {
        return "tooltip.aquanaut.large_handheld_air_bladder.ascends";
    }
}
