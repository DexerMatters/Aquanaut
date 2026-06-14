package com.dexer.aquanaut.common.worldgen;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

final class JellyJungleStemForestPlanner {
    private static final HorizontalDirection[] HORIZONTALS = {
            new HorizontalDirection(1, 0),
            new HorizontalDirection(-1, 0),
            new HorizontalDirection(0, 1),
            new HorizontalDirection(0, -1)
    };

    private JellyJungleStemForestPlanner() {
    }

    static Plan plan(long seed) {
        Random random = new Random(seed);
        LinkedHashSet<Pos> stems = new LinkedHashSet<>();
        List<Segment> segments = new ArrayList<>();
        LinkedHashSet<Pos> foliageTargets = new LinkedHashSet<>();
        int trunkHeight = 14 + random.nextInt(11);

        addSegment(stems, segments, new Pos(0, 0, 0), 0, 1, 0, trunkHeight, Axis.VERTICAL);

        for (int y = 6; y < trunkHeight - 2; y += 2 + random.nextInt(3)) {
            HorizontalDirection primary = HORIZONTALS[random.nextInt(HORIZONTALS.length)];
            growBranch(stems, segments, foliageTargets, new Pos(0, y, 0), primary, random, 0);
            if (y >= trunkHeight / 2 && random.nextFloat() < 0.45F) {
                HorizontalDirection secondary = perpendicular(primary, random.nextBoolean());
                growBranch(stems, segments, foliageTargets, new Pos(0, y + 1, 0), secondary, random, 1);
            }
        }

        return new Plan(Set.copyOf(stems), List.copyOf(segments), List.copyOf(foliageTargets), trunkHeight);
    }

    static Pos originAt(int y) {
        return new Pos(0, y, 0);
    }

    private static void growBranch(Set<Pos> stems,
                                   List<Segment> segments,
                                   Set<Pos> foliageTargets,
                                   Pos anchor,
                                   HorizontalDirection direction,
                                   Random random,
                                   int depth) {
        int runLength = 2 + random.nextInt(depth == 0 ? 3 : 2);
        Pos horizontalStart = anchor.offset(direction.stepX(), 0, direction.stepZ());
        addSegment(stems, segments, horizontalStart, direction.stepX(), 0, direction.stepZ(), runLength,
                direction.axis());

        Pos branchEnd = horizontalStart.offset(direction.stepX() * (runLength - 1), 0,
                direction.stepZ() * (runLength - 1));
        int spurHeight = 2 + random.nextInt(depth == 0 ? 5 : 4);
        addSegment(stems, segments, branchEnd.offset(0, 1, 0), 0, 1, 0, spurHeight, Axis.VERTICAL);
        Pos spurTop = branchEnd.offset(0, spurHeight, 0);
        foliageTargets.add(branchEnd.offset(direction.stepX(), 0, direction.stepZ()));
        foliageTargets.add(spurTop.offset(0, 1, 0));

        for (int step = 1; step <= spurHeight; step++) {
            Pos stem = branchEnd.offset(0, step, 0);
            if (random.nextFloat() < 0.55F) {
                HorizontalDirection side = perpendicular(direction, random.nextBoolean());
                foliageTargets.add(stem.offset(side.stepX(), 0, side.stepZ()));
            }
        }

        if (depth >= 2) {
            return;
        }

        if (random.nextFloat() < 0.85F) {
            growBranch(stems, segments, foliageTargets, spurTop, perpendicular(direction, true), random, depth + 1);
        }
        if (random.nextFloat() < 0.55F) {
            growBranch(stems, segments, foliageTargets, spurTop.offset(0, -1 - random.nextInt(Math.max(1, spurHeight)),
                    0), perpendicular(direction, false), random, depth + 1);
        }
    }

    private static void addSegment(Set<Pos> stems,
                                   List<Segment> segments,
                                   Pos start,
                                   int stepX,
                                   int stepY,
                                   int stepZ,
                                   int length,
                                   Axis axis) {
        segments.add(new Segment(start, stepX, stepY, stepZ, length, axis));
        Pos current = start;
        for (int i = 0; i < length; i++) {
            stems.add(current);
            current = current.offset(stepX, stepY, stepZ);
        }
    }

    private static HorizontalDirection perpendicular(HorizontalDirection direction, boolean clockwise) {
        return switch (direction.axis()) {
            case HORIZONTAL_X -> clockwise ? new HorizontalDirection(0, 1) : new HorizontalDirection(0, -1);
            case HORIZONTAL_Z -> clockwise ? new HorizontalDirection(1, 0) : new HorizontalDirection(-1, 0);
            case VERTICAL -> throw new IllegalStateException("vertical direction has no horizontal perpendicular");
        };
    }

    record Plan(Set<Pos> stemPositions, List<Segment> segments, List<Pos> foliageTargets, int trunkHeight) {
    }

    record Segment(Pos start, int stepX, int stepY, int stepZ, int length, Axis axis) {
    }

    record Pos(int x, int y, int z) {
        Pos offset(int dx, int dy, int dz) {
            return new Pos(x + dx, y + dy, z + dz);
        }
    }

    enum Axis {
        VERTICAL,
        HORIZONTAL_X,
        HORIZONTAL_Z
    }

    private record HorizontalDirection(int stepX, int stepZ) {
        Axis axis() {
            return stepX != 0 ? Axis.HORIZONTAL_X : Axis.HORIZONTAL_Z;
        }
    }
}
