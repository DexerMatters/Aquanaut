package com.dexer.aquanaut.client.searchlight;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.client.light.ClientDynamicLightCollector;
import com.dexer.aquanaut.client.light.ClientDynamicLightProvider;
import com.dexer.aquanaut.common.light.DynamicLightOwner;
import com.dexer.aquanaut.common.searchlight.SearchlightServerProvider;
import com.dexer.aquanaut.common.searchlight.SearchlightTargeting;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Produces the smooth visual counterpart of each server-authoritative searchlight. */
public final class SearchlightClientProvider implements ClientDynamicLightProvider {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "searchlight");
    private static final double RESPONSE_SECONDS = 0.08D;
    private final Map<UUID, Vec3> smoothedDirections = new HashMap<>();
    private long lastNanos;
    private ClientLevel lastLevel;

    @Override
    public void collect(ClientLevel level, float partialTick, ClientDynamicLightCollector collector) {
        if (level != lastLevel) {
            smoothedDirections.clear();
            lastLevel = level;
        }
        long now = System.nanoTime();
        double deltaSeconds = lastNanos == 0L ? 1.0D / 60.0D
                : Math.min(0.1D, Math.max(0.0D, (now - lastNanos) / 1_000_000_000.0D));
        lastNanos = now;
        double alpha = 1.0D - Math.exp(-deltaSeconds / RESPONSE_SECONDS);
        Set<UUID> seen = new HashSet<>();

        for (Player player : level.players()) {
            InteractionHand hand = SearchlightServerProvider.litLamp(player);
            if (hand == null || !player.isAlive() || player.isSpectator()) {
                continue;
            }
            seen.add(player.getUUID());
            Vec3 raw = player.getViewVector(partialTick);
            if (raw.lengthSqr() < 1.0E-8D) {
                continue;
            }
            raw = raw.normalize();
            Vec3 previous = smoothedDirections.get(player.getUUID());
            Vec3 smoothed = previous == null ? raw : previous.lerp(raw, alpha).normalize();
            smoothedDirections.put(player.getUUID(), smoothed);
            Vec3 target = SearchlightTargeting.continuousTarget(level, player, hand, smoothed, partialTick);
            if (target == null || !level.isInWorldBounds(net.minecraft.core.BlockPos.containing(target))
                    || !level.isAreaLoaded(net.minecraft.core.BlockPos.containing(target), 0)) {
                continue;
            }
            collector.submit(new DynamicLightOwner.Entity(player.getUUID()), 0, target, 15, 7.75D);
        }
        smoothedDirections.keySet().removeIf(id -> !seen.contains(id));
    }
}
