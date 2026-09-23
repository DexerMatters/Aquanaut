package com.dexer.aquanaut.client.light;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.light.DynamicLightMath;
import com.dexer.aquanaut.common.light.DynamicLightId;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/** Frame-local, renderer-independent dynamic-light snapshot and packed-light calculator. */
@EventBusSubscriber(modid = Aquanaut.MODID, value = Dist.CLIENT)
public final class ClientDynamicLightManager {
    public static final double DEFAULT_RADIUS = 7.75D;
    public static final int MAX_SOURCES = 8;

    private static final Map<net.minecraft.resources.ResourceLocation, ClientDynamicLightProvider> PROVIDERS =
            new LinkedHashMap<>();
    private static volatile ClientLevel snapshotLevel;
    private static volatile List<ClientDynamicLightRequest> snapshot = List.of();

    private ClientDynamicLightManager() {
    }

    public static void registerProvider(net.minecraft.resources.ResourceLocation id,
            ClientDynamicLightProvider provider) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(provider, "provider");
        if (PROVIDERS.putIfAbsent(id, provider) != null) {
            throw new IllegalStateException("Duplicate dynamic-light provider: " + id);
        }
    }

    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            if (snapshotLevel != null && minecraft.levelRenderer != null) {
                dirtySections(minecraft.levelRenderer, snapshot, List.of());
            }
            clear();
            return;
        }

        List<ClientDynamicLightRequest> old = snapshot;
        if (snapshotLevel != null && snapshotLevel != level) {
            // The renderer is being reused for a new world/dimension. Invalidate the old
            // sections before replacing the immutable frame snapshot so stale illumination
            // cannot survive a level switch.
            dirtySections(minecraft.levelRenderer, old, List.of());
            old = List.of();
            snapshot = List.of();
        }
        Map<DynamicLightId, ClientDynamicLightRequest> requests = new LinkedHashMap<>();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        for (Map.Entry<net.minecraft.resources.ResourceLocation, ClientDynamicLightProvider> entry : PROVIDERS.entrySet()) {
            ClientDynamicLightCollector collector = new ClientDynamicLightCollector(entry.getKey());
            entry.getValue().collect(level, partialTick, collector);
            for (ClientDynamicLightRequest request : collector.requests()) {
                BlockPos sourceCell = BlockPos.containing(request.position());
                if (level.isInWorldBounds(sourceCell) && level.isAreaLoaded(sourceCell, 0)) {
                    requests.put(request.id(), request);
                }
            }
        }

        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        List<ClientDynamicLightRequest> next = new ArrayList<>(requests.values());
        next.sort(Comparator.comparingDouble(request -> request.position().distanceToSqr(camera)));
        if (next.size() > MAX_SOURCES) {
            next = new ArrayList<>(next.subList(0, MAX_SOURCES));
        }
        List<ClientDynamicLightRequest> immutable = List.copyOf(next);
        snapshotLevel = level;
        snapshot = immutable;
        dirtySections(minecraft.levelRenderer, old, immutable);
    }

    public static int applyPackedLight(ClientLevel level, BlockPos pos, int vanillaPackedLight) {
        if (snapshotLevel != level || snapshot.isEmpty()) {
            return vanillaPackedLight;
        }
        Vec3 sample = Vec3.atCenterOf(pos);
        return applyDynamicLevel(vanillaPackedLight, dynamicLevelAt(sample, snapshot));
    }

    public static int dynamicLevelAt(Vec3 sample, List<ClientDynamicLightRequest> sources) {
        int dynamicLevel = 0;
        for (ClientDynamicLightRequest source : sources) {
            double distance = source.position().distanceTo(sample);
            dynamicLevel = Math.max(dynamicLevel,
                    DynamicLightMath.falloffLevel(source.level(), distance, source.radius()));
        }
        return dynamicLevel;
    }

    public static int applyDynamicLevel(int vanillaPackedLight, int dynamicLevel) {
        return DynamicLightMath.applyPackedBlockLevel(vanillaPackedLight, dynamicLevel);
    }

    public static List<ClientDynamicLightRequest> snapshot() {
        return snapshot;
    }

    private static void dirtySections(LevelRenderer renderer, List<ClientDynamicLightRequest> old,
            List<ClientDynamicLightRequest> next) {
        Map<DynamicLightId, ClientDynamicLightRequest> before = byId(old);
        Map<DynamicLightId, ClientDynamicLightRequest> after = byId(next);
        List<ClientDynamicLightRequest> changed = new ArrayList<>();
        before.forEach((id, source) -> {
            ClientDynamicLightRequest replacement = after.get(id);
            if (replacement == null) {
                changed.add(source);
            } else if (source.position().distanceToSqr(replacement.position()) >= (1.0D / 16.0D) * (1.0D / 16.0D)
                    || source.level() != replacement.level()
                    || source.radius() != replacement.radius()) {
                changed.add(source);
                changed.add(replacement);
            }
        });
        after.forEach((id, source) -> {
            if (!before.containsKey(id)) {
                changed.add(source);
            }
        });
        for (ClientDynamicLightRequest source : changed) {
            int radius = (int) Math.ceil(source.radius());
            int minX = SectionCoords.section(source.position().x - radius);
            int maxX = SectionCoords.section(source.position().x + radius);
            int minY = SectionCoords.section(source.position().y - radius);
            int maxY = SectionCoords.section(source.position().y + radius);
            int minZ = SectionCoords.section(source.position().z - radius);
            int maxZ = SectionCoords.section(source.position().z + radius);
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        renderer.setSectionDirty(x, y, z);
                    }
                }
            }
        }
    }

    private static Map<DynamicLightId, ClientDynamicLightRequest> byId(List<ClientDynamicLightRequest> requests) {
        Map<DynamicLightId, ClientDynamicLightRequest> unique = new LinkedHashMap<>();
        requests.forEach(request -> unique.put(request.id(), request));
        return unique;
    }

    private static void clear() {
        snapshot = List.of();
        snapshotLevel = null;
    }

    private static final class SectionCoords {
        private static int section(double coordinate) {
            return (int) Math.floor(coordinate) >> 4;
        }
    }
}
