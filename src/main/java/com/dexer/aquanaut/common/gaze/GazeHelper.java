package com.dexer.aquanaut.common.gaze;

import com.dexer.aquanaut.core.GazeRegistry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GazeHelper {

    private GazeHelper() {
    }

    public static DataComponentType<List<GazeInstance>> componentType() {
        return GazeRegistry.GAZE.get();
    }

    public static List<GazeInstance> getGaze(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Collections.emptyList();
        }
        List<GazeInstance> list = stack.get(componentType());
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

    public static boolean hasGaze(ItemStack stack) {
        return !getGaze(stack).isEmpty();
    }

    public static boolean hasGaze(ItemStack stack, ResourceLocation typeId) {
        for (GazeInstance instance : getGaze(stack)) {
            if (instance.id().equals(typeId)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasGaze(ItemStack stack, GazeType type) {
        for (GazeInstance instance : getGaze(stack)) {
            if (instance.type() == type) {
                return true;
            }
        }
        return false;
    }

    public static int getLevel(ItemStack stack, ResourceLocation typeId) {
        for (GazeInstance instance : getGaze(stack)) {
            if (instance.id().equals(typeId)) {
                return instance.level();
            }
        }
        return 0;
    }

    public static void setGaze(ItemStack stack, List<GazeInstance> gazes) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (gazes == null || gazes.isEmpty()) {
            stack.remove(componentType());
        } else {
            stack.set(componentType(), gazes.stream()
                    .map(GazeHelper::clampLevel)
                    .toList());
        }
    }

    public static void addGaze(ItemStack stack, GazeInstance instance) {
        List<GazeInstance> list = new ArrayList<>(getGaze(stack));
        list.removeIf(existing -> existing.id().equals(instance.id()));
        list.add(instance);
        setGaze(stack, list);
    }

    public static void removeGaze(ItemStack stack, ResourceLocation typeId) {
        List<GazeInstance> list = new ArrayList<>(getGaze(stack));
        list.removeIf(existing -> existing.id().equals(typeId));
        setGaze(stack, list);
    }

    private static GazeInstance clampLevel(GazeInstance instance) {
        int level = Math.min(instance.level(), GazeCatalog.maxLevel(instance.id()));
        return level == instance.level() ? instance : new GazeInstance(instance.id(), instance.type(), level);
    }
}
