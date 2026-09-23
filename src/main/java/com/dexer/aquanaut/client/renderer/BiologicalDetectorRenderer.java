package com.dexer.aquanaut.client.renderer;

import javax.annotation.Nullable;

import com.dexer.aquanaut.client.model.BiologicalDetectorModel;
import com.dexer.aquanaut.common.entity.BiologicalDetectorEntity;
import com.dexer.aquanaut.common.entity.DetectorScan;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Draws the biological detector, and its emissions through its glowmask: the bio-green collar that
 * wraps its equator, the phased-array cells on the belt, the ring seams, the pole tell-tales and the
 * sensor core that the clamshell exposes. GeckoLib's glow layer derives the mask path from the base
 * texture, so the two files are paired by name and nothing here has to know the mask's contents.
 *
 * <p>
 * The hull is opaque stainless, so this cuts rather than blends: the only transparency in the
 * texture is the gutter around the atlas islands.
 *
 * <p>
 * The yaw is handed to GeckoLib explicitly. {@code GeoEntityRenderer} reads a model's yaw from the
 * living-body fields, and a machine has none, so for anything that is not a {@code LivingEntity} it
 * passes a yaw of zero and the ball would be drawn at one fixed heading no matter how the code turns
 * it. The detector is radially symmetric, so its own yaw <em>is</em> the scan angle — which is why
 * the same yaw is handed to {@link DetectorHologram} to plot the sweep on.
 *
 * <p>
 * The hologram is drawn after the model, in the entity's own translated frame with no rotation on
 * it: the projection is world-aligned, and it is the instrument that spins, not the map. A
 * {@code super.render} call leaves the pose stack exactly where it found it — at the buoy's feet —
 * so the sphere is simply centred a little above them.
 */
public final class BiologicalDetectorRenderer extends GeoEntityRenderer<BiologicalDetectorEntity> {

    public BiologicalDetectorRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BiologicalDetectorModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    /**
     * Keeps the detector drawn while any of its hologram is on screen.
     *
     * <p>
     * The default test culls on the entity's own half-block box, which would make the sphere wink
     * out the moment the player looked away from the ball at its centre — the most likely moment to
     * be reading it, since a diver who has swum inside is very often looking at the shell rather
     * than at the instrument. Inflating the box to the projection keeps the whole thing alive as one
     * object.
     */
    @Override
    public boolean shouldRender(BiologicalDetectorEntity entity, Frustum frustum, double camX, double camY,
            double camZ) {
        if (entity.isRemoved() || !entity.shouldRender(camX, camY, camZ)) {
            return false;
        }
        return frustum.isVisible(entity.getBoundingBoxForCulling().inflate(DetectorScan.SPHERE_RADIUS));
    }

    @Override
    public void render(BiologicalDetectorEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        poseStack.pushPose();
        poseStack.translate(0.0D, DetectorScan.SPHERE_CENTRE_Y, 0.0D);
        DetectorHologram.render(entity, poseStack, bufferSource, partialTick);
        poseStack.popPose();
    }

    @Override
    protected void applyRotations(BiologicalDetectorEntity animatable, PoseStack poseStack, float ageInTicks,
            float rotationYaw, float partialTick, float nativeScale) {
        super.applyRotations(animatable, poseStack, ageInTicks, animatable.getViewYRot(partialTick),
                partialTick, nativeScale);
    }

    @Override
    public @Nullable RenderType getRenderType(BiologicalDetectorEntity animatable, ResourceLocation texture,
            @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutout(this.getTextureLocation(animatable));
    }
}
