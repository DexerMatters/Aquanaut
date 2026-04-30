package com.dexer.aquanaut.common.item;

import com.dexer.aquanaut.common.diving.DivingEquipmentSlotType;
import net.minecraft.world.item.Item;

public final class DivingEquipmentItem extends Item {

    private final DivingEquipmentSlotType slotType;
    private final int tankAirBubbles;
    private final int maskRegenBonus;
    private final float underwaterSpeedMultiplier;

    public DivingEquipmentItem(Properties properties, DivingEquipmentSlotType slotType, int tankAirBubbles,
            int maskRegenBonus, float underwaterSpeedMultiplier) {
        super(properties);
        this.slotType = slotType;
        this.tankAirBubbles = Math.max(0, tankAirBubbles);
        this.maskRegenBonus = Math.max(0, maskRegenBonus);
        this.underwaterSpeedMultiplier = Math.max(1.0F, underwaterSpeedMultiplier);
    }

    public DivingEquipmentSlotType slotType() {
        return slotType;
    }

    public int tankAirBubbles() {
        return tankAirBubbles;
    }

    public int maskRegenBonus() {
        return maskRegenBonus;
    }

    public float underwaterSpeedMultiplier() {
        return underwaterSpeedMultiplier;
    }
}
