package com.dexer.aquanaut.common.diving.inventory;

import com.dexer.aquanaut.common.diving.DivingEquipmentSlotType;

/**
 * Shared coordinates for diving inventory slots relative to InventoryMenu
 * left/top.
 */
public final class DivingInventoryLayout {

    private DivingInventoryLayout() {
    }

    public static final int SLOT_X = -30;
    public static final int MASK_Y = 8;
    public static final int SLOT_Y_STEP = 18;

    public static int slotY(DivingEquipmentSlotType slotType) {
        return switch (slotType) {
            case MASK -> MASK_Y;
            case TANK -> MASK_Y + SLOT_Y_STEP;
            case FLIPPERS -> MASK_Y + (SLOT_Y_STEP * 2);
        };
    }
}
