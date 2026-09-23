package com.dexer.aquanaut.common.light;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.block.DynamicLightBlock;
import com.dexer.aquanaut.core.BlockRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Server-side engine for all Aquanaut dynamic lights. Providers only describe their current
 * sources; this class owns block placement, transitions, overlap and lifecycle cleanup.
 */
@EventBusSubscriber(modid = Aquanaut.MODID)
public final class ServerDynamicLightManager {
    public static final int STALE_CHECK_TICKS = 40;

    private static final Map<net.minecraft.resources.ResourceLocation, ServerDynamicLightProvider> PROVIDERS =
            new LinkedHashMap<>();
    private static final Map<ServerLevel, Map<DynamicLightId, SourceState>> SOURCES = new IdentityHashMap<>();
    private static final Map<ServerLevel, Set<BlockPos>> ACTIVE = new IdentityHashMap<>();
    private static final Map<ServerLevel, Map<BlockPos, FluidState>> ORIGINAL_FLUIDS = new IdentityHashMap<>();

    private ServerDynamicLightManager() {
    }

    public static void registerProvider(net.minecraft.resources.ResourceLocation id,
            ServerDynamicLightProvider provider) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(provider, "provider");
        if (PROVIDERS.putIfAbsent(id, provider) != null) {
            throw new IllegalStateException("Duplicate dynamic-light provider: " + id);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Set<ServerLevel> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ServerLevel level : event.getServer().getAllLevels()) {
            seen.add(level);
            collectAndReconcile(level);
        }
        SOURCES.keySet().removeIf(level -> !seen.contains(level));
        ACTIVE.keySet().removeIf(level -> !seen.contains(level));
        ORIGINAL_FLUIDS.keySet().removeIf(level -> !seen.contains(level));
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MinecraftServer server = event.getServer();
        for (ServerLevel level : server.getAllLevels()) {
            Set<BlockPos> active = ACTIVE.get(level);
            if (active == null) {
                continue;
            }
            for (BlockPos pos : new HashSet<>(active)) {
                if (level.isAreaLoaded(pos, 0)) {
                    removeOwnedSource(level, pos);
                }
            }
        }
        clear();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        clear();
    }

    public static boolean isActive(ServerLevel level, BlockPos pos) {
        Set<BlockPos> active = ACTIVE.get(level);
        return active != null && active.contains(pos);
    }

    public static void removeOwnedSource(ServerLevel level, BlockPos pos) {
        FluidState original = storedFluid(level, pos);
        BlockState current = level.getBlockState(pos);
        if (!current.is(BlockRegistry.DYNAMIC_LIGHT.get())) {
            forgetOriginalFluid(level, pos);
            return;
        }

        BlockState restored = original != null && original.is(FluidTags.WATER)
                ? original.createLegacyBlock()
                : current.getValue(DynamicLightBlock.WATERLOGGED)
                        ? Fluids.WATER.defaultFluidState().createLegacyBlock()
                        : Blocks.AIR.defaultBlockState();
        level.setBlock(pos, restored, Block.UPDATE_CLIENTS);
        forgetOriginalFluid(level, pos);
    }

    private static void collectAndReconcile(ServerLevel level) {
        Map<DynamicLightId, ServerDynamicLightRequest> desired = new LinkedHashMap<>();
        for (Map.Entry<net.minecraft.resources.ResourceLocation, ServerDynamicLightProvider> entry : PROVIDERS.entrySet()) {
            ServerDynamicLightCollector collector = new ServerDynamicLightCollector(entry.getKey());
            entry.getValue().collect(level, collector);
            for (ServerDynamicLightRequest request : collector.requests()) {
                if (validTarget(level, request.position())) {
                    desired.put(request.id(), request);
                }
            }
        }

        Map<DynamicLightId, SourceState> states = SOURCES.computeIfAbsent(level, ignored -> new LinkedHashMap<>());
        states.keySet().removeIf(id -> !desired.containsKey(id));
        for (ServerDynamicLightRequest request : desired.values()) {
            SourceState state = states.get(request.id());
            if (state == null) {
                states.put(request.id(), SourceState.stable(request));
            } else {
                state.update(request);
            }
        }
        for (SourceState state : states.values()) {
            state.advance();
        }

        Map<BlockPos, Integer> contributions = new HashMap<>();
        for (SourceState state : states.values()) {
            state.contributions(contributions);
        }
        reconcileCells(level, contributions);
    }

    private static void reconcileCells(ServerLevel level, Map<BlockPos, Integer> desired) {
        Set<BlockPos> previous = ACTIVE.getOrDefault(level, Set.of());
        Set<BlockPos> active = new HashSet<>();

        // Make before break: install and brighten every replacement first. Minecraft propagates
        // block light asynchronously, so removing the old source first creates a visible dark
        // frame even when both mutations happen during the same server tick.
        for (Map.Entry<BlockPos, Integer> entry : desired.entrySet()) {
            BlockPos pos = entry.getKey();
            if (!level.isInWorldBounds(pos) || !level.isAreaLoaded(pos, 0)) {
                continue;
            }
            BlockState current = level.getBlockState(pos);
            if (current.is(BlockRegistry.DYNAMIC_LIGHT.get())) {
                BlockState wanted = current.setValue(DynamicLightBlock.LIGHT_LEVEL,
                        Math.max(1, Math.min(15, entry.getValue())));
                if (!wanted.equals(current)) {
                    level.setBlock(pos, wanted, Block.UPDATE_CLIENTS);
                }
                active.add(pos);
                continue;
            }
            if (!placeable(current)) {
                // The source was externally replaced. Keep the safety guarantee (never delete
                // foreign blocks) and discard any fluid bookkeeping for this ownership episode.
                forgetOriginalFluid(level, pos);
                continue;
            }
            // A player may have replaced an old transient block while the provider still targets
            // the cell. That cell is a new ownership episode; never restore the earlier fluid.
            forgetOriginalFluid(level, pos);
            FluidState original = current.getFluidState();
            if (level.setBlock(pos, DynamicLightBlock.stateFor(level, pos, entry.getValue()), Block.UPDATE_CLIENTS)) {
                active.add(pos);
                if (original.is(FluidTags.WATER)) {
                    ORIGINAL_FLUIDS.computeIfAbsent(level, ignored -> new HashMap<>()).put(pos, original);
                }
            }
        }

        for (BlockPos old : previous) {
            if (!desired.containsKey(old) && level.isAreaLoaded(old, 0)) {
                removeOwnedSource(level, old);
            }
        }
        ACTIVE.put(level, active);
    }

    private static boolean validTarget(ServerLevel level, BlockPos pos) {
        if (!level.isInWorldBounds(pos) || !level.isAreaLoaded(pos, 0)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.is(BlockRegistry.DYNAMIC_LIGHT.get()) || placeable(state);
    }

    private static boolean placeable(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER);
    }

    private static FluidState storedFluid(ServerLevel level, BlockPos pos) {
        Map<BlockPos, FluidState> fluids = ORIGINAL_FLUIDS.get(level);
        return fluids == null ? null : fluids.get(pos);
    }

    private static FluidState forgetOriginalFluid(ServerLevel level, BlockPos pos) {
        Map<BlockPos, FluidState> fluids = ORIGINAL_FLUIDS.get(level);
        if (fluids == null) {
            return null;
        }
        FluidState result = fluids.remove(pos);
        if (fluids.isEmpty()) {
            ORIGINAL_FLUIDS.remove(level);
        }
        return result;
    }

    private static void clear() {
        SOURCES.clear();
        ACTIVE.clear();
        ORIGINAL_FLUIDS.clear();
    }

    private static final class SourceState {
        private BlockPos stablePosition;
        private int stableLevel;
        private BlockPos outgoingPosition;
        private int outgoingLevel;
        private BlockPos targetPosition;
        private int targetLevel;
        private int progress;
        private LightTransition transition = LightTransition.IMMEDIATE;

        private static SourceState stable(ServerDynamicLightRequest request) {
            SourceState state = new SourceState();
            state.stablePosition = request.position();
            state.stableLevel = request.level();
            return state;
        }

        private void update(ServerDynamicLightRequest request) {
            if (!transitioning()) {
                if (stablePosition.equals(request.position())) {
                    stableLevel = request.level();
                } else if (request.transition().ticks() > 0) {
                    outgoingPosition = stablePosition;
                    outgoingLevel = stableLevel;
                    targetPosition = request.position();
                    targetLevel = request.level();
                    progress = 0;
                    transition = request.transition();
                } else {
                    stablePosition = request.position();
                    stableLevel = request.level();
                }
                return;
            }

            if (request.transition().ticks() <= 0) {
                // A provider can override an in-flight fade with an immediate request (for
                // example, a hard lifecycle transition or a gameplay-critical brightness edit).
                stablePosition = request.position();
                stableLevel = request.level();
                outgoingPosition = null;
                targetPosition = null;
                progress = 0;
                transition = LightTransition.IMMEDIATE;
                return;
            }

            if (outgoingPosition.equals(request.position())) {
                stablePosition = outgoingPosition;
                stableLevel = request.level();
                outgoingPosition = null;
                targetPosition = null;
                progress = 0;
                transition = LightTransition.IMMEDIATE;
            } else {
                targetPosition = request.position();
                targetLevel = request.level();
            }
        }

        private void advance() {
            if (transitioning()) {
                progress++;
                if (progress >= transition.ticks()) {
                    stablePosition = targetPosition;
                    stableLevel = targetLevel;
                    outgoingPosition = null;
                    targetPosition = null;
                    progress = 0;
                    transition = LightTransition.IMMEDIATE;
                }
            }
        }

        private boolean transitioning() {
            return outgoingPosition != null && targetPosition != null;
        }

        private void contributions(Map<BlockPos, Integer> result) {
            if (!transitioning()) {
                result.merge(stablePosition, stableLevel, Math::max);
                return;
            }
            int outgoing = transition.outgoingLevel(outgoingLevel, progress);
            int incoming = transition.incomingLevel(targetLevel, progress);
            if (outgoing > 0) {
                result.merge(outgoingPosition, outgoing, Math::max);
            }
            if (incoming > 0) {
                result.merge(targetPosition, incoming, Math::max);
            }
        }
    }
}
