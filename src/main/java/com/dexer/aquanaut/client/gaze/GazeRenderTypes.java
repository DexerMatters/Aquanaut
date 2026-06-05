package com.dexer.aquanaut.client.gaze;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

public final class GazeRenderTypes {

    private static RenderType GAZE_GLINT;
    private static RenderType SHADOW_GAZE_GLINT;

    private GazeRenderTypes() {
    }

    public static RenderType getGazeGlint() {
        if (GAZE_GLINT == null) {
            GAZE_GLINT = createGazeGlint();
        }
        return GAZE_GLINT;
    }

    public static RenderType getShadowGazeGlint() {
        if (SHADOW_GAZE_GLINT == null) {
            SHADOW_GAZE_GLINT = createShadowGazeGlint();
        }
        return SHADOW_GAZE_GLINT;
    }

    private static RenderType createGazeGlint() {
        return RenderType.create(
                "aquanaut:gaze_glint",
                DefaultVertexFormat.POSITION_TEX,
                VertexFormat.Mode.QUADS,
                256,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.RENDERTYPE_GLINT_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(
                                GazeTextureLoader.GAZE_GLINT_LOCATION, true, false))
                        .setTexturingState(RenderStateShard.GLINT_TEXTURING)
                        .setTransparencyState(RenderStateShard.GLINT_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .createCompositeState(false));
    }

    private static RenderType createShadowGazeGlint() {
        return RenderType.create(
                "aquanaut:shadow_gaze_glint",
                DefaultVertexFormat.POSITION_TEX,
                VertexFormat.Mode.QUADS,
                256,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.RENDERTYPE_GLINT_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(
                                GazeTextureLoader.GAZE_GLINT_LOCATION, true, false))
                        .setTexturingState(RenderStateShard.GLINT_TEXTURING)
                        .setTransparencyState(RenderStateShard.GLINT_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .createCompositeState(false));
    }
}
