package com.dexer.aquanaut.client.renderer.item;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.item.HandheldSearchlightItem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * Draws the handheld searchlight, in both of its states.
 *
 * <p>
 * Each state needs a pair of models, because the lamp is painted in the slot
 * (a crisp 16x16 sprite, like every other Aquanaut item) but is a real object in
 * the hand, on the ground and in an item frame. The pairs are:
 *
 * <ul>
 *   <li>{@code item/handheld_searchlight_gui} and {@code ..._gui_on} -- the
 *       inventory and notebook sprites;</li>
 *   <li>{@code item/handheld_searchlight_held} and {@code ..._held_on} -- the
 *       element model. The two share one geometry and differ only in the lens
 *       texture, so switching the lamp on can never move the handle in the
 *       fist.</li>
 * </ul>
 *
 * The lamp state is read from the stack, so the server-side switch is what the
 * client draws. Like {@link GasFlowMeterItemRenderer}, all four models are
 * registered as standalone variants and are never referenced by an item model
 * of their own; the item model itself is the {@code builtin/entity} bridge that
 * carries the display transforms.
 */
public final class HandheldSearchlightItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final String STANDALONE_VARIANT = "standalone";

    private static final ModelResourceLocation GUI = standalone("item/handheld_searchlight_gui");
    private static final ModelResourceLocation GUI_ON = standalone("item/handheld_searchlight_gui_on");
    private static final ModelResourceLocation HELD = standalone("item/handheld_searchlight_held");
    private static final ModelResourceLocation HELD_ON = standalone("item/handheld_searchlight_held_on");

    private static HandheldSearchlightItemRenderer instance;

    private final ItemRenderer itemRenderer;
    private final ModelManager modelManager;

    private HandheldSearchlightItemRenderer(Minecraft minecraft) {
        super(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
        this.itemRenderer = minecraft.getItemRenderer();
        this.modelManager = minecraft.getModelManager();
    }

    public static HandheldSearchlightItemRenderer getInstance() {
        if (instance == null) {
            instance = new HandheldSearchlightItemRenderer(Minecraft.getInstance());
        }

        return instance;
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(GUI);
        event.register(GUI_ON);
        event.register(HELD);
        event.register(HELD_ON);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, int packedOverlay) {
        boolean gui = displayContext == ItemDisplayContext.GUI;
        boolean lit = HandheldSearchlightItem.isLit(stack);
        BakedModel model = this.modelManager.getModel(lit
                ? (gui ? GUI_ON : HELD_ON)
                : (gui ? GUI : HELD));
        poseStack.pushPose();
        // ItemRenderer already applied the outer builtin/entity model transform and one -0.5 item
        // centering before delegating to this renderer. Cancel that once so the helper model is
        // rendered in the normal single-item-model basis instead of being offset a second time.
        poseStack.translate(0.5D, 0.5D, 0.5D);
        this.itemRenderer.render(stack, displayContext, isLeftHand(displayContext), poseStack, buffer, packedLight,
                packedOverlay, model);
        poseStack.popPose();
    }

    private static ModelResourceLocation standalone(String path) {
        return new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, path),
                STANDALONE_VARIANT);
    }

    private static boolean isLeftHand(ItemDisplayContext displayContext) {
        return displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    }
}
