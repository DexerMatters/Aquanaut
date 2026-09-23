package com.dexer.aquanaut.common.entity;

/**
 * Pure geometry for the submarine drone.
 *
 * <p>
 * The pilot's camera rides the drone's nose sensor, and where that sensor sits is <em>derived</em>
 * from the shipped model instead of being typed in by hand. The hull's front-most cube in
 * {@code geo/submarine_drone.geo.json} is the lens-and-sensor pod that
 * {@code SubmarineDroneRenderer} lights through the glowmask, and {@link #EYE_HEIGHT} is the middle
 * of it: the height the feed has to look from to be the pod's own eyes rather than something riding
 * on the roof.
 *
 * <p>
 * That distinction is not academic. The drone first carried a hand-picked eye height that sat level
 * with the top of its own shell — the same drift, and the same cure, as the cursor's hitbox, which
 * is why {@code DroneGeometryTest} re-derives this cube from the actual asset whenever the model is
 * re-exported.
 *
 * <p>
 * Deliberately free of Minecraft types so the rule can be unit tested without the game runtime.
 */
public final class DroneGeometry {

    /** Blockbench/Bedrock geometry is authored in sixteenths of a block. */
    public static final double UNITS_PER_BLOCK = 16.0D;

    /**
     * Vertical extent of the nose sensor cube, in model units: the front-most cube of the hull, at
     * {@code z = -5}, two units tall and centred on the drone's midline.
     */
    public static final double NOSE_SENSOR_MIN_Y = 3.0D;
    public static final double NOSE_SENSOR_MAX_Y = 5.0D;

    /**
     * EntityType eye height, in blocks: the middle of the nose sensor, so the operator's feed looks
     * out of the drone's own eye rather than over the top of it.
     */
    public static final float EYE_HEIGHT = (float)
            (((NOSE_SENSOR_MIN_Y + NOSE_SENSOR_MAX_Y) / 2.0D) / UNITS_PER_BLOCK);

    private DroneGeometry() {
    }
}
