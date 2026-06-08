package com.dexer.aquanaut.common.inventory.aquarium;

import com.dexer.aquanaut.core.MenuRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class AquariumContainerMenu extends AbstractContainerMenu {

    public static final int AQUARIUM_COLS = 9;
    public static final int AQUARIUM_ROWS = 2;

    private static final int HOTBAR_COUNT = 9;
    private static final int MAIN_INV_COUNT = 27;

    static final int MAIN_START = 0;
    static final int MAIN_END = MAIN_START + MAIN_INV_COUNT;
    static final int HOTBAR_START = MAIN_END;
    static final int HOTBAR_END = HOTBAR_START + HOTBAR_COUNT;

    public static final int AQUARIUM_GRID_Y = 30;
    public static final int MAIN_INV_Y = 84;
    public static final int HOTBAR_Y = 142;

    public AquariumContainerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, (RegistryFriendlyByteBuf) null);
    }

    public AquariumContainerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(MenuRegistry.AQUARIUM.get(), containerId);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory,
                        col + row * 9 + HOTBAR_COUNT,
                        8 + col * 18, MAIN_INV_Y + row * 18));
            }
        }

        for (int col = 0; col < HOTBAR_COUNT; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, HOTBAR_Y));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (!slot.hasItem()) {
            return result;
        }

        ItemStack stackInSlot = slot.getItem();
        result = stackInSlot.copy();

        if (index >= MAIN_START && index < MAIN_END) {
            if (!this.moveItemStackTo(stackInSlot, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= HOTBAR_START && index < HOTBAR_END) {
            if (!this.moveItemStackTo(stackInSlot, MAIN_START, MAIN_END, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stackInSlot.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY, result);
        } else {
            slot.setChanged();
        }

        if (stackInSlot.getCount() == result.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stackInSlot);
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
