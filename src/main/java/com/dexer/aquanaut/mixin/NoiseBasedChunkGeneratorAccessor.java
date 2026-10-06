package com.dexer.aquanaut.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The generator's noise settings holder, so spawn placement can tell the water world preset
 * apart from the normal world. The private {@code settings} field has no vanilla getter.
 */
@Mixin(NoiseBasedChunkGenerator.class)
public interface NoiseBasedChunkGeneratorAccessor {
    @Accessor("settings")
    Holder<NoiseGeneratorSettings> aquanaut$getSettings();
}
