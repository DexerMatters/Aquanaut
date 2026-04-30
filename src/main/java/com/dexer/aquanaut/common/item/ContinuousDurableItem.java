package com.dexer.aquanaut.common.item;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public abstract class ContinuousDurableItem extends Item {

    public ContinuousDurableItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (slotChanged)
            return true;
        if (!ItemStack.isSameItem(oldStack, newStack))
            return true;
        if (!newStack.isDamageableItem() || !oldStack.isDamageableItem())
            return !ItemStack.isSameItemSameComponents(oldStack, newStack);

        DataComponentMap newComponents = newStack.getComponents();
        DataComponentMap oldComponents = oldStack.getComponents();

        if (newComponents.isEmpty() || oldComponents.isEmpty())
            return !(newComponents.isEmpty() && oldComponents.isEmpty());

        Set<DataComponentType<?>> newKeys = new HashSet<>(newComponents.keySet());
        Set<DataComponentType<?>> oldKeys = new HashSet<>(oldComponents.keySet());

        newKeys.remove(DataComponents.DAMAGE);
        oldKeys.remove(DataComponents.DAMAGE);

        if (!newKeys.equals(oldKeys))
            return true;

        for (DataComponentType<?> key : oldKeys) {
            Object oldVal = oldComponents.get(key);
            Object newVal = newComponents.get(key);
            if (!Objects.equals(oldVal, newVal))
                return true;
        }

        return false;
    }

    protected boolean isCreative(Player player) {
        return player.getAbilities().instabuild;
    }

    protected boolean isBroken(ItemStack stack) {
        return stack.getDamageValue() >= stack.getMaxDamage();
    }
}
