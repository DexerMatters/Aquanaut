package com.dexer.aquanaut.common.inventory.aquarium;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class AquariumPlacementMath {

    private AquariumPlacementMath() {
    }

    public static int findDensePlacement(Collection<Placement> placements, int width, int height, int cols,
            int rows) {
        for (int row = 0; row <= rows - height; row++) {
            for (int col = 0; col <= cols - width; col++) {
                int anchorIndex = row * cols + col;
                if (canPlaceAt(placements, width, height, anchorIndex, cols, rows, -1)) {
                    return anchorIndex;
                }
            }
        }
        return -1;
    }

    public static boolean canPlaceAt(Collection<Placement> placements, int width, int height, int anchorIndex,
            int cols, int rows, int ignoredAnchorIndex) {
        if (width < 1 || height < 1) {
            return false;
        }
        if (anchorIndex < 0 || anchorIndex >= cols * rows) {
            return false;
        }

        int anchorCol = anchorIndex % cols;
        int anchorRow = anchorIndex / cols;
        if (anchorCol + width > cols || anchorRow + height > rows) {
            return false;
        }

        Placement candidate = new Placement(anchorIndex, width, height);
        for (Placement placement : placements) {
            if (placement.anchorIndex() == ignoredAnchorIndex) {
                continue;
            }
            if (rectanglesOverlap(candidate, placement, cols)) {
                return false;
            }
        }
        return true;
    }

    public static List<Integer> occupiedCells(int anchorIndex, int width, int height, int cols) {
        List<Integer> cells = new ArrayList<>(width * height);
        int anchorCol = anchorIndex % cols;
        int anchorRow = anchorIndex / cols;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                cells.add((anchorRow + row) * cols + anchorCol + col);
            }
        }
        return cells;
    }

    public static List<Integer> coveredCells(int anchorIndex, int width, int height, int cols, int rows) {
        if (width < 1 || height < 1 || anchorIndex < 0 || anchorIndex >= cols * rows) {
            return List.of();
        }

        List<Integer> cells = new ArrayList<>(width * height);
        int anchorCol = anchorIndex % cols;
        int anchorRow = anchorIndex / cols;
        for (int row = 0; row < height; row++) {
            int cellRow = anchorRow + row;
            if (cellRow >= rows) {
                break;
            }
            for (int col = 0; col < width; col++) {
                int cellCol = anchorCol + col;
                if (cellCol >= cols) {
                    break;
                }
                cells.add(cellRow * cols + cellCol);
            }
        }
        return cells;
    }

    public static boolean containsCell(Placement placement, int cellIndex, int cols) {
        int cellCol = cellIndex % cols;
        int cellRow = cellIndex / cols;
        int anchorCol = placement.anchorIndex() % cols;
        int anchorRow = placement.anchorIndex() / cols;
        return cellCol >= anchorCol && cellCol < anchorCol + placement.width()
                && cellRow >= anchorRow && cellRow < anchorRow + placement.height();
    }

    private static boolean rectanglesOverlap(Placement a, Placement b, int cols) {
        int aCol = a.anchorIndex() % cols;
        int aRow = a.anchorIndex() / cols;
        int bCol = b.anchorIndex() % cols;
        int bRow = b.anchorIndex() / cols;

        int aRight = aCol + a.width();
        int aBottom = aRow + a.height();
        int bRight = bCol + b.width();
        int bBottom = bRow + b.height();

        return aCol < bRight && aRight > bCol && aRow < bBottom && aBottom > bRow;
    }

    public record Placement(int anchorIndex, int width, int height) {
    }
}
