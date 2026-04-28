package com.dexer.aquanaut.common.diving.inventory;

import com.dexer.aquanaut.common.diving.DivingEquipmentSlotType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A real menu slot used by InventoryMenu, restricted to one diving equipment
 * type.
 */
public final class DivingEquipmentMenuSlot extends Slot {

    private final DivingEquipmentSlotType slotType;

    public DivingEquipmentMenuSlot(DivingEquipmentContainer container, DivingEquipmentSlotType slotType, int x, int y) {
        super(container, slotType.toIndex(), x, y);
        this.slotType = slotType;
    }

    public DivingEquipmentSlotType slotType() {
        return slotType;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return slotType.accepts(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }

    @Override
    public boolean mayPickup(Player player) {
        return true;
    }
}
