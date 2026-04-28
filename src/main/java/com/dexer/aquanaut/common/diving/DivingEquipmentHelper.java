package com.dexer.aquanaut.common.diving;

import com.dexer.aquanaut.Config;
import com.dexer.aquanaut.common.AirSupplyHelper;
import com.dexer.aquanaut.core.AttachmentRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Central helper for the logical diving equipment layer.
 *
 * <p>
 * This layer is independent from vanilla armor slots. Future acquisition or
 * slot systems should write into this helper instead of duplicating gameplay
 * logic.
 */
public final class DivingEquipmentHelper {

    private DivingEquipmentHelper() {
    }

    public static DivingEquipmentState getState(LivingEntity entity) {
        if (!entity.hasData(AttachmentRegistry.DIVING_EQUIPMENT_STATE.get())) {
            return DivingEquipmentState.EMPTY;
        }
        DivingEquipmentState state = entity.getData(AttachmentRegistry.DIVING_EQUIPMENT_STATE.get());
        return state == null ? DivingEquipmentState.EMPTY : state;
    }

    public static void setState(LivingEntity entity, DivingEquipmentState state) {
        DivingEquipmentState normalized = state == null ? DivingEquipmentState.EMPTY : state;
        entity.setData(AttachmentRegistry.DIVING_EQUIPMENT_STATE.get(), normalized);
        AirSupplyHelper.clampAirToUnifiedMax(entity);
    }

    public static void setTankEquipped(LivingEntity entity, boolean equipped) {
        setState(entity, getState(entity).withTankEquipped(equipped));
    }

    public static void setMaskEquipped(LivingEntity entity, boolean equipped) {
        setState(entity, getState(entity).withMaskEquipped(equipped));
    }

    public static void setFlippersEquipped(LivingEntity entity, boolean equipped) {
        setState(entity, getState(entity).withFlippersEquipped(equipped));
    }

    public static ItemStack getEquippedStack(LivingEntity entity, DivingEquipmentSlotType slotType) {
        ItemStack stack = switch (slotType) {
            case MASK -> entity.getData(AttachmentRegistry.DIVING_MASK_STACK.get());
            case TANK -> entity.getData(AttachmentRegistry.DIVING_TANK_STACK.get());
            case FLIPPERS -> entity.getData(AttachmentRegistry.DIVING_FLIPPERS_STACK.get());
        };
        return stack == null ? ItemStack.EMPTY : stack;
    }

    public static void setEquippedStack(LivingEntity entity, DivingEquipmentSlotType slotType, ItemStack stack) {
        ItemStack normalized = ItemStack.EMPTY;
        if (stack != null && !stack.isEmpty()) {
            normalized = stack.copy();
            normalized.setCount(1);
        }

        switch (slotType) {
            case MASK -> entity.setData(AttachmentRegistry.DIVING_MASK_STACK.get(), normalized);
            case TANK -> entity.setData(AttachmentRegistry.DIVING_TANK_STACK.get(), normalized);
            case FLIPPERS -> entity.setData(AttachmentRegistry.DIVING_FLIPPERS_STACK.get(), normalized);
        }

        syncStateWithStacks(entity);
        AirSupplyHelper.clampAirToUnifiedMax(entity);
    }

    public static void handleSlotClick(ServerPlayer player, DivingEquipmentSlotType slotType, int button,
            String clientCarriedItemId) {
        if (button != 0 && button != 1) {
            return;
        }

        ItemStack carried = player.containerMenu.getCarried();
        boolean creativeFallbackCarried = false;
        if (carried.isEmpty() && player.getAbilities().instabuild) {
            ItemStack fallback = stackFromItemId(clientCarriedItemId);
            if (!fallback.isEmpty()) {
                carried = fallback;
                creativeFallbackCarried = true;
            }
        }

        ItemStack equipped = getEquippedStack(player, slotType);

        if (carried.isEmpty()) {
            if (equipped.isEmpty()) {
                return;
            }

            player.containerMenu.setCarried(equipped.copy());
            setEquippedStack(player, slotType, ItemStack.EMPTY);
            player.containerMenu.broadcastChanges();
            return;
        }

        if (!slotType.accepts(carried)) {
            return;
        }

        if (equipped.isEmpty()) {
            ItemStack toEquip = carried.copy();
            toEquip.setCount(1);
            setEquippedStack(player, slotType, toEquip);

            if (!creativeFallbackCarried) {
                carried.shrink(1);
                player.containerMenu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
            }
            player.containerMenu.broadcastChanges();
            return;
        }

        // Keep swap behavior deterministic and safe for a dedicated 1-stack equipment
        // slot.
        if (carried.getCount() != 1) {
            return;
        }

        setEquippedStack(player, slotType, carried.copy());
        if (!creativeFallbackCarried) {
            player.containerMenu.setCarried(equipped.copy());
        }
        player.containerMenu.broadcastChanges();
    }

    public static boolean hasTank(LivingEntity entity) {
        return !getEquippedStack(entity, DivingEquipmentSlotType.TANK).isEmpty() || getState(entity).tankEquipped();
    }

    public static boolean hasMask(LivingEntity entity) {
        return !getEquippedStack(entity, DivingEquipmentSlotType.MASK).isEmpty() || getState(entity).maskEquipped();
    }

    public static boolean hasFlippers(LivingEntity entity) {
        return !getEquippedStack(entity, DivingEquipmentSlotType.FLIPPERS).isEmpty()
                || getState(entity).flippersEquipped();
    }

    public static int getTankCapacityBonus(LivingEntity entity) {
        return hasTank(entity) ? Config.DIVING_TANK_CAPACITY_BONUS.get() : 0;
    }

    public static int getMaskRegenBonus(LivingEntity entity) {
        return hasMask(entity) ? Config.DIVING_MASK_REGEN_BONUS.get() : 0;
    }

    public static float getFlipperSpeedMultiplier(LivingEntity entity) {
        return hasFlippers(entity) ? Config.DIVING_FLIPPER_SPEED_MULTIPLIER.get().floatValue() : 1.0F;
    }

    public static String getSyncItemId(LivingEntity entity, DivingEquipmentSlotType slotType) {
        ItemStack stack = getEquippedStack(entity, slotType);
        if (stack.isEmpty()) {
            return "";
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    private static void syncStateWithStacks(LivingEntity entity) {
        DivingEquipmentState state = new DivingEquipmentState(
                !getEquippedStack(entity, DivingEquipmentSlotType.TANK).isEmpty(),
                !getEquippedStack(entity, DivingEquipmentSlotType.MASK).isEmpty(),
                !getEquippedStack(entity, DivingEquipmentSlotType.FLIPPERS).isEmpty());
        entity.setData(AttachmentRegistry.DIVING_EQUIPMENT_STATE.get(), state);
    }

    private static ItemStack stackFromItemId(String rawId) {
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