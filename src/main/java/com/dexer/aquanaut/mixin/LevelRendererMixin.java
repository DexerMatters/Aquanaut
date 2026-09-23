package com.dexer.aquanaut.mixin;

import com.dexer.aquanaut.client.light.ClientDynamicLightManager;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds continuous client dynamic light to vanilla block and block-entity light queries. */
@Mixin(value = LevelRenderer.class, remap = false)
public abstract class LevelRendererMixin {
    @Inject(method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I",
            at = @At("RETURN"), cancellable = true, remap = false)
    private static void aquanaut$applyDynamicLightAtPosition(BlockAndTintGetter level, BlockPos pos,
            CallbackInfoReturnable<Integer> cir) {
        if (level instanceof ClientLevel clientLevel) {
            cir.setReturnValue(ClientDynamicLightManager.applyPackedLight(clientLevel, pos, cir.getReturnValueI()));
        }
    }

    @Inject(method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;"
            + "Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
            at = @At("RETURN"), cancellable = true, remap = false)
    private static void aquanaut$applyDynamicLightAtState(BlockAndTintGetter level, BlockState state, BlockPos pos,
            CallbackInfoReturnable<Integer> cir) {
        if (level instanceof ClientLevel clientLevel) {
            cir.setReturnValue(ClientDynamicLightManager.applyPackedLight(clientLevel, pos, cir.getReturnValueI()));
        }
    }
}
