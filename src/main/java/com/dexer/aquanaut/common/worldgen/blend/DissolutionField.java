package com.dexer.aquanaut.common.worldgen.blend;

/**
 * Strategy: how solid rock dissolves into open water as a district's "void appetite"
 * grows. This is the single mechanism behind every hole in the terrain — the crystal
 * nest's open reef, the brine gorge's fissures and the reef cap's shafts — and its
 * defining invariant is that openness is continuous in <em>all three</em> of its inputs:
 * void amount, noise and depth. Voids are therefore lens- and funnel-shaped: they pinch
 * shut along their edges and along district borders instead of ending on vertical walls
 * or being cut off by an argmax contour.
 */
public interface DissolutionField {
    /**
     * @param voidAmount how much this column's district mix wants open water, [0, 1]
     *                   (0 = fully solid, 1 = fully dissolved core)
     * @param noise      modulating field in [0, 1] — ridged/warped fbm so the openings
     *                   meander, branch and vary along their length
     * @param depthBias  additive bias in field units, e.g. positive where fissures should
     *                   widen downward
     * @return openness in [0, 1]; callers treat ≥ 0.5 as "open water"
     */
    double openness(double voidAmount, double noise, double depthBias);

    /** Never opens, regardless of inputs. */
    DissolutionField SEALED = (voidAmount, noise, depthBias) -> 0.0D;

    /**
     * Karst dissolution: openness = smoothstep((voidAmount·gain + noise·modulation +
     * depthBias − cut) / width). Defaults are calibrated so
     * <ul>
     *   <li>voidAmount = 1 opens everything (argument ≥ 2.4 ⇒ openness = 1),</li>
     *   <li>voidAmount = 0 stays sealed for every noise value (argument ≤ −1.2),</li>
     *   <li>intermediate amounts produce speckled, irregular dissolution windows whose
     *       size grows monotonically with the district weight.</li>
     * </ul>
     */
    record Karst(double gain, double modulation, double cut, double width) implements DissolutionField {
        public static final Karst DEFAULT = new Karst(1.25D, 0.45D, 0.72D, 0.22D);

        public Karst {
            if (width <= 0.0D) {
                throw new IllegalArgumentException("dissolution width must be positive");
            }
        }

        @Override
        public double openness(double voidAmount, double noise, double depthBias) {
            double arg = (BlendMath.clamp01(voidAmount) * gain
                    + BlendMath.clamp01(noise) * modulation
                    + depthBias
                    - cut) / width;
            return BlendMath.smoothstep(arg);
        }
    }
}
