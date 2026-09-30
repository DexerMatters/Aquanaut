package com.dexer.aquanaut.common.worldgen.blend;

/**
 * Strategy: how a district's structural relief (volcanic edifices, satellite vents,
 * sedimentary massifs, lattice spires) emerges from the shared floor as the district
 * weight grows. The defining invariant is {@code apply(0) == 0} and continuity: features
 * grow out of the plain, they never pop into existence at a threshold contour — the
 * classic cliff-ring generator of hard {@code weight > MIN} gates.
 */
public interface EmergenceCurve {
    /** Relief scale factor in [0, 1] for a district weight in [0, 1]. */
    double apply(double weight);

    /** Relief tracks the (already smoothed) weight directly. */
    EmergenceCurve IDENTITY = weight -> BlendMath.clamp01(weight);

    /**
     * Smoothstep window over [w0, w1]. Unlike a hard gate, the relief at w0 is exactly
     * zero and grows with zero initial slope, so the feature is born as a flush part of
     * the surrounding floor.
     */
    record SmoothstepWindow(double w0, double w1) implements EmergenceCurve {
        public SmoothstepWindow {
            if (w1 <= w0) {
                throw new IllegalArgumentException("emergence window requires w1 > w0");
            }
        }

        @Override
        public double apply(double weight) {
            return BlendMath.smoothstep((weight - w0) / (w1 - w0));
        }
    }

    /** Edifices of Brimstone Caldera: mounds at the district fringe, full cones at the core. */
    SmoothstepWindow DEFAULT_EDIFICE = new SmoothstepWindow(0.0D, 0.45D);

    /** Satellite vents and small dressings emerge slightly later than the giants. */
    SmoothstepWindow DEFAULT_SATELLITE = new SmoothstepWindow(0.05D, 0.5D);
}
