package com.dexer.aquanaut.mixin;

import com.dexer.aquanaut.common.worldgen.blend.SpawnIslandMask;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps vanilla structure starts off the water world's spawn island. Structure placement
 * checks the climate biome source, which the water world fills with deep oceans — ocean
 * biome tags match everywhere — and a sunk shipwreck anchors on its chunk centre at the
 * OCEAN_FLOOR heightmap, which the mod's fill raises to the island surface. The wreck then
 * surfaces on the island's grass. Anchors whose planned ground can be dry are refused here;
 * the ring beyond keeps its wrecks, guaranteed to anchor under water.
 *
 * <p>The hook sits on {@code tryGenerateStructure} rather than on the placement check:
 * vanilla runs that check inside a {@code forEach} lambda, and a redirect cannot reach
 * across the lambda boundary. Refusing here is exactly vanilla's "this structure did not
 * generate" result, so the weighted-pick loop simply moves on to the next candidate.</p>
 */
@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorStructureMixin {
    private static final ResourceLocation WATER_WORLD_SETTINGS =
            ResourceLocation.fromNamespaceAndPath("aquanaut", "water_world");

    @Inject(method = "tryGenerateStructure", at = @At("HEAD"), cancellable = true, remap = false)
    private void aquanaut$keepStructuresOffTheIsland(
            StructureSet.StructureSelectionEntry structureSelectionEntry,
            StructureManager structureManager,
            RegistryAccess registryAccess,
            RandomState randomState,
            StructureTemplateManager structureTemplateManager,
            long seed,
            ChunkAccess chunk,
            ChunkPos chunkPos,
            SectionPos sectionPos,
            CallbackInfoReturnable<Boolean> cir) {
        if (!aquanaut$isWaterWorldGenerator(this)
                || !SpawnIslandMask.islandClaimsChunk(seed,
                        chunkPos.getMinBlockX(), chunkPos.getMinBlockZ())) {
            return;
        }
        cir.setReturnValue(false);
    }

    private static boolean aquanaut$isWaterWorldGenerator(Object generator) {
        if (!(generator instanceof NoiseBasedChunkGenerator noiseGenerator)) {
            return false;
        }
        Holder<NoiseGeneratorSettings> settings =
                ((NoiseBasedChunkGeneratorAccessor) noiseGenerator).aquanaut$getSettings();
        return settings.unwrapKey()
                .map(key -> key.location().equals(WATER_WORLD_SETTINGS))
                .orElse(false);
    }
}
