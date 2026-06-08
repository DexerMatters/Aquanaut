package com.dexer.aquanaut.network;

import com.dexer.aquanaut.Aquanaut;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CloseAquariumPayload() implements CustomPacketPayload {

    public static final Type<CloseAquariumPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "close_aquarium"));

    public static final StreamCodec<ByteBuf, CloseAquariumPayload> STREAM_CODEC = StreamCodec.unit(new CloseAquariumPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CloseAquariumPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                serverPlayer.containerMenu.removed(serverPlayer);
                serverPlayer.containerMenu = serverPlayer.inventoryMenu;
            }
        });
    }
}
