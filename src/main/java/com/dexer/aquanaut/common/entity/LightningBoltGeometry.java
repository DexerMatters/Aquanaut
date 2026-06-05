package com.dexer.aquanaut.common.entity;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class LightningBoltGeometry {
    static final Vec3 FORWARD = new Vec3(0.0D, 0.0D, 1.0D);

    public record Point(float x, float y, float z) {
        public Vec3 toVec3() {
            return new Vec3(x, y, z);
        }
    }

    public record Path(List<Point> points, float width, int depth) {
    }

    public record Segment(Point start, Point end, float width, int depth) {
        public Vec3 center() {
            return new Vec3(
                    (start.x() + end.x()) * 0.5D,
                    (start.y() + end.y()) * 0.5D,
                    (start.z() + end.z()) * 0.5D);
        }

        public float length() {
            return (float) start.toVec3().distanceTo(end.toVec3());
        }
    }

    private LightningBoltGeometry() {
    }

    public static List<Path> buildPaths(long seed, float length, int branchDepth, int branchCount, float width,
            float branchScale) {
        List<Path> paths = new ArrayList<>();
        RandomSource random = RandomSource.create(seed);
        buildPath(paths, random, new Point(0.0F, 0.0F, 0.0F), FORWARD, length, width, 0, branchDepth,
                branchCount, branchScale);
        return paths;
    }

    public static Vec3 transformPoint(Point point, Vec3 origin, float yaw, float pitch) {
        Vec3 forward = Vec3.directionFromRotation(pitch, yaw);
        Vec3 side = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-6D) {
            side = forward.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        side = side.normalize();
        Vec3 up = side.cross(forward).normalize();

        return origin.add(side.scale(point.x())).add(up.scale(point.y())).add(forward.scale(point.z()));
    }

    public static AABB boundsForPaths(List<Path> paths, Vec3 origin, float yaw, float pitch) {
        AABB bounds = null;
        for (Path path : paths) {
            float radius = Math.max(path.width(), 0.05F);
            for (Point point : path.points()) {
                Vec3 world = transformPoint(point, origin, yaw, pitch);
                AABB box = AABB.ofSize(world, radius * 2.0D, radius * 2.0D, radius * 2.0D);
                bounds = bounds == null ? box : bounds.minmax(box);
            }
        }
        return bounds == null ? AABB.ofSize(origin, 0.5D, 0.5D, 0.5D) : bounds;
    }

    public static List<Segment> flattenSegments(List<Path> paths) {
        return flattenSegments(paths, false);
    }

    public static List<Segment> flattenRenderSegments(List<Path> paths) {
        return flattenSegments(paths, true, true);
    }

    public static List<Segment> flattenHitboxSegments(List<Path> paths) {
        return flattenSegments(paths, true, false);
    }

    private static List<Segment> flattenSegments(List<Path> paths, boolean coalesce) {
        return flattenSegments(paths, coalesce, false);
    }

    private static List<Segment> flattenSegments(List<Path> paths, boolean coalesce, boolean renderSegments) {
        List<Segment> segments = new ArrayList<>();
        for (Path path : paths) {
            List<Point> points = path.points();
            int step = coalesce ? segmentStride(path.depth(), renderSegments) : 1;
            for (int i = 0; i < points.size() - 1; i += step) {
                int endIndex = Math.min(i + step, points.size() - 1);
                segments.add(new Segment(points.get(i), points.get(endIndex), path.width(), path.depth()));
            }
        }
        return List.copyOf(segments);
    }

    public static AABB boundsForSegments(List<Segment> segments, Vec3 origin, float yaw, float pitch) {
        AABB bounds = null;
        for (Segment segment : segments) {
            Vec3 start = transformPoint(segment.start(), origin, yaw, pitch);
            Vec3 end = transformPoint(segment.end(), origin, yaw, pitch);
            float radius = Math.max(segment.width(), 0.05F);
            AABB startBox = AABB.ofSize(start, radius * 2.0D, radius * 2.0D, radius * 2.0D);
            AABB endBox = AABB.ofSize(end, radius * 2.0D, radius * 2.0D, radius * 2.0D);
            AABB box = startBox.minmax(endBox);
            bounds = bounds == null ? box : bounds.minmax(box);
        }
        return bounds == null ? AABB.ofSize(origin, 0.5D, 0.5D, 0.5D) : bounds;
    }

    private static void buildPath(List<Path> paths, RandomSource random, Point start, Vec3 direction, float length,
            float width, int depth, int branchDepth, int branchCount, float branchScale) {
        int segmentCount = Math.max(5, Mth.ceil(length * 4.0F));
        float segmentLength = length / segmentCount;
        Vec3 currentDirection = direction.normalize();
        Point current = start;
        List<Point> points = new ArrayList<>(segmentCount + 1);
        points.add(current);

        for (int i = 0; i < segmentCount; i++) {
            currentDirection = jitteredDirection(random, currentDirection, depth);
            current = step(current, currentDirection, segmentLength);
            points.add(current);
        }

        paths.add(new Path(List.copyOf(points), width, depth));

        if (depth >= branchDepth || branchCount <= 0) {
            return;
        }

        int childBranches = Math.min(Math.max(1, branchCount - depth + (depth == 0 ? 0 : 1)),
                Math.max(1, segmentCount / 3));
        int nextBranchCount = Math.max(0, branchCount - (depth == 0 ? 1 : 0));
        for (int branchIndex = 0; branchIndex < childBranches; branchIndex++) {
            int minAnchor = depth == 0 ? 1 : Math.max(1, (int) (segmentCount * 0.55F));
            int maxAnchor = Math.max(minAnchor, segmentCount - 2);
            float t = (branchIndex + 1.0F) / (childBranches + 1.0F);
            float anchorBias = depth == 0 ? t : (float) Math.pow(t, 0.45F);
            int spreadIndex = Mth.clamp((int) (minAnchor + anchorBias * (maxAnchor - minAnchor)), minAnchor, maxAnchor);
            int anchorIndex = Mth.clamp(spreadIndex + random.nextInt(3) - 1, minAnchor, maxAnchor);
            Point anchor = points.get(anchorIndex);
            Vec3 anchorDirection = segmentDirection(points, anchorIndex);
            Vec3 branchDirection = splitDirection(random, anchorDirection, depth);
            float nextDepthScale = Mth.clamp(1.0F - (depth + 1) * 0.18F, 0.30F, 1.0F);
            float childLength = length * branchScale * nextDepthScale * (0.58F + random.nextFloat() * 0.16F);
            float childWidthScale = Mth.clamp(0.52F - depth * 0.10F, 0.24F, 0.52F);
            float childWidth = width * childWidthScale;
            RandomSource branchRandom = RandomSource.create(random.nextLong());
            buildPath(paths, branchRandom, anchor, branchDirection, childLength, childWidth, depth + 1, branchDepth,
                    nextBranchCount, branchScale);
        }
    }

    private static Vec3 jitteredDirection(RandomSource random, Vec3 direction, int depth) {
        Vec3 side = perpendicular(direction);
        Vec3 up = direction.cross(side).normalize();
        float bendScale = depth == 0 ? 0.10F : 0.32F + depth * 0.08F;
        double baseSideJitter = depth == 0 ? 0.015D : 0.04D;
        double bendSide = (random.nextBoolean() ? 1.0D : -1.0D) * (baseSideJitter + random.nextDouble() * bendScale);
        double bendUp = (random.nextDouble() - 0.5D) * bendScale * (depth == 0 ? 0.18D : 0.35D);
        Vec3 bent = direction.scale(1.0D)
                .add(side.scale(bendSide))
                .add(up.scale(bendUp));
        return bent.normalize();
    }

    private static Vec3 splitDirection(RandomSource random, Vec3 direction, int depth) {
        Vec3 side = perpendicular(direction);
        Vec3 up = direction.cross(side).normalize();
        double forwardWeight = 0.28D + random.nextDouble() * 0.14D;
        double sideWeight = 0.58D + random.nextDouble() * 0.22D;
        double upWeight = (random.nextDouble() - 0.5D) * (0.18D + depth * 0.05D);
        double sideways = random.nextBoolean() ? sideWeight : -sideWeight;
        Vec3 branch = direction.scale(forwardWeight)
                .add(side.scale(sideways))
                .add(up.scale(upWeight));
        return branch.normalize();
    }

    private static Vec3 perpendicular(Vec3 direction) {
        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-6D) {
            side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        return side.normalize();
    }

    private static Point step(Point point, Vec3 direction, float distance) {
        Vec3 next = point.toVec3().add(direction.normalize().scale(distance));
        return new Point((float) next.x, (float) next.y, (float) next.z);
    }

    private static Vec3 segmentDirection(List<Point> points, int index) {
        Point start = points.get(index - 1);
        Point end = points.get(index);
        return end.toVec3().subtract(start.toVec3()).normalize();
    }

    private static int segmentStride(int depth, boolean renderSegments) {
        if (renderSegments) {
            return depth == 0 ? 3 : 4;
        }

        return depth == 0 ? 4 : 5;
    }
}
