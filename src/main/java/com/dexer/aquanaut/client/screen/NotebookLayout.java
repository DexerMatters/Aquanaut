package com.dexer.aquanaut.client.screen;

import com.dexer.aquanaut.common.notebook.NotebookCatalog;
import com.dexer.aquanaut.common.notebook.NotebookCategory;
import com.dexer.aquanaut.common.notebook.NotebookProgress;
import com.dexer.aquanaut.common.notebook.NotebookSpecies;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.List;

public final class NotebookLayout {

    private static final int SCREEN_MARGIN = 8;
    private static final int MIN_BOOK_WIDTH = 336;
    private static final int MAX_BOOK_WIDTH = 430;
    private static final int MIN_BOOK_HEIGHT = 224;
    private static final int MAX_BOOK_HEIGHT = 282;

    public static final int CATEGORY_TAB_WIDTH = 22;
    public static final int CATEGORY_TAB_HEIGHT = 32;
    public static final int CATEGORY_TAB_GAP = 8;

    public static final int PAGE_TOP = 48;
    public static final int PAGE_BOTTOM_MARGIN = 18;
    public static final int PAGE_SIDE_INSET = 20;
    public static final int PAGE_GUTTER = 20;

    public static final int INDEX_ROW_HEIGHT = 18;
    public static final int INDEX_ROWS_VISIBLE = 8;

    public static final int PROPERTY_ROW_HEIGHT = 12;

    private static final List<NotebookCategory> CATEGORY_ORDER = List.of(
            NotebookCategory.FRIENDLY,
            NotebookCategory.THREATENING,
            NotebookCategory.TITAN);

    private NotebookLayout() {
    }

    public record TabBounds(NotebookCategory category, int x, int y, int width, int height) {
    }

    public static List<NotebookCategory> categoryOrder() {
        return CATEGORY_ORDER;
    }

    public static int bookWidth(int screenWidth) {
        return responsiveDimension(screenWidth - 48, MIN_BOOK_WIDTH, MAX_BOOK_WIDTH, screenWidth);
    }

    public static int bookHeight(int screenHeight) {
        return responsiveDimension(screenHeight - 44, MIN_BOOK_HEIGHT, MAX_BOOK_HEIGHT, screenHeight);
    }

    public static int bookLeft(int screenWidth) {
        int bookWidth = bookWidth(screenWidth);
        int centered = (screenWidth - bookWidth) / 2;
        int maxLeft = screenWidth - bookWidth - SCREEN_MARGIN;
        if (maxLeft <= SCREEN_MARGIN) {
            return Math.max(0, centered);
        }
        return Mth.clamp(centered, SCREEN_MARGIN, maxLeft);
    }

    public static int bookTop(int screenHeight) {
        int bookHeight = bookHeight(screenHeight);
        int centered = (screenHeight - bookHeight) / 2;
        int maxTop = screenHeight - bookHeight - SCREEN_MARGIN;
        if (maxTop <= SCREEN_MARGIN) {
            return Math.max(0, centered);
        }
        return Mth.clamp(centered, SCREEN_MARGIN, maxTop);
    }

    public static int bookRight(int screenWidth) {
        return bookLeft(screenWidth) + bookWidth(screenWidth);
    }

    public static int bookBottom(int screenHeight) {
        return bookTop(screenHeight) + bookHeight(screenHeight);
    }

    public static int categoryTabWidth(int screenWidth) {
        return CATEGORY_TAB_WIDTH;
    }

    public static TabBounds categoryTabBounds(int screenWidth, int screenHeight, NotebookCategory category) {
        int width = categoryTabWidth(screenWidth);
        int x = bookRight(screenWidth) - 1;
        int y = bookTop(screenHeight) + 54 + category.ordinal() * (CATEGORY_TAB_HEIGHT + CATEGORY_TAB_GAP);
        return new TabBounds(category, x, y, width, CATEGORY_TAB_HEIGHT);
    }

    public static List<TabBounds> categoryTabs(int screenWidth, int screenHeight) {
        return categoryOrder().stream()
                .map(category -> categoryTabBounds(screenWidth, screenHeight, category))
                .toList();
    }

    public static int leftPageX(int screenWidth) {
        return bookLeft(screenWidth) + PAGE_SIDE_INSET;
    }

    public static int rightPageX(int screenWidth) {
        return leftPageX(screenWidth) + pageWidth(screenWidth) + PAGE_GUTTER;
    }

    public static int pageWidth(int screenWidth) {
        return (bookWidth(screenWidth) - PAGE_SIDE_INSET * 2 - PAGE_GUTTER) / 2;
    }

    public static int pageY(int screenHeight) {
        return bookTop(screenHeight) + PAGE_TOP;
    }

    public static int pageBottom(int screenHeight) {
        return bookTop(screenHeight) + bookHeight(screenHeight) - PAGE_BOTTOM_MARGIN;
    }

    public static int indexRowsVisible(int screenHeight) {
        return Math.max(5, (pageBottom(screenHeight) - pageY(screenHeight) - 4) / INDEX_ROW_HEIGHT);
    }

    public static List<NotebookSpecies> visibleSpecies(NotebookCategory category, NotebookProgress progress) {
        return NotebookCatalog.visibleByCategory(category, progress);
    }

    public static List<NotebookSpecies> visibleSpeciesWindow(NotebookCategory category, NotebookProgress progress,
            int scrollOffset) {
        return visibleSpeciesWindow(category, progress, scrollOffset, INDEX_ROWS_VISIBLE);
    }

    public static List<NotebookSpecies> visibleSpeciesWindow(NotebookCategory category, NotebookProgress progress,
            int scrollOffset, int visibleRows) {
        List<NotebookSpecies> visible = visibleSpecies(category, progress);
        if (visible.isEmpty()) {
            return List.of();
        }

        int maxScroll = Math.max(0, visible.size() - visibleRows);
        int clampedScroll = Mth.clamp(scrollOffset, 0, maxScroll);
        int endIndex = Math.min(visible.size(), clampedScroll + visibleRows);
        return List.copyOf(visible.subList(clampedScroll, endIndex));
    }

    public static int clampScroll(int scrollOffset, NotebookCategory category, NotebookProgress progress) {
        return clampScroll(scrollOffset, category, progress, INDEX_ROWS_VISIBLE);
    }

    public static int clampScroll(int scrollOffset, NotebookCategory category, NotebookProgress progress,
            int visibleRows) {
        int visible = visibleSpecies(category, progress).size();
        return Mth.clamp(scrollOffset, 0, Math.max(0, visible - visibleRows));
    }

    public static NotebookSpecies resolveSelection(NotebookCategory category, ResourceLocation selectedSpeciesId,
            NotebookProgress progress) {
        List<NotebookSpecies> visible = visibleSpecies(category, progress);
        if (visible.isEmpty()) {
            return null;
        }

        if (selectedSpeciesId != null) {
            ResourceLocation canonical = NotebookCatalog.canonicalId(selectedSpeciesId);
            for (NotebookSpecies species : visible) {
                if (species.id().equals(canonical)) {
                    return species;
                }
            }
        }

        return visible.getFirst();
    }

    public static NotebookSpecies firstVisibleSpecies(NotebookCategory category, NotebookProgress progress) {
        List<NotebookSpecies> visible = visibleSpecies(category, progress);
        return visible.isEmpty() ? null : visible.getFirst();
    }

    private static int responsiveDimension(int preferred, int min, int max, int screenSize) {
        int available = Math.max(180, screenSize - SCREEN_MARGIN * 2);
        int lowerBound = Math.min(min, available);
        int upperBound = Math.min(max, available);
        return Mth.clamp(preferred, lowerBound, upperBound);
    }
}
