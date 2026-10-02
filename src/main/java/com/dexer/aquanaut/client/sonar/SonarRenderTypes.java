package com.dexer.aquanaut.client.sonar;

import com.dexer.aquanaut.Aquanaut;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * The render type the pulse and its echoes are drawn with.
 *
 * <p>
 * One type is enough, because the whole effect is vertex-coloured geometry with no texture and no
 * light of its own: the shell is a fresnel-weighted fill, the rings and the comet tails are
 * camera-facing ribbons, and a contact is a little camera-facing disc. Everything the eye needs is
 * already in the colour and the alpha.
 *
 * <p>
 * The three choices here are the same three the biological detector's projection makes, for the
 * same reasons, and they are worth repeating because each one is load-bearing:
 *
 * <ul>
 * <li><b>Borrowed unlit program, not {@code position_color}.</b> A {@code POSITION_COLOR} vertex
 * carries no lightmap and no normal, so a shader pipeline — which re-lights every fragment out of
 * those two attributes — is handed a lightmap of zero and the pulse comes out as a smear of
 * nothing. Vanilla's {@code rendertype_eyes} program is unlit and vertex-coloured, and its vertex
 * format carries both, so the ordinary game sees flat self-illuminated geometry and a pack sees
 * something it can light and tone-map. The one-pixel white texture it samples is the price of
 * borrowing a textured program, and it is cheaper than disappearing.</li>
 * <li><b>Additive.</b> A pulse is light laid over water: it should add to what is behind it rather
 * than hide it. Addition also cannot be sorted wrong, which matters when a sphere of overlapping
 * quads is drawn in one batch.</li>
 * <li><b>No depth write, depth test on.</b> The wave travels through the water but not through the
 * rock, so it is occluded by terrain and never by itself.</li>
 * </ul>
 *
 * <p>
 * The blend is {@code srcAlpha, one} with the colour written <em>amplified</em> and the alpha as
 * the reciprocal of the amplification — see {@link SonarWaveRenderer}, which pairs them. The
 * ordinary game multiplies one by the other and sees exactly the designed intensity; a pipeline
 * forces its buffer opaque, which drops the undoing half and leaves the amplified colour, so the
 * faint end of a fresnel skirt arrives there lifted instead of crushed.
 */
public final class SonarRenderTypes {

    /** The flat white this borrowed program samples. One opaque texel, so colour is all there is. */
    private static final ResourceLocation FLAT_WHITE = ResourceLocation.fromNamespaceAndPath(
            Aquanaut.MODID, "textures/entity/sonar_white.png");

    private static final RenderStateShard.TransparencyStateShard ALPHA_ADDITIVE =
            new RenderStateShard.TransparencyStateShard(
                    "aquanaut_sonar_alpha_additive",
                    () -> {
                        RenderSystem.enableBlend();
                        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                                GlStateManager.DestFactor.ONE);
                    },
                    () -> {
                        RenderSystem.disableBlend();
                        RenderSystem.defaultBlendFunc();
                    });

    private static RenderType wave;

    private SonarRenderTypes() {
    }

    public static RenderType wave() {
        if (wave == null) {
            wave = RenderType.create(
                    "aquanaut:sonar_wave",
                    DefaultVertexFormat.NEW_ENTITY,
                    VertexFormat.Mode.QUADS,
                    1536,
                    false,
                    false,
                    RenderType.CompositeState.builder()
                            .setShaderState(RenderStateShard.RENDERTYPE_EYES_SHADER)
                            .setTextureState(new RenderStateShard.TextureStateShard(FLAT_WHITE, false, false))
                            .setTransparencyState(ALPHA_ADDITIVE)
                            .setCullState(RenderStateShard.NO_CULL)
                            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                            .createCompositeState(false));
        }
        return wave;
    }
}
