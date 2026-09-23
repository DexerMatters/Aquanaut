package com.dexer.aquanaut.network;

import java.util.List;
import java.util.UUID;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.entity.AbstractTaggableEntity;
import com.dexer.aquanaut.common.item.SubmarineCompassItem;
import com.dexer.aquanaut.core.ItemRegistry;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Records which marker the compass has been pointed at.
 *
 * <p>
 * The wheel is a client-side gesture, so the choice has to be sent up rather than written to the
 * stack locally — otherwise the NBT would desync the moment the item is dropped or traded. An empty
 * id means "back to nearest".
 */
public record CompassTargetPayload(String targetId) implements CustomPacketPayload {

    /** How far away a marker may be and still be chosen. */
    private static final double MAX_TARGET_DISTANCE_SQ = 256.0D * 256.0D;

    public static final Type<CompassTargetPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "compass_target"));

    public static final StreamCodec<ByteBuf, CompassTargetPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CompassTargetPayload::targetId,
            CompassTargetPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CompassTargetPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ItemStack compass = heldCompass(player);
            if (compass.isEmpty()) {
                return;
            }

            if (payload.targetId().isEmpty()) {
                SubmarineCompassItem.setTarget(compass, SubmarineCompassItem.TargetMode.NEAREST, null);
                return;
            }

            UUID id;
            try {
                id = UUID.fromString(payload.targetId());
            } catch (IllegalArgumentException malformed) {
                return;
            }

            // The client picks from what it can see; the server still checks the marker is real and
            // in range before writing it down.
            if (findMarker(player, id) == null) {
                return;
            }
            SubmarineCompassItem.setTarget(compass, SubmarineCompassItem.TargetMode.MARKER, id);
        });
    }

    private static ItemStack heldCompass(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(ItemRegistry.SUBMARINE_COMPASS.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static AbstractTaggableEntity findMarker(ServerPlayer player, UUID id) {
        if (!(player.level() instanceof ServerLevel level)) {
            return null;
        }
        List<AbstractTaggableEntity> found = level.getEntitiesOfClass(AbstractTaggableEntity.class,
                player.getBoundingBox().inflate(Math.sqrt(MAX_TARGET_DISTANCE_SQ)),
                marker -> id.equals(marker.getUUID()));
        return found.isEmpty() ? null : found.get(0);
    }
}
