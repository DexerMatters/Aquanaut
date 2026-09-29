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

/** Uses a crisp painted sprite in inventories and the full shell camera model everywhere else. */
public final class ShellCameraItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final String STANDALONE_VARIANT = "standalone";
    private static final ModelResourceLocation GUI = standalone("item/shell_camera_gui");
    private static final ModelResourceLocation HELD = standalone("item/shell_camera_held");

    private static ShellCameraItemRenderer instance;

    private final ItemRenderer itemRenderer;
    private final ModelManager modelManager;

    private ShellCameraItemRenderer(Minecraft minecraft) {
        super(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
        this.itemRenderer = minecraft.getItemRenderer();
        this.modelManager = minecraft.getModelManager();
    }

    public static ShellCameraItemRenderer getInstance() {
        if (instance == null) {
            instance = new ShellCameraItemRenderer(Minecraft.getInstance());
        }
        return instance;
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(GUI);
        event.register(HELD);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BakedModel model = this.modelManager.getModel(displayContext == ItemDisplayContext.GUI ? GUI : HELD);
        poseStack.pushPose();
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
