package com.dexer.aquanaut.mixin;

import com.dexer.aquanaut.common.diving.DivingEquipmentSlotType;
import com.dexer.aquanaut.common.diving.inventory.DivingEquipmentContainer;
import com.dexer.aquanaut.common.diving.inventory.DivingEquipmentMenuSlot;
import com.dexer.aquanaut.common.diving.inventory.DivingInventoryLayout;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/**
 * Appends three real menu slots to the player's inventory menu for diving gear.
 */
@Mixin(value = InventoryMenu.class, remap = false)
public abstract class InventoryMenuMixin extends AbstractContainerMenu {

    @Unique
    private static final int AQUANAUT_DIVING_SLOT_START = 46;

    @Unique
    private static final int AQUANAUT_DIVING_SLOT_END = AQUANAUT_DIVING_SLOT_START
            + DivingEquipmentContainer.SLOT_COUNT;

    @Unique
    private DivingEquipmentContainer aquanaut$divingContainer;

    protected InventoryMenuMixin(@Nullable MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void aquanaut$addDivingSlots(Inventory playerInventory, boolean active, Player owner, CallbackInfo ci) {
        this.aquanaut$divingContainer = new DivingEquipmentContainer(owner);

        this.addSlot(new DivingEquipmentMenuSlot(
                this.aquanaut$divingContainer,
                DivingEquipmentSlotType.MASK,
                DivingInventoryLayout.SLOT_X,
                DivingInventoryLayout.slotY(DivingEquipmentSlotType.MASK)));
        this.addSlot(new DivingEquipmentMenuSlot(
                this.aquanaut$divingContainer,
                DivingEquipmentSlotType.TANK,
                DivingInventoryLayout.SLOT_X,
                DivingInventoryLayout.slotY(DivingEquipmentSlotType.TANK)));
        this.addSlot(new DivingEquipmentMenuSlot(
                this.aquanaut$divingContainer,
                DivingEquipmentSlotType.FLIPPERS,
                DivingInventoryLayout.SLOT_X,
                DivingInventoryLayout.slotY(DivingEquipmentSlotType.FLIPPERS)));
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true, remap = false)
    private void aquanaut$quickMoveStack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index < 0 || index >= this.slots.size()) {
            return;
        }

        Slot sourceSlot = this.slots.get(index);
        if (!sourceSlot.hasItem()) {
            return;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack originalCopy = sourceStack.copy();

        boolean moved = false;
        if (index >= AQUANAUT_DIVING_SLOT_START && index < AQUANAUT_DIVING_SLOT_END) {
            moved = this.moveItemStackTo(sourceStack, InventoryMenu.INV_SLOT_START, InventoryMenu.SHIELD_SLOT + 1,
                    false);
        } else if (index >= InventoryMenu.INV_SLOT_START && index <= InventoryMenu.SHIELD_SLOT
                && aquanaut$canAnyDivingSlotAccept(sourceStack)) {
            moved = this.moveItemStackTo(sourceStack, AQUANAUT_DIVING_SLOT_START, AQUANAUT_DIVING_SLOT_END, false);
        }

        if (!moved) {
            return;
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.setByPlayer(ItemStack.EMPTY, originalCopy);
        } else {
            sourceSlot.setChanged();
        }

        if (sourceStack.getCount() == originalCopy.getCount()) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }

        sourceSlot.onTake(player, sourceStack);
        cir.setReturnValue(originalCopy);
    }

    @Unique
    private static boolean aquanaut$canAnyDivingSlotAccept(ItemStack stack) {
        return DivingEquipmentSlotType.MASK.accepts(stack)
                || DivingEquipmentSlotType.TANK.accepts(stack)
                || DivingEquipmentSlotType.FLIPPERS.accepts(stack);
    }
}
