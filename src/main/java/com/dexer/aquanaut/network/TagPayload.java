package com.dexer.aquanaut.network;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.entity.AbstractTaggableEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Applies a tag the player typed into the tag editor.
 *
 * <p>
 * The client never decides anything: it sends what the player asked for and the server re-validates
 * the name, the duplication rule and the colour before touching the entity. A rejection comes back
 * as an action-bar message so the player learns why the tag did not change.
 *
 * <p>
 * It carries an entity id rather than a kind, because a tag is a tag: the same request names a
 * cursor or a drone, and the server only has to check that whatever it finds is taggable.
 */
public record TagPayload(int entityId, String name, int color) implements CustomPacketPayload {

    /** How far the player may be from the marker they are renaming. */
    private static final double MAX_EDIT_DISTANCE_SQ = 64.0D;

    public static final Type<TagPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "tag"));

    public static final StreamCodec<ByteBuf, TagPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, TagPayload::entityId,
            ByteBufCodecs.STRING_UTF8, TagPayload::name,
            ByteBufCodecs.INT, TagPayload::color,
            TagPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TagPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!(player.level() instanceof ServerLevel level)) {
                return;
            }
            Entity entity = level.getEntity(payload.entityId());
            if (!(entity instanceof AbstractTaggableEntity taggable)) {
                return;
            }
            if (player.distanceToSqr(entity) > MAX_EDIT_DISTANCE_SQ) {
                return;
            }

            AbstractTaggableEntity.TagUpdate update = taggable.applyCustomTag(level, payload.name(), payload.color());
            if (update != AbstractTaggableEntity.TagUpdate.APPLIED) {
                player.displayClientMessage(Component.translatable(messageKey(update)), true);
            }
        });
    }

    private static String messageKey(AbstractTaggableEntity.TagUpdate update) {
        return switch (update) {
            case INVALID_NAME -> "gui.aquanaut.tag.error.invalid";
            case NAME_TAKEN -> "gui.aquanaut.tag.error.taken";
            case UNKNOWN_COLOR -> "gui.aquanaut.tag.error.color";
            case APPLIED -> "gui.aquanaut.tag.applied";
        };
    }
}
