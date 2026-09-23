package com.dexer.aquanaut.client.fog;

import com.dexer.aquanaut.common.fog.FogMath;

/**
 * How far the abyss dims the vanilla lightmap.
 *
 * <h3>Why this is not the same as the fog</h3>
 * Fog takes the distance away; this takes the light. Without a shader pack the two are
 * complementary — a player at crushing depth sees a few blocks of murk and the torch in their
 * hand no longer reaches the wall. With a shader pack the lightmap is not the mod's to touch
 * at all (the pack computes lighting in its own g-buffer passes), so this returns to full
 * brightness and the veil drawn over the frame carries the darkness instead.</p>
 *
 * <p>The scale is sampled once per lightmap update and then read by every brightness lookup in
 * that update, because the lightmap is rebuilt 256 times per update and each of those lookups
 * passes through {@code LightTexture#getBrightness}.</p>
 */
public final class AbyssLightmap {
    private static volatile float sampledScale = 1.0F;

    private AbyssLightmap() {
    }

    /** Re-sample the frame's darkening. Called once at the start of a lightmap update. */
    public static void sample() {
        sampledScale = computeScale();
    }

    /** The scale the lightmap's brightness is multiplied by, 1 = untouched. */
    public static float scale() {
        return sampledScale;
    }

    private static float computeScale() {
        try {
            if (!FogClientConfig.lightmapDarkeningEnabled() || IrisCompat.isPackInUse()) {
                return 1.0F;
            }
            ClientFogState.Snapshot snapshot = ClientFogState.get();
            if (snapshot.submersion() <= 0.001F) {
                return 1.0F;
            }
            // Scaled by the submersion crossfade as well as the depth, so surfacing hands the light
            // back over the same fraction of a second that the fog opens up over.
            return FogMath.lightmapScale(snapshot.ramp() * snapshot.submersion(),
                    FogClientConfig.lightmapFloor());
        } catch (RuntimeException e) {
            // Reading a config before it is loaded must never break the lightmap: the abyss
            // simply does not darken until the mod is properly up.
            return 1.0F;
        }
    }
}