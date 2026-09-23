package com.dexer.aquanaut.common;

/**
 * The buoyancy an air bladder grants its holder.
 *
 * <p>
 * Keeping the rule here, away from the event plumbing, means the numbers can be
 * reasoned about and tested without a running game.
 *
 * <p>
 * Vanilla sinks a swimmer by applying {@code gravity / 16} to the vertical
 * velocity every tick -- 0.005 blocks/tick for a player -- and then drags the
 * vertical velocity by 0.8, which settles into a steady 0.025 blocks/tick sink.
 * A bladder either cancels that sink or overwhelms it.
 */
public final class FlotationHelper {

    /**
     * What a held bladder does, and the gate that keeps it from doing something
     * silly at the surface.
     */
    public enum FlotationDevice {
        /** Empty hands. */
        NONE(0, 0.0, false),
        /**
         * The handheld air bladder: clamps the sink back to level, so the holder
         * keeps their depth. Gated on the *body* being in water, so a holder
         * floating at the surface -- head in the air, able to breathe -- is held
         * there rather than pushed under.
         */
        HOLD_DEPTH(1, 0.0, false),
        /**
         * The large handheld air bladder: a hard ascent. Gated on the *head*
         * being under water, so it rockets the holder up and then stops, leaving
         * them floating at the surface instead of being fired out of it.
         */
        FAST_ASCENT(2, 0.18, true);

        private final int rank;
        private final double verticalVelocity;
        private final boolean needsSubmergedHead;

        FlotationDevice(int rank, double verticalVelocity, boolean needsSubmergedHead) {
            this.rank = rank;
            this.verticalVelocity = verticalVelocity;
            this.needsSubmergedHead = needsSubmergedHead;
        }

        /**
         * Upward velocity this device holds. {@link #FAST_ASCENT} is roughly
         * seven times vanilla's steady sink and three times a bubble column, so
         * it reads as a hard ascent rather than a nudge.
         */
        public double verticalVelocity() {
            return verticalVelocity;
        }

        /** The stronger of two devices, so the large bladder wins if both are held. */
        public FlotationDevice stronger(FlotationDevice other) {
            return this.rank >= other.rank ? this : other;
        }
    }

    private FlotationHelper() {
    }

    /**
     * Whether the device is acting on its holder this tick.
     *
     * @param device         what is held
     * @param inWater        the holder's body is in water
     * @param headUnderwater the holder's eyes are in water
     * @param descending     the holder is deliberately swimming down
     */
    public static boolean isActive(FlotationDevice device, boolean inWater, boolean headUnderwater,
            boolean descending) {
        if (descending) {
            return false; // a diver keeps control of their depth
        }
        if (device.needsSubmergedHead) {
            return headUnderwater;
        }
        return device != FlotationDevice.NONE && inWater;
    }

    /**
     * The vertical velocity to install.
     *
     * <p>
     * A holder who is already rising faster than the device can lift them is
     * left alone, so swimming up and jumping out of the water still work.
     */
    public static double verticalVelocity(FlotationDevice device, double currentVerticalVelocity) {
        return Math.max(currentVerticalVelocity, device.verticalVelocity);
    }
}
