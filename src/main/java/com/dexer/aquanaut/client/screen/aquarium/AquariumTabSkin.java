package com.dexer.aquanaut.client.screen.aquarium;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Draws a tab with the vanilla creative-inventory silhouette: a 26 pixel wide tab whose corners step
 * in three times, one pixel at a time, so it reads as the same control as
 * {@code container/creative_inventory/tab_bottom_*}. The pixel runs below were lifted from those
 * sprites, so the shape is exact; only the palette changes.
 *
 * <p>Two palettes ship: the vanilla grey chest tab, so the inventory half of the pair stays
 * indistinguishable from the creative screen's own tab, and a deep teal variant for the aquarium.
 */
public final class AquariumTabSkin {

    /** Which edge of a tab row the tab is drawn against. */
    public enum Shape {
        /** Hung from a panel edge: the top four rows hide behind the panel, the bottom corners step in. */
        ATTACHED,
        /** The selected tab: it reaches four pixels lower and reads as merged with the panel above it. */
        SELECTED,
        /** Free standing, used when every column of the row is taken: both corner pairs step in. */
        FREE
    }

    /** Column runs shared by every body row: outline, light edge, fill, dark edge, outline. */
    private static final int LEFT_OUTLINE_END = 1;
    private static final int LEFT_EDGE_END = 3;
    private static final int RIGHT_EDGE_START = 23;
    private static final int RIGHT_OUTLINE_START = 25;

    private static final int HOVER_LIFT = 0x18;

    /**
     * The mod's own tab, painted in the palette of the diving equipment panel that
     * {@code ClientInventoryPanelEvents} puts beside the inventory: the same light edge, body and dark
     * edge, with the unselected fill taking the same step down from the body that vanilla's grey tab
     * takes from its own panel.
     */
    private static final Palette AQUARIUM = new Palette(
            0xFF324848, // outline: darker than the panel edge, so the tab still reads against the world
            0xFFF2FFFF, // highlight: the equipment panel's light edge
            0xFF889A9A, // fill: the panel body stepped down the way vanilla's unselected tab is
            0xFFC2DBDB, // selected fill: the equipment panel's body
            0xFF5D7A7A); // shadow: the equipment panel's dark edge
    private static final Palette INVENTORY = new Palette(
            0xFF000000, 0xFFFFFFFF, 0xFF8B8B8B, 0xFFC6C6C6, 0xFF555555);

    private AquariumTabSkin() {
    }

    /**
     * Draws one tab.
     *
     * @param x        left edge of the drawn tab
     * @param y        top edge of the drawn tab: for {@link Shape#ATTACHED} that is the first row below
     *                 the panel edge, because the four rows above it are hidden; for
     *                 {@link Shape#SELECTED} it is four pixels higher, which is what makes the tab
     *                 overlap the panel and read as attached
     * @param aquarium {@code true} for the mod's teal tab, {@code false} for the vanilla grey one
     * @param shape    how the tab meets its neighbours
     * @param hovered  {@code true} while the pointer is over an interactive tab
     */
    public static void draw(GuiGraphics graphics, int x, int y, boolean aquarium, Shape shape, boolean hovered) {
        Palette palette = aquarium ? AQUARIUM : INVENTORY;
        boolean selected = shape == Shape.SELECTED;
        int outline = palette.outline();
        int highlight = hovered ? 0xFFFFFFFF : palette.highlight();
        int fill = hovered ? lift(palette.fill(selected)) : palette.fill(selected);
        int shadow = palette.shadow();

        if (shape == Shape.SELECTED) {
            // Sprite rows 0..3: the part that overlaps the panel and makes the tab look attached.
            graphics.fill(x, y, x + 26, y + 1, fill);
            graphics.fill(x, y + 1, x + 2, y + 3, shadow);
            graphics.fill(x + 2, y + 1, x + 3, y + 3, highlight);
            graphics.fill(x + 3, y + 1, x + 23, y + 3, fill);
            graphics.fill(x + 23, y + 1, x + 26, y + 3, shadow);
            graphics.fill(x, y + 3, x + 1, y + 4, outline);
            graphics.fill(x + 1, y + 3, x + 3, y + 4, highlight);
            graphics.fill(x + 3, y + 3, x + 23, y + 4, fill);
            graphics.fill(x + 23, y + 3, x + 25, y + 4, shadow);
            graphics.fill(x + 25, y + 3, x + 26, y + 4, outline);
            body(graphics, x, y + 4, y + 29, outline, highlight, fill, shadow);
            bottomSteps(graphics, x, y + 29, outline, fill, shadow);
            return;
        }

        if (shape == Shape.FREE) {
            topSteps(graphics, x, y, outline, highlight, fill, shadow);
            body(graphics, x, y + 6, y + 25, outline, highlight, fill, shadow);
            bottomSteps(graphics, x, y + 25, outline, fill, shadow);
            return;
        }

        body(graphics, x, y, y + 21, outline, highlight, fill, shadow);
        bottomSteps(graphics, x, y + 21, outline, fill, shadow);
    }

    /**
     * {@return the y of a tab's 16x16 icon} Vanilla centres the icon in the tab body rather than in the
     * sprite, so an attached tab carries it three pixels below the panel edge.
     */
    public static int iconY(int spriteY, Shape shape) {
        return switch (shape) {
            case ATTACHED -> spriteY + 3;
            case SELECTED -> spriteY + 7;
            case FREE -> spriteY + 8;
        };
    }

    /** {@return the x of a tab's 16x16 icon, centred in the 26 pixel sprite} */
    public static int iconX(int spriteX) {
        return spriteX + 5;
    }

    private static void body(GuiGraphics graphics, int x, int fromY, int toY, int outline, int highlight, int fill,
            int shadow) {
        graphics.fill(x, fromY, x + LEFT_OUTLINE_END, toY, outline);
        graphics.fill(x + LEFT_OUTLINE_END, fromY, x + LEFT_EDGE_END, toY, highlight);
        graphics.fill(x + LEFT_EDGE_END, fromY, x + RIGHT_EDGE_START, toY, fill);
        graphics.fill(x + RIGHT_EDGE_START, fromY, x + RIGHT_OUTLINE_START, toY, shadow);
        graphics.fill(x + RIGHT_OUTLINE_START, fromY, x + 26, toY, outline);
    }

    /** The three pixel staircase that closes the bottom of every tab. */
    private static void bottomSteps(GuiGraphics graphics, int x, int y, int outline, int fill, int shadow) {
        graphics.fill(x + 1, y, x + 2, y + 1, outline);
        graphics.fill(x + 2, y, x + 3, y + 1, fill);
        graphics.fill(x + 3, y, x + 25, y + 1, shadow);
        graphics.fill(x + 25, y, x + 26, y + 1, outline);

        graphics.fill(x + 2, y + 1, x + 3, y + 2, outline);
        graphics.fill(x + 3, y + 1, x + 24, y + 2, shadow);
        graphics.fill(x + 24, y + 1, x + 25, y + 2, outline);

        graphics.fill(x + 3, y + 2, x + 24, y + 3, outline);
    }

    /** The mirrored staircase, which only a free standing tab shows along its top edge. */
    private static void topSteps(GuiGraphics graphics, int x, int y, int outline, int highlight, int fill, int shadow) {
        graphics.fill(x + 2, y + 2, x + 23, y + 3, outline);
        graphics.fill(x + 1, y + 3, x + 2, y + 4, outline);
        graphics.fill(x + 2, y + 3, x + 23, y + 4, highlight);
        graphics.fill(x + 23, y + 3, x + 24, y + 4, outline);

        graphics.fill(x, y + 4, x + 1, y + 5, outline);
        graphics.fill(x + 1, y + 4, x + 23, y + 5, highlight);
        graphics.fill(x + 23, y + 4, x + 24, y + 5, fill);
        graphics.fill(x + 24, y + 4, x + 25, y + 5, outline);

        graphics.fill(x, y + 5, x + 1, y + 6, outline);
        graphics.fill(x + 1, y + 5, x + 4, y + 6, highlight);
        graphics.fill(x + 4, y + 5, x + 23, y + 6, fill);
        graphics.fill(x + 23, y + 5, x + 25, y + 6, shadow);
        graphics.fill(x + 25, y + 5, x + 26, y + 6, outline);
    }

    private static int lift(int argb) {
        int alpha = argb & 0xFF000000;
        int red = Math.min(0xFF, ((argb >> 16) & 0xFF) + HOVER_LIFT);
        int green = Math.min(0xFF, ((argb >> 8) & 0xFF) + HOVER_LIFT);
        int blue = Math.min(0xFF, (argb & 0xFF) + HOVER_LIFT);
        return alpha | (red << 16) | (green << 8) | blue;
    }

    private record Palette(int outline, int highlight, int fill, int selectedFill, int shadow) {

        int fill(boolean selected) {
            return selected ? this.selectedFill : this.fill;
        }
    }
}
