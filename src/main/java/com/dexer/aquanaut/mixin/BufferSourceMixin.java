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

/**
 * Gives Aquanaut's own render types a vertex buffer each.
 *
 * <p>
 * A render type that is not registered here shares the buffer source's one shared buffer, and asking
 * for the buffer of an unregistered type flushes whatever was being built in it. The gaze glint is
 * drawn at its own point in the frame and would rather not disturb, or be disturbed by, anyone
 * else's half-built geometry.
 */
@Mixin(value = MultiBufferSource.BufferSource.class, remap = false)
public abstract class BufferSourceMixin {

    @Shadow
    @Final
    private SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers;

    @Unique
    private boolean aquanaut$gazeTypeRegistered;

    @Inject(method = "getBuffer", at = @At("HEAD"), remap = false)
    private void aquanaut$registerRenderTypes(RenderType type, CallbackInfoReturnable<VertexConsumer> cir) {
        if (!aquanaut$gazeTypeRegistered && type == GazeRenderTypes.getGazeGlint()) {
            aquanaut$gazeTypeRegistered = true;
            fixedBuffers.put(type, new ByteBufferBuilder(256));
        }
    }
}
