package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.InvestigationBoardGeoModel;
import com.dexer.aquanaut.common.block.InvestigationBoardBlock;
import com.dexer.aquanaut.common.block.InvestigationBoardPlacement;
import com.dexer.aquanaut.common.block.InvestigationBoardPlacement.Part;
import com.dexer.aquanaut.common.block.entity.InvestigationBoardBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/** Draws the complete board once from its bottom-left cell. */
public class InvestigationBoardBlockEntityRenderer extends GeoBlockRenderer<InvestigationBoardBlockEntity> {
    public InvestigationBoardBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(new InvestigationBoardGeoModel());
    }

    @Override
    public void render(InvestigationBoardBlockEntity board, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!board.getBlockState().getValue(InvestigationBoardBlock.PART).isAnchor()) {
            return;
        }

        InvestigationBoardPlacement.Pose pose = InvestigationBoardPlacement.forFacing(
                board.getBlockState().getValue(InvestigationBoardBlock.FACING));
        poseStack.pushPose();
        poseStack.translate(pose.x(), pose.y(), pose.z());
        // GeoBlockRenderer reads FACING and rotates the model after applying its standard
        // half-block centring translation. Rotating here as well would both double the yaw and
        // rotate that centring offset, moving the board into the wrong pair of cells.
        super.render(board, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(InvestigationBoardBlockEntity board) {
        Direction facing = board.getBlockState().getValue(InvestigationBoardBlock.FACING);
        BlockPos anchor = InvestigationBoardPlacement.anchorFor(
                board.getBlockState().getValue(InvestigationBoardBlock.PART), facing, board.getBlockPos());
        BlockPos topRight = InvestigationBoardPlacement.cell(Part.TOP_RIGHT, facing, anchor);
        return new AABB(
                Math.min(anchor.getX(), topRight.getX()), anchor.getY(),
                Math.min(anchor.getZ(), topRight.getZ()),
                Math.max(anchor.getX(), topRight.getX()) + 1, topRight.getY() + 1,
                Math.max(anchor.getZ(), topRight.getZ()) + 1);
    }
}
