package com.dexer.aquanaut.client;

import com.dexer.aquanaut.common.diving.DivingEquipmentSlotType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ClientDivingEquipmentData {

    private static ItemStack maskStack = ItemStack.EMPTY;
    private static ItemStack tankStack = ItemStack.EMPTY;
    private static ItemStack flippersStack = ItemStack.EMPTY;

    private ClientDivingEquipmentData() {
    }

    public static void setFromIds(String maskId, String tankId, String flippersId) {
        maskStack = stackFromId(maskId);
        tankStack = stackFromId(tankId);
        flippersStack = stackFromId(flippersId);
    }

    public static ItemStack getStack(DivingEquipmentSlotType slotType) {
        return switch (slotType) {
            case MASK -> maskStack;
            case TANK -> tankStack;
            case FLIPPERS -> flippersStack;
        };
    }

    private static ItemStack stackFromId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return ItemStack.EMPTY;
        }

        ResourceLocation id;
        try {
            id = ResourceLocation.parse(rawId);
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }

        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == null || item == BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace("air"))) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(item);
    }
}