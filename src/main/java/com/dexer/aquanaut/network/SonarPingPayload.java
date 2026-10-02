package com.dexer.aquanaut.network;

import java.util.List;
import java.util.UUID;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.client.sonar.ClientSonarData;
import com.dexer.aquanaut.common.sonar.SonarReturn;
import com.dexer.aquanaut.common.sonar.SonarScan;
import com.dexer.aquanaut.common.sonar.SonarSignal;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * One ping, from the server that read it to everyone who can watch it.
 *
 * <p>
 * The reading is the server's: it owns the world, it owns the blocks and the creatures in it, and
 * the client is told only what came back and from where. Nothing about the answer is re-derived on
 * the client, so two players looking at the same ping can never be told two different things — and
 * a client with a modified item cannot invent contacts the server never heard.
 *
 * <p>
 * What the client does with it is the whole animation: the ring crossing the water, the echoes
 * running home, the sweep on the scope and the moment each voice sounds are all functions of the
 * ping's age, so a single packet per ping is enough and a client that joins late is simply not told
 * about pings it missed. What the packet does <em>not</em> carry is any statement of what was found:
 * a contact is a direction, a distance and a strength, and the voice it answers in is the only thing
 * the instrument says about its nature. A sounder tells a diver where to look; it has never told
 * them what they will see.
 *
 * <p>
 * The shooter is named so that a reading replaces the one that instrument last took rather than
 * piling up behind it. Without it a busy dive would fill the dial with a history nobody asked for.
 *
 * <p>
 * It goes to everybody nearby rather than to the diver alone. A pulse is loud and its ring is a
 * twenty-block sphere of moving water: a diver's buddy should be able to see that their companion
 * has sounded, and see what came back. The list is capped by the scan itself, so the packet is a few
 * hundred bytes.
 */
public record SonarPingPayload(UUID shooter, Vec3 origin, List<SonarReturn> returns) implements CustomPacketPayload {

    /**
     * How far away a player may be and still be told about a ping, in blocks. Comfortably past the
     * instrument's own range, because the wavefront is worth watching from outside it.
     */
    private static final double BROADCAST_RADIUS = 48.0D;

    /** How many contacts a packet may carry. The scan caps itself at the same number. */
    private static final int MAX_RETURNS = SonarScan.MAX_CONTACTS;

    public static final Type<SonarPingPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "sonar_ping"));

    /**
     * Three doubles rather than three floats: the origin is a world position, and twenty blocks of
     * effect drawn from a position that has been rounded to single precision is a visible wobble on
     * a contact that is meant to be pinned to a block.
     */
    private static final StreamCodec<ByteBuf, Vec3> VECTOR = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, vector -> vector.x,
            ByteBufCodecs.DOUBLE, vector -> vector.y,
            ByteBufCodecs.DOUBLE, vector -> vector.z,
            Vec3::new);

    private static final StreamCodec<ByteBuf, SonarReturn> CONTACT = StreamCodec.composite(
            ByteBufCodecs.BYTE, contact -> (byte) contact.signal().ordinal(),
            VECTOR, SonarReturn::offset,
            ByteBufCodecs.FLOAT, SonarReturn::strength,
            ByteBufCodecs.VAR_INT, SonarReturn::count,
            (signal, offset, strength, count) -> new SonarReturn(SonarSignal.byOrdinal(signal), offset, strength,
                    count));

    public static final StreamCodec<ByteBuf, SonarPingPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SonarPingPayload::shooter,
            VECTOR, SonarPingPayload::origin,
            CONTACT.apply(ByteBufCodecs.list(MAX_RETURNS)), SonarPingPayload::returns,
            SonarPingPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Sends a ping to everybody standing close enough to watch it. */
    public static void broadcast(ServerLevel level, UUID shooter, Vec3 origin, List<SonarReturn> returns) {
        SonarPingPayload payload = new SonarPingPayload(shooter, origin, List.copyOf(returns));
        double reach = BROADCAST_RADIUS * BROADCAST_RADIUS;

        for (ServerPlayer player : level.players()) {
            if (player.level() == level && player.position().distanceToSqr(origin) <= reach) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    public static void handle(SonarPingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientSonarData.accept(payload.shooter(), payload.origin(), payload.returns()));
    }
}
