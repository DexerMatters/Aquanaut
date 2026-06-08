package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.common.block.entity.GasPipeBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import java.util.EnumMap;
import java.util.Map;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

public final class GasPipeBlockEntityRenderer implements BlockEntityRenderer<GasPipeBlockEntity> {
    private static final ResourceLocation BUBBLE_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/particle/bubble.png");

    private static final float BUBBLE_HALF_MIN = 0.14F;
    private static final float BUBBLE_HALF_MAX = 0.24F;
    private static final float BUBBLE_SPACING = 0.34F;
    private static final float ROUTE_WOBBLE = 0.055F;

    public GasPipeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(GasPipeBlockEntity blockEntity, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        int flow = blockEntity.getFlow();
        if (flow == 0 || blockEntity.getLevel() == null) {
            return;
        }

        Map<PipeFlowLayout.Endpoint, Integer> faceFlows = new EnumMap<>(PipeFlowLayout.Endpoint.class);
        for (Map.Entry<net.minecraft.core.Direction, Integer> entry : blockEntity.getFaceFlows().entrySet()) {
            faceFlows.merge(endpoint(entry.getKey()), entry.getValue(), Integer::sum);
        }
        List<PipeFlowLayout.RouteFlow> routes = PipeFlowLayout.routes(faceFlows);
        if (routes.isEmpty()) {
            return;
        }

        BlockPos pos = blockEntity.getBlockPos();
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        PoseStack.Pose pose = poseStack.last();
        Vector3f cameraLeft = new Vector3f(Minecraft.getInstance().gameRenderer.getMainCamera().getLeftVector());
        Vector3f cameraUp = new Vector3f(Minecraft.getInstance().gameRenderer.getMainCamera().getUpVector());
        Vector3f cameraLook = new Vector3f(Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector());

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityTranslucent(BUBBLE_TEXTURE));
        long gameTime = blockEntity.getLevel().getGameTime();

        for (int routeIndex = 0; routeIndex < routes.size(); routeIndex++) {
            PipeFlowLayout.RouteFlow routeFlow = routes.get(routeIndex);
            renderRoute(buffer, pose, cameraLeft, cameraUp, cameraLook, pos, routeFlow, gameTime, partialTick,
                    packedLight, routeIndex);
        }

        poseStack.popPose();
    }

    private void renderRoute(VertexConsumer buffer, PoseStack.Pose pose, Vector3f cameraLeft, Vector3f cameraUp,
            Vector3f cameraLook, BlockPos pos, PipeFlowLayout.RouteFlow routeFlow, long gameTime, float partialTick,
            int packedLight, int routeIndex) {
        PipeFlowLayout.Route route = routeFlow.route();
        int routeAmount = Math.max(1, routeFlow.amount());
        float speed = Mth.clamp(routeAmount * 0.0085F, 0.01F, 0.12F);
        int bubbleCount = Mth.clamp(2 + Mth.floor(routeAmount * 0.65F), 2, 10);
        float baseTime = (gameTime + partialTick) * speed;
        float routeSeed = routeSeed(routeFlow, routeIndex);
        float routeOffset = routePhaseOffset(pos, route);
        for (int i = 0; i < bubbleCount; i++) {
            float phase = Mth.frac(baseTime + routeOffset + routeSeed + i * BUBBLE_SPACING);
            PipeFlowLayout.Point point = route.pointAt(phase);
            PipeFlowLayout.Axis axis = route.axisAt(phase);
            float wobbleA = Mth.sin((phase + routeSeed + i * 0.53F) * 6.2831855F) * ROUTE_WOBBLE;
            float wobbleB = Mth.cos((phase + routeSeed + i * 0.71F) * 6.2831855F) * ROUTE_WOBBLE;
            float sizePhase = Mth.frac(phase + routeSeed * 0.37F);
            float bubbleHalf = Mth.lerp(sizePhase, BUBBLE_HALF_MIN, BUBBLE_HALF_MAX);
            float size = bubbleHalf + Math.min(0.05F, 0.012F + routeAmount * 0.0014F + i * 0.0014F);
            float x = point.x();
            float y = point.y();
            float z = point.z();

            switch (axis) {
                case X -> {
                    y += wobbleA;
                    z += wobbleB;
                }
                case Y -> {
                    x += wobbleA;
                    z += wobbleB;
                }
                case Z -> {
                    x += wobbleA;
                    y += wobbleB;
                }
            }

            drawBillboardBubble(buffer, pose, cameraLeft, cameraUp, cameraLook, x, y, z, size, packedLight);
        }
    }

    private float routePhaseOffset(BlockPos pos, PipeFlowLayout.Route route) {
        PipeFlowLayout.Point anchor = route.anchorPoint();
        PipeFlowLayout.Axis axis = route.anchorAxis();
        float worldCoordinate = switch (axis) {
            case X -> pos.getX() + 0.5F + anchor.x();
            case Y -> pos.getY() + 0.5F + anchor.y();
            case Z -> pos.getZ() + 0.5F + anchor.z();
        };
        return worldCoordinate / route.length();
    }

    private float routeSeed(PipeFlowLayout.RouteFlow routeFlow, int routeIndex) {
        PipeFlowLayout.Route route = routeFlow.route();
        int hash = route.start().ordinal() * 73428767 ^ route.end().ordinal() * 91227169 ^ routeIndex * 19349663;
        hash ^= routeFlow.amount() * 83492791;
        hash ^= hash >>> 16;
        return (hash & 0xFFFF) / 65535.0F;
    }

    private void drawBillboardBubble(VertexConsumer buffer, PoseStack.Pose pose, Vector3f cameraLeft,
            Vector3f cameraUp, Vector3f cameraLook, float x, float y, float z, float halfSize, int packedLight) {
        Matrix4f matrix = pose.pose();
        Vector3f left = new Vector3f(cameraLeft).mul(halfSize);
        Vector3f up = new Vector3f(cameraUp).mul(halfSize);
        Vector3f center = new Vector3f(x, y, z);
        Vector3f look = new Vector3f(cameraLook).normalize().mul(0.004F);

        Vector3f topLeft = new Vector3f(center).sub(left).add(up).add(look);
        Vector3f bottomLeft = new Vector3f(center).sub(left).sub(up).add(look);
        Vector3f bottomRight = new Vector3f(center).add(left).sub(up).add(look);
        Vector3f topRight = new Vector3f(center).add(left).add(up).add(look);

        vertex(buffer, pose, matrix, topLeft.x, topLeft.y, topLeft.z, 0.0F, 0.0F, packedLight, 0, 0, 1);
        vertex(buffer, pose, matrix, bottomLeft.x, bottomLeft.y, bottomLeft.z, 0.0F, 1.0F, packedLight, 0, 0, 1);
        vertex(buffer, pose, matrix, bottomRight.x, bottomRight.y, bottomRight.z, 1.0F, 1.0F, packedLight, 0, 0, 1);
        vertex(buffer, pose, matrix, topRight.x, topRight.y, topRight.z, 1.0F, 0.0F, packedLight, 0, 0, 1);
    }

    private void vertex(VertexConsumer buffer, PoseStack.Pose pose, Matrix4f matrix, float x, float y, float z,
            float u, float v, int packedLight, int nx, int ny, int nz) {
        buffer.addVertex(matrix, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, nx, ny, nz);
    }

    private PipeFlowLayout.Endpoint endpoint(net.minecraft.core.Direction direction) {
        return switch (direction) {
            case NORTH -> PipeFlowLayout.Endpoint.NORTH;
            case SOUTH -> PipeFlowLayout.Endpoint.SOUTH;
            case EAST -> PipeFlowLayout.Endpoint.EAST;
            case WEST -> PipeFlowLayout.Endpoint.WEST;
            case UP -> PipeFlowLayout.Endpoint.UP;
            case DOWN -> PipeFlowLayout.Endpoint.DOWN;
        };
    }
}
