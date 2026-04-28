package com.dexer.aquanaut.common;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.core.AttachmentRegistry;
import com.dexer.aquanaut.network.ExtraAirPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Intercepts vanilla air-supply drain by comparing air before and after
 * {@code Entity.baseTick()} runs each tick. When the player's air went down
 * and they have extra air remaining, the main air bar is restored and the
 * extra supply is consumed instead.
 *
 * <p>
 * This avoids hooking {@code decreaseAirSupply}, which is not present as a
 * named method in Minecraft 1.21's searge mappings and therefore cannot be
 * targeted by a Mixin {@code @Inject}.
 *
 * <p>
 * After any change that would alter the client-side bubble display, a sync
 * packet is sent so {@link com.dexer.aquanaut.client.ClientHudEvents} can
 * render the extra bubble layers correctly.
 */
@EventBusSubscriber(modid = Aquanaut.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class PlayerAirEvents {

    /** Stores each server-player's air supply sampled just before their tick. */
    private static final Map<UUID, Integer> PRE_TICK_AIR = new HashMap<>();

    /**
     * Tracks the display state sent to each client so we only re-send when
     * something visible has changed (avoids one packet per tick while diving).
     */
    private static final Map<UUID, Long> LAST_SENT_STATE = new HashMap<>();

    private PlayerAirEvents() {
    }

    /** Clean up tracked state when a player disconnects. */
    @SubscribeEvent
    public static void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        PRE_TICK_AIR.remove(uuid);
        LAST_SENT_STATE.remove(uuid);
        AirRegenTracker.AIR_INCREASE_CALLED.remove(uuid);
    }

    @SubscribeEvent
    public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer))
            return;
        PRE_TICK_AIR.put(player.getUUID(), player.getAirSupply());
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer))
            return;

        Integer prevAir = PRE_TICK_AIR.remove(player.getUUID());
        if (prevAir == null)
            return;

        int currAir = player.getAirSupply();

        int extraAirBefore = player.getData(AttachmentRegistry.EXTRA_AIR_SUPPLY.get());
        int maxExtraAir = player.getData(AttachmentRegistry.MAX_EXTRA_AIR_SUPPLY.get());
        int maxAirSupply = player.getMaxAirSupply();

        // Vanilla drained some air this tick — consume extra air instead.
        if (currAir < prevAir) {
            if (extraAirBefore > 0) {
                int drained = prevAir - currAir;
                int consumed = Math.min(drained, extraAirBefore);
                player.setAirSupply(currAir + consumed);
                int newExtra = extraAirBefore - consumed;
                player.setData(AttachmentRegistry.EXTRA_AIR_SUPPLY.get(), newExtra);
            }
        } else if (extraAirBefore < maxExtraAir && maxExtraAir > 0) {
            // Extra air only regens once the main bar is fully topped off.
            // - AIR_INCREASE_CALLED flag: increaseAirSupply was invoked but main bar was
            // already full (bubble column while extra tanks are keeping air at max)
            // - surfaced: player is not underwater — start slow regen regardless
            // of whether the main bar ticked up this frame.
            boolean mainBarFull = currAir == maxAirSupply;
            boolean vanillaRegenning = mainBarFull
                    && AirRegenTracker.AIR_INCREASE_CALLED.remove(player.getUUID());
            boolean surfaced = !player.isUnderWater();
            if (vanillaRegenning || surfaced) {
                int regenPerTick = computeRegenPerTick(maxAirSupply, maxExtraAir);
                int newExtra = Math.min(extraAirBefore + regenPerTick, maxExtraAir);
                player.setData(AttachmentRegistry.EXTRA_AIR_SUPPLY.get(), newExtra);
            }
        }
        // Clear any leftover flag for this player (drain path didn't consume it)
        AirRegenTracker.AIR_INCREASE_CALLED.remove(player.getUUID());

        // Sync the display state to the client whenever it changes.
        syncIfNeeded(serverPlayer);
    }

    // ── Sync helpers ────────────────────────────────────────────────────────

    /**
     * Computes how many extra-air ticks to restore per game tick so that a full
     * recharge takes 2.5–4 s, interpolated by total air capacity.
     */
    private static int computeRegenPerTick(int maxAirSupply, int maxExtraAir) {
        // Normalise extra tanks in range [0, maxAirSupply*5]:
        // t=0 (1 tank) → 50 ticks ≈ 2.5 s
        // t=1 (6+ tanks) → 80 ticks ≈ 4 s
        float t = Mth.clamp((float) maxExtraAir / (maxAirSupply * 5f), 0f, 1f);
        float periodTicks = Mth.lerp(t, 50f, 80f);
        return Math.max(1, (int) Math.ceil(maxExtraAir / periodTicks));
    }

    /**
     * Sends a sync packet if the extra-air display (bubble count / layer count)
     * or the max extra air has changed since the last packet was sent.
     */
    private static void syncIfNeeded(ServerPlayer player) {
        int extraAir = player.getData(AttachmentRegistry.EXTRA_AIR_SUPPLY.get());
        int maxExtraAir = player.getData(AttachmentRegistry.MAX_EXTRA_AIR_SUPPLY.get());
        int maxAirSupply = player.getMaxAirSupply();

        long newState = encodeState(extraAir, maxExtraAir, maxAirSupply);
        Long lastState = LAST_SENT_STATE.get(player.getUUID());
        // Force-send when fully recharged so the client can hide the overlay.
        // The bubble-level throttle skips the final regen tick otherwise.
        boolean forceSend = extraAir >= maxExtraAir;

        if (lastState == null || lastState != newState || forceSend) {
            PacketDistributor.sendToPlayer(player, new ExtraAirPayload(extraAir, maxExtraAir));
            LAST_SENT_STATE.put(player.getUUID(), newState);
        }
    }

    /**
     * Encodes the visual display state as a single {@code long}:
     * upper 32 bits = max extra air, lower 32 bits = display bubble-level
     * (layer * 11 + filled-bubbles-in-top-layer).
     * <p>
     * Packets are sent only when this value changes, throttling to at most
     * one packet per bubble change (~120 ticks per bubble per layer).
     */
    private static long encodeState(int extraAir, int maxExtraAir, int maxAirSupply) {
        int bubbleLevel;
        if (extraAir <= 0 || maxAirSupply <= 0) {
            bubbleLevel = 0;
        } else {
            int layer = (extraAir - 1) / maxAirSupply;
            int topAir = extraAir - layer * maxAirSupply;
            int topBubbles = Mth.ceil((double) topAir * 10.0 / maxAirSupply);
            bubbleLevel = layer * 11 + topBubbles;
        }
        return ((long) maxExtraAir << 32) | (bubbleLevel & 0xFFFFFFFFL);
    }
}
