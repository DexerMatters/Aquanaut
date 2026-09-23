package com.dexer.aquanaut.client.fog;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Player-facing controls for the fog authority's shader-facing half.
 *
 * <p>Client-side and per-install, because how a shader pack draws fog is a property of the
 * player's pack, not of the world they are standing in.</p>
 */
public final class FogClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /**
     * Whether the fallback veil is drawn while a shader pack is driving the frame. A pack
     * that ignores vanilla fog would otherwise show no biome cast and no abyss at all.
     */
    public static final ModConfigSpec.BooleanValue SHADER_FOG_FALLBACK = BUILDER
            .comment("Draw this mod's own fog veil when a shader pack is in use, so biome fog and",
                    "abyss darkness stay visible with packs that compute their own atmosphere.")
            .define("shaderFogFallback", true);

    /** How heavy that veil is allowed to be; 0 disables it as surely as the switch above. */
    public static final ModConfigSpec.DoubleValue SHADER_FOG_CAST_STRENGTH = BUILDER
            .comment("Strength of the shader-mode fog veil, 0.0 to 1.0. Lower it if your shader",
                    "pack already reads vanilla fog and the two darken the view together.")
            .defineInRange("shaderFogCastStrength", 0.75D, 0.0D, 1.0D);

    /**
     * Whether the abyss also darkens the vanilla lightmap. Shader packs replace the lightmap
     * wholesale, so this applies only when no pack is driving the frame.
     */
    public static final ModConfigSpec.DoubleValue ABYSS_LIGHTMAP_DARKENING = BUILDER
            .comment("How far the abyss dims the light itself (not just the fog) without shaders,",
                    "0.0 to 1.0. Shader packs own the lightmap, so this has no effect with one loaded.")
            .defineInRange("abyssLightmapDarkening", 0.6D, 0.0D, 1.0D);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private FogClientConfig() {
    }

    /** Whether the veil should be drawn for this frame, given the configured strength. */
    public static boolean fallbackEnabled() {
        return SHADER_FOG_FALLBACK.get() && castStrength() > 0.0F;
    }

    /** The configured veil strength, clamped for safety even though the spec already bounds it. */
    public static float castStrength() {
        return (float) Math.max(0.0D, Math.min(1.0D, SHADER_FOG_CAST_STRENGTH.get()));
    }

    /** How far the lightmap is dimmed at the very bottom, 1 = untouched. */
    public static float lightmapFloor() {
        double darkening = Math.max(0.0D, Math.min(1.0D, ABYSS_LIGHTMAP_DARKENING.get()));
        return (float) (1.0D - darkening);
    }

    /** Whether the vanilla lightmap should be dimmed for this frame at all. */
    public static boolean lightmapDarkeningEnabled() {
        return ABYSS_LIGHTMAP_DARKENING.get() > 0.0D;
    }

    /** Human-readable reason the veil is off, for the F3 readout. */
    public static String describe() {
        if (!SHADER_FOG_FALLBACK.get()) {
            return "off (disabled in config)";
        }
        if (castStrength() <= 0.0F) {
            return "off (strength 0)";
        }
        return "on at " + Math.round(castStrength() * 100.0F) + "%";
    }
}