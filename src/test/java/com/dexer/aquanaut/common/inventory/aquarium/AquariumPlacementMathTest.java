package com.dexer.aquanaut.common.inventory.aquarium;

import java.util.ArrayList;
import java.util.List;

public final class AquariumPlacementMathTest {

    public static void main(String[] args) {
        AquariumPlacementMathTest test = new AquariumPlacementMathTest();
        test.densePackingUsesTheFirstAvailableTopLeftAnchor();
        test.densePackingFailsWhenNoSpaceFits();
        test.canPlaceAtCanIgnoreTheSourceFishDuringMoves();
        test.occupiedCellsCoverTheWholeFootprint();
        test.coveredCellsStayInsideTheAquariumGrid();
    }

    private void densePackingUsesTheFirstAvailableTopLeftAnchor() {
        List<AquariumPlacementMath.Placement> placements = List.of(
                new AquariumPlacementMath.Placement(0, 2, 2),
                new AquariumPlacementMath.Placement(3, 1, 2));

        int fit = AquariumPlacementMath.findDensePlacement(placements, 1, 2, 9, 2);

        assertEquals(2, fit, "dense packing should choose the first open top-left anchor");
    }

    private void densePackingFailsWhenNoSpaceFits() {
        List<AquariumPlacementMath.Placement> placements = new ArrayList<>();
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 9; col++) {
                placements.add(new AquariumPlacementMath.Placement(row * 9 + col, 1, 1));
            }
        }

        int fit = AquariumPlacementMath.findDensePlacement(placements, 1, 1, 9, 2);

        assertEquals(-1, fit, "full aquarium should reject additional fish");
    }

    private void canPlaceAtCanIgnoreTheSourceFishDuringMoves() {
        List<AquariumPlacementMath.Placement> placements = List.of(
                new AquariumPlacementMath.Placement(0, 2, 2),
                new AquariumPlacementMath.Placement(6, 1, 1));

        boolean canMove = AquariumPlacementMath.canPlaceAt(placements, 2, 2, 1, 9, 2, 0);
        boolean blockedWithoutIgnore = AquariumPlacementMath.canPlaceAt(placements, 2, 2, 1, 9, 2, -1);

        assertTrue(canMove, "source fish should not block its own move target");
        assertTrue(!blockedWithoutIgnore, "the same move should fail if the source fish is not ignored");
    }

    private void occupiedCellsCoverTheWholeFootprint() {
        List<Integer> cells = AquariumPlacementMath.occupiedCells(4, 2, 2, 9);

        assertEquals(List.of(4, 5, 13, 14), cells, "occupied cells should match the full rectangle");
    }

    private void coveredCellsStayInsideTheAquariumGrid() {
        List<Integer> cells = AquariumPlacementMath.coveredCells(8, 3, 2, 9, 2);

        assertEquals(List.of(8, 17), cells, "covered cells should clip at the aquarium boundary");
    }

    private void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + " expected " + expected + " but was " + actual);
        }
    }

    private void assertEquals(List<Integer> expected, List<Integer> actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " expected " + expected + " but was " + actual);
        }
    }

    private void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
