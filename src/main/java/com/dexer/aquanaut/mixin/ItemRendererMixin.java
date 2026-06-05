package com.dexer.aquanaut.mixin;

import com.dexer.aquanaut.client.gaze.GazeRenderTypes;
import com.dexer.aquanaut.common.gaze.GazeHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemRenderer.class, remap = false)
public abstract class ItemRendererMixin {

    @Inject(
            method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Z"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource;II"
                    + "Lnet/minecraft/client/resources/model/BakedModel;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V",
                    shift = At.Shift.BEFORE),
            remap = false)
    private void aquanaut$renderGazeOutline(ItemStack stack, ItemDisplayContext displayContext, boolean leftHand,
            PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay,
            BakedModel model, CallbackInfo ci) {
        if (!GazeHelper.hasGaze(stack)) {
            return;
        }

        VertexConsumer consumer = bufferSource.getBuffer(GazeRenderTypes.getGazeGlint());
        RandomSource random = RandomSource.create();

        for (Direction direction : Direction.values()) {
            for (BakedQuad quad : model.getQuads(null, direction, random, ModelData.EMPTY, null)) {
                consumer.putBulkData(poseStack.last(), quad, 1.0F, 1.0F, 1.0F, 1.0F, combinedLight, combinedOverlay);
            }
        }
        for (BakedQuad quad : model.getQuads(null, null, random, ModelData.EMPTY, null)) {
            consumer.putBulkData(poseStack.last(), quad, 1.0F, 1.0F, 1.0F, 1.0F, combinedLight, combinedOverlay);
        }
    }
}
