package com.dexer.aquanaut.common;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.diving.DivingEquipmentHelper;
import com.dexer.aquanaut.common.diving.DivingEquipmentSlotType;
import com.dexer.aquanaut.network.DivingEquipmentSyncPayload;
import com.dexer.aquanaut.network.ExtraAirPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Syncs effective extra-air capacity to the client.
 *
 * <p>
 * The current air value no longer needs a custom packet because vanilla entity
 * sync already carries {@code airSupply}. We only send capacity updates when
 * the effective extra-air value changes.
 */
@EventBusSubscriber(modid = Aquanaut.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class PlayerAirEvents {

    private static final Map<UUID, Integer> LAST_SENT_MAX_EXTRA = new HashMap<>();
    private static final Map<UUID, String> LAST_SENT_DIVING_ITEMS = new HashMap<>();

    private PlayerAirEvents() {
    }

    @SubscribeEvent
    public static void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SENT_MAX_EXTRA.remove(event.getEntity().getUUID());
        LAST_SENT_DIVING_ITEMS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        int maxExtra = AirSupplyHelper.getEffectiveExtraCapacity(serverPlayer);
        UUID uuid = serverPlayer.getUUID();
        Integer lastSent = LAST_SENT_MAX_EXTRA.get(uuid);

        if (lastSent == null || lastSent.intValue() != maxExtra) {
            PacketDistributor.sendToPlayer(serverPlayer, new ExtraAirPayload(maxExtra));
            LAST_SENT_MAX_EXTRA.put(uuid, maxExtra);
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
        if (multiplier <= 1.0F || !serverPlayer.isInWater()) {
            return;
        }

        Vec3 velocity = serverPlayer.getDeltaMovement();
        Vec3 horizontalVelocity = new Vec3(velocity.x, 0.0D, velocity.z);
        double horizontalSpeedSq = horizontalVelocity.lengthSqr();
        if (horizontalSpeedSq <= 1.0E-6D) {
            return;
        }

        double boostScale = (multiplier - 1.0F) * 0.05D;
        Vec3 boost = horizontalVelocity.normalize().scale(boostScale);
        serverPlayer.setDeltaMovement(velocity.add(boost.x, 0.0D, boost.z));
    }
}
