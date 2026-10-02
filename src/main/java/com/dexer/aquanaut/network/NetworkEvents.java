package com.dexer.aquanaut.network;

import com.dexer.aquanaut.Aquanaut;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Aquanaut.MODID)
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
        registrar.playToClient(
                AquariumInventorySyncPayload.TYPE,
                AquariumInventorySyncPayload.STREAM_CODEC,
                AquariumInventorySyncPayload::handle);
        registrar.playToClient(
                NotebookProgressSyncPayload.TYPE,
                NotebookProgressSyncPayload.STREAM_CODEC,
                NotebookProgressSyncPayload::handle);
        registrar.playToClient(
                InvestigationProgressSyncPayload.TYPE,
                InvestigationProgressSyncPayload.STREAM_CODEC,
                InvestigationProgressSyncPayload::handle);
        registrar.playToClient(
                SonarPingPayload.TYPE,
                SonarPingPayload.STREAM_CODEC,
                SonarPingPayload::handle);
        registrar.playToServer(
                DivingEquipmentClickPayload.TYPE,
                DivingEquipmentClickPayload.STREAM_CODEC,
                DivingEquipmentClickPayload::handle);
        registrar.playToServer(
                RecallHarpoonPayload.TYPE,
                RecallHarpoonPayload.STREAM_CODEC,
                RecallHarpoonPayload::handle);
        registrar.playToServer(
                OpenAquariumPayload.TYPE,
                OpenAquariumPayload.STREAM_CODEC,
                OpenAquariumPayload::handle);
        registrar.playToServer(
                CloseAquariumPayload.TYPE,
                CloseAquariumPayload.STREAM_CODEC,
                CloseAquariumPayload::handle);
        registrar.playToServer(
                AquariumFishTransferPayload.TYPE,
                AquariumFishTransferPayload.STREAM_CODEC,
                AquariumFishTransferPayload::handle);
        registrar.playToServer(
                TagPayload.TYPE,
                TagPayload.STREAM_CODEC,
                TagPayload::handle);
        registrar.playToServer(
                CompassTargetPayload.TYPE,
                CompassTargetPayload.STREAM_CODEC,
                CompassTargetPayload::handle);
        registrar.playToServer(
                TakePhotoPayload.TYPE,
                TakePhotoPayload.STREAM_CODEC,
                TakePhotoPayload::handle);
        registrar.playToServer(
                SubmarineDroneLinkPayload.TYPE,
                SubmarineDroneLinkPayload.STREAM_CODEC,
                SubmarineDroneLinkPayload::handle);
        registrar.playToServer(
                SubmarineDroneControlPayload.TYPE,
                SubmarineDroneControlPayload.STREAM_CODEC,
                SubmarineDroneControlPayload::handle);
        registrar.playToServer(
                SubmarineDroneHeadlightPayload.TYPE,
                SubmarineDroneHeadlightPayload.STREAM_CODEC,
                SubmarineDroneHeadlightPayload::handle);
    }
}
