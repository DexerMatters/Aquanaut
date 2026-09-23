package com.dexer.aquanaut.network;

import java.util.UUID;

import com.dexer.aquanaut.Aquanaut;
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
 * "Link the controller I am holding to this drone", or "let go of it".
 *
 * <p>
 * Only the id and the intent cross the wire. The client names a drone it believes it can reach; the
 * server re-runs the range, line-of-sight and ownership checks before anything changes. Detaching
 * carries the id too, so the server can check that the controller really is pointing at the drone
 * being released rather than trusting a bare "stop".
 *
 * <p>
 * Which check applies depends on how the drone was named, and the client says so rather than being
 * trusted about it: a {@link Action#ATTACH_AIMED} drone has to be under the crosshair, an
 * {@link Action#ATTACH_LISTED} one only has to be within radio range.
 */
public record SubmarineDroneLinkPayload(UUID droneId, Action action) implements CustomPacketPayload {

    /** What the client is asking for. */
    public enum Action {
        /** Link to the drone this press started on, which must be in sight. */
        ATTACH_AIMED,
        /** Link to the drone chosen from the controller's list, which must be in range. */
        ATTACH_LISTED,
        /** Let go of whatever the controller is currently pointing at. */
        DETACH
    }

    public static final Type<SubmarineDroneLinkPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "submarine_drone_link"));

    public static final StreamCodec<ByteBuf, SubmarineDroneLinkPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SubmarineDroneLinkPayload::droneId,
            ByteBufCodecs.BYTE, payload -> (byte) payload.action().ordinal(),
            (droneId, ordinal) -> new SubmarineDroneLinkPayload(droneId, actionOf(ordinal)));

    /** An attach request for the drone under the crosshair. */
    public static SubmarineDroneLinkPayload attaching(UUID droneId) {
        return new SubmarineDroneLinkPayload(droneId, Action.ATTACH_AIMED);
    }

    /** An attach request for a drone picked out of the controller's list. */
    public static SubmarineDroneLinkPayload selecting(UUID droneId) {
        return new SubmarineDroneLinkPayload(droneId, Action.ATTACH_LISTED);
    }

    /** A detach request for whatever the controller is currently pointing at. */
    public static SubmarineDroneLinkPayload detaching(UUID droneId) {
        return new SubmarineDroneLinkPayload(droneId, Action.DETACH);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * An action this version does not know is read as a release: it is the one request that grants
     * nothing, so a stale or edited client cannot talk its way into a link.
     */
    private static Action actionOf(byte ordinal) {
        Action[] actions = Action.values();
        return ordinal >= 0 && ordinal < actions.length ? actions[ordinal] : Action.DETACH;
    }

    public static void handle(SubmarineDroneLinkPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            switch (payload.action()) {
                case ATTACH_AIMED -> SubmarineDroneService.attach(player, payload.droneId(), true);
                case ATTACH_LISTED -> SubmarineDroneService.attach(player, payload.droneId(), false);
                case DETACH -> SubmarineDroneService.detach(player);
            }
        });
    }
}
