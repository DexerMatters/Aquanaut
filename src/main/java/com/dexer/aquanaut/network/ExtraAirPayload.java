package com.dexer.aquanaut.network;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.client.ClientAirData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Syncs only effective extra-air capacity; current air uses vanilla entity
 * sync.
 */
public record ExtraAirPayload(int maxExtraAir) implements CustomPacketPayload {

    public static final Type<ExtraAirPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "extra_air"));

    public static final StreamCodec<ByteBuf, ExtraAirPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ExtraAirPayload::maxExtraAir,
            ExtraAirPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ExtraAirPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientAirData.setMaxExtraAir(payload.maxExtraAir()));
    }
}
