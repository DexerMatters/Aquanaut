package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.GiantOctopusTentacleModel;
import com.dexer.aquanaut.client.model.GiantOctopusTentaclePartModel;
import com.dexer.aquanaut.client.renderer.serpentine.BaseSerpentineRenderer;
import com.dexer.aquanaut.common.entity.GiantOctopusTentacleEntity;
import com.dexer.aquanaut.common.entity.serpentine.SerpentineSegment;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class GiantOctopusTentacleRenderer extends BaseSerpentineRenderer<GiantOctopusTentacleEntity> {

    // Native dimensions of each model in blocks (1 GeckoLib unit = 1/16 block).
    // Section main cube: 22 × 22 × 24 units → 1.375 × 1.375 × 1.5 blocks
    // Tip geometry z-extent: 24 units → 1.5 blocks; cross-section: 10 × 10 units →
    // 0.625 blocks
    private static final double SECTION_NATIVE_WIDTH = 1.375D;
    private static final double SECTION_NATIVE_HEIGHT = 1.375D;
    private static final double SECTION_NATIVE_LENGTH = 1.5D;
    private static final double TIP_NATIVE_WIDTH = 0.625D;
    private static final double TIP_NATIVE_HEIGHT = 0.625D;
    private static final double TIP_NATIVE_LENGTH = 1.5D;

    private static final ResourceLocation SECTION_MODEL = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "geo/giant_octopus_tentacle_section.geo.json");
    private static final ResourceLocation TIP_MODEL = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "geo/giant_octopus_tentacle_tip.geo.json");
    private static final ResourceLocation SECTION_TEXTURE = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "textures/entity/giant_octopus_tentacle_section.png");
    private static final ResourceLocation TIP_TEXTURE = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "textures/entity/giant_octopus_tentacle_tip.png");

    private final GiantOctopusTentaclePartModel sectionModel = new GiantOctopusTentaclePartModel(SECTION_MODEL,
            SECTION_TEXTURE);
    private final GiantOctopusTentaclePartModel tipModel = new GiantOctopusTentaclePartModel(TIP_MODEL, TIP_TEXTURE);

    private SerpentineSegment currentSegment;
    private ResourceLocation currentTexture;

    public GiantOctopusTentacleRenderer(EntityRendererProvider.Context context) {
        super(context, new GiantOctopusTentacleModel());
        this.shadowRadius = 1.8F;
    }

    @Override
    public void render(GiantOctopusTentacleEntity entity, float entityYaw, float partialTick,
            PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);

        this.animatable = entity;

        try {
            BakedGeoModel sectionBaked = this.sectionModel.getBakedModel(this.sectionModel.getModelResource(entity));
            BakedGeoModel tipBaked = this.tipModel.getBakedModel(this.tipModel.getModelResource(entity));
            int packedOverlay = getPackedOverlay(entity, 0.0F, partialTick);
            int colour = getRenderColor(entity, partialTick, packedLight).argbInt();
            SerpentineSegment[] segments = entity.getSegments();

            for (int i = 0; i < segments.length; i++) {
                this.currentSegment = segments[i];
                this.currentTexture = i == segments.length - 1 ? TIP_TEXTURE : SECTION_TEXTURE;

                BakedGeoModel bakedModel = i == segments.length - 1 ? tipBaked : sectionBaked;
                RenderType renderType = getRenderType(entity, this.currentTexture, buffers, partialTick);
                if (renderType == null) {
                    continue;
                }

                VertexConsumer buffer = buffers.getBuffer(renderType);
                reRender(bakedModel, poseStack, buffers, entity, renderType, buffer, partialTick, packedLight,
                        packedOverlay, colour);
            }
        } finally {
            this.currentSegment = null;
            this.currentTexture = null;
            this.animatable = null;
        }
    }

    @Override
    public ResourceLocation getTextureLocation(GiantOctopusTentacleEntity animatable) {
        return this.currentTexture != null ? this.currentTexture : super.getTextureLocation(animatable);
    }

    @Override
    protected void applyRotations(GiantOctopusTentacleEntity animatable, PoseStack poseStack, float ageInTicks,
            float rotationYaw, float partialTick, float nativeScale) {
        if (this.currentSegment == null) {
            return;
        }

        double entityX = Mth.lerp(partialTick, animatable.xo, animatable.getX());
        double entityY = Mth.lerp(partialTick, animatable.yo, animatable.getY());
        double entityZ = Mth.lerp(partialTick, animatable.zo, animatable.getZ());
        double segmentX = Mth.lerp(partialTick, this.currentSegment.xo, this.currentSegment.getX());
        double segmentY = Mth.lerp(partialTick, this.currentSegment.yo, this.currentSegment.getY())
                + this.currentSegment.getBbHeight() * 0.5D;
        double segmentZ = Mth.lerp(partialTick, this.currentSegment.zo, this.currentSegment.getZ());

        float yaw = Mth.rotLerp(partialTick, this.currentSegment.yRotO, this.currentSegment.getYRot());
        float pitch = Mth.lerp(partialTick, this.currentSegment.xRotO, this.currentSegment.getXRot());

        boolean tip = this.currentTexture != null && this.currentTexture.equals(TIP_TEXTURE);
        double nativeWidth = tip ? TIP_NATIVE_WIDTH : SECTION_NATIVE_WIDTH;
        double nativeHeight = tip ? TIP_NATIVE_HEIGHT : SECTION_NATIVE_HEIGHT;
        double nativeLength = tip ? TIP_NATIVE_LENGTH : SECTION_NATIVE_LENGTH;
        double scaleX = this.currentSegment.getDefinition().width / nativeWidth;
        double scaleY = this.currentSegment.getDefinition().height / nativeHeight;
        // For the tip, use uniform scale (based on cross-section width) to preserve the
        // model's
        // natural taper aspect ratio instead of squashing it into a cubic hitbox shape.
        double scaleZ = tip ? scaleX : this.currentSegment.getDefinition().spacing / nativeLength;
        Vec3 forward = Vec3.directionFromRotation(pitch, yaw);
        double halfLength = this.currentSegment.getDefinition().spacing * 0.5D;
        double pivotX = segmentX - forward.x * halfLength;
        double pivotY = segmentY - forward.y * halfLength;
        double pivotZ = segmentZ - forward.z * halfLength;

        poseStack.translate(
                (pivotX - entityX) / nativeScale,
                (pivotY - entityY) / nativeScale,
                (pivotZ - entityZ) / nativeScale);
        // YP(-yaw) then XP(+pitch): maps model +Z exactly onto the segment's forward
        // direction.
        // (Verified: YP(-yaw) * XP(pitch) * [0,0,1] = directionFromRotation(pitch,
        // yaw))
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.scale((float) scaleX, (float) scaleY, (float) scaleZ);
    }
}