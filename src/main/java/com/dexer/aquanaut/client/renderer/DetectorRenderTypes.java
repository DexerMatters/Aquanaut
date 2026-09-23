package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.Aquanaut;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * The render type the biological detector's hologram is drawn with.
 *
 * <p>
 * One type is enough because the whole projection is vertex-coloured geometry: the glass of the
 * sphere is a fresnel-weighted fill, the grid and the sweep are camera-facing ribbons, and a return
 * is a pair of little camera-facing discs. Everything the eye needs is in the colour and the alpha,
 * so nothing is ever textured or lit by the world's own light.
 *
 * <h3>Why it is not simply {@code position_color}</h3>
 *
 * <p>
 * The obvious choice is a {@code POSITION_COLOR} type — position and colour, nothing else — and that
 * is what this used to be. It is wrong the moment anything else drives the world's rendering. A
 * shader pipeline does not care what a fragment's <em>colours</em> are: it re-lights the fragment
 * afterwards out of two other pieces of per-vertex data, the lightmap and the normal, which it reads
 * from the buffer it is given. A vertex that carries neither is handed a lightmap of zero, and the
 * hologram comes out as a smear of almost nothing. That is the "it goes dim under shaders" bug, and
 * it is not about any one pack: it is what happens whenever geometry is drawn with a vertex format
 * that leaves out the two attributes a renderer needs in order to light it.
 *
 * <h3>The shape of the fix</h3>
 *
 * <p>
 * The hologram is therefore drawn through vanilla's {@code rendertype_eyes} program, with a
 * {@code NEW_ENTITY} vertex — the same shape of vertex a mob is drawn with:
 *
 * <ul>
 * <li>that program is <em>unlit and vertex-coloured</em> — {@code texture * vertexColor} and no
 * lighting term at all — so the ordinary game sees exactly the flat, self-illuminated projection it
 * saw before, with the appended one-pixel white texture contributing nothing;</li>
 * <li>its vertex format carries a lightmap and a normal. Those are what a shader pipeline reads to
 * light the fragment, and every vertex below hands them over: the lightmap pinned at full
 * brightness, because a projection is its own light, and a normal turned to face the eye, because a
 * ribbon hanging in the water has no other side;</li>
 * <li>every pipeline already routes that program to the pass it keeps for glowing eyes — a pass that
 * writes the <em>whole</em> buffer, lighting and material tag included, and that packs treat as an
 * emitter. So the hologram comes through the pack at full strength, in any pack, without this class
 * having to know that packs exist.</li>
 * </ul>
 *
 * <p>
 * A near miss worth recording, because it looks like the better answer until it is tried: vanilla's
 * {@code rendertype_crumbling} is also unlit and also carries a lightmap and a normal, and it is
 * unfogged as well. But a pack renders crumbling as the block-breaking decal it is — IterationT's
 * version writes the colour buffer and nothing else — so the fragment is never lit and never tagged,
 * and the hologram disappears entirely. The leftover white texture is the price of borrowing a
 * textured program; it is cheaper than disappearing.
 *
 * <p>
 * Additive rather than translucent, for three reasons: it cannot be sorted wrong — thousands of
 * overlapping bits of one sphere would show their seams the moment an ordering heuristic guessed
 * badly, and addition does not care about order at all. It writes no depth, so a diver inside the
 * sphere is not swallowed by it and the ball is still occluded by the terrain around it. And it is
 * what light actually does: a projection laid over the water should add to it, not hide it.
 *
 * <p>
 * The additive blend is {@code srcAlpha, one} rather than the engine's {@code one, one}, and the two
 * halves of that are used as a pair of channels. The colour is written <em>amplified</em> and the
 * alpha as the reciprocal of the amplification, so the ordinary game — which multiplies one by the
 * other — sees exactly the designed intensity, to the bit. A pipeline writes its buffer with the
 * alpha forced opaque, which drops the undoing half and leaves the amplified colour: the faint end
 * of a fresnel skirt arrives there lifted instead of crushed, and the whole projection reads at the
 * strength a lit, tone-mapped buffer needs. Both worlds get the same one blend and the same one
 * buffer, and neither is special-cased.
 */
public final class DetectorRenderTypes {

    /**
     * The flat white the borrowed program samples.
     *
     * <p>
     * One opaque texel, so {@code texture * vertexColor} is the vertex colour and nothing else.
     */
    private static final ResourceLocation FLAT_WHITE = ResourceLocation.fromNamespaceAndPath(
            Aquanaut.MODID, "textures/entity/detector_hologram_white.png");

    /**
     * Additive, weighted by alpha: the colour is added to the water at the strength the alpha says.
     *
     * <p>
     * The hologram pairs this with DetectorHologram's amplified colour and reciprocal alpha, which
     * multiply back to the designed intensity here and do not multiply at all in a pipeline that
     * forces its buffer opaque.
     */
    private static final RenderStateShard.TransparencyStateShard ALPHA_ADDITIVE =
            new RenderStateShard.TransparencyStateShard(
                    "aquanaut_detector_alpha_additive",
                    () -> {
                        RenderSystem.enableBlend();
                        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                                GlStateManager.DestFactor.ONE);
                    },
                    () -> {
                        RenderSystem.disableBlend();
                        RenderSystem.defaultBlendFunc();
                    });

    private static RenderType hologram;

    private DetectorRenderTypes() {
    }

    public static RenderType hologram() {
        if (hologram == null) {
            hologram = RenderType.create(
                    "aquanaut:detector_hologram",
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
        return hologram;
    }
}
