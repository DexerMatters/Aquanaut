package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.common.entity.AbstractTaggableEntity;
import com.dexer.aquanaut.common.entity.TagRules;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

/**
 * Draws the floating tag that every {@link AbstractTaggableEntity} wears.
 *
 * <p>
 * A hand-rolled billboard rather than a vanilla nameplate for one reason: vanilla nameplates only
 * take one of sixteen team colours, and a tag's colour is arbitrary. It is also culled much closer
 * than vanilla's 64 blocks — at {@link #TAG_VISIBLE_RANGE} blocks the label disappears, so a field
 * of markers stays readable instead of turning into a wall of numbers.
 *
 * <p>
 * Shared by the cursor and the drone renderers so the two cannot drift apart; each passes the height
 * of its own model so the label clears it.
 */
public final class TagLabelRenderer {

    /** Distance beyond which the tag is not drawn at all. */
    private static final double TAG_VISIBLE_RANGE = 32.0D;

    /** Vanilla's nameplate scale, so tags sit in the same visual language. */
    private static final float TAG_SCALE = 0.025F;

    private TagLabelRenderer() {
    }

    /**
     * Draws {@code entity}'s label above its model.
     *
     * @param topOffset how far above the entity's hitbox the label floats, in blocks
     */
    public static void render(AbstractTaggableEntity entity, float topOffset,
            EntityRenderDispatcher dispatcher, Font font, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        if (!entity.hasTag()) {
            return;
        }
        if (dispatcher.distanceToSqr(entity) > TAG_VISIBLE_RANGE * TAG_VISIBLE_RANGE) {
            return;
        }

        Component label = entity.getTagComponent();
        int color = TagRules.toArgb(entity.getTagColor());

        poseStack.pushPose();
        // Sit above the model, then face the camera. The negative Y scale is what turns the text
        // upright once the camera orientation has rotated the stack, exactly as vanilla nameplates
        // do it.
        poseStack.translate(0.0D, entity.getBbHeight() + topOffset, 0.0D);
        poseStack.mulPose(dispatcher.cameraOrientation());
        poseStack.scale(TAG_SCALE, -TAG_SCALE, TAG_SCALE);

        Matrix4f matrix = poseStack.last().pose();
        float x = -font.width(label) / 2.0F;

        float backgroundOpacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
        int background = (int) (backgroundOpacity * 255.0F) << 24;

        // A faint pass that bleeds through terrain, then the solid one on top of it.
        font.drawInBatch(label, x, 0.0F, (color & 0x00FFFFFF) | 0x20000000, false, matrix,
                bufferSource, Font.DisplayMode.SEE_THROUGH, background, packedLight);
        font.drawInBatch(label, x, 0.0F, color, false, matrix, bufferSource,
                Font.DisplayMode.NORMAL, 0, packedLight);

        poseStack.popPose();
    }
}
