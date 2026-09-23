package com.dexer.aquanaut.client.renderer.item;

import com.dexer.aquanaut.Aquanaut;
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
 * Draws the air bladders. Every bladder needs two models:
 *
 * <ul>
 *   <li>{@code item/<name>_gui} -- the 16x16 sprite, so the inventory slot shows
 *       the same painted icon every other Aquanaut item does;</li>
 *   <li>{@code item/<name>_held} -- the element model, so the bladder is
 *       genuinely three dimensional in the hand, on the ground, in an item
 *       frame and in the notebook.</li>
 * </ul>
 *
 * Both are registered as standalone models and are never referenced by an item
 * model of their own, exactly like {@link GasFlowMeterItemRenderer}. One
 * instance per bladder item, because the two differ only by which pair of models
 * they draw.
 */
public final class HandheldAirBladderItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final String STANDALONE_VARIANT = "standalone";

    private static final ModelResourceLocation SMALL_GUI = standalone("item/handheld_air_bladder_gui");
    private static final ModelResourceLocation SMALL_HELD = standalone("item/handheld_air_bladder_held");
    private static final ModelResourceLocation LARGE_GUI = standalone("item/large_handheld_air_bladder_gui");
    private static final ModelResourceLocation LARGE_HELD = standalone("item/large_handheld_air_bladder_held");

    private static HandheldAirBladderItemRenderer smallInstance;
    private static HandheldAirBladderItemRenderer largeInstance;

    private final ItemRenderer itemRenderer;
    private final ModelManager modelManager;
    private final ModelResourceLocation guiModel;
    private final ModelResourceLocation heldModel;

    private HandheldAirBladderItemRenderer(Minecraft minecraft, ModelResourceLocation guiModel,
            ModelResourceLocation heldModel) {
        super(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
        this.itemRenderer = minecraft.getItemRenderer();
        this.modelManager = minecraft.getModelManager();
        this.guiModel = guiModel;
        this.heldModel = heldModel;
    }

    private static ModelResourceLocation standalone(String path) {
        return new ModelResourceLocation(
                ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, path), STANDALONE_VARIANT);
    }

    public static BlockEntityWithoutLevelRenderer small() {
        if (smallInstance == null) {
            smallInstance = new HandheldAirBladderItemRenderer(Minecraft.getInstance(), SMALL_GUI, SMALL_HELD);
        }
        return smallInstance;
    }

    public static BlockEntityWithoutLevelRenderer large() {
        if (largeInstance == null) {
            largeInstance = new HandheldAirBladderItemRenderer(Minecraft.getInstance(), LARGE_GUI, LARGE_HELD);
        }
        return largeInstance;
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(SMALL_GUI);
        event.register(SMALL_HELD);
        event.register(LARGE_GUI);
        event.register(LARGE_HELD);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BakedModel model = this.modelManager.getModel(
                displayContext == ItemDisplayContext.GUI ? this.guiModel : this.heldModel);
        poseStack.pushPose();
        // ItemRenderer already applied the outer builtin/entity model transform and one -0.5 item centering
        // before delegating to this renderer. Cancel that once so the helper model is rendered in the normal
        // single-item-model basis instead of being offset a second time.
        poseStack.translate(0.5D, 0.5D, 0.5D);
        this.itemRenderer.render(stack, displayContext, isLeftHand(displayContext), poseStack, buffer, packedLight,
                packedOverlay, model);
        poseStack.popPose();
    }

    private static boolean isLeftHand(ItemDisplayContext displayContext) {
        return displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    }
}
