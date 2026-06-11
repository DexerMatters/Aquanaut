package com.dexer.aquanaut.client;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.network.RecallHarpoonPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Aquanaut.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
@SuppressWarnings("removal")
public final class ClientGazeEvents {
    private ClientGazeEvents() {
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || event.getButton() != minecraft.options.keyAttack.getKey().getValue()
                || event.getAction() != 1 || !minecraft.player.getMainHandItem().isEmpty()
                || !minecraft.player.getOffhandItem().isEmpty()) {
            return;
        }

        PacketDistributor.sendToServer(new RecallHarpoonPayload());
    }

}
