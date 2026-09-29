package com.dexer.aquanaut.common.inventory;

import com.dexer.aquanaut.common.block.entity.PhotoRinsingBasinBlockEntity;
import com.dexer.aquanaut.core.ItemRegistry;
import com.dexer.aquanaut.core.MenuRegistry;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Three-slot menu: exposed film + developing salts -> developed photograph. */
public final class PhotoRinsingMenu extends AbstractContainerMenu {
    private static final int BASIN_SLOTS = 3;
    private static final int PLAYER_START = BASIN_SLOTS;
    private static final int PLAYER_END = PLAYER_START + 36;

    public static final int MAIN_INV_Y = 84;
    public static final int HOTBAR_Y = 142;

    private final Container container;
    private final ContainerData data;

    public PhotoRinsingMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf ignored) {
        this(containerId, playerInventory, new SimpleContainer(BASIN_SLOTS), new SimpleContainerData(2));
    }

    public PhotoRinsingMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(MenuRegistry.PHOTO_RINSING_BASIN.get(), containerId);
        checkContainerSize(container, BASIN_SLOTS);
        checkContainerDataCount(data, 2);
        this.container = container;
        this.data = data;
        container.startOpen(playerInventory.player);

        addSlot(new Slot(container, PhotoRinsingBasinBlockEntity.INPUT_SLOT, 24, 22) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ItemRegistry.EXPOSED_FILM.get());
            }
        });
        addSlot(new Slot(container, PhotoRinsingBasinBlockEntity.SALTS_SLOT, 24, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ItemRegistry.BRINE_DEVELOPING_SALTS.get());
            }
        });
        addSlot(new Slot(container, PhotoRinsingBasinBlockEntity.OUTPUT_SLOT, 136, 34) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        8 + col * 18, MAIN_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, HOTBAR_Y));
        }
        addDataSlots(data);
    }

    public int progressPixels(int width) {
        return Math.min(width, this.data.get(0) * width / Math.max(1, this.data.get(1)));
    }

    /** Development progress in the range 0..1. */
    public float progressFraction() {
        int total = Math.max(1, this.data.get(1));
        return Math.min(1.0F, (float) this.data.get(0) / total);
    }

    /** The latent image carried by the film sitting in the input slot, if any. */
    public java.util.Optional<com.dexer.aquanaut.common.item.DevelopedPhotoItem.PhotoData> filmPhoto() {
        return com.dexer.aquanaut.common.item.DevelopedPhotoItem
                .photo(this.container.getItem(PhotoRinsingBasinBlockEntity.INPUT_SLOT));
    }

    public boolean isDeveloping() {
        return this.data.get(0) > 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < BASIN_SLOTS) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(ItemRegistry.EXPOSED_FILM.get())) {
            if (!moveItemStackTo(stack, PhotoRinsingBasinBlockEntity.INPUT_SLOT,
                    PhotoRinsingBasinBlockEntity.INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(ItemRegistry.BRINE_DEVELOPING_SALTS.get())) {
            if (!moveItemStackTo(stack, PhotoRinsingBasinBlockEntity.SALTS_SLOT,
                    PhotoRinsingBasinBlockEntity.SALTS_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY, original);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }
}
