package com.dexer.aquanaut.common.block;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class DissectionTableMultiblockTest {

    private static DissectionTableMultiblock.Group groupOf(Set<BlockPos> cells, BlockPos pos) {
        Optional<DissectionTableMultiblock.Group> resolved = DissectionTableMultiblock.resolve(cells::contains, pos);
        assertTrue(resolved.isPresent(), "expected " + pos + " to resolve to a group");
        return resolved.get();
    }

    @Test
    void singleTableResolvesToItself() {
        Set<BlockPos> cells = Set.of(new BlockPos(4, 1, 7));
        DissectionTableMultiblock.Group group = groupOf(cells, new BlockPos(4, 1, 7));

        assertEquals(1, group.widthX());
        assertEquals(1, group.depthZ());
        assertEquals("", group.modelSuffix());
        assertTrue(group.isMaster(new BlockPos(4, 1, 7)));
    }

    @Test
    void twoTablesAlongXMergeIntoTheLongBench() {
        Set<BlockPos> cells = Set.of(new BlockPos(10, 2, 3), new BlockPos(11, 2, 3));
        DissectionTableMultiblock.Group west = groupOf(cells, new BlockPos(10, 2, 3));
        DissectionTableMultiblock.Group east = groupOf(cells, new BlockPos(11, 2, 3));

        assertEquals(west, east, "both cells must report the same group");
        assertEquals(2, west.widthX());
        assertEquals(1, west.depthZ());
        assertEquals("_x2", west.modelSuffix());
        assertEquals(new BlockPos(10, 2, 3), west.origin());
        assertTrue(west.isMaster(new BlockPos(10, 2, 3)));
        assertFalse(west.isMaster(new BlockPos(11, 2, 3)));
    }

    @Test
    void fourTablesInASquareMergeIntoTheFullBench() {
        Set<BlockPos> cells = Set.of(
                new BlockPos(0, 5, 0), new BlockPos(1, 5, 0),
                new BlockPos(0, 5, 1), new BlockPos(1, 5, 1));

        DissectionTableMultiblock.Group group = groupOf(cells, new BlockPos(1, 5, 1));
        assertEquals(2, group.widthX());
        assertEquals(2, group.depthZ());
        assertEquals("_x4", group.modelSuffix());
        assertEquals(new BlockPos(0, 5, 0), group.origin());

        for (BlockPos cell : cells) {
            assertEquals(group, groupOf(cells, cell));
        }
    }

    @Test
    void separateBenchesDoNotInterfere() {
        Set<BlockPos> cells = Set.of(
                new BlockPos(0, 0, 0), new BlockPos(1, 0, 0),
                new BlockPos(4, 0, 0), new BlockPos(5, 0, 0));

        DissectionTableMultiblock.Group first = groupOf(cells, new BlockPos(0, 0, 0));
        DissectionTableMultiblock.Group second = groupOf(cells, new BlockPos(5, 0, 0));
        assertEquals("_x2", first.modelSuffix());
        assertEquals("_x2", second.modelSuffix());
        assertEquals(new BlockPos(0, 0, 0), first.origin());
        assertEquals(new BlockPos(4, 0, 0), second.origin());
    }

    @Test
    void aBenchAttachedToAnotherBenchStaysSingle() {
        // Growing only from a free north-west corner is what keeps merged benches from overlapping:
        // a pair glued to the east side of an existing square would otherwise double-render.
        Set<BlockPos> cells = Set.of(
                new BlockPos(0, 0, 0), new BlockPos(1, 0, 0),
                new BlockPos(0, 0, 1), new BlockPos(1, 0, 1),
                new BlockPos(2, 0, 0), new BlockPos(3, 0, 0));

        assertEquals("_x4", groupOf(cells, new BlockPos(0, 0, 0)).modelSuffix());
        assertEquals("_x4", groupOf(cells, new BlockPos(1, 0, 1)).modelSuffix());
        assertEquals(1, groupOf(cells, new BlockPos(2, 0, 0)).area());
        assertEquals(1, groupOf(cells, new BlockPos(3, 0, 0)).area());
    }

    @Test
    void partialArrangementsMergeOnlyWhereAFreeCornerAllows() {
        // L shape: the western pair has a free corner and merges; the northern cell stays single.
        Set<BlockPos> lShape = Set.of(new BlockPos(0, 0, 0), new BlockPos(1, 0, 0), new BlockPos(0, 0, 1));
        assertEquals(2, groupOf(lShape, new BlockPos(0, 0, 0)).area());
        assertEquals(2, groupOf(lShape, new BlockPos(1, 0, 0)).area());
        DissectionTableMultiblock.Group stray = groupOf(lShape, new BlockPos(0, 0, 1));
        assertEquals(1, stray.area());
        assertEquals(new BlockPos(0, 0, 1), stray.origin());

        // Diagonal contact never merges: the far cell has an occupied diagonal neighbour.
        Set<BlockPos> diagonal = Set.of(new BlockPos(0, 0, 0), new BlockPos(1, 0, 1));
        for (BlockPos cell : diagonal) {
            assertEquals(1, groupOf(diagonal, cell).area());
        }
    }

    @Test
    void twoTablesAlongZMergeIntoTheTurnedBench() {
        Set<BlockPos> cells = Set.of(new BlockPos(0, 0, 0), new BlockPos(0, 0, 1));
        DissectionTableMultiblock.Group north = groupOf(cells, new BlockPos(0, 0, 0));
        DissectionTableMultiblock.Group south = groupOf(cells, new BlockPos(0, 0, 1));

        assertEquals(north, south, "both cells must report the same group");
        assertEquals(1, north.widthX());
        assertEquals(2, north.depthZ());
        assertEquals("_x2", north.modelSuffix());
        assertEquals(new BlockPos(0, 0, 0), north.origin());
        assertTrue(north.isMaster(new BlockPos(0, 0, 0)));
        assertFalse(north.isMaster(new BlockPos(0, 0, 1)));
    }

    @Test
    void anLPrefersTheXPairSoCrossingBenchesNeverOverlap() {
        // (0,0)-(0,1) also looks like a north-south pair and (0,0)-(1,0) like an east-west pair. The
        // shared corner must resolve to exactly one bench or both would draw over each other.
        Set<BlockPos> lShape = Set.of(new BlockPos(0, 0, 0), new BlockPos(1, 0, 0), new BlockPos(0, 0, 1));

        DissectionTableMultiblock.Group pair = groupOf(lShape, new BlockPos(0, 0, 0));
        assertEquals(2, pair.widthX());
        assertEquals(1, pair.depthZ());
        assertEquals(pair, groupOf(lShape, new BlockPos(1, 0, 0)));
        assertEquals(new BlockPos(0, 0, 0), pair.origin());

        DissectionTableMultiblock.Group loose = groupOf(lShape, new BlockPos(0, 0, 1));
        assertEquals(1, loose.area(), "the northern table must not grow a crossing bench");
        assertEquals(new BlockPos(0, 0, 1), loose.origin());
    }

    @Test
    void aFreeStandingColumnOfThreeMergesOnce() {
        Set<BlockPos> cells = Set.of(new BlockPos(0, 0, 0), new BlockPos(0, 0, 1), new BlockPos(0, 0, 2));

        DissectionTableMultiblock.Group pair = groupOf(cells, new BlockPos(0, 0, 0));
        assertEquals(2, pair.depthZ());
        assertEquals(pair, groupOf(cells, new BlockPos(0, 0, 1)));
        assertEquals(1, groupOf(cells, new BlockPos(0, 0, 2)).area());
    }

    @Test
    void emptySpaceDoesNotResolve() {
        Set<BlockPos> cells = new HashSet<>();
        cells.add(new BlockPos(2, 2, 2));
        assertTrue(DissectionTableMultiblock.resolve(cells::contains, new BlockPos(9, 9, 9)).isEmpty());
    }

    @Test
    void threeByThreeFieldResolvesWithoutOverlappingBenches() {
        Set<BlockPos> cells = new HashSet<>();
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                cells.add(new BlockPos(x, 0, z));
            }
        }

        // The north-west corner grows the only bench; every other cell degrades to a single table,
        // so no two cells ever draw the same merged model.
        DissectionTableMultiblock.Group bench = groupOf(cells, new BlockPos(0, 0, 0));
        assertEquals(4, bench.area());
        assertEquals(new BlockPos(0, 0, 0), bench.origin());
        for (BlockPos cell : cells) {
            if (bench.contains(cell)) {
                assertEquals(bench, groupOf(cells, cell));
            } else {
                DissectionTableMultiblock.Group single = groupOf(cells, cell);
                assertEquals(1, single.area(), cell + " should not merge");
                assertEquals(cell, single.origin());
            }
        }
    }
}
