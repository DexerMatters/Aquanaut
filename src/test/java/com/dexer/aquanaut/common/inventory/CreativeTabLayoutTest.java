package com.dexer.aquanaut.common.inventory;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The header row planner behind the natural tab: what it means to lay items out "below a header".
 *
 * <p>
 * The client draws a banner over a header's row, so the planner's one hard rule is that a header
 * owns a whole row of the grid -- nine empty slots -- starting on a row boundary. Everything else
 * falls out of that: sections pad out the row above them, a filtered list still opens each section
 * it has items for, and re-running the plan over its own output must not move anything, because
 * the screen fills its list in layers and the transform sees its own output again.
 */
final class CreativeTabLayoutTest {

    private static final int GRID = CreativeTabLayout.GRID_COLUMNS;

    /** A stack of its own unregistered item: enough identity for the planner's item matching. */
    private static ItemStack stack() {
        return new ItemStack(new Item(new Item.Properties()));
    }

    private static CreativeTabLayout.Section section(String title, ItemStack... items) {
        return new CreativeTabLayout.Section(
                new CreativeTabHeader(Component.literal(title),
                                ResourceLocation.fromNamespaceAndPath("aquanaut",
                                                "textures/gui/creative_tab/" + title + ".png")),
                List.of(items));
    }

    private static String titleAt(CreativeTabLayout.Plan plan, int row) {
        CreativeTabHeader header = plan.headerRows().get(row);
        return header == null ? null : header.title().getString();
    }

    @Test
    void aHeaderOwnsAWholeRowAndItsItemsFollowBelowIt() {
        ItemStack a = stack();
        ItemStack b = stack();
        ItemStack c = stack();
        ItemStack d = stack();
        CreativeTabLayout layout = new CreativeTabLayout(List.of(
                section("first", a, b),
                section("second", c, d)));

        CreativeTabLayout.Plan plan = layout.inline(List.of(a, b, c, d));

        // Row 0 is the first banner, row 1 its two items, row 2 the second banner -- padded up
        // from the two-item row above so the banner is whole -- and row 3 its two items.
        assertEquals(GRID + GRID + GRID + 2, plan.items().size());
        assertEquals("first", titleAt(plan, 0));
        assertEquals("second", titleAt(plan, 2));
        assertEquals(2, plan.headerRows().size());

        for (int row : new int[] { 0, 2 }) {
            for (int column = 0; column < GRID; column++) {
                assertTrue(plan.items().get(row * GRID + column).isEmpty(),
                        "a banner row is nine empty slots (" + row + "," + column + ")");
            }
        }

        assertEquals(a, plan.items().get(GRID));
        assertEquals(b, plan.items().get(GRID + 1));
        assertEquals(c, plan.items().get(2 * GRID + GRID));
        assertEquals(d, plan.items().get(2 * GRID + GRID + 1));
    }

    @Test
    void inliningTwiceMovesNothing() {
        ItemStack a = stack();
        ItemStack b = stack();
        ItemStack c = stack();
        CreativeTabLayout layout = new CreativeTabLayout(List.of(
                section("first", a),
                section("second", b, c)));

        CreativeTabLayout.Plan plan = layout.inline(List.of(a, b, c));
        CreativeTabLayout.Plan again = layout.inline(plan.items());

        assertEquals(plan.items(), again.items(),
                "the screen's layered fills run the plan over its own output; that must be stable");
        assertEquals(plan.headerRows(), again.headerRows());
    }

    @Test
    void aFilteredListStillOpensEverySectionItKeepsItemsFor() {
        ItemStack a = stack();
        ItemStack b = stack();
        ItemStack c = stack();
        ItemStack d = stack();
        CreativeTabLayout layout = new CreativeTabLayout(List.of(
                section("first", a, b),
                section("second", c, d)));

        // A search that keeps a, c and d: both banners survive, each above its first survivor.
        CreativeTabLayout.Plan plan = layout.inline(List.of(a, c, d));

        assertEquals("first", titleAt(plan, 0));
        assertEquals("second", titleAt(plan, 2));
        assertEquals(a, plan.items().get(GRID));
        assertEquals(c, plan.items().get(2 * GRID + GRID));
    }

    @Test
    void aSectionAllOfWhoseItemsAreFilteredOutLosesItsBanner() {
        ItemStack a = stack();
        ItemStack b = stack();
        ItemStack c = stack();
        CreativeTabLayout layout = new CreativeTabLayout(List.of(
                section("first", a, b),
                section("second", c)));

        CreativeTabLayout.Plan plan = layout.inline(List.of(c));

        assertEquals(1, plan.headerRows().size());
        assertEquals("second", titleAt(plan, 0));
    }

    @Test
    void itemsWithNoSectionFlowThroughBeforeTheFirstBanner() {
        ItemStack stray = stack();
        ItemStack a = stack();
        ItemStack b = stack();
        CreativeTabLayout layout = new CreativeTabLayout(List.of(section("first", a, b)));

        CreativeTabLayout.Plan plan = layout.inline(List.of(stray, a, b));

        // The stray item is claimed by no section, so it leads the tab and the banner pads up
        // behind it instead of swallowing it.
        assertEquals(stray, plan.items().get(0));
        assertEquals("first", titleAt(plan, 1));
        assertTrue(plan.items().get(GRID).isEmpty(), "the banner row is the padding row above it");
    }
}
