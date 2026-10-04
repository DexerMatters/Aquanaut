package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.Aquanaut;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reports the vertical envelope the running overworld actually has, and warns once per level when
 * a save contains chunks that were generated before the abyssal height upgrade.
 *
 * <p>
 * The engine has no runtime height migration: chunks keep the sections they were written with, so
 * a chunk generated while the overworld was 384 blocks tall stays empty below Y=-64 even though the
 * dimension now reaches Y=-512, and new chunks grow the full depth next to it. That is a visible
 * seam in an old world, never silent — this listener names it in the log and points at the fix
 * (a fresh world), instead of leaving the player to guess why the abyss is hollow.
 * </p>
 */
@EventBusSubscriber(modid = Aquanaut.MODID)
public final class AbyssHeightNotice {

    /** Vanilla overworld floor; a save's chunks can only be hollow below it. */
    private static final int REFERENCE_MIN_BUILD_HEIGHT = -64;

    /** The floor the shipped abyssal overworld reaches. */
    private static final int ABYSSAL_MIN_BUILD_HEIGHT = -512;

    private static final Set<ResourceKey<Level>> WARNED = ConcurrentHashMap.newKeySet();

    private AbyssHeightNotice() {
    }

    /**
     * One line per overworld at startup, so "is the deep world actually active here?" is answered
     * by the log instead of by digging to Y=-500.
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (level.dimension() != Level.OVERWORLD) {
                continue;
            }
            int minY = level.getMinBuildHeight();
            int height = level.getHeight();
            if (minY <= ABYSSAL_MIN_BUILD_HEIGHT) {
                Aquanaut.LOGGER.info("Overworld envelope is abyssal: y={}..{} ({} blocks).",
                        minY, minY + height - 1, height);
            } else {
                Aquanaut.LOGGER.info("Overworld envelope is y={}..{} ({} blocks); the abyssal height "
                                + "(y=-512..319, 832 blocks) applies while the 'Abyssal Overworld' data "
                                + "pack is enabled and not overridden by another pack.",
                        minY, minY + height - 1, height);
            }
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.isNewChunk() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (level.getMinBuildHeight() >= REFERENCE_MIN_BUILD_HEIGHT) {
            return;
        }
        ResourceKey<Level> dimension = level.dimension();
        if (WARNED.contains(dimension)) {
            return;
        }
        ChunkAccess chunk = event.getChunk();
        if (!chunk.getPersistedStatus().isOrAfter(ChunkStatus.FULL)) {
            return;
        }
        // New chunks place the bedrock floor at the new bottom, so an empty bottom section can
        // only belong to a chunk that predates the height upgrade.
        if (!chunk.getSection(chunk.getSectionIndex(level.getMinBuildHeight())).hasOnlyAir()) {
            return;
        }
        WARNED.add(dimension);
        Aquanaut.LOGGER.warn(
                "World '{}' contains chunks generated before the abyssal overworld upgrade: their terrain stops "
                        + "at Y={} and is empty below it, while newly generated chunks reach Y={}. The seam is "
                        + "permanent for those chunks — start a new world for a seamless abyss.",
                level.getServer().getWorldData().getLevelName(), REFERENCE_MIN_BUILD_HEIGHT,
                level.getMinBuildHeight());
    }
}
