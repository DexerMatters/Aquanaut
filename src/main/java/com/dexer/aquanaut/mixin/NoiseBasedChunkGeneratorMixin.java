package com.dexer.aquanaut.mixin;

import com.dexer.aquanaut.common.worldgen.CrystalNestTerrain;
import com.dexer.aquanaut.common.worldgen.MiddleLevelOceanPlacement;
import com.dexer.aquanaut.common.worldgen.blend.SpawnIslandMask;
import com.dexer.aquanaut.common.worldgen.layers.BiomeRewriter;
import com.dexer.aquanaut.common.worldgen.layers.OceanChunkSampler;
import com.dexer.aquanaut.common.worldgen.layers.OceanColumnPlanner;
import com.dexer.aquanaut.common.worldgen.layers.OceanColumnShading;
import com.dexer.aquanaut.common.worldgen.layers.OceanGenSampler;
import com.dexer.aquanaut.common.worldgen.layers.OceanLayerStack;
import com.dexer.aquanaut.common.worldgen.layers.OceanLayerStacks;
import com.dexer.aquanaut.common.worldgen.layers.TerrainModule;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final ResourceLocation WATER_WORLD_SETTINGS =
            ResourceLocation.fromNamespaceAndPath("aquanaut", "water_world");

    @Shadow
    @Final
    private Holder<NoiseGeneratorSettings> settings;

    @Shadow
    protected abstract NoiseChunk createNoiseChunk(ChunkAccess chunk, StructureManager structureManager,
            Blender blender, RandomState randomState);

    /** Whether this generator runs the water world preset, whose terrain plans the spawn island. */
    private boolean aquanaut$isWaterWorld() {
        return this.settings.unwrapKey()
                .map(key -> key.location().equals(WATER_WORLD_SETTINGS))
                .orElse(false);
    }

    /**
     * The level seed the island's coastline, amplitudes and dunes are scrambled from. The worldgen
     * structure manager carries the world's options; a foreign manager degrades to the seedless
     * fixed silhouette rather than guessing.
     */
    private long aquanaut$islandSeed(StructureManager structureManager) {
        if (structureManager instanceof StructureManagerAccessor accessor) {
            WorldOptions worldOptions = accessor.aquanaut$getWorldOptions();
            if (worldOptions != null) {
                return worldOptions.seed();
            }
        }
        return 0L;
    }

    /**
     * Resolves any biome id (including vanilla-only island surface biomes that the water
     * world's biome source never carries) against the world's biome registry; {@code null}
     * when the manager exposes no level, letting the caller skip gracefully.
     */
    private java.util.function.Function<ResourceLocation, Holder<Biome>> aquanaut$biomeLookup(
            StructureManager structureManager) {
        if (structureManager instanceof StructureManagerAccessor accessor
                && accessor.aquanaut$getLevel() != null) {
            Registry<Biome> biomes = accessor.aquanaut$getLevel()
                    .registryAccess()
                    .registryOrThrow(Registries.BIOME);
            return id -> biomes.getHolder(ResourceKey.create(Registries.BIOME, id)).orElse(null);
        }
        return id -> null;
    }

    @Inject(method = "doFill", at = @At("HEAD"), cancellable = true, remap = false)
    private void aquanaut$integratedFill(Blender blender,
            StructureManager structureManager,
            RandomState randomState,
            ChunkAccess chunk,
            int minCellY,
            int cellCountY,
            CallbackInfoReturnable<ChunkAccess> cir) {
        NoiseChunk noiseChunk = chunk.getOrCreateNoiseChunk(
                c -> this.createNoiseChunk(c, structureManager, blender, randomState));
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        ChunkPos chunkPos = chunk.getPos();
        int minBlockX = chunkPos.getMinBlockX();
        int minBlockZ = chunkPos.getMinBlockZ();
        Aquifer aquifer = noiseChunk.aquifer();
        noiseChunk.initializeForFirstCellX();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int cellWidth = noiseChunk.cellWidth();
        int cellHeight = noiseChunk.cellHeight();
        int cellsX = 16 / cellWidth;
        int cellsZ = 16 / cellWidth;
        BlockState defaultBlock = this.settings.value().defaultBlock();

        OceanLayerStack stack = OceanLayerStacks.active();
        TerrainModule terrain = OceanChunkSampler.stackTerrain(stack);
        ChunkGeneratorAccessor generatorAccessor = (ChunkGeneratorAccessor) this;
        int surfaceQuartY = MiddleLevelOceanPlacement.surfaceSampleQuartY();
        boolean spawnIsland = aquanaut$isWaterWorld();
        long islandSeed = aquanaut$islandSeed(structureManager);

        OceanGenSampler sampler = OceanChunkSampler.sample(
                chunk,
                stack,
                terrain.topWaterY(),
                spawnIsland,
                islandSeed,
                minCellY,
                cellCountY,
                cellWidth,
                cellHeight,
                defaultBlock,
                () -> this.createNoiseChunk(chunk, structureManager, blender, randomState),
                (qx, qz) -> generatorAccessor.aquanaut$getBiomeSource()
                        .getNoiseBiome(qx, surfaceQuartY, qz, randomState.sampler())
                        .unwrapKey()
                        .map(key -> key.location())
                        .orElse(null));

        BiomeRewriter.rewrite(chunk, sampler, generatorAccessor.aquanaut$getBiomeSource(),
                aquanaut$biomeLookup(structureManager));
        // One blend grid per chunk: district weights, the guarded geological floor and the
        // composed reef fields are evaluated once per column and shared by every consumer.
        com.dexer.aquanaut.common.worldgen.layers.ChunkTerrainBlend blend =
                com.dexer.aquanaut.common.worldgen.layers.ChunkTerrainBlend.build(chunk, sampler, terrain);
        OceanColumnPlanner.ColumnPlan[] columns = blend.chunkPlans();
        // The crystal nest skins its 3D lattice and decorates every surface up front; cells
        // it does not claim fall through to the regular column plan below.
        CrystalNestTerrain.Chunk crystalNest = CrystalNestTerrain.build(chunk, sampler, terrain, blend);

        for (int cx = 0; cx < cellsX; cx++) {
            noiseChunk.advanceCellX(cx);

            for (int cz = 0; cz < cellsZ; cz++) {
                int sectionIdx = chunk.getSectionsCount() - 1;
                LevelChunkSection section = chunk.getSection(sectionIdx);

                for (int cy = cellCountY - 1; cy >= 0; cy--) {
                    noiseChunk.selectCellYZ(cy, cz);

                    for (int ly = cellHeight - 1; ly >= 0; ly--) {
                        int blockY = (minCellY + cy) * cellHeight + ly;
                        int localY = blockY & 15;
                        int newSectionIdx = chunk.getSectionIndex(blockY);
                        if (sectionIdx != newSectionIdx) {
                            sectionIdx = newSectionIdx;
                            section = chunk.getSection(newSectionIdx);
                        }

                        double fracY = (double) ly / (double) cellHeight;
                        noiseChunk.updateForY(blockY, fracY);

                        for (int lx = 0; lx < cellWidth; lx++) {
                            int blockX = minBlockX + cx * cellWidth + lx;
                            int localX = blockX & 15;
                            double fracX = (double) lx / (double) cellWidth;
                            noiseChunk.updateForX(blockX, fracX);

                            for (int lz = 0; lz < cellWidth; lz++) {
                                int blockZ = minBlockZ + cz * cellWidth + lz;
                                int localZ = blockZ & 15;
                                double fracZ = (double) lz / (double) cellWidth;
                                noiseChunk.updateForZ(blockZ, fracZ);

                                BlockState blockstate;
                                OceanColumnPlanner.ColumnPlan col = columns[localX * 16 + localZ];

                                if (col != null && blockY <= col.topCarveY() && blockY > col.bottomY()) {
                                    noiseChunk.getInterpolatedState();
                                    blockstate = crystalNest != null
                                            ? crystalNest.stateAt(blockX, blockY, blockZ)
                                            : null;
                                    if (blockstate == null) {
                                        blockstate = OceanColumnShading.stateForY(col, blockY);
                                    }
                                } else {
                                    blockstate = noiseChunk.getInterpolatedState();
                                    if (blockstate == null) {
                                        blockstate = defaultBlock;
                                    }
                                }

                                if (blockstate != AIR && !SharedConstants.debugVoidTerrain(chunkPos)) {
                                    section.setBlockState(localX, localY, localZ, blockstate, false);
                                    oceanFloor.update(localX, blockY, localZ, blockstate);
                                    worldSurface.update(localX, blockY, localZ, blockstate);
                                    if (aquifer.shouldScheduleFluidUpdate() && !blockstate.getFluidState().isEmpty()) {
                                        mutablePos.set(blockX, blockY, blockZ);
                                        chunk.markPosForPostprocessing(mutablePos);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            noiseChunk.swapSlices();
        }

        noiseChunk.stopInterpolation();
        cir.setReturnValue(chunk);
    }

    /**
     * Cancels vanilla carving for chunks the ocean layer stack plans terrain in. Carvers are
     * started from every chunk within 8 chunks of the one being carved and would punch air
     * tunnels through the drowned terrain from lists chosen by neighbouring biomes (land and
     * shallow coasts keep their carvers); the mod carves its own voids analytically instead.
     */
    @Inject(method = "applyCarvers", at = @At("HEAD"), cancellable = true, remap = false)
    private void aquanaut$suppressVanillaCarversInCoveredChunks(WorldGenRegion worldGenRegion,
            long seed,
            RandomState randomState,
            BiomeManager biomeManager,
            StructureManager structureManager,
            ChunkAccess chunk,
            GenerationStep.Carving carvingStep,
            CallbackInfo ci) {
        NoiseChunk dimProbe = chunk.getOrCreateNoiseChunk(
                c -> this.createNoiseChunk(c, structureManager, Blender.of(worldGenRegion), randomState));
        if (aquanaut$isCoveredChunk(chunk, structureManager, worldGenRegion, randomState,
                dimProbe.cellWidth(), dimProbe.cellHeight())) {
            ci.cancel();
        }
    }

    /**
     * Runs the vanilla herd pass against the island surface biome for chunks the island mask
     * claims. Vanilla samples the biome at build-limit height, which the water world preset
     * fills with climate ocean biomes whose creature list is empty — the island would
     * otherwise never receive its initial animal herds (passive mobs only spawn in this
     * pass, never replenish naturally). The random derivation replicates vanilla exactly.
     */
    @Inject(method = "spawnOriginalMobs", at = @At("HEAD"), cancellable = true, remap = false)
    private void aquanaut$islandOriginalMobs(WorldGenRegion level, CallbackInfo ci) {
        if (!aquanaut$isWaterWorld() || this.settings.value().disableMobGeneration()) {
            return;
        }
        OceanLayerStack stack = OceanLayerStacks.active();
        if (stack == null || stack.layers().isEmpty()) {
            return;
        }
        ChunkPos chunkPos = level.getCenter();
        if (SpawnIslandMask.maskAt(level.getSeed(),
                chunkPos.getMinBlockX() + 8, chunkPos.getMinBlockZ() + 8) <= 0.0D) {
            return;
        }
        // Sample the rewritten island palette at the plateau surface (in the island biome
        // band), not the raw climate ocean sitting at the build limit.
        Holder<Biome> biome = level.getBiome(new BlockPos(chunkPos.getMinBlockX() + 8,
                SpawnIslandMask.ISLAND_TOP_Y - 4, chunkPos.getMinBlockZ() + 8));
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
        random.setDecorationSeed(level.getSeed(), chunkPos.getMinBlockX(), chunkPos.getMinBlockZ());
        NaturalSpawner.spawnMobsForChunkGeneration(level, biome, chunkPos, random);
        ci.cancel();
    }

    private boolean aquanaut$isCoveredChunk(ChunkAccess chunk, StructureManager structureManager,
            WorldGenRegion worldGenRegion, RandomState randomState, int cellWidth, int cellHeight) {
        OceanLayerStack stack = OceanLayerStacks.active();
        if (stack == null || stack.layers().isEmpty()) {
            return false;
        }
        int minCellY = Math.floorDiv(chunk.getMinBuildHeight(), cellHeight);
        int cellCountY = Math.ceilDiv(chunk.getHeight(), cellHeight);
        ChunkGeneratorAccessor generatorAccessor = (ChunkGeneratorAccessor) this;
        return OceanChunkSampler.isCovered(
                chunk,
                stack,
                OceanChunkSampler.topWaterY(stack),
                aquanaut$isWaterWorld(),
                worldGenRegion.getSeed(),
                minCellY,
                cellCountY,
                cellWidth,
                cellHeight,
                this.settings.value().defaultBlock(),
                () -> this.createNoiseChunk(chunk, structureManager, Blender.of(worldGenRegion), randomState),
                (qx, qz) -> generatorAccessor.aquanaut$getBiomeSource()
                        .getNoiseBiome(qx, MiddleLevelOceanPlacement.surfaceSampleQuartY(), qz, randomState.sampler())
                        .unwrapKey()
                        .map(key -> key.location())
                        .orElse(null));
    }
}
