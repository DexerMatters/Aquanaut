package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.entity.AirBubbleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class AirBubbleRenderer extends EntityRenderer<AirBubbleEntity> {

    private static final ResourceLocation BUBBLE_TEXTURE = ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID,
            "textures/entity/airbubble.png");
    private static final ResourceLocation BURST_TEXTURE = ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID,
            "textures/entity/airbubble_burst.png");

    private static final int BURST_FRAMES = 8;

    // 15 pixels out of 16 per block = 0.9375 blocks
    private static final float SIZE = 15f / 16f;
    private static final float HALF = SIZE / 2f;

    public AirBubbleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AirBubbleEntity entity, float entityYaw, float partialTick,
            PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // Billboard: apply camera orientation so the quad always faces the player
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        int burstFrame = entity.getBurstFrame();
        ResourceLocation texture;
        float v0, v1;

        if (burstFrame == 0) {
            // Intact bubble — full texture
            texture = BUBBLE_TEXTURE;
            v0 = 0f;
            v1 = 1f;
        } else {
            // Burst animation — slice the vertical spritesheet
            // burstFrame 1–7 maps to spritesheet rows 1–7 out of 8
            texture = BURST_TEXTURE;
            v0 = (float) burstFrame / BURST_FRAMES;
            v1 = (float) (burstFrame + 1) / BURST_FRAMES;
        }

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucentCull(texture));

        // Quad vertices: TL -> BL -> BR -> TR
        consumer.addVertex(matrix, -HALF, HALF, 0f)
                .setColor(255, 255, 255, 255)
                .setUv(0f, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(0f, 0f, 1f);

        consumer.addVertex(matrix, -HALF, -HALF, 0f)
                .setColor(255, 255, 255, 255)
                .setUv(0f, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(0f, 0f, 1f);

        consumer.addVertex(matrix, HALF, -HALF, 0f)
                .setColor(255, 255, 255, 255)
                .setUv(1f, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(0f, 0f, 1f);

        consumer.addVertex(matrix, HALF, HALF, 0f)
                .setColor(255, 255, 255, 255)
                .setUv(1f, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(0f, 0f, 1f);

        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(AirBubbleEntity entity) {
        return entity.getBurstFrame() == 0 ? BUBBLE_TEXTURE : BURST_TEXTURE;
    }
}
