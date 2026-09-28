package com.dexer.aquanaut.common.investigation;

import com.dexer.aquanaut.Aquanaut;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = Aquanaut.MODID)
public final class InvestigationEvents {
    private InvestigationEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            InvestigationBoardApi.syncTo(player, false);
        }
    }
}
