package com.dexer.aquanaut.mixin;

import com.dexer.aquanaut.common.worldgen.CrystalNestTerrain;
import com.dexer.aquanaut.common.worldgen.MiddleLevelOceanPlacement;
import com.dexer.aquanaut.common.worldgen.layers.BiomeRewriter;
import com.dexer.aquanaut.common.worldgen.layers.OceanChunkSampler;
import com.dexer.aquanaut.common.worldgen.layers.OceanColumnPlanner;
import com.dexer.aquanaut.common.worldgen.layers.OceanGenSampler;
import com.dexer.aquanaut.common.worldgen.layers.OceanLayerStack;
import com.dexer.aquanaut.common.worldgen.layers.OceanLayerStacks;
import com.dexer.aquanaut.common.worldgen.layers.TerrainModule;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
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

    @Shadow
    @Final
    private Holder<NoiseGeneratorSettings> settings;

    @Shadow
    protected abstract NoiseChunk createNoiseChunk(ChunkAccess chunk, StructureManager structureManager,
            Blender blender, RandomState randomState);

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

        OceanGenSampler sampler = OceanChunkSampler.sample(
                chunk,
                stack,
                terrain.topWaterY(),
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

        BiomeRewriter.rewrite(chunk, sampler, generatorAccessor.aquanaut$getBiomeSource());
        OceanColumnPlanner.ColumnPlan[] columns = OceanColumnPlanner.planColumns(chunk, sampler, terrain);
        // The crystal nest skins its 3D lattice and decorates every surface up front; cells
        // it does not claim fall through to the regular column plan below.
        CrystalNestTerrain.Chunk crystalNest = CrystalNestTerrain.build(chunk, sampler, terrain);

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
                                        blockstate = col.stateForY(blockY);
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
