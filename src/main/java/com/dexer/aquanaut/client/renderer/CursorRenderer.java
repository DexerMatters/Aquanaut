package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.CursorModel;
import com.dexer.aquanaut.common.entity.CursorEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Draws the cursor and its floating tag.
 *
 * <p>
 * The model is the whole of the cursor's body; the label above it comes from the shared
 * {@link TagLabelRenderer}, which every taggable marker wears.
 */
public final class CursorRenderer extends GeoEntityRenderer<CursorEntity> {

    /** Gap between the top of the model and the label. */
    private static final float TAG_HEIGHT_OFFSET = 0.6F;

    public CursorRenderer(EntityRendererProvider.Context context) {
        super(context, new CursorModel());
        // The beacon on top of the antenna is the only thing the glowmask lights up.
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public void render(CursorEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        TagLabelRenderer.render(entity, TAG_HEIGHT_OFFSET, this.entityRenderDispatcher, getFont(),
                poseStack, bufferSource, packedLight);
    }
}
