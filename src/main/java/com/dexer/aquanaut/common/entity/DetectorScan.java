package com.dexer.aquanaut.common.entity;

/**
 * The biological detector's array numbers, in one place.
 *
 * <p>
 * {@link DetectorGeometry} holds what the shipped model decides; this holds what the instrument
 * decides: how far out into the water it listens, how large a picture of that volume it draws, how
 * fast its hull turns once it is running, and where a return sits relative to the sweeping beam. The
 * two are kept apart on purpose — a re-export can move the ball without touching the range, and a
 * balance pass on the range should not look like somebody changed the model.
 *
 * <p>
 * Deliberately free of Minecraft types, so the constants and the sweep arithmetic can be checked by
 * a plain unit test with nothing but numbers.
 */
public final class DetectorScan {

    /**
     * How far out into the water the array listens, in blocks.
     *
     * <p>
     * Wide enough to be a picture of the neighbourhood rather than of the room, and comfortably
     * inside the distance the client is told about the mobs around it, so a return is something the
     * client can actually see rather than something the server had to describe.
     */
    public static final float RANGE = 48.0F;

    /**
     * The radius of the sphere the returns are plotted in, in blocks.
     *
     * <p>
     * A little over three blocks: big enough that a diver can swim inside the picture and read it
     * from within, small enough that a deployed buoy is still a piece of equipment rather than a
     * landmark.
     */
    public static final float SPHERE_RADIUS = 3.5F;

    /**
     * How far above the buoy's own origin the hologram is centred, in blocks.
     *
     * <p>
     * The middle of the folded hull, so the sphere sits on the instrument rather than hovering over
     * it or sinking through the seabed.
     */
    public static final float SPHERE_CENTRE_Y = 0.25F;

    /**
     * How far the hull turns each tick once the array is live, in degrees.
     *
     * <p>
     * Ninety degrees a second: a full sweep every four seconds, which is quick enough to read as a
     * beam going round and slow enough that a player can follow a bearing and see what is on it.
     * The renderer plots the sweep on the hull's own yaw, so this one number is both the spin the
     * player watches and the azimuth of the scan.
     */
    public static final float SPIN_DEGREES_PER_TICK = 4.5F;

    /**
     * How long the hologram takes to swell into place once the array wakes, in ticks.
     *
     * <p>
     * The deployment clip is still finishing as the array comes up, so the sphere is allowed to
     * grow and brighten over the last moments of it rather than snapping into existence.
     */
    public static final float BOOT_TICKS = 16.0F;

    /** How much of the sphere's full size is present the instant it switches on. */
    public static final float BOOT_START_SCALE = 0.55F;

    /** The tick the deployment clip hands over to the working loop, which is when the array wakes. */
    public static final int DEPLOY_TICKS = Math.round(DetectorGeometry.RELEASE_SECONDS * 20.0F);

    /** Blocks of water per block of hologram: the whole scan, shrunk into the sphere. */
    public static float scale() {
        return SPHERE_RADIUS / RANGE;
    }

    /**
     * How far the sweep has travelled past a bearing, in degrees, in {@code [0, 360)}.
     *
     * <p>
     * Zero means the beam is on that bearing right now; the value grows as the beam moves on and
     * wraps when it comes back around. That single number is what a plotted return is drawn from:
     * its glow is brightest at zero, and the ring around it is a function of how long ago the beam
     * went past — so the picture reads as something being scanned rather than something merely
     * placed.
     */
    public static float sweepLag(float sweepDegrees, float bearingDegrees) {
        float lag = (sweepDegrees - bearingDegrees) % 360.0F;
        return lag < 0.0F ? lag + 360.0F : lag;
    }

    /** How bright a return is between sweeps, before the pass over it is taken into account. */
    public static final float CONTACT_BASE_GLOW = 0.55F;

    /** How far behind the beam a return's ping ring is still expanding, in degrees. */
    public static final float CONTACT_PING_SPAN = 80.0F;

    private DetectorScan() {
    }
}
