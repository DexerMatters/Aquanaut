package com.dexer.aquanaut.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

/**
 * Where the investigation board hangs, and which cells it owns.
 *
 * <p>
 * The board is two blocks wide and one and a half tall, so it covers a 2x2 run of cells on a wall.
 * Only one of them is clicked: the board grows to the viewer's right and upwards from there, which
 * keeps placement predictable the way a bed grows away from its foot. All four cells carry the
 * block, so the board collides properly and cannot be overwritten by another block halfway through
 * its surface.
 *
 * <p>
 * The geometry is authored centred on its own origin, with its rendered depth extending one model
 * unit to either side.
 * GeckoLib first moves that origin to the centre of the anchor block and then rotates the model from
 * the block's {@code FACING} property. The custom pose therefore contains translation only: half a
 * block toward the board's second column, and enough depth to leave one model unit between its back
 * and the support wall.
 *
 * <p>
 * This lives in {@code common} rather than in the renderer so the arithmetic can be unit tested
 * against the shipped geometry without a client runtime: getting it wrong hangs the board half a
 * block off the wall, which is invisible in a screenshot taken square-on and obvious from any
 * other angle.
 */
public final class InvestigationBoardPlacement {

    /** GeckoLib's {@code GeoBlockRenderer} draws block models half a block in from the block corner. */
    public static final double BLOCK_CENTRE = 0.5D;

    /** Half of the board's rendered depth: the model spans z in [-1, 1] model units. */
    public static final double MODEL_HALF_DEPTH = 1.0D / 16.0D;

    /** Air kept between the board's back and the wall so the two faces cannot fight for depth. */
    public static final double WALL_CLEARANCE = 1.0D / 16.0D;

    /**
     * The four cells of the board, named as the viewer sees the board's front.
     *
     * <p>
     * {@link #BOTTOM_LEFT} is the cell the player clicked; the offsets are counted along
     * {@link #viewerRight(Direction)} and upwards from there.
     */
    public enum Part implements StringRepresentable {
        BOTTOM_LEFT("bottom_left", 0, 0),
        BOTTOM_RIGHT("bottom_right", 1, 0),
        TOP_LEFT("top_left", 0, 1),
        TOP_RIGHT("top_right", 1, 1);

        private final String name;
        private final int right;
        private final int up;

        Part(String name, int right, int up) {
            this.name = name;
            this.right = right;
            this.up = up;
        }

        /** Cells to the viewer's right of the clicked cell. */
        public int right() {
            return this.right;
        }

        /** Cells above the clicked cell. */
        public int up() {
            return this.up;
        }

        /** The clicked cell, which draws the model and answers for the whole board. */
        public boolean isAnchor() {
            return this == BOTTOM_LEFT;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    /** Translation applied before GeckoLib centres and rotates the block model. */
    public record Pose(double x, double y, double z) {
    }

    private InvestigationBoardPlacement() {
    }

    /** The wall the board hangs on: the block directly behind the board's back. */
    public static Direction wall(Direction facing) {
        return facing.getOpposite();
    }

    /** The direction the viewer's right hand points when they look at the board's front. */
    public static Direction viewerRight(Direction facing) {
        return facing.getCounterClockWise();
    }

    /** World position of {@code part} when the board hangs at {@code anchor} facing {@code facing}. */
    public static BlockPos cell(Part part, Direction facing, BlockPos anchor) {
        return anchor.relative(viewerRight(facing), part.right()).above(part.up());
    }

    /**
     * The clicked cell for the board that covers {@code pos}, or {@code null} when {@code pos} is
     * not one of its cells. Used to find the rest of a board from any of its parts.
     */
    public static BlockPos anchorFor(Part part, Direction facing, BlockPos pos) {
        return pos.relative(viewerRight(facing), -part.right()).below(part.up());
    }

    /**
     * The pose that hangs the model on the board's two columns, with its bottom on the anchor's
     * floor and its back one model unit off the wall.
     */
    public static Pose forFacing(Direction facing) {
        Direction right = viewerRight(facing);
        Direction back = wall(facing);
        // The model is centred on its own origin, so it has to sit on the boundary between the two
        // cells it spans; the board's back is one model unit behind the origin.
        double depth = BLOCK_CENTRE - MODEL_HALF_DEPTH - WALL_CLEARANCE;
        return new Pose(
                BLOCK_CENTRE * right.getStepX() + depth * back.getStepX(),
                0.0D,
                BLOCK_CENTRE * right.getStepZ() + depth * back.getStepZ());
    }
}
