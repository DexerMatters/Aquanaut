package com.dexer.aquanaut.common.entity;

/**
 * The biological detector's model-derived numbers, in one place.
 *
 * <p>
 * A detector is authored as a half-block ball, and everything the code needs to know about it comes
 * from that: the hitbox matches the folded hull, the deployment lifts the shell by a fixed number of
 * model units, and the animation clips the entity plays are the ones the exported animation file
 * actually contains. Keeping those as named constants — and checking them against the shipped assets
 * in {@code BiologicalDetectorAssetTest} — is what stops a re-export from quietly changing the size
 * or the clip names out from under the entity.
 *
 * <p>
 * Deliberately free of Minecraft and GeckoLib types, so the numbers can be checked by a plain unit
 * test with nothing but the JSON files.
 */
public final class DetectorGeometry {

    /** Model units to a block, the same sixteen every other model in the pack is authored at. */
    public static final float UNITS_PER_BLOCK = 16.0F;

    /** The shipped ball's diameter across its belt, in model units: exactly half a block. */
    public static final float BALL_UNITS = 8.0F;

    /** The shipped ball's folded height, in blocks: a half-block buoy, hull closed. */
    public static final float FOLDED_HEIGHT = BALL_UNITS / UNITS_PER_BLOCK;

    /**
     * How far the upper shell lifts when the buoy deploys, in model units. Its base pose is the
     * closed ball, so this is the working loop's hover height as well as the release clip's target.
     */
    public static final float OPEN_LIFT_UNITS = 1.75F;

    /**
     * The same lift in blocks, for reasoning about the opened hull's height. The animation channels
     * themselves carry model units, exactly as the exported geometry does.
     */
    public static final float OPEN_LIFT_BLOCKS = OPEN_LIFT_UNITS / UNITS_PER_BLOCK;

    /** The clip the buoy plays once as it deploys: the shell lifts off the array belt. */
    public static final String RELEASE_ANIMATION = "release";

    /** The clip it loops for the rest of its life: a hovering scan. */
    public static final String WORKING_ANIMATION = "working";

    /** The release clip's length in seconds, which is how long the deployment takes. */
    public static final float RELEASE_SECONDS = 1.8F;

    private DetectorGeometry() {
    }
}
