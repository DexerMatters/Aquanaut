package com.dexer.aquanaut.common.entity;

/**
 * Pure geometry for the cursor entity.
 *
 * <p>
 * The hitbox is <em>derived</em> from the shipped model instead of being typed in by hand:
 * {@link #MODEL_BOUNDS} is the union of every cube in {@code geo/cursor.geo.json}, and the entity
 * dimensions are computed from it. {@code CursorHitboxTest} re-derives the same union from the
 * actual asset, so the two cannot silently drift apart when the model is re-exported from
 * Blockbench — which is how the entity ended up with a hitbox that did not match its model in the
 * first place.
 *
 * <p>
 * Deliberately free of Minecraft types so the rule can be unit tested without the game runtime.
 */
public final class CursorGeometry {

    /** Blockbench/Bedrock geometry is authored in sixteenths of a block. */
    public static final double UNITS_PER_BLOCK = 16.0D;

    /** Union of every cube in cursor.geo.json: 7 x 29 x 7 units, standing on the origin. */
    public static final Bounds MODEL_BOUNDS = new Bounds(-3.5D, 0.0D, -3.5D, 3.5D, 29.0D, 3.5D);

    /** EntityType width, in blocks: the model's 7-unit span. */
    public static final float HITBOX_WIDTH = (float) (MODEL_BOUNDS.sizeX() / UNITS_PER_BLOCK);

    /** EntityType height, in blocks: the model's 29-unit span, measured from the origin. */
    public static final float HITBOX_HEIGHT = (float) (MODEL_BOUNDS.sizeY() / UNITS_PER_BLOCK);

    private CursorGeometry() {
    }

    /** An axis-aligned box in model units. */
    public record Bounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {

        public double sizeX() {
            return maxX - minX;
        }

        public double sizeY() {
            return maxY - minY;
        }

        public double sizeZ() {
            return maxZ - minZ;
        }

        public Bounds union(Bounds other) {
            return new Bounds(
                    Math.min(minX, other.minX),
                    Math.min(minY, other.minY),
                    Math.min(minZ, other.minZ),
                    Math.max(maxX, other.maxX),
                    Math.max(maxY, other.maxY),
                    Math.max(maxZ, other.maxZ));
        }

        /** Builds a box from a Bedrock cube's {@code origin} and {@code size}. */
        public static Bounds ofCube(double originX, double originY, double originZ,
                double sizeX, double sizeY, double sizeZ) {
            return new Bounds(originX, originY, originZ,
                    originX + sizeX, originY + sizeY, originZ + sizeZ);
        }
    }

    /** Union of a set of part boxes. */
    public static Bounds union(Iterable<Bounds> parts) {
        Bounds result = null;
        for (Bounds part : parts) {
            result = result == null ? part : result.union(part);
        }
        if (result == null) {
            throw new IllegalArgumentException("no parts to union");
        }
        return result;
    }
}
