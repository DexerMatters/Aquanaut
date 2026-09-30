package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.client.ClientDivingEquipmentData;
import com.dexer.aquanaut.common.diving.DivingEquipmentSlotType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the worn diving equipment as real box geometry around the player model:
 * a mask on the face with a strap band around the head, an oxygen tank on the
 * back with a shoulder-and-waist harness, and flipper blades stepping forward
 * past the toes.
 *
 * <p>Each piece is a handful of cuboids attached to a vanilla model part
 * ({@code head}, {@code body}, {@code leftLeg} / {@code rightLeg}), so the gear
 * follows every head turn, body twist, and leg swing exactly like armour does.
 * The UV layout of the {@code textures/equipment/*_on_body.png} textures follows
 * Minecraft's own box unwrap (the {@code CubeListBuilder.texOffs} layout) at two
 * texels per model unit.
 *
 * <p>The geometry and atlas numbers are mirrored from
 * {@code scripts/diving_equipment_layout.py} — keep the two in sync. That script
 * also generates the textures and previews the worn look.
 */
public final class DivingEquipmentRenderLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    /** Texels per model unit in the on-body textures. */
    private static final float TEXELS_PER_UNIT = 1.0F;
    /**
     * Model units -> blocks. {@code ModelPart.translateAndRotate} leaves the pose
     * stack in block space (it divides part pivots by 16), so every cuboid corner
     * has to be converted before it is emitted, or the gear renders 16x oversized.
     */
    private static final float UNIT = 1.0F / 16.0F;
    /** Side length of every on-body texture, in texels. */
    private static final float ATLAS = 64.0F;

    /**
     * One cuboid of a worn piece: {@code pos} is the minimum corner in the parent
     * model part's space, {@code size} the box size in model units, {@code uv} the
     * atlas region origin in texels. {@code mirrorX} marks the box whose twin is
     * drawn mirrored across x = 0.
     */
    private record Box(float x0, float y0, float z0, float w, float h, float d,
            int u, int v, boolean mirrorX) {
    }

    // Mask -> head: a diving helmet enclosing the whole head cube (x/z -4..4, y -8..0).
    private static final Box MASK_SHELL = new Box(-4.5F, -8.5F, -4.5F, 9.0F, 9.0F, 9.0F, 0, 0, false);
    private static final Box MASK_VISOR = new Box(-3.0F, -6.0F, 4.2F, 6.0F, 4.0F, 1.0F, 36, 0, false);
    private static final Box MASK_VALVE = new Box(-1.0F, -9.4F, -1.0F, 2.0F, 1.0F, 2.0F, 50, 0, false);
    private static final Box MASK_POD = new Box(4.3F, -5.5F, -1.0F, 1.0F, 2.0F, 2.0F, 36, 5, true);

    // Oxygen tank -> body (body cube spans x -4..4, z -2..2, y 0..12; valve up, boot ring down).
    private static final Box TANK_BOTTLE = new Box(-2.5F, 2.0F, -4.9F, 5.0F, 8.0F, 3.0F, 0, 0, false);
    private static final Box TANK_CAP = new Box(-1.5F, 0.05F, -4.2F, 3.0F, 2.0F, 2.0F, 16, 0, false);
    private static final Box TANK_VALVE = new Box(-0.5F, -0.9F, -5.1F, 1.0F, 1.0F, 1.0F, 26, 0, false);
    private static final Box TANK_RING = new Box(-3.0F, 9.9F, -5.1F, 6.0F, 1.0F, 3.0F, 0, 11, false);
    private static final Box TANK_SHOULDER =
            new Box(1.0F, -0.6F, -3.8F, 2.0F, 1.0F, 6.0F, 0, 15, true);
    private static final Box TANK_CHEST = new Box(1.0F, 0.2F, 1.6F, 2.0F, 9.0F, 1.0F, 16, 15, true);
    private static final Box TANK_BELT = new Box(-4.5F, 8.5F, -2.5F, 9.0F, 1.0F, 5.0F, 0, 26, false);

    // Flippers -> legs (leg cube spans x/z -2..2, y 0..12).
    private static final Box FLIPPER_POCKET = new Box(-2.5F, 8.5F, -2.5F, 5.0F, 4.0F, 5.0F, 0, 0, false);
    private static final Box FLIPPER_STRAP = new Box(-2.5F, 8.0F, -2.5F, 5.0F, 1.0F, 5.0F, 0, 9, false);
    private static final Box FLIPPER_BLADE1 = new Box(-2.5F, 11.5F, 2.4F, 5.0F, 1.0F, 2.0F, 0, 15, false);
    private static final Box FLIPPER_BLADE2 = new Box(-2.0F, 11.4F, 4.3F, 4.0F, 1.0F, 1.0F, 0, 18, false);
    private static final Box FLIPPER_BLADE3 = new Box(-1.5F, 11.3F, 5.2F, 3.0F, 1.0F, 1.0F, 0, 20, false);

    private static final Box[] MASK_PIECE = {MASK_SHELL, MASK_VALVE, MASK_POD, MASK_VISOR};
    private static final Box[] TANK_PIECE =
            {TANK_BOTTLE, TANK_CAP, TANK_VALVE, TANK_RING, TANK_SHOULDER, TANK_CHEST, TANK_BELT};
    private static final Box[] FLIPPER_PIECE =
            {FLIPPER_POCKET, FLIPPER_STRAP, FLIPPER_BLADE1, FLIPPER_BLADE2, FLIPPER_BLADE3};

    public DivingEquipmentRenderLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight,
            AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick,
            float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible() || player.isSpectator()) {
            return;
        }

        PlayerModel<AbstractClientPlayer> model = getParentModel();

        ItemStack mask = ClientDivingEquipmentData.getStack(player, DivingEquipmentSlotType.MASK);
        if (!mask.isEmpty() && model.head.visible) {
            poseStack.pushPose();
            model.head.translateAndRotate(poseStack);
            drawPiece(poseStack, buffers, packedLight, textureFor(mask), MASK_PIECE);
            poseStack.popPose();
        }

        ItemStack tank = ClientDivingEquipmentData.getStack(player, DivingEquipmentSlotType.TANK);
        if (!tank.isEmpty() && model.body.visible) {
            poseStack.pushPose();
            model.body.translateAndRotate(poseStack);
            drawPiece(poseStack, buffers, packedLight, textureFor(tank), TANK_PIECE);
            poseStack.popPose();
        }

        ItemStack flippers = ClientDivingEquipmentData.getStack(player, DivingEquipmentSlotType.FLIPPERS);
        if (!flippers.isEmpty()) {
            ResourceLocation texture = textureFor(flippers);
            if (model.leftLeg.visible) {
                poseStack.pushPose();
                model.leftLeg.translateAndRotate(poseStack);
                drawPiece(poseStack, buffers, packedLight, texture, FLIPPER_PIECE);
                poseStack.popPose();
            }
            if (model.rightLeg.visible) {
                poseStack.pushPose();
                model.rightLeg.translateAndRotate(poseStack);
                drawPiece(poseStack, buffers, packedLight, texture, FLIPPER_PIECE);
                poseStack.popPose();
            }
        }
    }

    private static void drawPiece(PoseStack poseStack, MultiBufferSource buffers, int packedLight,
            ResourceLocation texture, Box[] boxes) {
        PoseStack.Pose pose = poseStack.last();
        for (Box box : boxes) {
            VertexConsumer consumer = buffers.getBuffer(isTranslucent(box)
                    ? RenderType.entityTranslucent(texture)
                    : RenderType.entityCutoutNoCull(texture));
            drawBox(pose, consumer, packedLight, box, false);
            if (box.mirrorX()) {
                drawBox(pose, consumer, packedLight, box, true);
            }
        }
    }

    /** The helmet visor is glass: blended, so the player's face shows through it. */
    private static boolean isTranslucent(Box box) {
        return box == MASK_VISOR;
    }

    private static void drawBox(PoseStack.Pose pose, VertexConsumer consumer, int packedLight,
            Box box, boolean mirrored) {
        float x0 = box.x0();
        float x1 = x0 + box.w();
        if (mirrored) {
            x0 = -x1;
            x1 = -box.x0();
        }
        float y0 = box.y0();
        float y1 = y0 + box.h();
        // LivingEntityRenderer composes YP.rotation(180 - bodyYaw) with scale(-1, -1, 1),
        // so part-local +z ends up pointing backwards in the world. Vanilla bakes that
        // into its cube UV layouts; our boxes compensate here so that a box written
        // with +z towards the wearer's front (as in diving_equipment_layout.py) really
        // lands in front: mask on the face, tank on the back, fin blades past the toes.
        float z0 = -(box.z0() + box.d());
        float z1 = -box.z0();

        // Face rectangles inside the atlas region, matching the generator's unwrap.
        float du = box.d() * TEXELS_PER_UNIT; // depth, in texels
        float wu = box.w() * TEXELS_PER_UNIT; // width, in texels
        float hu = box.h() * TEXELS_PER_UNIT; // height, in texels
        float u = box.u();
        float v = box.v();

        // top / bottom: w x d, -x / +x: d x h, front/back: w x h.
        // In this z-negated space the wearer-front plane is z0 and the back plane
        // is z1; corner order per quad is top-left, top-right, bottom-right,
        // bottom-left exactly as the generator painted them into the atlas.
        quad(pose, consumer, packedLight, u + du, v, wu, du,
                x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0.0F, -1.0F, 0.0F);
        quad(pose, consumer, packedLight, u + du + wu, v, wu, du,
                x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0.0F, 1.0F, 0.0F);
        quad(pose, consumer, packedLight, u, v + du, du, hu,
                x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1.0F, 0.0F, 0.0F);
        quad(pose, consumer, packedLight, u + du + wu, v + du, du, hu,
                x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1.0F, 0.0F, 0.0F);
        quad(pose, consumer, packedLight, u + du, v + du, wu, hu,
                x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, 0.0F, 0.0F, -1.0F);
        quad(pose, consumer, packedLight, u + du + wu + du, v + du, wu, hu,
                x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, 0.0F, 0.0F, 1.0F);
    }

    /**
     * Emits one textured quad. The four corners are given in order top-left,
     * top-right, bottom-right, bottom-left as seen from outside the box, and
     * {@code (u, v)} is the top-left texel of the face's rectangle.
     */
    private static void quad(PoseStack.Pose pose, VertexConsumer consumer, int packedLight,
            float u, float v, float uw, float vh,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float x2, float y2, float z2, float x3, float y3, float z3,
            float nx, float ny, float nz) {
        float u0 = u / ATLAS;
        float u1 = (u + uw) / ATLAS;
        float v0 = v / ATLAS;
        float v1 = (v + vh) / ATLAS;
        vertex(consumer, pose, x0 * UNIT, y0 * UNIT, z0 * UNIT, u0, v0, packedLight, nx, ny, nz);
        vertex(consumer, pose, x1 * UNIT, y1 * UNIT, z1 * UNIT, u1, v0, packedLight, nx, ny, nz);
        vertex(consumer, pose, x2 * UNIT, y2 * UNIT, z2 * UNIT, u1, v1, packedLight, nx, ny, nz);
        vertex(consumer, pose, x3 * UNIT, y3 * UNIT, z3 * UNIT, u0, v1, packedLight, nx, ny, nz);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
            float x, float y, float z, float u, float v, int packedLight,
            float nx, float ny, float nz) {
        consumer.addVertex(pose.pose(), x, y, z)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, nx, ny, nz);
    }

    private static ResourceLocation textureFor(ItemStack stack) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "textures/equipment/" + path + "_on_body.png");
    }
}
