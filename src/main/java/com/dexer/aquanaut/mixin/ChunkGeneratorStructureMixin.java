package com.dexer.aquanaut.mixin;

import com.dexer.aquanaut.common.worldgen.blend.SpawnIslandMask;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Keeps vanilla structure starts off the water world's spawn island. Structure placement
 * checks the climate biome source, which the water world fills with deep oceans — ocean
 * biome tags match everywhere — and a sunk shipwreck anchors on its chunk centre at the
 * OCEAN_FLOOR heightmap, which the mod's fill raises to the island surface. The wreck then
 * surfaces on the island's grass. Anchors whose planned ground can be dry are refused here;
 * the ring beyond keeps its wrecks, guaranteed to anchor under water.
 */
@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorStructureMixin {
    private static final ResourceLocation WATER_WORLD_SETTINGS =
            ResourceLocation.fromNamespaceAndPath("aquanaut", "water_world");

    @Redirect(method = "createStructures", remap = false,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/structure/placement/StructurePlacement;isStructureChunk(Lnet/minecraft/world/level/chunk/ChunkGeneratorStructureState;II)Z"))
    private boolean aquanaut$keepStructuresOffTheIsland(StructurePlacement placement,
            ChunkGeneratorStructureState structureState, int chunkX, int chunkZ) {
        boolean placementChunk = placement.isStructureChunk(structureState, chunkX, chunkZ);
        if (placementChunk && aquanaut$isWaterWorldGenerator(this)
                && SpawnIslandMask.islandClaimsChunk(structureState.getLevelSeed(),
                        chunkX << 4, chunkZ << 4)) {
            return false;
        }
        return placementChunk;
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
