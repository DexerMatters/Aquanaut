package com.dexer.aquanaut.client;

import com.dexer.aquanaut.common.diving.DivingEquipmentSlotType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.dexer.aquanaut.common.diving.DivingEquipmentHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ClientDivingEquipmentData {
    private static final Map<UUID, EquipmentStacks> EQUIPPED_BY_PLAYER = new HashMap<>();

    private ClientDivingEquipmentData() {
    }

    public static void setFromIds(String playerId, String maskId, String tankId, String flippersId) {
        if (playerId == null || playerId.isBlank()) {
            return;
        }

        UUID playerUuid;
        try {
            playerUuid = UUID.fromString(playerId);
        } catch (IllegalArgumentException ignored) {
            return;
        }

        EquipmentStacks stacks = new EquipmentStacks(
                stackFromId(maskId), stackFromId(tankId), stackFromId(flippersId));
        if (stacks.isEmpty()) {
            EQUIPPED_BY_PLAYER.remove(playerUuid);
        } else {
            EQUIPPED_BY_PLAYER.put(playerUuid, stacks);
        }
    }

    public static ItemStack getStack(LivingEntity entity, DivingEquipmentSlotType slotType) {
        if (entity instanceof LocalPlayer) {
            // The local inventory menu writes straight into the client-side attachment,
            // so taking a piece off clears the worn model on the very same tick as the
            // click instead of waiting for a server round trip.
            return DivingEquipmentHelper.getEquippedStack(entity, slotType);
        }
        EquipmentStacks stacks = EQUIPPED_BY_PLAYER.get(entity.getUUID());
        return stacks == null ? ItemStack.EMPTY : stacks.get(slotType);
    }

    /**
     * Mirrors a sync payload into the local player's client-side attachment so that
     * server-driven changes (damage, commands, death drops) also reach the worn model.
     */
    public static void applyToLocalPlayer(String playerId, String maskId, String tankId,
            String flippersId) {
        LocalPlayer local = Minecraft.getInstance().player;
        if (local == null || !local.getUUID().toString().equals(playerId)) {
            return;
        }
        DivingEquipmentHelper.setEquippedStack(local, DivingEquipmentSlotType.MASK, stackFromId(maskId));
        DivingEquipmentHelper.setEquippedStack(local, DivingEquipmentSlotType.TANK, stackFromId(tankId));
        DivingEquipmentHelper.setEquippedStack(local, DivingEquipmentSlotType.FLIPPERS,
                stackFromId(flippersId));
    }

    public static void remove(UUID playerUuid) {
        EQUIPPED_BY_PLAYER.remove(playerUuid);
    }

    public static void clear() {
        EQUIPPED_BY_PLAYER.clear();
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

    private record EquipmentStacks(ItemStack mask, ItemStack tank, ItemStack flippers) {
        private ItemStack get(DivingEquipmentSlotType slotType) {
            return switch (slotType) {
                case MASK -> mask;
                case TANK -> tank;
                case FLIPPERS -> flippers;
            };
        }

        private boolean isEmpty() {
            return mask.isEmpty() && tank.isEmpty() && flippers.isEmpty();
        }
    }
}
