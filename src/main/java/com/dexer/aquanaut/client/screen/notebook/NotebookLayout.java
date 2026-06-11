package com.dexer.aquanaut.client.screen.notebook;

import com.dexer.aquanaut.common.notebook.NotebookCategory;

public final class NotebookLayout {

    public static final int BOOK_WIDTH = 252;
    public static final int BOOK_HEIGHT = 220;

    private static final int EDGE_MARGIN = 8;
    private static final int BOOK_INSET = 12;
    private static final int TAB_WIDTH = 68;
    private static final int TAB_HEIGHT = 18;
    private static final int TAB_GAP = 4;
    private static final int TAB_TOP_OFFSET = 10;
    private static final int CONTENT_TOP_OFFSET = 34;
    private static final int CONTENT_BOTTOM_PADDING = 12;
    private static final int INDEX_PANE_WIDTH = 96;
    private static final int PANE_GAP = 8;

    private final int bookLeft;
    private final int bookTop;

    private NotebookLayout(int bookLeft, int bookTop) {
        this.bookLeft = bookLeft;
        this.bookTop = bookTop;
    }

    public static NotebookLayout forScreen(int screenWidth, int screenHeight) {
        int left = Math.max(EDGE_MARGIN, (screenWidth - BOOK_WIDTH) / 2);
        int top = Math.max(EDGE_MARGIN, (screenHeight - BOOK_HEIGHT) / 2);
        return new NotebookLayout(left, top);
    }

    public int bookLeft() {
        return this.bookLeft;
    }

    public int bookTop() {
        return this.bookTop;
    }

    public int bookWidth() {
        return BOOK_WIDTH;
    }

    public int bookHeight() {
        return BOOK_HEIGHT;
    }

    public int bookRight() {
        return this.bookLeft + BOOK_WIDTH;
    }

    public int tabStartX() {
        return this.bookLeft + (BOOK_WIDTH - totalTabWidth()) / 2;
    }

    public int tabTop() {
        return this.bookTop + TAB_TOP_OFFSET;
    }

    public int tabWidth() {
        return TAB_WIDTH;
    }

    public int tabHeight() {
        return TAB_HEIGHT;
    }

    public int categoryTabLeft(NotebookCategory category) {
        return tabStartX() + category.ordinal() * (TAB_WIDTH + TAB_GAP);
    }

    public int categoryTabRight(NotebookCategory category) {
        return categoryTabLeft(category) + TAB_WIDTH;
    }

    public int indexPaneLeft() {
        return this.bookLeft + BOOK_INSET;
    }

    public int indexPaneTop() {
        return this.bookTop + CONTENT_TOP_OFFSET;
    }

    public int indexPaneWidth() {
        return INDEX_PANE_WIDTH;
    }

    public int indexPaneHeight() {
        return BOOK_HEIGHT - CONTENT_TOP_OFFSET - CONTENT_BOTTOM_PADDING;
    }

    public int indexPaneRight() {
        return indexPaneLeft() + indexPaneWidth();
    }

    public int detailPaneLeft() {
        return indexPaneRight() + PANE_GAP;
    }

    public int detailPaneTop() {
        return indexPaneTop();
    }

    public int detailPaneWidth() {
        return BOOK_WIDTH - (BOOK_INSET * 2) - INDEX_PANE_WIDTH - PANE_GAP;
    }

    public int detailPaneHeight() {
        return indexPaneHeight();
    }

    public int detailPaneRight() {
        return detailPaneLeft() + detailPaneWidth();
    }

    public int detailPaneBottom() {
        return detailPaneTop() + detailPaneHeight();
    }

    private int totalTabWidth() {
        return (TAB_WIDTH * NotebookCategory.values().length)
                + (TAB_GAP * (NotebookCategory.values().length - 1));
    }
}
