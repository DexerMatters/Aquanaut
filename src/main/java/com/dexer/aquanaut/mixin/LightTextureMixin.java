package com.dexer.aquanaut.mixin;

import com.dexer.aquanaut.client.fog.AbyssLightmap;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets the abyss take the light away, not only the distance.
 *
 * <p>Every entry of the lightmap is built from {@code LightTexture#getBrightness}, so scaling
 * that one lookup dims the whole curve — sky and block light together, which is exactly what
 * "too deep for a torch to help" means. The scale is sampled once per update; with a shader
 * pack loaded it stays at 1, because the pack owns lighting and the veil carries the darkness
 * instead (see {@link AbyssLightmap}).</p>
 */
@Mixin(value = LightTexture.class, remap = false)
public abstract class LightTextureMixin {

    @Inject(method = "updateLightTexture", at = @At("HEAD"), remap = false)
    private void aquanaut$sampleAbyssDarkening(float partialTicks, CallbackInfo callback) {
        AbyssLightmap.sample();
    }

    @Inject(method = "getBrightness", at = @At("RETURN"), cancellable = true, remap = false)
    private static void aquanaut$dimTheAbyss(DimensionType dimensionType, int lightLevel,
            CallbackInfoReturnable<Float> callback) {
        float scale = AbyssLightmap.scale();
        if (scale < 1.0F) {
            callback.setReturnValue(callback.getReturnValueF() * scale);
        }
    }
}