package com.dexer.aquanaut.network;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.client.ClientAquariumData;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumInventoryData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AquariumInventorySyncPayload(String serializedData) implements CustomPacketPayload {

    public static final Type<AquariumInventorySyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "aquarium_inventory_sync"));

    public static final StreamCodec<ByteBuf, AquariumInventorySyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, AquariumInventorySyncPayload::serializedData,
            AquariumInventorySyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static AquariumInventorySyncPayload fromData(AquariumInventoryData data) {
        return new AquariumInventorySyncPayload(data.serialize());
    }

    public static void handle(AquariumInventorySyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientAquariumData.setFromSerialized(payload.serializedData()));
    }
}
