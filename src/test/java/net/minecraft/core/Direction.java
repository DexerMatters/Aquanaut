package net.minecraft.core;

/**
 * Test-side shim for {@code net.minecraft.core.Direction}.
 *
 * <p>
 * The horizontal six were added first, for board placement maths; {@code UP} and {@code DOWN} were
 * added for the sonar's cavity probes, which walk into a face and therefore have to be able to walk
 * downwards through the sea floor. They are appended rather than inserted in vanilla's order so the
 * ordinals the older tests were written against do not move.
 */
public enum Direction {
    NORTH(0, 0, -1, 180.0F),
    EAST(1, 0, 0, 90.0F),
    SOUTH(0, 0, 1, 0.0F),
    WEST(-1, 0, 0, 270.0F),
    UP(0, 1, 0, 0.0F),
    DOWN(0, -1, 0, 0.0F);

    private final int stepX;
    private final int stepY;
    private final int stepZ;
    private final float yRot;

    Direction(int stepX, int stepY, int stepZ, float yRot) {
        this.stepX = stepX;
        this.stepY = stepY;
        this.stepZ = stepZ;
        this.yRot = yRot;
    }

    public Direction getOpposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case EAST -> WEST;
            case SOUTH -> NORTH;
            case WEST -> EAST;
            case UP -> DOWN;
            case DOWN -> UP;
        };
    }

    public Direction getCounterClockWise() {
        return switch (this) {
            case NORTH -> WEST;
            case EAST -> NORTH;
            case SOUTH -> EAST;
            case WEST -> SOUTH;
            case UP, DOWN -> this;
        };
    }

    public float toYRot() {
        return this.yRot;
    }

    public int getStepX() {
        return this.stepX;
    }

    public int getStepY() {
        return this.stepY;
    }

    public int getStepZ() {
        return this.stepZ;
    }
}
