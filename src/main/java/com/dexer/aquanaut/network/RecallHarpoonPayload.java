package com.dexer.aquanaut.network;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.entity.HarpoonEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RecallHarpoonPayload() implements CustomPacketPayload {
    public static final Type<RecallHarpoonPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "recall_harpoon"));
    public static final StreamCodec<ByteBuf, RecallHarpoonPayload> STREAM_CODEC = StreamCodec.unit(new RecallHarpoonPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RecallHarpoonPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !player.getMainHandItem().isEmpty()
                    || !player.getOffhandItem().isEmpty()) {
                return;
            }

            for (HarpoonEntity harpoon : player.level().getEntitiesOfClass(HarpoonEntity.class,
                    player.getBoundingBox().inflate(96.0D), entity -> entity.getOwner() == player)) {
                harpoon.beginRecall();
            }
        });
    }
}
