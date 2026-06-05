package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.GiantAbyssWormModel;
import com.dexer.aquanaut.client.model.GiantAbyssWormPartModel;
import com.dexer.aquanaut.client.renderer.serpentine.BaseSerpentineRenderer;
import com.dexer.aquanaut.common.entity.GiantAbyssWormEntity;
import com.dexer.aquanaut.common.entity.serpentine.SerpentineSegment;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class GiantAbyssWormRenderer extends BaseSerpentineRenderer<GiantAbyssWormEntity> {

    // Native model half-lengths in blocks (1 GeckoLib unit = 1/16 block).
    // Head z-extent: 0 → 48 units = 3.0 blocks → half = 1.5
    // Section z-extent: 0 → 40 units = 2.5 blocks → half = 1.25
    // Tail z-extent: 0 → 64 units = 4.0 blocks → half = 2.0
    private static final double HEAD_HALF_LEN = 1.5D;
    private static final double SECTION_HALF_LEN = 1.25D;
    private static final double TAIL_HALF_LEN = 2.0D;

    private static final ResourceLocation SECTION_MODEL = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "geo/giant_abyss_worm_section.geo.json");
    private static final ResourceLocation TAIL_MODEL = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "geo/giant_abyss_worm_tail.geo.json");
    private static final ResourceLocation HEAD_TEXTURE = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "textures/entity/giant_abyss_worm_head.png");
    private static final ResourceLocation SECTION_TEXTURE = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "textures/entity/giant_abyss_worm_section.png");
    private static final ResourceLocation TAIL_TEXTURE = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "textures/entity/giant_abyss_worm_tail.png");

    private final GiantAbyssWormPartModel sectionModel = new GiantAbyssWormPartModel(SECTION_MODEL, SECTION_TEXTURE);
    private final GiantAbyssWormPartModel tailModel = new GiantAbyssWormPartModel(TAIL_MODEL, TAIL_TEXTURE);

    // State shared between render() and applyRotations()
    private SerpentineSegment currentSegment = null;
    private boolean renderingHead = true;
    private ResourceLocation currentTexture = null;

    public GiantAbyssWormRenderer(EntityRendererProvider.Context context) {
        super(context, new GiantAbyssWormModel());
        this.shadowRadius = 2.5F;
    }

    // -------------------------------------------------------------------------
    // Render: head via super, then all body segments
    // -------------------------------------------------------------------------
    @Override
    public boolean shouldRender(GiantAbyssWormEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(GiantAbyssWormEntity entity, float entityYaw, float partialTick,
            PoseStack poseStack, MultiBufferSource buffers, int packedLight) {

        // 1. Render head (entity itself) via standard GeckoLib pipeline
        this.renderingHead = true;
        this.currentSegment = null;
        this.currentTexture = HEAD_TEXTURE;
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);

        // 2. Render body segments
        this.animatable = entity;
        try {
            BakedGeoModel sectionBaked = sectionModel.getBakedModel(sectionModel.getModelResource(entity));
            BakedGeoModel tailBaked = tailModel.getBakedModel(tailModel.getModelResource(entity));
            int packedOverlay = getPackedOverlay(entity, 0.0F, partialTick);
            int colour = getRenderColor(entity, partialTick, packedLight).argbInt();
            SerpentineSegment[] segments = entity.getSegments();

            for (int i = 0; i < segments.length; i++) {
                this.currentSegment = segments[i];
                this.renderingHead = false;
                boolean isTail = (i == segments.length - 1);
                this.currentTexture = isTail ? TAIL_TEXTURE : SECTION_TEXTURE;

                BakedGeoModel bakedModel = isTail ? tailBaked : sectionBaked;
                RenderType renderType = getRenderType(entity, this.currentTexture, buffers, partialTick);
                if (renderType == null)
                    continue;

                VertexConsumer buffer = buffers.getBuffer(renderType);
                reRender(bakedModel, poseStack, buffers, entity, renderType, buffer,
                        partialTick, packedLight, packedOverlay, colour);
            }
        } finally {
            this.currentSegment = null;
            this.renderingHead = true;
            this.currentTexture = null;
            this.animatable = null;
        }
    }

    @Override
    public ResourceLocation getTextureLocation(GiantAbyssWormEntity animatable) {
        return this.currentTexture != null ? this.currentTexture : HEAD_TEXTURE;
    }

    // -------------------------------------------------------------------------
    // applyRotations: position and orient each piece at its world location
    // -------------------------------------------------------------------------
    @Override
    protected void applyRotations(GiantAbyssWormEntity entity, PoseStack poseStack,
            float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {

        // Entity origin in world space (feet position, lerped for smoothness)
        double entityX = Mth.lerp(partialTick, entity.xo, entity.getX());
        double entityY = Mth.lerp(partialTick, entity.yo, entity.getY());
        double entityZ = Mth.lerp(partialTick, entity.zo, entity.getZ());

        double centerX, centerY, centerZ;
        float yaw, pitch, roll;
        double halfLen;

        if (renderingHead) {
            // Head: centre is at entity centre (feet + half-height)
            centerX = entityX;
            centerY = entityY + entity.getBbHeight() * 0.5D;
            centerZ = entityZ;
            yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
            pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
            roll = entity.getHeadRoll(partialTick);
            halfLen = HEAD_HALF_LEN;
        } else {
            // Body segment
            SerpentineSegment seg = this.currentSegment;
            centerX = Mth.lerp(partialTick, seg.xo, seg.getX());
            centerY = Mth.lerp(partialTick, seg.yo, seg.getY()) + seg.getBbHeight() * 0.5D;
            centerZ = Mth.lerp(partialTick, seg.zo, seg.getZ());
            yaw = Mth.rotLerp(partialTick, seg.yRotO, seg.getYRot());
            pitch = Mth.lerp(partialTick, seg.xRotO, seg.getXRot());
            roll = Mth.lerp(partialTick, seg.rollO, seg.getRoll());
            boolean isTail = currentTexture != null && currentTexture.equals(TAIL_TEXTURE);
            halfLen = isTail ? TAIL_HALF_LEN : SECTION_HALF_LEN;
        }

        // The model's z=0 end (rear connection) should sit at: centre - forward * halfLen
        // For tail: flipped so tip points away from body
        Vec3 forward = Vec3.directionFromRotation(pitch, yaw);
        boolean isTailPiece = renderingHead ? false
                : currentTexture != null && currentTexture.equals(TAIL_TEXTURE);
        double pivX, pivY, pivZ;
        if (isTailPiece) {
            pivX = centerX + forward.x * halfLen;
            pivY = centerY + forward.y * halfLen;
            pivZ = centerZ + forward.z * halfLen;
        } else {
            pivX = centerX - forward.x * halfLen;
            pivY = centerY - forward.y * halfLen;
            pivZ = centerZ - forward.z * halfLen;
        }

        poseStack.translate(
                (pivX - entityX) / nativeScale,
                (pivY - entityY) / nativeScale,
                (pivZ - entityZ) / nativeScale);

        if (isTailPiece) {
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw + 180.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-roll));
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
            poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        }
    }
}
