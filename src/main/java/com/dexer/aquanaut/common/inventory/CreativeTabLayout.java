package com.dexer.aquanaut.common.inventory;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The sections of one creative tab: each {@link Section} is a header followed by the items that
 * belong under it, in the order the tab's contents generator accepted them.
 *
 * <p>
 * {@link #inline(List)} turns such a sectioned tab into what the client actually displays: the
 * tab's live display list with every header inlined as one full row of the item grid. The header
 * row is nine empty slots wide -- the whole horizontal span of the grid -- so it reads as a banner
 * cutting across the tab instead of as an item. Since the row is real layout, everything vanilla
 * computes from the list (row count, scrolling, which stack lands in which slot) follows for free.
 */
public record CreativeTabLayout(List<Section> sections) {

    /** Vanilla's creative picker grid is nine slots wide. */
    public static final int GRID_COLUMNS = 9;

    /**
     * A header and the items displayed below it.
     *
     * @param header the banner row that opens this section
     * @param items  the items accepted after the header, in display order
     */
    public record Section(CreativeTabHeader header, List<ItemStack> items) {
    }

    /**
     * A display list with headers inlined.
     *
     * @param items       the rows to show: real items plus the empty padding around headers
     * @param headerRows  the rows that are banners, by row index ({@code items index / GRID_COLUMNS})
     */
    public record Plan(List<ItemStack> items, Map<Integer, CreativeTabHeader> headerRows) {

        /** The plan of a tab without headers: its display list, untouched. */
        public static final Plan EMPTY = new Plan(List.of(), Map.of());
    }

    /**
     * Lays {@code displayItems} out below this layout's headers.
     *
     * <p>
     * A header is emitted when the first item of its section is reached, so the plan keeps working
     * on filtered lists (a search query) and on lists other mods have contributed to: items with no
     * section flow through untouched, and a section whose items were all filtered out contributes
     * no header. A header starts on a fresh row, padding the row above when the previous section
     * ended mid-row, because a banner is always a whole row wide.
     *
     * <p>
     * Empty stacks are padding from an earlier {@code inline} and are dropped, which makes the
     * transform stable to apply twice in a row.
     */
    public Plan inline(List<ItemStack> displayItems) {
        Map<StackKey, Integer> homeSection = new HashMap<>();
        for (int section = 0; section < this.sections.size(); section++) {
            for (ItemStack stack : this.sections.get(section).items()) {
                homeSection.putIfAbsent(new StackKey(stack), section);
            }
        }

        List<ItemStack> rows = new ArrayList<>();
        Map<Integer, CreativeTabHeader> headerRows = new LinkedHashMap<>();
        BitSet shown = new BitSet();
        for (ItemStack stack : displayItems) {
            if (stack.isEmpty()) {
                continue;
            }
            Integer section = homeSection.get(new StackKey(stack));
            if (section != null && !shown.get(section)) {
                shown.set(section);
                while (rows.size() % GRID_COLUMNS != 0) {
                    rows.add(ItemStack.EMPTY);
                }
                headerRows.put(rows.size() / GRID_COLUMNS, this.sections.get(section).header());
                for (int column = 0; column < GRID_COLUMNS; column++) {
                    rows.add(ItemStack.EMPTY);
                }
            }
            rows.add(stack);
        }

        return new Plan(List.copyOf(rows), Map.copyOf(headerRows));
    }

    /** The identity vanilla's creative display list de-duplicates stacks by: item plus components. */
    private record StackKey(Item item, DataComponentPatch components) {

        StackKey(ItemStack stack) {
            this(stack.getItem(), stack.getComponentsPatch());
        }
    }
}
