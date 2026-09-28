package net.minecraft.core;

/** Small test-side shim for the horizontal directions used by board placement math. */
public enum Direction {
    NORTH(0, -1, 180.0F),
    EAST(1, 0, 90.0F),
    SOUTH(0, 1, 0.0F),
    WEST(-1, 0, 270.0F);

    private final int stepX;
    private final int stepZ;
    private final float yRot;

    Direction(int stepX, int stepZ, float yRot) {
        this.stepX = stepX;
        this.stepZ = stepZ;
        this.yRot = yRot;
    }

    public Direction getOpposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case EAST -> WEST;
            case SOUTH -> NORTH;
            case WEST -> EAST;
        };
    }

    public Direction getCounterClockWise() {
        return switch (this) {
            case NORTH -> WEST;
            case EAST -> NORTH;
            case SOUTH -> EAST;
            case WEST -> SOUTH;
        };
    }

    public float toYRot() {
        return this.yRot;
    }

    public int getStepX() {
        return this.stepX;
    }

    public int getStepZ() {
        return this.stepZ;
    }
}
