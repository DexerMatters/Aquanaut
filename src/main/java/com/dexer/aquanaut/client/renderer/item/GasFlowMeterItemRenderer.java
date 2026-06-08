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

public final class GasFlowMeterItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final String STANDALONE_VARIANT = "standalone";
    private static final ModelResourceLocation GUI_MODEL = new ModelResourceLocation(
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "item/gas_flow_meter_gui"),
            STANDALONE_VARIANT);
    private static final ModelResourceLocation HELD_MODEL = new ModelResourceLocation(
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "item/gas_flow_meter_held"),
            STANDALONE_VARIANT);

    private static GasFlowMeterItemRenderer instance;

    private final ItemRenderer itemRenderer;
    private final ModelManager modelManager;

    private GasFlowMeterItemRenderer(Minecraft minecraft) {
        super(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
        this.itemRenderer = minecraft.getItemRenderer();
        this.modelManager = minecraft.getModelManager();
    }

    public static BlockEntityWithoutLevelRenderer getInstance() {
        if (instance == null) {
            instance = new GasFlowMeterItemRenderer(Minecraft.getInstance());
        }
        return instance;
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(GUI_MODEL);
        event.register(HELD_MODEL);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BakedModel model = this.modelManager.getModel(displayContext == ItemDisplayContext.GUI ? GUI_MODEL : HELD_MODEL);
        poseStack.pushPose();
        // ItemRenderer already applied the outer builtin/entity model transform and one -0.5 item centering
        // before delegating to this renderer. Cancel that once so the helper model is rendered in the normal
        // single-item-model basis instead of being offset a second time.
        poseStack.translate(0.5D, 0.5D, 0.5D);
        if (displayContext == ItemDisplayContext.GUI) {
            poseStack.scale(1.06F, 1.06F, 1.0F);
        }
        this.itemRenderer.render(stack, displayContext, isLeftHand(displayContext), poseStack, buffer, packedLight,
                packedOverlay, model);
        poseStack.popPose();
    }

    private static boolean isLeftHand(ItemDisplayContext displayContext) {
        return displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    }
}
