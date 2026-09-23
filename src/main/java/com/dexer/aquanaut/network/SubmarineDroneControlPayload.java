package com.dexer.aquanaut.network;

import java.util.UUID;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.drone.DroneControlInput;
import com.dexer.aquanaut.common.drone.SubmarineDroneService;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * One frame of thruster command: which of the drone's thrusters the pilot is holding.
 *
 * <p>
 * It is sent only while the pilot is at the controls, and the drone treats it as a dead-man switch —
 * if the frames stop, so does the drone. That is why a lost packet is harmless and a lost client is
 * not a runaway.
 */
public record SubmarineDroneControlPayload(UUID droneId, int input) implements CustomPacketPayload {

    public static final Type<SubmarineDroneControlPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "submarine_drone_control"));

    public static final StreamCodec<ByteBuf, SubmarineDroneControlPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SubmarineDroneControlPayload::droneId,
            ByteBufCodecs.BYTE, payload -> (byte) DroneControlInput.sanitize(payload.input()),
            (droneId, input) -> new SubmarineDroneControlPayload(droneId, input & 0xFF));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SubmarineDroneControlPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SubmarineDroneService.applyInput(player, payload.droneId(), payload.input());
            }
        });
    }
}
