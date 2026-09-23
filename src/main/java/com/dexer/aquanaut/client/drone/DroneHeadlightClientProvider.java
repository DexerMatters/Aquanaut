package com.dexer.aquanaut.client.drone;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.drone.DroneHeadlightGeometry;
import com.dexer.aquanaut.common.entity.SubmarineDroneEntity;
import com.dexer.aquanaut.common.light.DynamicLightOwner;
import com.dexer.aquanaut.client.light.ClientDynamicLightCollector;
import com.dexer.aquanaut.client.light.ClientDynamicLightProvider;
import com.dexer.aquanaut.common.searchlight.SearchlightTargeting;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.minecraft.world.phys.Vec3;

/** Smooth client counterpart of the drone's real endpoint light. */
public final class DroneHeadlightClientProvider implements ClientDynamicLightProvider {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID,
            "drone_headlight");
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

        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof SubmarineDroneEntity drone)
                    || !drone.isAlive()
                    || !drone.isControlled()
                    || !drone.isHeadlightOn()) {
                continue;
            }
            seen.add(drone.getUUID());
            Vec3 raw = drone.getViewVector(partialTick);
            if (raw.lengthSqr() < 1.0E-8D) {
                continue;
            }
            raw = raw.normalize();
            Vec3 previous = smoothedDirections.get(drone.getUUID());
            Vec3 smoothed = previous == null ? raw : previous.lerp(raw, alpha).normalize();
            smoothedDirections.put(drone.getUUID(), smoothed);
            Vec3 lens = DroneHeadlightGeometry.lens(drone, partialTick);
            double range = DroneHeadlightGeometry.range(drone.isEyeInFluidType(NeoForgeMod.WATER_TYPE.value()));
            Vec3 target = SearchlightTargeting.continuousTarget(level, lens, smoothed, range, drone);
            if (target == null || !level.isInWorldBounds(net.minecraft.core.BlockPos.containing(target))
                    || !level.isAreaLoaded(net.minecraft.core.BlockPos.containing(target), 0)) {
                continue;
            }
            collector.submit(new DynamicLightOwner.Entity(drone.getUUID()), 0, target, 15, 7.75D);
        }
        smoothedDirections.keySet().removeIf(id -> !seen.contains(id));
    }
}
