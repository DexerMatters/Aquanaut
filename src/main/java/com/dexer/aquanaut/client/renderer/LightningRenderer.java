package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.entity.LightningBoltGeometry;
import com.dexer.aquanaut.common.entity.LightningEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class LightningRenderer extends EntityRenderer<LightningEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID,
            "textures/entity/lightning.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    public LightningRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LightningEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        Vec3 renderOrigin = new Vec3(
                Mth.lerp(partialTick, entity.xo, entity.getX()),
                Mth.lerp(partialTick, entity.yo, entity.getY()),
                Mth.lerp(partialTick, entity.zo, entity.getZ()));
        Vec3 cameraLocal = this.entityRenderDispatcher.camera.getPosition().subtract(renderOrigin);
        float twinkle = twinkleStrength(entity, partialTick);
        float yaw = entity.getLightningYaw();
        float pitch = entity.getLightningPitch();

        PoseStack.Pose pose = poseStack.last();
        for (LightningBoltGeometry.Segment local : entity.getLightningRenderSegments()) {
            Vec3 start = LightningBoltGeometry.transformPoint(local.start(), renderOrigin, yaw, pitch)
                    .subtract(renderOrigin);
            Vec3 end = LightningBoltGeometry.transformPoint(local.end(), renderOrigin, yaw, pitch)
                    .subtract(renderOrigin);
            float plateWidth = renderPlateWidth(local);
            VertexConsumer bodyBuffer = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE));
            renderSegment(pose, bodyBuffer, start, end, cameraLocal, plateWidth * (0.95F + twinkle * 0.12F),
                    FULL_BRIGHT, (int) (150.0F + twinkle * 105.0F));
        }

        // BufferSource reuses a shared builder for these render types, so glow and body
        // must be emitted in separate passes instead of alternating per segment.
        for (LightningBoltGeometry.Segment local : entity.getLightningRenderSegments()) {
            Vec3 start = LightningBoltGeometry.transformPoint(local.start(), renderOrigin, yaw, pitch)
                    .subtract(renderOrigin);
            Vec3 end = LightningBoltGeometry.transformPoint(local.end(), renderOrigin, yaw, pitch)
                    .subtract(renderOrigin);
            float plateWidth = renderPlateWidth(local);
            VertexConsumer glowBuffer = bufferSource.getBuffer(RenderType.lightning());
            renderGlowSegment(pose, glowBuffer, start, end, cameraLocal, plateWidth * (1.45F + twinkle * 0.42F),
                    0.15F + twinkle * 0.30F);
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LightningEntity entity) {
        return TEXTURE;
    }

    private float renderPlateWidth(LightningBoltGeometry.Segment segment) {
        float depthScale = segment.depth() == 0 ? 1.0F : 0.82F;
        return Math.max(segment.depth() == 0 ? 0.55F : 0.35F, segment.width() * 1.6F * depthScale);
    }

    private void renderSegment(PoseStack.Pose pose, VertexConsumer buffer, Vec3 start, Vec3 end, Vec3 cameraLocal,
            float width,
            int packedLight, int alpha) {
        Vec3 delta = end.subtract(start);
        if (delta.lengthSqr() < 1.0E-8D) {
            return;
        }

        Matrix4f matrix = pose.pose();
        Vec3 offset = billboardOffset(start, end, cameraLocal, width);
        Vec3 normal = quadNormal(start, end, offset, cameraLocal);
        drawPlate(buffer, matrix, pose, start, end, offset, normal, packedLight, alpha);
    }

    private void renderGlowSegment(PoseStack.Pose pose, VertexConsumer buffer, Vec3 start, Vec3 end, Vec3 cameraLocal,
            float width, float alpha) {
        if (end.subtract(start).lengthSqr() < 1.0E-8D) {
            return;
        }

        Matrix4f matrix = pose.pose();
        Vec3 offset = billboardOffset(start, end, cameraLocal, width);
        drawGlowPlate(buffer, matrix, start, end, offset, alpha);
    }

    private void drawPlate(VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose, Vec3 start, Vec3 end,
            Vec3 offset, Vec3 normal, int packedLight, int alpha) {
        Vec3 startLeft = start.add(offset);
        Vec3 startRight = start.subtract(offset);
        Vec3 endRight = end.subtract(offset);
        Vec3 endLeft = end.add(offset);

        buffer.addVertex(matrix, (float) startLeft.x, (float) startLeft.y, (float) startLeft.z)
                .setColor(255, 255, 255, alpha)
                .setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
        buffer.addVertex(matrix, (float) startRight.x, (float) startRight.y, (float) startRight.z)
                .setColor(255, 255, 255, alpha)
                .setUv(1.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
        buffer.addVertex(matrix, (float) endRight.x, (float) endRight.y, (float) endRight.z)
                .setColor(255, 255, 255, alpha)
                .setUv(1.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
        buffer.addVertex(matrix, (float) endLeft.x, (float) endLeft.y, (float) endLeft.z)
                .setColor(255, 255, 255, alpha)
                .setUv(0.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }

    private void drawGlowPlate(VertexConsumer buffer, Matrix4f matrix, Vec3 start, Vec3 end, Vec3 offset,
            float alpha) {
        Vec3 startLeft = start.add(offset);
        Vec3 startRight = start.subtract(offset);
        Vec3 endRight = end.subtract(offset);
        Vec3 endLeft = end.add(offset);

        buffer.addVertex(matrix, (float) startLeft.x, (float) startLeft.y, (float) startLeft.z)
                .setColor(0.88F, 0.92F, 1.0F, alpha);
        buffer.addVertex(matrix, (float) startRight.x, (float) startRight.y, (float) startRight.z)
                .setColor(0.88F, 0.92F, 1.0F, alpha);
        buffer.addVertex(matrix, (float) endRight.x, (float) endRight.y, (float) endRight.z)
                .setColor(0.88F, 0.92F, 1.0F, alpha);
        buffer.addVertex(matrix, (float) endLeft.x, (float) endLeft.y, (float) endLeft.z)
                .setColor(0.88F, 0.92F, 1.0F, alpha);
    }

    private float twinkleStrength(LightningEntity entity, float partialTick) {
        long seed = entity.getLightningSeed();
        float age = entity.getLightningAge() + partialTick;
        float phase = age * 2.25F + (seed & 1023L) * 0.012F;
        float pulse = 0.5F + 0.5F * Mth.sin(phase);
        float shimmer = 0.5F + 0.5F * Mth.sin(phase * 3.1F + 1.7F);
        return Mth.clamp(0.65F + pulse * 0.25F + shimmer * 0.10F, 0.50F, 1.0F);
    }

    private Vec3 billboardOffset(Vec3 start, Vec3 end, Vec3 cameraLocal, float width) {
        Vec3 direction = end.subtract(start).normalize();
        Vec3 midpoint = start.add(end).scale(0.5D);
        Vec3 toCamera = cameraLocal.subtract(midpoint);
        if (toCamera.lengthSqr() < 1.0E-6D) {
            toCamera = perpendicular(direction);
        }

        Vec3 side = direction.cross(toCamera).normalize();
        if (side.lengthSqr() < 1.0E-6D) {
            side = perpendicular(direction);
        }

        return side.scale(width * 0.5D);
    }

    private Vec3 quadNormal(Vec3 start, Vec3 end, Vec3 offset, Vec3 cameraLocal) {
        Vec3 direction = end.subtract(start).normalize();
        Vec3 normal = offset.cross(direction).normalize();
        Vec3 midpoint = start.add(end).scale(0.5D);
        Vec3 toCamera = cameraLocal.subtract(midpoint);
        if (normal.dot(toCamera) < 0.0D) {
            normal = normal.scale(-1.0D);
        }
        return normal;
    }

    private Vec3 perpendicular(Vec3 direction) {
        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-6D) {
            side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        return side.normalize();
    }
}
