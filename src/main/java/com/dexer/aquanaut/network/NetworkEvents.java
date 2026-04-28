package com.dexer.aquanaut.network;

import com.dexer.aquanaut.Aquanaut;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Aquanaut.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class NetworkEvents {

    private NetworkEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                ExtraAirPayload.TYPE,
                ExtraAirPayload.STREAM_CODEC,
                ExtraAirPayload::handle);
        registrar.playToClient(
                DivingEquipmentSyncPayload.TYPE,
                DivingEquipmentSyncPayload.STREAM_CODEC,
                DivingEquipmentSyncPayload::handle);
        registrar.playToServer(
                DivingEquipmentClickPayload.TYPE,
                DivingEquipmentClickPayload.STREAM_CODEC,
                DivingEquipmentClickPayload::handle);
    }
}
