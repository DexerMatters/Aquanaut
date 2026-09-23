package com.dexer.aquanaut.network;

import java.util.UUID;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.drone.SubmarineDroneService;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** One server-authoritative edge-triggered headlight toggle. */
public record SubmarineDroneHeadlightPayload(UUID droneId) implements CustomPacketPayload {
    public static final Type<SubmarineDroneHeadlightPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "submarine_drone_headlight"));

    public static final StreamCodec<ByteBuf, SubmarineDroneHeadlightPayload> STREAM_CODEC =
            StreamCodec.composite(UUIDUtil.STREAM_CODEC, SubmarineDroneHeadlightPayload::droneId,
                    SubmarineDroneHeadlightPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SubmarineDroneHeadlightPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SubmarineDroneService.toggleHeadlight(player, payload.droneId());
            }
        });
    }
}
