package com.dexer.aquanaut.network;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.diving.DivingEquipmentHelper;
import com.dexer.aquanaut.common.diving.DivingEquipmentSlotType;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server click action for the custom diving equipment slots.
 */
public record DivingEquipmentClickPayload(int slotIndex, int mouseButton, String carriedItemId)
        implements CustomPacketPayload {

    public static final Type<DivingEquipmentClickPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "diving_slot_click"));

    public static final StreamCodec<ByteBuf, DivingEquipmentClickPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, DivingEquipmentClickPayload::slotIndex,
            ByteBufCodecs.INT, DivingEquipmentClickPayload::mouseButton,
            ByteBufCodecs.STRING_UTF8, DivingEquipmentClickPayload::carriedItemId,
            DivingEquipmentClickPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DivingEquipmentClickPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }

            DivingEquipmentSlotType slotType = DivingEquipmentSlotType.fromIndex(payload.slotIndex());
            DivingEquipmentHelper.handleSlotClick(serverPlayer, slotType, payload.mouseButton(),
                    payload.carriedItemId());
            PacketDistributor.sendToPlayer(serverPlayer, DivingEquipmentSyncPayload.fromPlayer(serverPlayer));
        });
    }
}