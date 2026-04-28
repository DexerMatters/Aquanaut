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
    private static final ResourceLocation GLOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID,
            "textures/entity/airbubble_glowmask.png");
    private static final ResourceLocation BURST_TEXTURE = ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID,
            "textures/entity/airbubble_burst.png");

    private static final int BURST_FRAMES = 8;

    /** Base half-size: 15/16 block ÷ 2 = 15/32. */
    private static final float BASE_HALF = (15f / 16f) / 2f;

    public AirBubbleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AirBubbleEntity entity, float entityYaw, float partialTick,
            PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // Billboard: apply camera orientation so the quad always faces the player
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        // SIZE directly controls scale, matching getDimensions (like vanilla Slime)
        float scale = entity.getSize();
        float half = BASE_HALF * scale;

        int burstFrame = entity.getBurstFrame();
        boolean intact = burstFrame == 0;

        // Compute UV for intact vs burst spritesheet
        float v0, v1;
        if (intact) {
            v0 = 0f;
            v1 = 1f;
        } else {
            v0 = (float) burstFrame / BURST_FRAMES;
            v1 = (float) (burstFrame + 1) / BURST_FRAMES;
        }

        Matrix4f matrix = poseStack.last().pose();

        // First pass: normal opaque texture
        VertexConsumer mainBuf = bufferSource.getBuffer(RenderType.entityCutout(
                intact ? BUBBLE_TEXTURE : BURST_TEXTURE));
        renderQuad(matrix, mainBuf, half, v0, v1, packedLight);

        // Second pass: glow overlay (only for intact bubbles)
        if (intact) {
            VertexConsumer glowBuf = bufferSource.getBuffer(RenderType.eyes(GLOW_TEXTURE));
            renderQuad(matrix, glowBuf, half, v0, v1, 2728640);
        }

        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    /**
     * Draws a single billboarded quad with the given parameters.
     *
     * @param matrix      the current pose matrix
     * @param consumer    the vertex consumer (buffered via the appropriate
     *                    RenderType)
     * @param half        half the quad size (width/2)
     * @param v0          top V coordinate
     * @param v1          bottom V coordinate
     * @param packedLight light value (use 15728640 for full-bright glow)
     */
    private void renderQuad(Matrix4f matrix, VertexConsumer consumer, float half,
            float v0, float v1, int packedLight) {
        // Quad vertices: TL -> BL -> BR -> TR
        consumer.addVertex(matrix, -half, half, 0f)
                .setColor(255, 255, 255, 255)
                .setUv(0f, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(0f, 0f, 1f);

        consumer.addVertex(matrix, -half, -half, 0f)
                .setColor(255, 255, 255, 255)
                .setUv(0f, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(0f, 0f, 1f);

        consumer.addVertex(matrix, half, -half, 0f)
                .setColor(255, 255, 255, 255)
                .setUv(1f, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(0f, 0f, 1f);

        consumer.addVertex(matrix, half, half, 0f)
                .setColor(255, 255, 255, 255)
                .setUv(1f, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(0f, 0f, 1f);
    }

    @Override
    public ResourceLocation getTextureLocation(AirBubbleEntity entity) {
        return entity.getBurstFrame() == 0 ? BUBBLE_TEXTURE : BURST_TEXTURE;
    }
}
