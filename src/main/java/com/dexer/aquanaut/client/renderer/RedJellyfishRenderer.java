package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.RedJellyfishModel;
import com.dexer.aquanaut.common.entity.RedJellyfishEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class RedJellyfishRenderer extends BaseFishRenderer<RedJellyfishEntity> {
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "textures/entity/red_jellyfish_glowmask.png");

    public RedJellyfishRenderer(EntityRendererProvider.Context ctx) { super(ctx, new RedJellyfishModel()); }

    @Override public @Nullable RenderType getRenderType(RedJellyfishEntity a, ResourceLocation t,
            @Nullable MultiBufferSource b, float p) {
        return RenderType.entityTranslucent(getTextureLocation(a));
    }

    @Override
    public void actuallyRender(PoseStack ps, RedJellyfishEntity a, BakedGeoModel m, @Nullable RenderType rt,
            MultiBufferSource buf, @Nullable VertexConsumer vc, boolean re, float pt, int pl, int po, int c) {
        if (re) { super.actuallyRender(ps, a, m, rt, buf, vc, true, pt, pl, po, c); return; }

        // Render body first
        VertexConsumer body = rt == null ? vc : buf.getBuffer(rt);
        super.actuallyRender(ps, a, m, rt, buf, body, true, pt, pl, po, c);

        // Glow overlay on top with full bright — matching electrofish pattern
        RenderType grt = RenderType.eyes(GLOW);
        super.actuallyRender(ps, a, m, grt, buf, buf.getBuffer(grt), false, pt, 15728640, po, c);
    }
}
