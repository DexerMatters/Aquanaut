package com.dexer.aquanaut.common.diving;

import com.dexer.aquanaut.common.AirSupplyHelper;
import com.dexer.aquanaut.common.item.DivingEquipmentItem;
import com.dexer.aquanaut.core.AttachmentRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
        AirSupplyHelper.clampAir(entity);
        AirSupplyHelper.fillExtraAirToMax(entity);
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
        AirSupplyHelper.clampAir(entity);
        AirSupplyHelper.fillExtraAirToMax(entity);
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
        DivingEquipmentItem tank = getEquippedItem(entity, DivingEquipmentSlotType.TANK);
        return tank == null ? 0 : AirSupplyHelper.bubblesToAirTicks(tank.tankAirBubbles());
    }

    public static int getMaskRegenBonus(LivingEntity entity) {
        DivingEquipmentItem mask = getEquippedItem(entity, DivingEquipmentSlotType.MASK);
        return mask == null ? 0 : mask.maskRegenBonus();
    }

    public static float getFlipperSpeedMultiplier(LivingEntity entity) {
        DivingEquipmentItem flippers = getEquippedItem(entity, DivingEquipmentSlotType.FLIPPERS);
        return flippers == null ? 1.0F : flippers.underwaterSpeedMultiplier();
    }

    public static void hurtEquippedItem(Player player, DivingEquipmentSlotType slotType, DamageSource damageSource,
            float damageAmount, EquipmentSlot visualSlot) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) || damageAmount <= 0.0F) {
            return;
        }

        ItemStack equippedStack = getEquippedStack(player, slotType);
        if (equippedStack.isEmpty() || !equippedStack.isDamageableItem() || !equippedStack.canBeHurtBy(damageSource)) {
            return;
        }

        int durabilityDamage = (int) Math.max(1.0F, damageAmount / 4.0F);
        equippedStack.hurtAndBreak(durabilityDamage, serverLevel, player,
                item -> player.onEquippedItemBroken(item, visualSlot));
        setEquippedStack(player, slotType, equippedStack);
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

    private static DivingEquipmentItem getEquippedItem(LivingEntity entity, DivingEquipmentSlotType slotType) {
        ItemStack stack = getEquippedStack(entity, slotType);
        if (stack.isEmpty() || !(stack.getItem() instanceof DivingEquipmentItem divingEquipmentItem)) {
            return null;
        }

        return divingEquipmentItem.slotType() == slotType ? divingEquipmentItem : null;
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