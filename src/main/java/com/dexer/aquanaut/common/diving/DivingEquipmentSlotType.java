package com.dexer.aquanaut.common.diving;

import com.dexer.aquanaut.Aquanaut;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public enum DivingEquipmentSlotType {
    MASK("mask", "mask", TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "diving_mask"))),
    TANK("tank", "tank", TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "diving_tank"))),
    FLIPPERS("flippers", "flipper", TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "diving_flippers")));

    private final String serializedName;
    private final String fallbackKeyword;
    private final TagKey<Item> acceptedTag;

    DivingEquipmentSlotType(String serializedName, String fallbackKeyword, TagKey<Item> acceptedTag) {
        this.serializedName = serializedName;
        this.fallbackKeyword = fallbackKeyword;
        this.acceptedTag = acceptedTag;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean accepts(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.is(acceptedTag)) {
            return true;
        }

        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key == null) {
            return false;
        }

        String path = key.getPath();
        return path.contains(fallbackKeyword);
    }

    public static DivingEquipmentSlotType fromIndex(int index) {
        return switch (index) {
            case 0 -> MASK;
            case 1 -> TANK;
            case 2 -> FLIPPERS;
            default -> MASK;
        };
    }

    public int toIndex() {
        return switch (this) {
            case MASK -> 0;
            case TANK -> 1;
            case FLIPPERS -> 2;
        };
    }
}