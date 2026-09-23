package com.dexer.aquanaut.mixin;

import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The generator's biome source, for the worldgen sampler.
 *
 * <p>
 * {@code NoiseBasedChunkGenerator} resolves every noise biome through its own private
 * {@code biomeSource} field. The middle-ocean planner needs the same resolution from outside the
 * generator — there is no getter — so this accessor exposes it read-only.
 */
@Mixin(ChunkGenerator.class)
public interface ChunkGeneratorAccessor {
    @Accessor("biomeSource")
    BiomeSource aquanaut$getBiomeSource();
}
