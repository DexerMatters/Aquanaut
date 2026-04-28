package com.dexer.aquanaut.mixin;

import com.dexer.aquanaut.common.diving.DivingEquipmentSlotType;
import com.dexer.aquanaut.common.diving.inventory.DivingEquipmentContainer;
import com.dexer.aquanaut.common.diving.inventory.DivingInventoryLayout;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Repositions wrapped diving slots in the creative inventory tab so creative
 * and
 * survival share the same real-slot interaction model.
 */
@Mixin(value = CreativeModeInventoryScreen.class, remap = false)
public abstract class CreativeModeInventoryScreenMixin {

    @ModifyArgs(method = "selectTab", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;<init>(Lnet/minecraft/world/inventory/Slot;III)V"), remap = false)
    private void aquanaut$positionDivingSlots(Args args) {
        Slot targetSlot = args.get(0);
        if (!(targetSlot.container instanceof DivingEquipmentContainer)) {
            return;
        }

        DivingEquipmentSlotType slotType = switch (targetSlot.getContainerSlot()) {
            case 0 -> DivingEquipmentSlotType.MASK;
            case 1 -> DivingEquipmentSlotType.TANK;
            case 2 -> DivingEquipmentSlotType.FLIPPERS;
            default -> null;
        };
        if (slotType == null) {
            return;
        }

        args.set(2, DivingInventoryLayout.SLOT_X);
        args.set(3, DivingInventoryLayout.slotY(slotType));
    }
}
