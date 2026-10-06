package com.dexer.aquanaut.client.screen.aquarium;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class AquariumTabLayoutTest {

    @Test
    void rightAlignedColumnsMatchTheVanillaTabRow() {
        // Vanilla places the survival-inventory tab flush with the panel's right edge.
        assertEquals(150, AquariumTabLayout.rowX(176, 6, true), "176 wide panel, last column");
        assertEquals(123, AquariumTabLayout.rowX(176, 5, true), "176 wide panel, one column in");
        assertEquals(169, AquariumTabLayout.rowX(195, 6, true), "creative panel, last column");
        assertEquals(142, AquariumTabLayout.rowX(195, 5, true), "creative panel, op tab column");
    }

    @Test
    void leftAlignedColumnsStartAtThePanelEdge() {
        assertEquals(0, AquariumTabLayout.rowX(195, 0, false), "first column");
        assertEquals(27, AquariumTabLayout.rowX(195, 1, false), "second column");
    }

    @Test
    void rowsHangBelowThePanelAndTuckUnderItsEdge() {
        assertEquals(-28, AquariumTabLayout.rowY(166, true), "top rows overlap the panel by four pixels");
        assertEquals(162, AquariumTabLayout.rowY(166, false), "bottom row of a 166 tall panel");
        assertEquals(132, AquariumTabLayout.rowY(136, false), "bottom row of the creative panel");
    }

    @Test
    void theExtraTabSitsOneColumnLeftOfTheInventoryTab() {
        assertEquals(123, AquariumTabLayout.neighbourX(150), "aquarium tab beside the chest tab");
        assertEquals(142, AquariumTabLayout.neighbourX(169), "creative chest tab");
    }

    @Test
    void anOccupiedNeighbourColumnIsRejected() {
        AquariumTabLayout.Box candidate = new AquariumTabLayout.Box(123, 162);
        assertTrue(AquariumTabLayout.isFree(candidate, List.of()), "empty row leaves the slot free");
        assertTrue(AquariumTabLayout.isFree(candidate, List.of(new AquariumTabLayout.Box(150, 162))),
                "the inventory tab itself does not block its left neighbour");
        assertFalse(AquariumTabLayout.isFree(candidate, List.of(new AquariumTabLayout.Box(142, 162))),
                "an op tab in the same column blocks the slot");
    }

    @Test
    void stackedTabsClearTheTabAboveThem() {
        assertEquals(186, AquariumTabLayout.stackedY(162, false), "below an unselected tab");
        assertEquals(190, AquariumTabLayout.stackedY(162, true), "below a selected tab");
    }

    @Test
    void boxesIntersectOnlyWhenTheyOverlap() {
        AquariumTabLayout.Box box = new AquariumTabLayout.Box(100, 162);
        assertTrue(box.intersects(new AquariumTabLayout.Box(101, 163)), "overlapping boxes");
        assertFalse(box.intersects(new AquariumTabLayout.Box(126, 162)), "touching edges stay clear");
        assertFalse(box.intersects(new AquariumTabLayout.Box(100, 194)), "touching bottom edges stay clear");
        assertEquals(126, box.right(), "box width follows the vanilla sprite");
        assertEquals(194, box.bottom(), "box height follows the vanilla sprite");
    }
}
