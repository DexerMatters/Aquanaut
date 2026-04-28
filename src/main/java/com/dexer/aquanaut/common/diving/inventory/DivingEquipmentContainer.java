package com.dexer.aquanaut.common.diving.inventory;

import com.dexer.aquanaut.common.diving.DivingEquipmentHelper;
import com.dexer.aquanaut.common.diving.DivingEquipmentSlotType;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Lightweight container facade over the player's diving equipment attachments.
 */
public final class DivingEquipmentContainer implements Container {

    public static final int SLOT_COUNT = 3;

    private final Player owner;

    public DivingEquipmentContainer(Player owner) {
        this.owner = owner;
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        DivingEquipmentSlotType slotType = slotTypeAt(slot);
        if (slotType == null) {
            return ItemStack.EMPTY;
        }
        return DivingEquipmentHelper.getEquippedStack(owner, slotType);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }

        DivingEquipmentSlotType slotType = slotTypeAt(slot);
        if (slotType == null) {
            return ItemStack.EMPTY;
        }

        ItemStack existing = DivingEquipmentHelper.getEquippedStack(owner, slotType);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack removed = existing.copy();
        DivingEquipmentHelper.setEquippedStack(owner, slotType, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return removeItem(slot, 1);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        DivingEquipmentSlotType slotType = slotTypeAt(slot);
        if (slotType == null) {
            return;
        }

        if (stack == null || stack.isEmpty()) {
            DivingEquipmentHelper.setEquippedStack(owner, slotType, ItemStack.EMPTY);
            return;
        }

        ItemStack normalized = stack.copy();
        normalized.setCount(1);
        DivingEquipmentHelper.setEquippedStack(owner, slotType, normalized);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void setChanged() {
        // Attachment writes in setItem/removeItem are authoritative already.
    }

    @Override
    public boolean stillValid(Player player) {
        return player == owner && owner.isAlive();
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            setItem(i, ItemStack.EMPTY);
        }
    }

    private static DivingEquipmentSlotType slotTypeAt(int slot) {
        return switch (slot) {
            case 0 -> DivingEquipmentSlotType.MASK;
            case 1 -> DivingEquipmentSlotType.TANK;
            case 2 -> DivingEquipmentSlotType.FLIPPERS;
            default -> null;
        };
    }
}
