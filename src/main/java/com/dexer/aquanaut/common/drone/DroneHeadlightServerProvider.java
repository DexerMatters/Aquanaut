package com.dexer.aquanaut.common.drone;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.entity.SubmarineDroneEntity;
import com.dexer.aquanaut.common.light.DynamicLightOwner;
import com.dexer.aquanaut.common.light.LightTransition;
import com.dexer.aquanaut.common.light.ServerDynamicLightCollector;
import com.dexer.aquanaut.common.light.ServerDynamicLightProvider;
import com.dexer.aquanaut.common.searchlight.SearchlightTargeting;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForgeMod;

/** Supplies a real source at each active drone headlight's forward endpoint. */
public final class DroneHeadlightServerProvider implements ServerDynamicLightProvider {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID,
            "drone_headlight");

    @Override
    public void collect(ServerLevel level, ServerDynamicLightCollector collector) {
        for (var entity : level.getEntities().getAll()) {
            if (!(entity instanceof SubmarineDroneEntity drone)
                    || !drone.isAlive()
                    || !drone.isControlled()
                    || !drone.isHeadlightOn()) {
                continue;
            }
            var direction = drone.getViewVector(1.0F);
            var lens = DroneHeadlightGeometry.lens(drone, 1.0F);
            double range = DroneHeadlightGeometry.range(drone.isEyeInFluidType(NeoForgeMod.WATER_TYPE.value()));
            BlockPos target = SearchlightTargeting.blockTarget(level, lens, direction, range, drone);
            if (target != null) {
                collector.submit(new DynamicLightOwner.Entity(drone.getUUID()), 0, target, 15,
                        LightTransition.FOUR_TICK_HANDOFF);
            }
        }
    }
}
