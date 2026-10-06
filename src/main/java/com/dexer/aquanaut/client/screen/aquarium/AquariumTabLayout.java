package com.dexer.aquanaut.client.screen.aquarium;

import java.util.List;

/**
 * Placement maths for the Aquarium tab, kept free of client classes so the rules stay unit testable.
 *
 * <p>The tab copies the vanilla creative-inventory tab geometry exactly: 26x32 sprites on a 27 pixel
 * pitch, hung from a panel edge so that the top {@value #ATTACHED_INSET} rows tuck under the panel
 * and only {@value #ATTACHED_HEIGHT} rows stay visible. A selected tab hangs four pixels lower
 * ({@value #SELECTED_HEIGHT} visible rows), which is what makes it read as attached to the panel
 * instead of merely next to it.
 */
public final class AquariumTabLayout {

    /** Sprite width, matching {@code container/creative_inventory/tab_bottom_*}. */
    public static final int TAB_WIDTH = 26;

    /** Full sprite height; the top {@value #ATTACHED_INSET} rows sit behind the panel edge. */
    public static final int TAB_HEIGHT = 32;

    /** Horizontal distance between neighbouring tab columns. */
    public static final int TAB_PITCH = 27;

    /** Rows of an attached sprite that the panel covers. */
    public static final int ATTACHED_INSET = 4;

    /** Visible height of an unselected attached tab: sprite rows 4..27. */
    public static final int ATTACHED_HEIGHT = 24;

    /** Visible height of a selected tab: sprite rows 0..31, four pixels lower than its neighbours. */
    public static final int SELECTED_HEIGHT = 28;

    /** Visible height of a free-standing tab: sprite rows 0..27. */
    public static final int FREE_HEIGHT = 28;

    /** Columns in a vanilla creative tab row; the right-aligned ones are placed from column 6 down. */
    public static final int ROW_COLUMNS = 7;

    private AquariumTabLayout() {
    }

    /**
     * {@return the x of a tab column relative to the panel's left edge} Mirrors
     * {@code CreativeModeInventoryScreen#getTabX}, including its right-aligned columns.
     */
    public static int rowX(int panelWidth, int column, boolean alignedRight) {
        return alignedRight ? panelWidth - TAB_PITCH * (ROW_COLUMNS - column) + 1 : TAB_PITCH * column;
    }

    /** {@return the sprite y of a tab row relative to the panel's top edge, as in vanilla} */
    public static int rowY(int panelHeight, boolean topRow) {
        return topRow ? -(TAB_HEIGHT - ATTACHED_INSET) : panelHeight - ATTACHED_INSET;
    }

    /** {@return the column immediately left of {@code referenceX}, where an extra tab would sit} */
    public static int neighbourX(int referenceX) {
        return referenceX - TAB_PITCH;
    }

    /**
     * {@return the sprite y for a tab stacked under another one} A selected tab is four pixels taller,
     * so the tab below it has to start four pixels lower to keep the pair flush.
     */
    public static int stackedY(int referenceY, boolean referenceSelected) {
        return referenceY + (referenceSelected ? SELECTED_HEIGHT : ATTACHED_HEIGHT);
    }

    /**
     * {@return whether a tab placed at {@code candidate} stays clear of every tab in {@code occupied}}
     * Hit boxes are the full 26x32 sprites, the same box vanilla uses when it checks a tab click.
     */
    public static boolean isFree(Box candidate, List<Box> occupied) {
        for (Box box : occupied) {
            if (candidate.intersects(box)) {
                return false;
            }
        }
        return true;
    }

    /** The sprite box of one tab: enough to test overlap between two of them. */
    public record Box(int x, int y) {

        public int right() {
            return this.x + TAB_WIDTH;
        }

        public int bottom() {
            return this.y + TAB_HEIGHT;
        }

        /** {@return whether this box and {@code other} share at least one pixel} Touching edges do not. */
        public boolean intersects(Box other) {
            return this.x < other.right() && other.x < this.right()
                    && this.y < other.bottom() && other.y < this.bottom();
        }
    }
}
