package com.dexer.aquanaut.mixin;

import com.dexer.aquanaut.client.gaze.GazeRenderTypes;
import com.dexer.aquanaut.common.entity.HarpoonEntity;
import com.dexer.aquanaut.common.gaze.GazeHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = com.dexer.aquanaut.client.renderer.HarpoonRenderer.class, remap = false)
public abstract class HarpoonRendererMixin {

    private static final int[][] GAZE_RUNS = {
            {26,27,1,1},{25,27,2,2},{24,26,3,3},{23,25,4,4},{29,30,4,4},
            {22,24,5,5},{28,30,5,5},{21,23,6,6},{27,29,6,6},{20,22,7,7},
            {26,28,7,7},{21,23,8,8},{25,27,8,8},{21,26,9,9},{20,25,10,10},
            {19,21,11,11},{24,24,11,11},{18,20,12,12},{17,19,13,13},{16,18,14,14},
            {15,17,15,15},{14,16,16,16},{13,15,17,17},{12,14,18,18},{11,13,19,19},
            {10,12,20,20},{9,11,21,21},{8,10,22,22},{7,9,23,23},{6,8,24,24},
            {5,7,25,25},{4,6,26,26},{3,5,27,27},{2,4,28,28},{1,3,29,29},{1,2,30,30},
    };

    @Inject(method = "render(Lcom/dexer/aquanaut/common/entity/HarpoonEntity;FF" +
            "Lcom/mojang/blaze3d/vertex/PoseStack;" +
            "Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", shift = At.Shift.BEFORE),
            remap = false)
    private void aquanaut$renderGazeOnHarpoon(HarpoonEntity entity, float entityYaw, float partialTick,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {

        ItemStack stack = entity.getSyncedHarpoonStack();
        if (stack.isEmpty() || (!entity.isShadow() && !GazeHelper.hasGaze(stack))) {
            return;
        }

        ResourceLocation tex = BuiltInRegistries.ITEM.getKey(stack.getItem()).withPrefix("item/");
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(tex);

        float su0 = sprite.getU0(), sv0 = sprite.getV0();
        float du = (sprite.getU1() - su0) / 32f, dv = (sprite.getV1() - sv0) / 32f;
        float pixel = 1f / 16f;
        int noOverlay = OverlayTexture.NO_OVERLAY;

        VertexConsumer vc = buffer.getBuffer(entity.isShadow()
                ? GazeRenderTypes.getShadowGazeGlint()
                : GazeRenderTypes.getGazeGlint());
        var pose = poseStack.last();

        for (int[] r : GAZE_RUNS) {
            float x1 = r[0], x2 = r[1], y1 = r[2], y2 = r[3];
            float cx = ((x1 + x2) * 0.5F - 16f) * pixel;
            float cy = ((y1 + y2) * 0.5F - 16f) * pixel;
            float hx = (x2 - x1 + 1) * pixel * 0.5f, hy = (y2 - y1 + 1) * pixel * 0.5f, hz = pixel * 0.5f;
            float uL = su0 + x1 * du, uR = su0 + (x2 + 1) * du;
            float vT = sv0 + y1 * dv, vB = sv0 + (y2 + 1) * dv;

            quad(vc, pose, cx, cy, hx, hy, hz, uL, uR, vT, vB, noOverlay, packedLight);
        }
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose,
            float cx, float cy, float hx, float hy, float hz,
            float uL, float uR, float vT, float vB, int overlay, int light) {
        // Each of 6 faces
        vc.addVertex(pose, cx - hx, cy + hy, hz).setColor(-1).setUv(uL, vT).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, cx - hx, cy - hy, hz).setColor(-1).setUv(uL, vB).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, cx + hx, cy - hy, hz).setColor(-1).setUv(uR, vB).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, cx + hx, cy + hy, hz).setColor(-1).setUv(uR, vT).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, cx + hx, cy + hy, -hz).setColor(-1).setUv(uL, vT).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        vc.addVertex(pose, cx + hx, cy - hy, -hz).setColor(-1).setUv(uL, vB).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        vc.addVertex(pose, cx - hx, cy - hy, -hz).setColor(-1).setUv(uR, vB).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        vc.addVertex(pose, cx - hx, cy + hy, -hz).setColor(-1).setUv(uR, vT).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        vc.addVertex(pose, cx + hx, cy + hy, hz).setColor(-1).setUv(uR, vT).setOverlay(overlay).setLight(light).setNormal(pose, 1, 0, 0);
        vc.addVertex(pose, cx + hx, cy + hy, -hz).setColor(-1).setUv(uR, vB).setOverlay(overlay).setLight(light).setNormal(pose, 1, 0, 0);
        vc.addVertex(pose, cx + hx, cy - hy, -hz).setColor(-1).setUv(uL, vB).setOverlay(overlay).setLight(light).setNormal(pose, 1, 0, 0);
        vc.addVertex(pose, cx + hx, cy - hy, hz).setColor(-1).setUv(uL, vT).setOverlay(overlay).setLight(light).setNormal(pose, 1, 0, 0);
        vc.addVertex(pose, cx - hx, cy - hy, hz).setColor(-1).setUv(uL, vT).setOverlay(overlay).setLight(light).setNormal(pose, -1, 0, 0);
        vc.addVertex(pose, cx - hx, cy - hy, -hz).setColor(-1).setUv(uL, vB).setOverlay(overlay).setLight(light).setNormal(pose, -1, 0, 0);
        vc.addVertex(pose, cx - hx, cy + hy, -hz).setColor(-1).setUv(uR, vB).setOverlay(overlay).setLight(light).setNormal(pose, -1, 0, 0);
        vc.addVertex(pose, cx - hx, cy + hy, hz).setColor(-1).setUv(uR, vT).setOverlay(overlay).setLight(light).setNormal(pose, -1, 0, 0);
        vc.addVertex(pose, cx + hx, cy + hy, hz).setColor(-1).setUv(uR, vT).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, cx + hx, cy + hy, -hz).setColor(-1).setUv(uR, vB).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, cx - hx, cy + hy, -hz).setColor(-1).setUv(uL, vB).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, cx - hx, cy + hy, hz).setColor(-1).setUv(uL, vT).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, cx - hx, cy - hy, hz).setColor(-1).setUv(uL, vT).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, cx - hx, cy - hy, -hz).setColor(-1).setUv(uL, vB).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, cx + hx, cy - hy, -hz).setColor(-1).setUv(uR, vB).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, cx + hx, cy - hy, hz).setColor(-1).setUv(uR, vT).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
    }
}
