package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.RadioanemoneModel;
import com.dexer.aquanaut.common.entity.RadioanemoneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class RadioanemoneRenderer extends GeoEntityRenderer<RadioanemoneEntity> {
    private static final double VISUAL_Y_OFFSET = -0.5D;

    public RadioanemoneRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new RadioanemoneModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    protected void applyRotations(RadioanemoneEntity a, PoseStack ps, float age, float yaw, float pt, float scale) {
        super.applyRotations(a, ps, age, yaw, pt, scale);
        ps.translate(0, VISUAL_Y_OFFSET / scale, 0);
    }

    @Override public @Nullable RenderType getRenderType(RadioanemoneEntity a, ResourceLocation t,
            @Nullable MultiBufferSource b, float p) { return RenderType.entityTranslucent(getTextureLocation(a)); }
}
