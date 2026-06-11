package com.dexer.aquanaut.network;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.client.ClientNotebookData;
import com.dexer.aquanaut.common.notebook.NotebookProgress;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record NotebookProgressSyncPayload(String serializedData) implements CustomPacketPayload {

    public static final Type<NotebookProgressSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "notebook_progress_sync"));

    public static final StreamCodec<ByteBuf, NotebookProgressSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, NotebookProgressSyncPayload::serializedData,
            NotebookProgressSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static NotebookProgressSyncPayload fromData(NotebookProgress data) {
        return new NotebookProgressSyncPayload(data.serialize());
    }

    public static void handle(NotebookProgressSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientNotebookData.setFromSerialized(payload.serializedData()));
    }
}
