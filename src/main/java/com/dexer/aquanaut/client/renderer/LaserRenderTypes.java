package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.Aquanaut;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Additive, full-bright render types for the Opticichthus laser.
 *
 * <p>
 * The stock {@link RenderType#eyes(ResourceLocation)} shard is reused because it multiplies the
 * texture by the per-vertex colour and ignores the lightmap, which is exactly what a laser needs.
 * The texture state is copied from it too — nearest filtering, no mipmaps — because the sprites are
 * 16px pixel art and linear filtering would smear them into a soft modern glow.
 */
public final class LaserRenderTypes {

    public static final ResourceLocation BEAM_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Aquanaut.MODID, "textures/entity/opticichthus_beam.png");
    public static final ResourceLocation FLARE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Aquanaut.MODID, "textures/entity/opticichthus_flare.png");

    private static RenderType beam;
    private static RenderType flare;

    private LaserRenderTypes() {
    }

    public static RenderType beam() {
        if (beam == null) {
            beam = create("opticichthus_beam", BEAM_TEXTURE);
        }
        return beam;
    }

    public static RenderType flare() {
        if (flare == null) {
            flare = create("opticichthus_flare", FLARE_TEXTURE);
        }
        return flare;
    }

    private static RenderType create(String name, ResourceLocation texture) {
        return RenderType.create(
                "aquanaut:" + name,
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.RENDERTYPE_EYES_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .createCompositeState(false));
    }
}
