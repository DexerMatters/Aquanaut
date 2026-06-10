package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.RadioanemoneModel;
import com.dexer.aquanaut.common.entity.RadioanemoneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class RadioanemoneRenderer extends GeoEntityRenderer<RadioanemoneEntity> {
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/radioanemone_glowmask.png");
    private static final double VISUAL_Y_OFFSET = 0.0D;

    public RadioanemoneRenderer(EntityRendererProvider.Context ctx) { super(ctx, new RadioanemoneModel()); }

    @Override
    protected void applyRotations(RadioanemoneEntity a, PoseStack ps, float age, float yaw, float pt, float scale) {
        super.applyRotations(a, ps, age, yaw, pt, scale);
        ps.translate(0, VISUAL_Y_OFFSET / scale, 0);
    }

    @Override public @Nullable RenderType getRenderType(RadioanemoneEntity a, ResourceLocation t, @Nullable MultiBufferSource b, float p) {
        return RenderType.entityTranslucent(getTextureLocation(a));
    }

    @Override
    public void actuallyRender(PoseStack ps, RadioanemoneEntity a, BakedGeoModel m, @Nullable RenderType rt,
            MultiBufferSource buf, @Nullable VertexConsumer vc, boolean re, float pt, int pl, int po, int c) {
        if (re) { super.actuallyRender(ps, a, m, rt, buf, vc, true, pt, pl, po, c); return; }
        VertexConsumer body = rt == null ? vc : buf.getBuffer(rt);
        super.actuallyRender(ps, a, m, rt, buf, body, true, pt, pl, po, c);
        RenderType grt = RenderType.eyes(GLOW);
        super.actuallyRender(ps, a, m, grt, buf, buf.getBuffer(grt), false, pt, 15728640, po, c);
    }
}
