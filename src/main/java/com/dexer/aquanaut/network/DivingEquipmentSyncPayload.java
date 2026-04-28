package com.dexer.aquanaut.network;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.client.ClientDivingEquipmentData;
import com.dexer.aquanaut.common.diving.DivingEquipmentHelper;
import com.dexer.aquanaut.common.diving.DivingEquipmentSlotType;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-to-client sync of diving slot display stacks.
 */
public record DivingEquipmentSyncPayload(String maskItemId, String tankItemId, String flippersItemId)
        implements CustomPacketPayload {

    public static final Type<DivingEquipmentSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "diving_slot_sync"));

    public static final StreamCodec<ByteBuf, DivingEquipmentSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, DivingEquipmentSyncPayload::maskItemId,
            ByteBufCodecs.STRING_UTF8, DivingEquipmentSyncPayload::tankItemId,
            ByteBufCodecs.STRING_UTF8, DivingEquipmentSyncPayload::flippersItemId,
            DivingEquipmentSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static DivingEquipmentSyncPayload fromPlayer(ServerPlayer player) {
        return new DivingEquipmentSyncPayload(
                DivingEquipmentHelper.getSyncItemId(player, DivingEquipmentSlotType.MASK),
                DivingEquipmentHelper.getSyncItemId(player, DivingEquipmentSlotType.TANK),
                DivingEquipmentHelper.getSyncItemId(player, DivingEquipmentSlotType.FLIPPERS));
    }

    public static void handle(DivingEquipmentSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientDivingEquipmentData.setFromIds(
                payload.maskItemId(), payload.tankItemId(), payload.flippersItemId()));
    }
}