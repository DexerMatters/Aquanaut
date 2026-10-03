package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.common.entity.SedimentWormEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SilverfishRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Silverfish;

public final class SedimentWormRenderer extends SilverfishRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "aquanaut", "textures/entity/mud_silverfish.png");

    public SedimentWormRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override public ResourceLocation getTextureLocation(Silverfish entity) { return TEXTURE; }
}
