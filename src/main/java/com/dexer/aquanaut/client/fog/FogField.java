package com.dexer.aquanaut.client.fog;

import com.dexer.aquanaut.common.fog.FogMath;
import com.dexer.aquanaut.common.fog.FogProfiles;
import com.dexer.aquanaut.common.fog.FogTable;
import com.dexer.aquanaut.common.fog.FogVisibility;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The fog sampled as a <em>field</em> rather than as a property of one biome.
 *
 * <h3>Why a kernel</h3>
 * Aquanaut's oceans are not separated by walls: the layer stack mixes them, and the biome
 * under the camera crosses over from one to another across tens of blocks. Asking only for the
 * biome at the camera's own feet therefore produced a step — the moment the dominant biome
 * flipped, the fog colour and the visible distance flipped with it. Sampling a small
 * three-dimensional cross around the camera and weighting each sample by distance turns that
 * same boundary into a gradient, and does it in the vertical direction too, so descending
 * between the ocean layers blends the way swimming sideways between them does.
 *
 * <p>The samples are taken through {@code level.getBiome}, which reads the chunk's own biome
 * (post-rewrite) rather than the raw noise source — the same lookup vanilla's fog renderer
 * uses, so a camera well inside one ocean still resolves to exactly the colour vanilla would
 * have chosen, and this mod only ever changes what happens <em>between</em> oceans.</p>
 */
public final class FogField {
    /**
     * Offsets of the kernel: the camera's own column plus six neighbours, so the field is
     * continuous in all three axes without needing a full lattice.
     */
    private static final int[][] OFFSETS = {
            { 0, 0, 0 },
            { 10, 0, 0 }, { -10, 0, 0 },
            { 0, 0, 10 }, { 0, 0, -10 },
            { 0, 10, 0 }, { 0, -10, 0 },
    };
    /** Distance at which a neighbour still carries about half the centre's influence. */
    private static final double FALLOFF = 10.0D;

    /** What the neighbourhood looks like: one blended profile and one blended colour. */
    public record Blend(FogVisibility visibility, int rgb) {
    }

    private FogField() {
    }

    public static Blend sample(ClientLevel level, Vec3 position) {
        FogTable table = FogProfiles.get();
        List<FogTable.WeightedBiome> weighted = new ArrayList<>(OFFSETS.length);
        int[] colours = new int[OFFSETS.length];
        double[] weights = new double[OFFSETS.length];
        int fallbackColour = 0x808080;

        int index = 0;
        for (int[] offset : OFFSETS) {
            BlockPos pos = BlockPos.containing(position.x + offset[0], position.y + offset[1],
                    position.z + offset[2]);
            var holder = level.getBiome(pos);
            Biome biome = holder.value();
            ResourceLocation id = holder.unwrapKey().map(key -> key.location()).orElse(null);
            double distanceSq = offset[0] * offset[0] + offset[1] * offset[1] + offset[2] * offset[2];
            double weight = 1.0D / (1.0D + distanceSq / (FALLOFF * FALLOFF));

            if (id != null) {
                weighted.add(new FogTable.WeightedBiome(id, weight));
            }
            colours[index] = biome.getWaterFogColor();
            weights[index] = weight;
            if (index == 0) {
                fallbackColour = colours[0];
            }
            index++;
        }

        FogVisibility visibility = weighted.isEmpty() ? table.fallback()
                : table.blendedVisibility(weighted);
        int rgb = FogMath.blendRgb(colours, weights, fallbackColour);
        return new Blend(visibility, rgb);
    }
}