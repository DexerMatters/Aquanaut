package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.common.entity.MudSilverfishEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SilverfishRenderer;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.resources.ResourceLocation;

public final class MudSilverfishRenderer extends SilverfishRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "aquanaut", "textures/entity/mud_silverfish.png");

    public MudSilverfishRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Silverfish entity) {
        return TEXTURE;
    }
}
