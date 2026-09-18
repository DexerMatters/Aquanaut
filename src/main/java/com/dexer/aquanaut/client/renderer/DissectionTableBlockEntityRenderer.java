package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.DissectionTableGeoModel;
import com.dexer.aquanaut.common.block.DissectionTableMultiblock;
import com.dexer.aquanaut.common.block.DissectionTablePlacement;
import com.dexer.aquanaut.common.block.entity.DissectionTableBlockEntity;
import com.dexer.aquanaut.core.BlockRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

import java.util.Optional;

/**
 * Draws the dissection bench.
 *
 * <p>
 * Every cell of a bench has a block entity, but only the group's origin draws anything: it selects the
 * 1x1, 2x1 or 2x2 model and applies the pose from {@link DissectionTablePlacement}, which stands the
 * bench on the block floor and centres it over the whole footprint — including the quarter turn a
 * bench needs when it runs north-south. That arithmetic lives in the common package so it can be unit
 * tested against the shipped geometry without a client runtime.
 */
public class DissectionTableBlockEntityRenderer extends GeoBlockRenderer<DissectionTableBlockEntity> {

    public DissectionTableBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(new DissectionTableGeoModel());
    }

    @Override
    public void render(DissectionTableBlockEntity table, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = table.getLevel();
        if (level == null) {
            return;
        }

        Optional<DissectionTableMultiblock.Group> resolved = DissectionTableMultiblock.resolve(
                pos -> level.getBlockState(pos).is(BlockRegistry.DISSECTION_TABLE.get()),
                table.getBlockPos());

        if (resolved.isEmpty() || !resolved.get().isMaster(table.getBlockPos())) {
            return;
        }

        DissectionTableMultiblock.Group group = resolved.get();
        if (this.getGeoModel() instanceof DissectionTableGeoModel model) {
            model.useGroup(group);
        }

        DissectionTablePlacement.Pose pose = DissectionTablePlacement.forGroup(group);
        poseStack.pushPose();
        poseStack.translate(pose.x(), pose.y(), pose.z());
        if (pose.yawDegrees() != 0.0F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(pose.yawDegrees()));
        }
        super.render(table, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
