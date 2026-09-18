package com.dexer.aquanaut.common.block;

import java.util.Map;

/**
 * Where a merged dissection bench has to be drawn.
 *
 * <p>
 * The three exported models are the same bench at three sizes, each authored centred on its own
 * origin: the 1x1 table spans one block, the 2x1 bench spans two blocks along X, and the 2x2 bench
 * spans two blocks on both axes. GeckoLib's block renderer then draws a block model half a block off
 * the block's north-west corner, and a bench's master block is always that corner — so the pose has
 * to move the model by the difference and, for a bench that runs north-south, turn it a quarter turn
 * first.
 *
 * <p>
 * This lives next to {@link DissectionTableMultiblock} rather than in the renderer so the arithmetic
 * can be unit tested against the shipped geometry without a client runtime: getting it wrong shifts
 * the bench off its footprint, which is invisible in a screenshot of a single table and obvious the
 * moment two are merged.
 */
public final class DissectionTablePlacement {

    /** GeckoLib's {@code GeoBlockRenderer} draws block models half a block in from the block corner. */
    public static final double BLOCK_CENTRE = 0.5D;

    /** Authored extents of each merged model, in blocks, measured from that model's own origin. */
    public record Span(double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
    }

    /** Extents of the three shipped models, keyed by {@link DissectionTableMultiblock.Group#modelSuffix()}. */
    public static final Map<String, Span> MODEL_SPANS = Map.of(
            "", new Span(-0.5D, 0.5D, -10.0D / 16.0D, 6.0D / 16.0D, -0.5D, 0.5D),
            "_x2", new Span(-1.0D, 1.0D, -10.0D / 16.0D, 6.0D / 16.0D, -0.5D, 0.5D),
            "_x4", new Span(-1.0D, 1.0D, -10.0D / 16.0D, 6.0D / 16.0D, -1.0D, 1.0D));

    /**
     * The renderer's pose: translate by {@code (x, y, z)} in block space and then turn
     * {@code yawDegrees} about Y, which is the order {@code PoseStack} composes them in.
     */
    public record Pose(double x, double y, double z, float yawDegrees) {
    }

    private DissectionTablePlacement() {
    }

    /**
     * The pose that stands {@code group}'s model on the block floor and centres it over the group's
     * whole footprint, with the model's own north-west corner on the master block.
     */
    public static Pose forGroup(DissectionTableMultiblock.Group group) {
        Span span = MODEL_SPANS.get(group.modelSuffix());
        if (span == null) {
            throw new IllegalArgumentException("No dissection table model for suffix '" + group.modelSuffix() + "'");
        }
        // The models are authored with their feet at y = -10 units; lift them onto the floor.
        double y = -span.minY();

        if (group.depthZ() <= group.widthX()) {
            // Bench already runs along X (this is also the 1x1 and the 2x2): just slide the model
            // onto the group, so its own minimum X/Z sits on the master cell's north-west corner.
            return new Pose(-(span.minX() + BLOCK_CENTRE), y, -(span.minZ() + BLOCK_CENTRE), 0.0F);
        }

        // A north-south bench is the same model turned a quarter turn clockwise (which is what a
        // player gets by rotating the long bench over the master cell): +X turns to face south, so
        // local X becomes world Z and local Z becomes the negative of world X. The world-X offset
        // therefore comes from the model's Z extent, and the world-Z offset is just the model's own
        // half-width — the turned model already runs the full two cells.
        return new Pose(span.maxZ() + BLOCK_CENTRE, y, -(span.minX() + BLOCK_CENTRE), -90.0F);
    }
}
