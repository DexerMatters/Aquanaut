package com.dexer.aquanaut.network;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumInventoryHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenAquariumPayload() implements CustomPacketPayload {

    public static final Type<OpenAquariumPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "open_aquarium"));

    public static final StreamCodec<ByteBuf, OpenAquariumPayload> STREAM_CODEC = StreamCodec.unit(new OpenAquariumPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenAquariumPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                net.minecraft.world.SimpleMenuProvider provider = new net.minecraft.world.SimpleMenuProvider(
                        (containerId, inv, p) -> new com.dexer.aquanaut.common.inventory.aquarium.AquariumContainerMenu(containerId, inv),
                        net.minecraft.network.chat.Component.translatable("gui.aquanaut.aquarium"));
                AquariumInventoryHelper.syncToClient(serverPlayer);
                serverPlayer.openMenu(provider);
            }
        });
    }
}
