package com.dexer.aquanaut.common;

import com.dexer.aquanaut.Aquanaut;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Player-only air lifecycle hooks that cannot be derived from generic entity
 * events.
 */
@EventBusSubscriber(modid = Aquanaut.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class PlayerAirEvents {

    private PlayerAirEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LivingAirEvents.clearPlayerSync(player.getUUID());
            AirSupplyHelper.fillExtraAirToMax(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer newPlayer) {
            LivingAirEvents.clearPlayerSync(newPlayer.getUUID());
        }

        if (event.isWasDeath()) {
            AirSupplyHelper.clearExtraAir(event.getOriginal().getId());
            if (event.getEntity() instanceof ServerPlayer newPlayer) {
                AirSupplyHelper.fillExtraAirToMax(newPlayer);
            }
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) {
            return;
        }

        int oldExtra = AirSupplyHelper.getExtraAir(event.getOriginal());
        AirSupplyHelper.clearExtraAir(event.getOriginal().getId());
        AirSupplyHelper.setExtraAir(newPlayer, oldExtra);
    }
}
