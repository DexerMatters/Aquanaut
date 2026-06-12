package com.dexer.aquanaut.mixin;

import com.dexer.aquanaut.common.worldgen.ScaledDensityFunction;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RandomState.class)
public abstract class RandomStateMixin {
    private static final double OCEAN_SCALE = 3.0D;

    @Shadow
    @Final
    @Mutable
    private NoiseRouter router;

    @Shadow
    @Final
    @Mutable
    private Climate.Sampler sampler;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void aquanaut$scaleDeepOcean(CallbackInfo ci) {
        DensityFunction scaledContinents = new ScaledDensityFunction(this.router.continents(), OCEAN_SCALE);

        this.router = new NoiseRouter(
                this.router.barrierNoise(),
                this.router.fluidLevelFloodednessNoise(),
                this.router.fluidLevelSpreadNoise(),
                this.router.lavaNoise(),
                this.router.temperature(),
                this.router.vegetation(),
                scaledContinents,
                this.router.erosion(),
                this.router.depth(),
                this.router.ridges(),
                this.router.initialDensityWithoutJaggedness(),
                this.router.finalDensity(),
                this.router.veinToggle(),
                this.router.veinRidged(),
                this.router.veinGap()
        );

        this.sampler = new Climate.Sampler(
                this.sampler.temperature(),
                this.sampler.humidity(),
                new ScaledDensityFunction(this.sampler.continentalness(), OCEAN_SCALE),
                this.sampler.erosion(),
                this.sampler.depth(),
                this.sampler.weirdness(),
                this.sampler.spawnTarget()
        );
    }
}
