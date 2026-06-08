package com.dexer.aquanaut.network;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumInventoryHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AquariumFishTransferPayload(int sourceIndex, int targetIndex) implements CustomPacketPayload {

    public static final Type<AquariumFishTransferPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "aquarium_fish_transfer"));

    public static final StreamCodec<ByteBuf, AquariumFishTransferPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, AquariumFishTransferPayload::sourceIndex,
            ByteBufCodecs.INT, AquariumFishTransferPayload::targetIndex,
            AquariumFishTransferPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AquariumFishTransferPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                AquariumInventoryHelper.moveFish(serverPlayer, payload.sourceIndex(), payload.targetIndex());
            }
        });
    }
}
