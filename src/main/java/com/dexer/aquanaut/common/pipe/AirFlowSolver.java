package com.dexer.aquanaut.common.pipe;

public final class AirFlowSolver {
    private AirFlowSolver() {
    }

    public enum Endpoint {
        CENTER(0.0F, 0.0F, 0.0F, null, 0),
        NORTH(0.0F, 0.0F, -1.0F, Axis.Z, -1),
        SOUTH(0.0F, 0.0F, 1.0F, Axis.Z, 1),
        EAST(1.0F, 0.0F, 0.0F, Axis.X, 1),
        WEST(-1.0F, 0.0F, 0.0F, Axis.X, -1),
        UP(0.0F, 1.0F, 0.0F, Axis.Y, 1),
        DOWN(0.0F, -1.0F, 0.0F, Axis.Y, -1);

        private final Point point;
        private final Axis axis;
        private final int axisSign;

        Endpoint(float x, float y, float z, Axis axis, int axisSign) {
            this.point = new Point(x, y, z);
            this.axis = axis;
            this.axisSign = axisSign;
        }

        public Point point() {
            return point;
        }

        public Axis axis() {
            return axis;
        }

        public boolean isPositiveDirection() {
            return axisSign > 0;
        }

        public boolean isNegativeDirection() {
            return axisSign < 0;
        }

        public Endpoint opposite() {
            return switch (this) {
                case CENTER -> CENTER;
                case NORTH -> SOUTH;
                case SOUTH -> NORTH;
                case EAST -> WEST;
                case WEST -> EAST;
                case UP -> DOWN;
                case DOWN -> UP;
            };
        }
    }

    public enum Axis {
        X,
        Y,
        Z
    }

    public record Point(float x, float y, float z) {
        public float coordinate(Axis axis) {
            return switch (axis) {
                case X -> x;
                case Y -> y;
                case Z -> z;
            };
        }
    }
}
