package com.dexer.aquanaut.mixin;

import com.dexer.aquanaut.client.gaze.GazeRenderTypes;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.SequencedMap;

@Mixin(targets = "net/minecraft/client/renderer/MultiBufferSource$BufferSource", remap = false)
public abstract class BufferSourceMixin {

    @Shadow
    @Final
    private SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers;

    @Unique
    private boolean aquanaut$gazeTypeRegistered;

    @Inject(method = "getBuffer", at = @At("HEAD"), remap = false)
    private void aquanaut$registerGazeType(RenderType type, CallbackInfoReturnable<VertexConsumer> cir) {
        if (!aquanaut$gazeTypeRegistered && type == GazeRenderTypes.getGazeGlint()) {
            aquanaut$gazeTypeRegistered = true;
            fixedBuffers.put(type, new ByteBufferBuilder(256));
        }
    }
}
