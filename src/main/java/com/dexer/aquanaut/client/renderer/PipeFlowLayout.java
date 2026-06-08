package com.dexer.aquanaut.client.renderer;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class PipeFlowLayout {
    private static final float FACE_DISTANCE = 0.56F;
    private static final List<Endpoint> ORDER = List.of(
            Endpoint.WEST,
            Endpoint.NORTH,
            Endpoint.DOWN,
            Endpoint.EAST,
            Endpoint.SOUTH,
            Endpoint.UP);

    private PipeFlowLayout() {
    }

    static List<Route> routes(Set<Endpoint> connections, int flow) {
        if (flow == 0 || connections.isEmpty()) {
            return List.of();
        }

        List<Endpoint> entries = orderedEndpoints(connections, false, flow);
        List<Endpoint> exits = orderedEndpoints(connections, true, flow);
        if (entries.isEmpty() && exits.isEmpty()) {
            return List.of();
        }

        List<Route> routes = new ArrayList<>();
        if (!entries.isEmpty() && !exits.isEmpty()) {
            EnumSet<Endpoint> usedExits = EnumSet.noneOf(Endpoint.class);
            int fallbackExitIndex = 0;
            for (Endpoint entry : entries) {
                Endpoint preferredExit = entry.opposite();
                Endpoint exit = exits.contains(preferredExit)
                        ? preferredExit
                        : exits.get(fallbackExitIndex++ % exits.size());
                routes.add(new Route(entry, exit));
                usedExits.add(exit);
            }

            int fallbackEntryIndex = 0;
            for (Endpoint exit : exits) {
                if (usedExits.contains(exit)) {
                    continue;
                }
                Endpoint preferredEntry = exit.opposite();
                Endpoint entry = entries.contains(preferredEntry)
                        ? preferredEntry
                        : entries.get(fallbackEntryIndex++ % entries.size());
                routes.add(new Route(entry, exit));
            }
        } else if (!entries.isEmpty()) {
            for (Endpoint entry : entries) {
                routes.add(new Route(entry, Endpoint.CENTER));
            }
        } else {
            for (Endpoint exit : exits) {
                routes.add(new Route(Endpoint.CENTER, exit));
            }
        }

        return List.copyOf(routes);
    }

    static List<RouteFlow> routes(Map<Endpoint, Integer> faceFlows) {
        if (faceFlows.isEmpty()) {
            return List.of();
        }

        List<FaceAmount> incoming = new ArrayList<>();
        List<FaceAmount> outgoing = new ArrayList<>();
        for (Endpoint endpoint : ORDER) {
            int amount = faceFlows.getOrDefault(endpoint, 0);
            if (amount < 0) {
                incoming.add(new FaceAmount(endpoint, -amount));
            } else if (amount > 0) {
                outgoing.add(new FaceAmount(endpoint, amount));
            }
        }

        if (incoming.isEmpty() && outgoing.isEmpty()) {
            return List.of();
        }

        if (incoming.isEmpty()) {
            List<RouteFlow> routes = new ArrayList<>(outgoing.size());
            for (FaceAmount exit : outgoing) {
                routes.add(new RouteFlow(new Route(Endpoint.CENTER, exit.endpoint()), exit.amount()));
            }
            return List.copyOf(routes);
        }

        if (outgoing.isEmpty()) {
            List<RouteFlow> routes = new ArrayList<>(incoming.size());
            for (FaceAmount entry : incoming) {
                routes.add(new RouteFlow(new Route(entry.endpoint(), Endpoint.CENTER), entry.amount()));
            }
            return List.copyOf(routes);
        }

        List<RouteFlow> routes = new ArrayList<>();
        List<FaceAmount> mutableOutgoing = new ArrayList<>(outgoing);
        for (FaceAmount entry : incoming) {
            int remaining = entry.amount();
            while (remaining > 0 && !mutableOutgoing.isEmpty()) {
                int exitIndex = chooseExitIndex(entry.endpoint(), mutableOutgoing);
                FaceAmount exit = mutableOutgoing.get(exitIndex);
                int amount = Math.min(remaining, exit.amount());
                routes.add(new RouteFlow(new Route(entry.endpoint(), exit.endpoint()), amount));
                remaining -= amount;
                int updated = exit.amount() - amount;
                if (updated == 0) {
                    mutableOutgoing.remove(exitIndex);
                } else {
                    mutableOutgoing.set(exitIndex, new FaceAmount(exit.endpoint(), updated));
                }
            }
        }

        return List.copyOf(routes);
    }

    private static List<Endpoint> orderedEndpoints(Set<Endpoint> connections, boolean wantsExit, int flow) {
        List<Endpoint> ordered = new ArrayList<>();
        for (Endpoint endpoint : ORDER) {
            if (connections.contains(endpoint) && isExit(endpoint, flow) == wantsExit) {
                ordered.add(endpoint);
            }
        }
        return ordered;
    }

    private static boolean isExit(Endpoint endpoint, int flow) {
        return flow > 0 ? endpoint.isPositiveDirection() : endpoint.isNegativeDirection();
    }

    private static int chooseExitIndex(Endpoint entry, List<FaceAmount> outgoing) {
        Endpoint preferred = entry.opposite();
        for (int i = 0; i < outgoing.size(); i++) {
            if (outgoing.get(i).endpoint() == preferred) {
                return i;
            }
        }
        return 0;
    }

    enum Axis {
        X,
        Y,
        Z
    }

    enum Endpoint {
        CENTER(0.0F, 0.0F, 0.0F, null, 0),
        NORTH(0.0F, 0.0F, -FACE_DISTANCE, Axis.Z, -1),
        SOUTH(0.0F, 0.0F, FACE_DISTANCE, Axis.Z, 1),
        EAST(FACE_DISTANCE, 0.0F, 0.0F, Axis.X, 1),
        WEST(-FACE_DISTANCE, 0.0F, 0.0F, Axis.X, -1),
        UP(0.0F, FACE_DISTANCE, 0.0F, Axis.Y, 1),
        DOWN(0.0F, -FACE_DISTANCE, 0.0F, Axis.Y, -1);

        private final Point point;
        private final Axis axis;
        private final int axisSign;

        Endpoint(float x, float y, float z, Axis axis, int axisSign) {
            this.point = new Point(x, y, z);
            this.axis = axis;
            this.axisSign = axisSign;
        }

        Point point() {
            return point;
        }

        Axis axis() {
            return axis;
        }

        boolean isPositiveDirection() {
            return axisSign > 0;
        }

        boolean isNegativeDirection() {
            return axisSign < 0;
        }

        Endpoint opposite() {
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

    record Point(float x, float y, float z) {
        float coordinate(Axis axis) {
            return switch (axis) {
                case X -> x;
                case Y -> y;
                case Z -> z;
            };
        }

        Point add(float dx, float dy, float dz) {
            return new Point(x + dx, y + dy, z + dz);
        }
    }

    record Route(Endpoint start, Endpoint end) {
        Route {
            if (start == Endpoint.CENTER && end == Endpoint.CENTER) {
                throw new IllegalArgumentException("Route must leave the center");
            }
        }

        float length() {
            if (start == Endpoint.CENTER || end == Endpoint.CENTER) {
                return FACE_DISTANCE;
            }
            return FACE_DISTANCE * 2.0F;
        }

        Point anchorPoint() {
            return start != Endpoint.CENTER ? start.point() : end.point();
        }

        Axis anchorAxis() {
            return start != Endpoint.CENTER ? start.axis() : end.axis();
        }

        Axis axisAt(float progress) {
            if (start == Endpoint.CENTER) {
                return end.axis();
            }
            if (end == Endpoint.CENTER) {
                return start.axis();
            }
            return progress < 0.5F ? start.axis() : end.axis();
        }

        Point pointAt(float progress) {
            float clamped = Math.max(0.0F, Math.min(1.0F, progress));
            Point center = Endpoint.CENTER.point();
            if (start == Endpoint.CENTER) {
                return lerp(center, end.point(), clamped);
            }
            if (end == Endpoint.CENTER) {
                return lerp(start.point(), center, clamped);
            }
            if (clamped < 0.5F) {
                return lerp(start.point(), center, clamped * 2.0F);
            }
            return lerp(center, end.point(), (clamped - 0.5F) * 2.0F);
        }

        private Point lerp(Point from, Point to, float progress) {
            return new Point(
                    from.x() + (to.x() - from.x()) * progress,
                    from.y() + (to.y() - from.y()) * progress,
                    from.z() + (to.z() - from.z()) * progress);
        }
    }

    record RouteFlow(Route route, int amount) {
    }

    private record FaceAmount(Endpoint endpoint, int amount) {
    }
}
