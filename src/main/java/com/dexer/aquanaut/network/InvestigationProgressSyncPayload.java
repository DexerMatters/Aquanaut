package com.dexer.aquanaut.network;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.client.ClientInvestigationData;
import com.dexer.aquanaut.common.investigation.InvestigationProgress;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record InvestigationProgressSyncPayload(String serializedData, boolean openBoard)
        implements CustomPacketPayload {
    public static final Type<InvestigationProgressSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "investigation_progress_sync"));

    public static final StreamCodec<ByteBuf, InvestigationProgressSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, InvestigationProgressSyncPayload::serializedData,
            ByteBufCodecs.BOOL, InvestigationProgressSyncPayload::openBoard,
            InvestigationProgressSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static InvestigationProgressSyncPayload fromData(InvestigationProgress progress, boolean openBoard) {
        return new InvestigationProgressSyncPayload(progress.serialize(), openBoard);
    }

    public static void handle(InvestigationProgressSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientInvestigationData.receive(payload.serializedData(), payload.openBoard()));
    }
}
