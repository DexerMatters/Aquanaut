package com.dexer.aquanaut.common.worldgen.blend;

import com.dexer.aquanaut.common.worldgen.layers.BiomeMix;

import java.util.HashMap;
import java.util.Map;

/**
 * Block-resolution district weight field: the foundation every terrain blend strategy
 * consumes. The biome palette is inherently quart-resolution, and flooring
 * {@code block >> 2} per column drags that 4-block lattice into the terrain as visible
 * stair-steps in every strength-driven feature. This field instead treats the quart-cell
 * weights as samples on a lattice whose nodes sit at cell centres, and interpolates
 * between them with a C2 quintic fade — a continuous, chunk-seamless function of world
 * position that passes exactly through the quart values at cell centres.
 *
 * <p>The field is pure: for a memoizing instance the cache only shortcuts re-evaluation
 * of {@link BiomeMix#weightsAt(int, int)}, it never changes a value, so a column sampled
 * from one chunk's field equals the same column sampled from its neighbour's.</p>
 */
public final class DistrictWeightField {
    /** Block-centre coordinate of the lattice node of quart cell {@code i}. */
    private static final double NODE_OFFSET = 2.0D;
    private static final double CELL_SPAN = 4.0D;

    private final BiomeMix mix;
    private final Map<Long, double[]> memo;
    private final double[] scratch = new double[8];

    public DistrictWeightField(BiomeMix mix) {
        this(mix, true);
    }

    public DistrictWeightField(BiomeMix mix, boolean memoized) {
        this.mix = mix;
        this.memo = memoized ? new HashMap<>() : null;
    }

    public BiomeMix mix() {
        return mix;
    }

    public int entryCount() {
        return mix.entries().size();
    }

    /** Weights of one quart cell (memoized when this field caches). */
    public double[] weightsAtQuart(int quartX, int quartZ) {
        if (memo == null) {
            return mix.weightsAt(quartX, quartZ);
        }
        long key = (((long) quartX) << 32) ^ (quartZ & 0xFFFFFFFFL);
        double[] cached = memo.get(key);
        if (cached == null) {
            cached = mix.weightsAt(quartX, quartZ);
            memo.put(key, cached);
        }
        return cached;
    }

    /**
     * Quintic bilinear interpolation of the quart lattice at block coordinates.
     * Writes {@code entryCount()} weights into {@code out} and returns it.
     */
    public double[] weightsAtBlock(int blockX, int blockZ, double[] out) {
        double px = blockX + 0.5D;
        double pz = blockZ + 0.5D;
        double ux = (px - NODE_OFFSET) / CELL_SPAN;
        double uz = (pz - NODE_OFFSET) / CELL_SPAN;
        int i0 = (int) Math.floor(ux);
        int j0 = (int) Math.floor(uz);
        double fx = BlendMath.quintic(ux - i0);
        double fz = BlendMath.quintic(uz - j0);

        int n = mix.entries().size();
        double[] w00 = weightsAtQuart(i0, j0);
        double[] w10 = weightsAtQuart(i0 + 1, j0);
        double[] w01 = weightsAtQuart(i0, j0 + 1);
        double[] w11 = weightsAtQuart(i0 + 1, j0 + 1);
        for (int i = 0; i < n; i++) {
            double top = BlendMath.lerp(fx, w00[i], w10[i]);
            double bottom = BlendMath.lerp(fx, w01[i], w11[i]);
            out[i] = BlendMath.lerp(fz, top, bottom);
        }
        return out;
    }

    /** Single-entry convenience read. */
    public double weightAtBlock(int entryIndex, int blockX, int blockZ) {
        double[] out = scratch.length >= mix.entries().size()
                ? scratch
                : new double[mix.entries().size()];
        weightsAtBlock(blockX, blockZ, out);
        return out[entryIndex];
    }

    /** Index of {@code biome} among the mix entries, or -1. */
    public int indexOf(net.minecraft.resources.ResourceLocation biome) {
        for (int i = 0; i < mix.entries().size(); i++) {
            if (mix.entries().get(i).biome().equals(biome)) {
                return i;
            }
        }
        return -1;
    }

    public void clear() {
        if (memo != null) {
            memo.clear();
        }
    }
}
