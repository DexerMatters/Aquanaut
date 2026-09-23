package com.dexer.aquanaut.common.drone;

/**
 * The command a pilot sends to a submarine drone, packed into one byte.
 *
 * <p>
 * Both sides share these bits: the client turns the keyboard into a mask, ships it up, and the drone
 * reads it back. Keeping the vocabulary in one place is what stops the two ends from drifting apart
 * when a control is added.
 *
 * <p>
 * The layout is deliberately a plain bit field rather than an enum set: it is what goes on the wire,
 * and a value of zero has to mean "surfaces centred, jet stopped".
 *
 * <p>
 * The command is a <em>held</em> state, not an event. It is re-sent every tick while the pilot is at
 * the controls and applied every tick on both sides, so holding a key pitches, turns or drives
 * continuously rather than nudging once per press.
 *
 * <h3>Why there are no vertical thrusters</h3>
 * Depth is flown, not lifted. The dive planes point the hull up or down, the jet pushes along it, and
 * the drone goes where it is pointed — which is what makes a submarine a submarine rather than an
 * elevator.
 */
public final class DroneControlInput {

    /** Dive planes, nose up. */
    public static final int PITCH_UP = 1;
    /** Dive planes, nose down. */
    public static final int PITCH_DOWN = 1 << 1;
    /** Rudder, to port. */
    public static final int YAW_LEFT = 1 << 2;
    /** Rudder, to starboard. */
    public static final int YAW_RIGHT = 1 << 3;
    /** Main water jet, ahead. */
    public static final int THRUST_FORWARD = 1 << 4;
    /** Main water jet, astern. */
    public static final int THRUST_BACKWARD = 1 << 5;

    /** Every bit this version understands; anything outside it is dropped on arrival. */
    public static final int ALL = PITCH_UP | PITCH_DOWN | YAW_LEFT | YAW_RIGHT
            | THRUST_FORWARD | THRUST_BACKWARD;

    /** The bits that put water through the jet, and therefore make noise. */
    public static final int POWERED = THRUST_FORWARD | THRUST_BACKWARD;

    private DroneControlInput() {
    }

    /** Whether {@code bit} is set in {@code input}. */
    public static boolean has(int input, int bit) {
        return (input & bit) != 0;
    }

    /** Whether the jet is running. */
    public static boolean powered(int input) {
        return (input & POWERED) != 0;
    }

    /** Strips anything this version does not recognise, so a stale client cannot smuggle bits in. */
    public static int sanitize(int input) {
        return input & ALL;
    }
}
