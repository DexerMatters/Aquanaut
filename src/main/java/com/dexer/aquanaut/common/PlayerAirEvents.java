package com.dexer.aquanaut.common;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.network.ExtraAirPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Syncs max extra-air capacity to the client.
 *
 * <p>
 * The current air value no longer needs a custom packet because vanilla entity
 * sync already carries {@code airSupply}. We only send capacity updates when
 * {@code MAX_EXTRA_AIR_SUPPLY} changes.
 */
@EventBusSubscriber(modid = Aquanaut.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class PlayerAirEvents {

    private static final Map<UUID, Integer> LAST_SENT_MAX_EXTRA = new HashMap<>();

    private PlayerAirEvents() {
    }

    @SubscribeEvent
    public static void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SENT_MAX_EXTRA.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        int maxExtra = AirSupplyHelper.getMaxExtraAir(serverPlayer);
        UUID uuid = serverPlayer.getUUID();
        Integer lastSent = LAST_SENT_MAX_EXTRA.get(uuid);

        if (lastSent == null || lastSent.intValue() != maxExtra) {
            PacketDistributor.sendToPlayer(serverPlayer, new ExtraAirPayload(maxExtra));
            LAST_SENT_MAX_EXTRA.put(uuid, maxExtra);
        }
    }
}
