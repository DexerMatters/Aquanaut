package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.mixin.NoiseBasedChunkGeneratorAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.PlayerRespawnLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Dries the water world's first impression: the preset floods the whole overworld, so vanilla
 * spawn selection finds no dry column anywhere and a fresh player spawns at sea level — and
 * drowns (design note {@code Aquanaut-Design/Changes.md}).
 *
 * <p>
 * The spawn island itself is planned by the regular ocean column pipeline ({@code SpawnIslandMask}
 * lifts the geological floor near the origin inside the water world preset only). This handler
 * pins the world spawn onto that island: it reuses vanilla's own dry-ground scan
 * ({@link PlayerRespawnLogic#getSpawnPosInChunk}) over chunk (0, 0), which the island plateau
 * always covers, and cancels the vanilla climate search whose spiral would settle on open water.
 * A null scan (the island edited away by a datapack, say) falls through uncancelled so vanilla
 * keeps its exact original behaviour.
 * </p>
 */
@EventBusSubscriber(modid = Aquanaut.MODID)
public final class SpawnIslandPlacement {
    private static final ResourceLocation WATER_WORLD_SETTINGS =
            ResourceLocation.fromNamespaceAndPath("aquanaut", "water_world");

    private SpawnIslandPlacement() {
    }

    @SubscribeEvent
    static void onCreateWorldSpawn(LevelEvent.CreateSpawnPosition event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD
                || !isWaterWorld(level)) {
            return;
        }
        BlockPos spawn = PlayerRespawnLogic.getSpawnPosInChunk(level, new ChunkPos(0, 0));
        if (spawn == null) {
            return;
        }
        event.getSettings().setSpawn(spawn, 0.0F);
        event.setCanceled(true);
    }

    private static boolean isWaterWorld(ServerLevel level) {
        if (level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator generator) {
            Holder<NoiseGeneratorSettings> settings =
                    ((NoiseBasedChunkGeneratorAccessor) generator).aquanaut$getSettings();
            return settings.unwrapKey()
                    .map(key -> key.location().equals(WATER_WORLD_SETTINGS))
                    .orElse(false);
        }
        return false;
    }
}
