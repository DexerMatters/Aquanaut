package com.dexer.aquanaut.common;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.diving.DivingEquipmentHelper;
import com.dexer.aquanaut.common.diving.DivingEquipmentSlotType;
import com.dexer.aquanaut.network.DivingEquipmentSyncPayload;
import com.dexer.aquanaut.network.ExtraAirPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Syncs effective extra-air capacity and current extra air to the client,
 * applies flipper speed boosts, and handles extra air consumption.
 */
@EventBusSubscriber(modid = Aquanaut.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class PlayerAirEvents {

    private static final Map<UUID, Integer> LAST_SENT_MAX_EXTRA = new HashMap<>();
    private static final Map<UUID, Integer> LAST_SENT_EXTRA_AIR = new HashMap<>();
    private static final Map<UUID, String> LAST_SENT_DIVING_ITEMS = new HashMap<>();
    private static final Map<UUID, Integer> PREVIOUS_BASE_AIR = new HashMap<>();
    private static final ResourceLocation FLIPPER_SPEED_MODIFIER_ID = ResourceLocation
            .fromNamespaceAndPath(Aquanaut.MODID, "flipper_speed_bonus");

    private PlayerAirEvents() {
    }

    @SubscribeEvent
    public static void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        LAST_SENT_MAX_EXTRA.remove(uuid);
        LAST_SENT_EXTRA_AIR.remove(uuid);
        LAST_SENT_DIVING_ITEMS.remove(uuid);
        PREVIOUS_BASE_AIR.remove(uuid);
        AttributeInstance attr = event.getEntity().getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
        if (attr != null) {
            attr.removeModifier(FLIPPER_SPEED_MODIFIER_ID);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AirSupplyHelper.fillExtraAirToMax(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            PREVIOUS_BASE_AIR.remove(event.getOriginal().getUUID());
        }
        if (event.getEntity() instanceof ServerPlayer newPlayer) {
            AirSupplyHelper.fillExtraAirToMax(newPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PREVIOUS_BASE_AIR.put(player.getUUID(), player.getAirSupply());
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        UUID uuid = serverPlayer.getUUID();
        Integer previousAir = PREVIOUS_BASE_AIR.get(uuid);
        int currentAir = serverPlayer.getAirSupply();

        // Consume extra air first by restoring vanilla air drops while extra exists.
        if (previousAir != null && currentAir < previousAir) {
            int decrease = previousAir - currentAir;
            int notConsumed = AirSupplyHelper.consumeExtraAir(serverPlayer, decrease);
            if (notConsumed < decrease) {
                int saved = decrease - notConsumed;
                serverPlayer.setAirSupply(currentAir + saved);
                currentAir = serverPlayer.getAirSupply();
            }
        }

        // Extra air regen: when breathing and base is full, fill extra tanks.
        if (!serverPlayer.isEyeInFluid(FluidTags.WATER)
                && serverPlayer.getAirSupply() >= AirSupplyHelper.BASE_AIR_SUPPLY_TICKS) {
            int regenPerTick = AirSupplyHelper.getRegenPerTick(serverPlayer);
            if (regenPerTick > 0) {
                AirSupplyHelper.fillExtraAir(serverPlayer, regenPerTick);
            }
        }

        int maxExtra = AirSupplyHelper.getEffectiveExtraCapacity(serverPlayer);
        int extraAir = AirSupplyHelper.getExtraAir(serverPlayer);

        Integer lastMax = LAST_SENT_MAX_EXTRA.get(uuid);
        Integer lastExtra = LAST_SENT_EXTRA_AIR.get(uuid);

        if (lastMax == null || lastMax.intValue() != maxExtra
                || lastExtra == null || lastExtra.intValue() != extraAir) {
            PacketDistributor.sendToPlayer(serverPlayer, new ExtraAirPayload(maxExtra, extraAir));
            LAST_SENT_MAX_EXTRA.put(uuid, maxExtra);
            LAST_SENT_EXTRA_AIR.put(uuid, extraAir);
        }

        String syncKey = DivingEquipmentHelper.getSyncItemId(serverPlayer, DivingEquipmentSlotType.MASK)
                + "|"
                + DivingEquipmentHelper.getSyncItemId(serverPlayer, DivingEquipmentSlotType.TANK)
                + "|"
                + DivingEquipmentHelper.getSyncItemId(serverPlayer, DivingEquipmentSlotType.FLIPPERS);

        String lastSyncKey = LAST_SENT_DIVING_ITEMS.get(uuid);
        if (lastSyncKey == null || !lastSyncKey.equals(syncKey)) {
            PacketDistributor.sendToPlayer(serverPlayer, DivingEquipmentSyncPayload.fromPlayer(serverPlayer));
            LAST_SENT_DIVING_ITEMS.put(uuid, syncKey);
        }

        applyFlipperBoost(serverPlayer);
    }

    private static void applyFlipperBoost(ServerPlayer serverPlayer) {
        float multiplier = DivingEquipmentHelper.getFlipperSpeedMultiplier(serverPlayer);
        AttributeInstance attr = serverPlayer.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
        if (attr == null) {
            return;
        }

        if (multiplier <= 1.0F || !serverPlayer.isInWater()) {
            attr.removeModifier(FLIPPER_SPEED_MODIFIER_ID);
            return;
        }

        attr.addOrUpdateTransientModifier(new AttributeModifier(
                FLIPPER_SPEED_MODIFIER_ID,
                multiplier - 1.0F,
                AttributeModifier.Operation.ADD_VALUE));
    }
}
