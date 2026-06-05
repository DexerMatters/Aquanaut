package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.common.entity.HarpoonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class HarpoonRenderer extends EntityRenderer<HarpoonEntity> {

    private static final float PIXEL = 1F / 16F;
    private static final int TEX_W = 32, TEX_H = 32;

    // Precomputed from iron_harpoon.png — merged contiguous pixel spans
    private static final int[][] RUNS = {
            { 26, 27, 1, 1 }, { 25, 27, 2, 2 }, { 24, 26, 3, 3 }, { 23, 25, 4, 4 },
            { 29, 30, 4, 4 }, { 22, 24, 5, 5 }, { 28, 30, 5, 5 }, { 21, 23, 6, 6 },
            { 27, 29, 6, 6 }, { 20, 22, 7, 7 }, { 26, 28, 7, 7 }, { 21, 23, 8, 8 },
            { 25, 27, 8, 8 }, { 21, 26, 9, 9 }, { 20, 25, 10, 10 }, { 19, 21, 11, 11 },
            { 24, 24, 11, 11 }, { 18, 20, 12, 12 }, { 17, 19, 13, 13 }, { 16, 18, 14, 14 },
            { 15, 17, 15, 15 }, { 14, 16, 16, 16 }, { 13, 15, 17, 17 }, { 12, 14, 18, 18 },
            { 11, 13, 19, 19 }, { 10, 12, 20, 20 }, { 9, 11, 21, 21 }, { 8, 10, 22, 22 },
            { 7, 9, 23, 23 }, { 6, 8, 24, 24 }, { 5, 7, 25, 25 }, { 4, 6, 26, 26 },
            { 3, 5, 27, 27 }, { 2, 4, 28, 28 }, { 1, 3, 29, 29 }, { 1, 2, 30, 30 },
    };

    public HarpoonRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(HarpoonEntity entity, float entityYaw, float partialTick,
            PoseStack poseStack, MultiBufferSource buffer,
            int packedLight) {

        ItemStack stack = entity.getSyncedHarpoonStack();
        if (stack.isEmpty())
            return;

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        ResourceLocation tex = itemId.withPrefix("item/");
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(tex);

        float su0 = sprite.getU0(), sv0 = sprite.getV0();
        float su1 = sprite.getU1(), sv1 = sprite.getV1();
        float du = (su1 - su0) / TEX_W;
        float dv = (sv1 - sv0) / TEX_H;

        poseStack.pushPose();

        // Flight direction (vanilla trident style)
        poseStack.mulPose(Axis.YP.rotationDegrees(
                Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(
                Mth.lerp(partialTick, entity.xRotO, entity.getXRot()) + 90.0F));

        // Visibility tilt
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));

        // Align the texture's diagonal with model Y (tip → -Y → flight direction)
        // Must be last in code = first applied to vertices (local-space)
        poseStack.mulPose(Axis.ZP.rotationDegrees(-40.0F));

        VertexConsumer vc = buffer.getBuffer(
                RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        var pose = poseStack.last();
        int light = packedLight;

        if (entity.isShadow()) {
            poseStack.popPose();
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }

        for (int[] r : RUNS) {
            float x1 = r[0], x2 = r[1], y1 = r[2], y2 = r[3];

            // Box position and size in model space (centered, 1 pix deep)
            float cx = ((x1 + x2) * 0.5F - 16F) * PIXEL;
            float cy = ((y1 + y2) * 0.5F - 16F) * PIXEL;
            float sx = (x2 - x1 + 1) * PIXEL;
            float sy = (y2 - y1 + 1) * PIXEL;
            float sz = PIXEL;

            float hx = sx * 0.5F, hy = sy * 0.5F, hz = sz * 0.5F;

            // UV for this pixel region on the atlas
            float u0 = su0 + x1 * du;
            float v0 = sv0 + y1 * dv;
            float u1 = su0 + (x2 + 1) * du;
            float v1 = sv0 + (y2 + 1) * dv;

            // Edge UVs — sample periphery colour for lateral faces
            float uL = u0, uR = u1, vT = v0, vB = v1;

            // ── Front (+Z) ──
            vc.addVertex(pose, cx - hx, cy + hy, hz)
                    .setColor(-1)
                    .setUv(uL, vT)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, 0, 0, 1);
            vc.addVertex(pose, cx - hx, cy - hy, hz)
                    .setColor(-1)
                    .setUv(uL, vB)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, 0, 0, 1);
            vc.addVertex(pose, cx + hx, cy - hy, hz)
                    .setColor(-1)
                    .setUv(uR, vB)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, 0, 0, 1);
            vc.addVertex(pose, cx + hx, cy + hy, hz)
                    .setColor(-1)
                    .setUv(uR, vT)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, 0, 0, 1);

            // ── Back (-Z) ──
            vc.addVertex(pose, cx + hx, cy + hy, -hz)
                    .setColor(-1)
                    .setUv(uL, vT)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, 0, 0, -1);
            vc.addVertex(pose, cx + hx, cy - hy, -hz)
                    .setColor(-1)
                    .setUv(uL, vB)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, 0, 0, -1);
            vc.addVertex(pose, cx - hx, cy - hy, -hz)
                    .setColor(-1)
                    .setUv(uR, vB)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, 0, 0, -1);
            vc.addVertex(pose, cx - hx, cy + hy, -hz)
                    .setColor(-1)
                    .setUv(uR, vT)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, 0, 0, -1);

            // ── Right (+X) ── samples rightmost column
            vc.addVertex(pose, cx + hx, cy + hy, hz)
                    .setColor(-1)
                    .setUv(uR, vT)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, 1, 0, 0);
            vc.addVertex(pose, cx + hx, cy + hy, -hz)
                    .setColor(-1)
                    .setUv(uR, vB)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, 1, 0, 0);
            vc.addVertex(pose, cx + hx, cy - hy, -hz)
                    .setColor(-1)
                    .setUv(uL, vB)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, 1, 0, 0);
            vc.addVertex(pose, cx + hx, cy - hy, hz)
                    .setColor(-1)
                    .setUv(uL, vT)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, 1, 0, 0);

            // ── Left (-X) ── samples leftmost column
            vc.addVertex(pose, cx - hx, cy - hy, hz)
                    .setColor(-1)
                    .setUv(uL, vT)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, -1, 0, 0);
            vc.addVertex(pose, cx - hx, cy - hy, -hz)
                    .setColor(-1)
                    .setUv(uL, vB)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, -1, 0, 0);
            vc.addVertex(pose, cx - hx, cy + hy, -hz)
                    .setColor(-1)
                    .setUv(uR, vB)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, -1, 0, 0);
            vc.addVertex(pose, cx - hx, cy + hy, hz)
                    .setColor(-1)
                    .setUv(uR, vT)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, -1, 0, 0);

            // ── Top (+Y) ── samples topmost row
            vc.addVertex(pose, cx + hx, cy + hy, hz)
                    .setColor(-1)
                    .setUv(uR, vT)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, 0, 1, 0);
            vc.addVertex(pose, cx + hx, cy + hy, -hz)
                    .setColor(-1)
                    .setUv(uR, vB)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, 0, 1, 0);
            vc.addVertex(pose, cx - hx, cy + hy, -hz)
                    .setColor(-1)
                    .setUv(uL, vB)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, 0, 1, 0);
            vc.addVertex(pose, cx - hx, cy + hy, hz)
                    .setColor(-1)
                    .setUv(uL, vT)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, 0, 1, 0);

            // ── Bottom (-Y) ── samples bottommost row
            vc.addVertex(pose, cx - hx, cy - hy, hz)
                    .setColor(-1)
                    .setUv(uL, vT)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, 0, -1, 0);
            vc.addVertex(pose, cx - hx, cy - hy, -hz)
                    .setColor(-1)
                    .setUv(uL, vB)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, 0, -1, 0);
            vc.addVertex(pose, cx + hx, cy - hy, -hz)
                    .setColor(-1)
                    .setUv(uR, vB)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, 0, -1, 0);
            vc.addVertex(pose, cx + hx, cy - hy, hz)
                    .setColor(-1)
                    .setUv(uR, vT)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, 0, -1, 0);
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer,
                packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(HarpoonEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
