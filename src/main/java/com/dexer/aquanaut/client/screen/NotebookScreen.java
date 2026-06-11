package com.dexer.aquanaut.client.screen;

import com.dexer.aquanaut.client.ClientNotebookData;
import com.dexer.aquanaut.client.renderer.NotebookPreviewRenderer;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumFish;
import com.dexer.aquanaut.common.notebook.NotebookBlock;
import com.dexer.aquanaut.common.notebook.NotebookCatalog;
import com.dexer.aquanaut.common.notebook.NotebookCategory;
import com.dexer.aquanaut.common.notebook.NotebookDiet;
import com.dexer.aquanaut.common.notebook.NotebookComponentKind;
import com.dexer.aquanaut.common.notebook.NotebookProgress;
import com.dexer.aquanaut.common.notebook.NotebookPropertyKey;
import com.dexer.aquanaut.common.notebook.NotebookSpecies;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class NotebookScreen extends Screen {

    private static final int COVER_DARK = 0xFF0A1427;
    private static final int COVER_MID = 0xFF112648;
    private static final int COVER_LIGHT = 0xFF1A3A66;
    private static final int BOOKMARK_DARK = 0xFF8F521A;
    private static final int BOOKMARK = 0xFFD9882B;
    private static final int BOOKMARK_LIGHT = 0xFFF2B75C;
    private static final int BOOKMARK_SELECTED = 0xFFEDA13B;
    private static final int BOOKMARK_SELECTED_LIGHT = 0xFFFFCF76;
    private static final int PAGE = 0xFFE8DFC8;
    private static final int PAGE_LIGHT = 0xFFF5EDD8;
    private static final int PAGE_DARK = 0xFFC6B48C;
    private static final int PAGE_SHADOW = 0xFF9E8F6C;
    private static final int TEXT = 0xFF1A2432;
    private static final int TEXT_DIM = 0xFF667180;
    private static final int ACCENT = 0xFF6B4A25;
    private static final int DESCRIPTION_TEXT = 0xFF72675C;
    private static final int PROPERTY_LABEL = 0xFF73583A;
    private static final int PROPERTY_VALUE = 0xFF5D6772;
    private static final int TITLE_CYAN = 0xFF144D5C;
    private static final int SELECTED = 0xFF334F7A;
    private static final int LOCKED = 0xFF7A8797;
    private static final int BACKGROUND = 0x18FFFFFF;
    private static final int CONTENT_SPACING = 6;
    private static final int PROPERTY_BLOCK_HEIGHT = 20;
    private static final int PREVIEW_BOX_HEIGHT = 64;
    private static final int PREVIEW_BOX_INSET = 2;
    private static final int DETAIL_CONTENT_BOTTOM_PADDING = 6;
    private static final int SCROLL_STEP = 16;

    private NotebookCategory selectedCategory = NotebookCategory.FRIENDLY;
    private ResourceLocation selectedSpeciesId;
    private int indexScroll;
    private int detailScroll;

    public NotebookScreen() {
        super(Component.translatable("gui.aquanaut.notebook.title"));
    }

    @Override
    protected void init() {
        super.init();
        NotebookSpecies first = NotebookLayout.firstVisibleSpecies(selectedCategory, ClientNotebookData.getNotebook());
        selectedSpeciesId = first == null ? null : first.id();
        indexScroll = NotebookLayout.clampScroll(indexScroll, selectedCategory, ClientNotebookData.getNotebook(),
                NotebookLayout.indexRowsVisible(height));
        detailScroll = 0;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        NotebookProgress progress = ClientNotebookData.getNotebook();
        NotebookSpecies selectedSpecies = ensureSelection(progress);
        int bookLeft = NotebookLayout.bookLeft(width);
        int bookTop = NotebookLayout.bookTop(height);

        renderBackdrop(graphics, bookLeft, bookTop);

        drawBook(graphics, bookLeft, bookTop);
        drawTabs(graphics, mouseX, mouseY);
        drawIndex(graphics, progress, mouseX, mouseY);
        drawEntry(graphics, progress, selectedSpecies);
        drawTabTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, BACKGROUND);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        NotebookProgress progress = ClientNotebookData.getNotebook();
        for (NotebookLayout.TabBounds tab : NotebookLayout.categoryTabs(width, height)) {
            if (contains(tab, mouseX, mouseY)) {
                selectCategory(tab.category(), progress);
                return true;
            }
        }

        int indexLeft = NotebookLayout.leftPageX(width);
        int indexTop = NotebookLayout.pageY(height) + 2;
        int pageWidth = NotebookLayout.pageWidth(width);
        int visibleRows = NotebookLayout.indexRowsVisible(height);
        int indexBottom = indexTop + visibleRows * NotebookLayout.INDEX_ROW_HEIGHT;
        if (mouseX >= indexLeft && mouseX < indexLeft + pageWidth
                && mouseY >= indexTop && mouseY < indexBottom) {
            List<NotebookSpecies> visible = NotebookLayout.visibleSpeciesWindow(selectedCategory, progress, indexScroll,
                    visibleRows);
            int row = (int) ((mouseY - indexTop) / NotebookLayout.INDEX_ROW_HEIGHT);
            if (row >= 0 && row < visible.size()) {
                selectedSpeciesId = visible.get(row).id();
                detailScroll = 0;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        NotebookProgress progress = ClientNotebookData.getNotebook();
        int indexLeft = NotebookLayout.leftPageX(width);
        int indexTop = NotebookLayout.pageY(height) + 2;
        int pageWidth = NotebookLayout.pageWidth(width);
        int visibleRows = NotebookLayout.indexRowsVisible(height);
        int indexBottom = indexTop + visibleRows * NotebookLayout.INDEX_ROW_HEIGHT;
        if (mouseX >= indexLeft && mouseX < indexLeft + pageWidth
                && mouseY >= indexTop && mouseY < indexBottom) {
            int delta = (int) Math.signum(scrollY);
            indexScroll = NotebookLayout.clampScroll(indexScroll - delta, selectedCategory, progress, visibleRows);
            return true;
        }
        int detailLeft = NotebookLayout.rightPageX(width);
        int detailTop = NotebookLayout.pageY(height);
        int detailBottom = NotebookLayout.pageBottom(height) - DETAIL_CONTENT_BOTTOM_PADDING;
        if (selectedSpeciesId != null && mouseX >= detailLeft && mouseX < detailLeft + pageWidth
                && mouseY >= detailTop && mouseY < detailBottom) {
            NotebookSpecies selectedSpecies = NotebookLayout.resolveSelection(selectedCategory, selectedSpeciesId, progress);
            if (selectedSpecies != null) {
                int maxScroll = detailScrollMax(selectedSpecies, progress, pageWidth - 6,
                        detailBottom - detailTop);
                if (maxScroll > 0) {
                    int delta = (int) Math.signum(scrollY);
                    detailScroll = Mth.clamp(detailScroll - delta * SCROLL_STEP, 0, maxScroll);
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void renderBackdrop(GuiGraphics graphics, int bookLeft, int bookTop) {
        int bookWidth = NotebookLayout.bookWidth(width);
        int bookHeight = NotebookLayout.bookHeight(height);
        graphics.fill(0, 0, width, height, BACKGROUND);
        graphics.fill(bookLeft - 12, bookTop - 12, bookLeft + bookWidth + 12,
                bookTop + bookHeight + 12, 0x18000000);
        graphics.fill(bookLeft - 3, bookTop - 3, bookLeft + bookWidth + 3,
                bookTop + bookHeight + 3, 0x10000000);
    }

    private void drawBook(GuiGraphics graphics, int bookLeft, int bookTop) {
        int bookWidth = NotebookLayout.bookWidth(width);
        int bookHeight = NotebookLayout.bookHeight(height);
        int bookRight = bookLeft + bookWidth;
        int bookBottom = bookTop + bookHeight;
        int spineX = bookLeft + bookWidth / 2;

        graphics.fill(bookLeft, bookTop, bookRight, bookBottom, COVER_DARK);
        graphics.fill(bookLeft + 2, bookTop + 2, bookRight - 2, bookBottom - 2, COVER_MID);
        graphics.fill(bookLeft + 4, bookTop + 4, bookRight - 4, bookBottom - 4, COVER_LIGHT);
        graphics.fill(bookLeft + 7, bookTop + 7, bookRight - 7, bookBottom - 7, PAGE);
        graphics.fill(bookLeft + 7, bookTop + 7, bookRight - 7, bookTop + 42, 0x1BFFFFFF);
        graphics.fill(bookLeft + 7, bookTop + 7, bookRight - 7, bookTop + 9, PAGE_LIGHT);
        graphics.fill(bookLeft + 7, bookBottom - 9, bookRight - 7, bookBottom - 7, PAGE_SHADOW);
        graphics.fill(bookLeft + 7, bookTop + 7, bookLeft + 9, bookBottom - 7, PAGE_LIGHT);
        graphics.fill(bookRight - 9, bookTop + 7, bookRight - 7, bookBottom - 7, COVER_DARK);
        graphics.fill(spineX - 3, bookTop + 12, spineX + 3, bookBottom - 12, COVER_DARK);
        graphics.fill(spineX - 1, bookTop + 12, spineX + 1, bookBottom - 12, COVER_LIGHT);

        drawClampedString(graphics, notebookText("title", "Aquanaut Notebook"), bookLeft + 18, bookTop + 15,
                bookWidth - 36, TITLE_CYAN);
    }

    private void drawTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        for (NotebookLayout.TabBounds tab : NotebookLayout.categoryTabs(width, height)) {
            boolean selected = tab.category() == selectedCategory;
            boolean hovered = contains(tab, mouseX, mouseY);
            int outer = selected ? BOOKMARK_SELECTED : BOOKMARK_DARK;
            int inner = selected ? BOOKMARK_SELECTED_LIGHT : hovered ? BOOKMARK_LIGHT : BOOKMARK;
            int fold = selected ? BOOKMARK_DARK : 0xFF764211;

            graphics.fill(tab.x(), tab.y(), tab.x() + tab.width(), tab.y() + tab.height() - 4, outer);
            graphics.fill(tab.x() + 1, tab.y() + 1, tab.x() + tab.width() - 1, tab.y() + tab.height() - 5, inner);
            graphics.fill(tab.x() + 5, tab.y() + tab.height() - 5, tab.x() + tab.width() - 5, tab.y() + tab.height(),
                    outer);
            graphics.fill(tab.x() + 1, tab.y() + tab.height() - 1, tab.x() + 6, tab.y() + tab.height(), BACKGROUND);
            graphics.fill(tab.x() + tab.width() - 6, tab.y() + tab.height() - 1, tab.x() + tab.width() - 1,
                    tab.y() + tab.height(), BACKGROUND);
            graphics.fill(tab.x(), tab.y() + 3, tab.x() + 2, tab.y() + tab.height() - 8, fold);

            graphics.blitSprite(tab.category().iconSpriteId(), tab.x() + 6, tab.y() + 8, 9, 9);
        }
    }

    private void drawIndex(GuiGraphics graphics, NotebookProgress progress, int mouseX, int mouseY) {
        int left = NotebookLayout.leftPageX(width);
        int top = NotebookLayout.pageY(height);
        int bottom = NotebookLayout.pageBottom(height);
        int pageWidth = NotebookLayout.pageWidth(width);
        int visibleRows = NotebookLayout.indexRowsVisible(height);
        int contentBottom = top + visibleRows * NotebookLayout.INDEX_ROW_HEIGHT;

        graphics.fill(left - 2, top - 2, left + pageWidth + 2, bottom + 2, PAGE_LIGHT);
        graphics.fill(left - 2, top - 2, left + pageWidth + 2, top - 1, PAGE_SHADOW);
        graphics.fill(left - 2, bottom + 1, left + pageWidth + 2, bottom + 2, PAGE_DARK);

        drawClampedString(graphics, notebookText("index", "Research Index"), left,
                top - 14, pageWidth, ACCENT);

        withScissor(graphics, left - 1, top - 1, left + pageWidth + 1, bottom + 1, () -> {
            List<NotebookSpecies> visible = NotebookLayout.visibleSpeciesWindow(selectedCategory, progress, indexScroll,
                    visibleRows);
            if (visible.isEmpty()) {
                drawWrappedString(graphics, notebookText("empty", "No unlocked entries yet."), left + 4, top + 10,
                        pageWidth - 8, TEXT_DIM, 3);
                return;
            }

            for (int row = 0; row < visible.size(); row++) {
                NotebookSpecies species = visible.get(row);
                int rowY = top + 2 + row * NotebookLayout.INDEX_ROW_HEIGHT;
                boolean selected = selectedSpeciesId != null
                        && species.id().equals(NotebookCatalog.canonicalId(selectedSpeciesId));
                boolean hovered = mouseX >= left && mouseX < left + pageWidth
                        && mouseY >= rowY && mouseY < rowY + NotebookLayout.INDEX_ROW_HEIGHT;

                if (selected || hovered) {
                    graphics.fill(left + 1, rowY, left + pageWidth - 1,
                            rowY + NotebookLayout.INDEX_ROW_HEIGHT - 1,
                            selected ? SELECTED : 0x2288A1C5);
                }

                Item iconItem = speciesIcon(species);
                graphics.renderItem(new ItemStack(iconItem), left + 2, rowY - 1);
                drawClampedString(graphics, speciesName(species), left + 20, rowY + 4,
                        pageWidth - 26, selected ? TEXT : TEXT_DIM);
            }
        });

        int maxScroll = Math.max(0, NotebookLayout.visibleSpecies(selectedCategory, progress).size() - visibleRows);
        drawScrollBar(graphics, left + pageWidth - 2, top + 2, contentBottom - top - 2,
                Math.max(visibleRows, NotebookLayout.visibleSpecies(selectedCategory, progress).size())
                        * NotebookLayout.INDEX_ROW_HEIGHT,
                indexScroll, maxScroll);
    }

    private void drawEntry(GuiGraphics graphics, NotebookProgress progress, NotebookSpecies selectedSpecies) {
        int left = NotebookLayout.rightPageX(width);
        int top = NotebookLayout.pageY(height);
        int bottom = NotebookLayout.pageBottom(height);
        int pageWidth = NotebookLayout.pageWidth(width);
        int contentBottom = bottom - DETAIL_CONTENT_BOTTOM_PADDING;
        int contentWidth = pageWidth - 6;
        NotebookBlock leadModelBlock = selectedSpecies == null ? null : leadModelBlock(selectedSpecies, progress);

        graphics.fill(left - 2, top - 2, left + pageWidth + 2, bottom, PAGE_LIGHT);
        graphics.fill(left - 2, top - 2, left + pageWidth + 2, top - 1, PAGE_SHADOW);
        graphics.fill(left - 2, bottom - 1, left + pageWidth + 2, bottom, PAGE_DARK);

        drawClampedString(graphics, notebookText("entry", "Notes"), left,
                top - 14, pageWidth, ACCENT);

        if (selectedSpecies == null) {
            drawWrappedString(graphics, notebookText("no_entry", "Discover a species to open its page."),
                    left + 6, top + 8, pageWidth - 12, TEXT_DIM, 3);
            return;
        }

        int contentTop = top;

        int contentHeight = measureEntryContentHeight(selectedSpecies, progress, contentWidth, leadModelBlock != null);
        int viewportHeight = contentBottom - contentTop;
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        detailScroll = Mth.clamp(detailScroll, 0, maxScroll);

        withScissor(graphics, left - 1, contentTop, left + pageWidth + 1, contentBottom, () -> {
            int cursorY = contentTop - detailScroll;
            if (leadModelBlock != null) {
                cursorY = drawPreview(graphics, selectedSpecies, left + 2, cursorY, contentWidth);
                cursorY += CONTENT_SPACING;
            }
            cursorY = drawProperties(graphics, selectedSpecies, left + 2, cursorY, contentWidth, progress);
            cursorY += CONTENT_SPACING;
            drawDescription(graphics, selectedSpecies, left + 2, cursorY, contentWidth, progress);
        });

        drawScrollBar(graphics, left + pageWidth - 2, contentTop, viewportHeight, contentHeight, detailScroll,
                maxScroll);
    }

    private int drawProperties(GuiGraphics graphics, NotebookSpecies species, int x, int y, int width,
            NotebookProgress progress) {
        int rowY = y;
        int labelWidth = Math.min(84, Math.max(54, width / 2 - 10));
        int gap = 8;
        int valueRight = x + width;
        int valueWidth = Math.max(24, width - labelWidth - gap);
        for (NotebookPropertyKey key : NotebookPropertyKey.values()) {
            Component label = propertyLabel(key);
            Component value = propertyValue(species, key, progress);
            drawClampedString(graphics, label.getString() + ":", x, rowY + 6, labelWidth, PROPERTY_LABEL);
            if (!value.getString().isEmpty()) {
                drawRightAlignedClampedString(graphics, value.getString(), valueRight, rowY + 6, valueWidth,
                        value.getString().equals("???") ? LOCKED : PROPERTY_VALUE);
            }
            graphics.fill(x, rowY + PROPERTY_BLOCK_HEIGHT - 2, x + width, rowY + PROPERTY_BLOCK_HEIGHT - 1,
                    0x1A000000);
            rowY += PROPERTY_BLOCK_HEIGHT;
        }
        return rowY;
    }

    private int drawPreview(GuiGraphics graphics, NotebookSpecies species, int x, int y, int width) {
        int boxHeight = PREVIEW_BOX_HEIGHT + 14;
        int previewWidth = Math.max(24, width);
        graphics.fill(x, y, x + previewWidth, y + boxHeight, 0x22000000);
        graphics.fill(x + 1, y + 1, x + previewWidth - 1, y + boxHeight - 1, 0x33FFFFFF);
        NotebookPreviewRenderer.renderSpeciesPreview(graphics, x + PREVIEW_BOX_INSET, y + PREVIEW_BOX_INSET,
                previewWidth - PREVIEW_BOX_INSET * 2, boxHeight - PREVIEW_BOX_INSET * 2, species);
        return y + boxHeight;
    }

    private void drawDescription(GuiGraphics graphics, NotebookSpecies species, int x, int y, int maxWidth,
            NotebookProgress progress) {
        int cursorY = y;
        boolean drewAny = false;

        for (NotebookBlock block : species.blocks()) {
            if (!progress.hasUnlocked(species.id(), block.stage())) {
                continue;
            }

            if (block.isParagraph()) {
                drewAny = true;
                cursorY = drawParagraph(graphics, block.text(), x, cursorY, maxWidth);
                cursorY += 2;
            }
        }

        if (!drewAny) {
            drawWrappedString(graphics, notebookText("locked", "More field research is required."),
                    x, cursorY + 4, maxWidth, LOCKED, 3);
        }
    }

    private int drawParagraph(GuiGraphics graphics, String text, int x, int y, int maxWidth) {
        List<FormattedCharSequence> lines = this.font.split(localizedText(text), maxWidth);
        for (FormattedCharSequence line : lines) {
            graphics.drawString(this.font, line, x, y, DESCRIPTION_TEXT, false);
            y += 9;
        }
        return y;
    }

    private int measureEntryContentHeight(NotebookSpecies species, NotebookProgress progress, int maxWidth,
            boolean hasLeadPreview) {
        int height = NotebookPropertyKey.values().length * PROPERTY_BLOCK_HEIGHT;
        if (hasLeadPreview) {
            height += PREVIEW_BOX_HEIGHT + 14;
            height += CONTENT_SPACING;
        }
        height += CONTENT_SPACING;

        boolean drewAny = false;
        for (NotebookBlock block : species.blocks()) {
            if (!progress.hasUnlocked(species.id(), block.stage())) {
                continue;
            }

            if (block.isParagraph()) {
                drewAny = true;
                height += this.font.split(localizedText(block.text()), maxWidth).size() * 9;
            }
            height += 2;
        }

        if (!drewAny) {
            height += 13;
        }

        return height;
    }

    private int detailScrollMax(NotebookSpecies species, NotebookProgress progress, int maxWidth, int viewportHeight) {
        return Math.max(0,
                measureEntryContentHeight(species, progress, maxWidth, leadModelBlock(species, progress) != null)
                        - viewportHeight);
    }

    private void drawScrollBar(GuiGraphics graphics, int x, int top, int viewportHeight, int contentHeight,
            int currentScroll, int maxScroll) {
        if (maxScroll <= 0 || contentHeight <= 0) {
            return;
        }

        int trackHeight = viewportHeight;
        int thumbHeight = Math.max(12, Math.round((float) viewportHeight * viewportHeight / contentHeight));
        int thumbTravel = Math.max(0, trackHeight - thumbHeight);
        int thumbY = top + Math.round(thumbTravel * (currentScroll / (float) maxScroll));

        graphics.fill(x, top, x + 1, top + trackHeight, 0x2E5C6778);
        graphics.fill(x, thumbY, x + 1, thumbY + thumbHeight, 0xFF6D8AB6);
    }

    private void drawTabTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        for (NotebookLayout.TabBounds tab : NotebookLayout.categoryTabs(width, height)) {
            if (!contains(tab, mouseX, mouseY)) {
                continue;
            }

            graphics.renderTooltip(this.font, List.of(
                    categoryText(tab.category()),
                    tabTooltip(tab.category())), Optional.empty(), mouseX, mouseY);
            return;
        }
    }

    private void drawClampedString(GuiGraphics graphics, Component component, int x, int y, int maxWidth,
            int color) {
        drawClampedString(graphics, component.getString(), x, y, maxWidth, color);
    }

    private int drawWrappedString(GuiGraphics graphics, Component component, int x, int y, int maxWidth, int color,
            int maxLines) {
        return drawWrappedString(graphics, component.getString(), x, y, maxWidth, color, maxLines);
    }

    private int drawWrappedString(GuiGraphics graphics, String text, int x, int y, int maxWidth, int color,
            int maxLines) {
        if (maxWidth <= 0 || text.isEmpty() || maxLines <= 0) {
            return y;
        }

        List<FormattedCharSequence> lines = this.font.split(Component.literal(text), maxWidth);
        int linesToDraw = Math.min(lines.size(), maxLines);
        for (int index = 0; index < linesToDraw; index++) {
            graphics.drawString(this.font, lines.get(index), x, y, color, false);
            y += 9;
        }
        return y;
    }

    private void drawClampedString(GuiGraphics graphics, String text, int x, int y, int maxWidth, int color) {
        if (maxWidth <= 0 || text.isEmpty()) {
            return;
        }

        String rendered = text;
        int textWidth = this.font.width(text);
        if (textWidth > maxWidth) {
            int ellipsisWidth = this.font.width("...");
            if (maxWidth <= ellipsisWidth) {
                rendered = this.font.plainSubstrByWidth(text, maxWidth);
            } else {
                rendered = this.font.plainSubstrByWidth(text, maxWidth - ellipsisWidth) + "...";
            }
        }

        graphics.drawString(this.font, rendered, x, y, color, false);
    }

    private Component localizedText(String textOrKey) {
        Component translated = Component.translatable(textOrKey);
        return translated.getString().equals(textOrKey) ? Component.literal(textOrKey) : translated;
    }

    private void drawRightAlignedClampedString(GuiGraphics graphics, String text, int rightX, int y, int maxWidth,
            int color) {
        if (maxWidth <= 0 || text.isEmpty()) {
            return;
        }

        String rendered = text;
        int textWidth = this.font.width(text);
        if (textWidth > maxWidth) {
            int ellipsisWidth = this.font.width("...");
            if (maxWidth <= ellipsisWidth) {
                rendered = this.font.plainSubstrByWidth(text, maxWidth);
            } else {
                rendered = this.font.plainSubstrByWidth(text, maxWidth - ellipsisWidth) + "...";
            }
        }

        graphics.drawString(this.font, rendered, rightX - this.font.width(rendered), y, color, false);
    }

    private void withScissor(GuiGraphics graphics, int left, int top, int right, int bottom, Runnable draw) {
        graphics.enableScissor(left, top, right, bottom);
        try {
            draw.run();
        } finally {
            graphics.disableScissor();
        }
    }

    private NotebookBlock leadModelBlock(NotebookSpecies species, NotebookProgress progress) {
        for (NotebookBlock block : species.blocks()) {
            if (!progress.hasUnlocked(species.id(), block.stage())) {
                continue;
            }
            if (block.isComponent() && block.componentKind() == NotebookComponentKind.MODEL) {
                return block;
            }
        }
        return null;
    }

    private NotebookSpecies ensureSelection(NotebookProgress progress) {
        indexScroll = NotebookLayout.clampScroll(indexScroll, selectedCategory, progress,
                NotebookLayout.indexRowsVisible(height));
        NotebookSpecies selected = NotebookLayout.resolveSelection(selectedCategory, selectedSpeciesId, progress);
        if (selected == null) {
            detailScroll = 0;
            return null;
        }
        selectedSpeciesId = selected.id();
        detailScroll = Math.max(0, detailScroll);
        return selected;
    }

    private void selectCategory(NotebookCategory category, NotebookProgress progress) {
        selectedCategory = category;
        indexScroll = 0;
        detailScroll = 0;
        NotebookSpecies first = NotebookLayout.firstVisibleSpecies(category, progress);
        selectedSpeciesId = first == null ? null : first.id();
    }

    private Component propertyValue(NotebookSpecies species, NotebookPropertyKey key, NotebookProgress progress) {
        if (key.isPlaceholder()) {
            return Component.empty();
        }

        if (!progress.hasUnlocked(species.id(), key.unlockStage())) {
            return Component.literal("???");
        }

        return switch (key) {
            case MAX_HEALTH -> Component.literal(formatNumber(maxHealth(species)));
            case DIET -> dietText(species.diet());
            case SIZE -> Component.literal(formatSize(species));
            case EXCREMENT, VARIANT_COUNT -> Component.empty();
        };
    }

    private float maxHealth(NotebookSpecies species) {
        LivingEntity entity = NotebookPreviewRenderer.previewEntity(species);
        return entity == null ? 0.0F : entity.getMaxHealth();
    }

    private String formatSize(NotebookSpecies species) {
        LivingEntity entity = NotebookPreviewRenderer.previewEntity(species);
        if (entity == null) {
            return "";
        }
        if (entity instanceof AquariumFish fish) {
            return formatNumber(fish.getAquariumModelLength()) + " x "
                    + formatNumber(fish.getAquariumModelWidth()) + " x "
                    + formatNumber(fish.getAquariumModelHeight());
        }
        return formatNumber(entity.getBbWidth()) + " x " + formatNumber(entity.getBbWidth()) + " x "
                + formatNumber(entity.getBbHeight());
    }

    private String formatNumber(float value) {
        if (Mth.equal(value, Math.round(value))) {
            return Integer.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private Item speciesIcon(NotebookSpecies species) {
        ResourceLocation iconId = ResourceLocation.fromNamespaceAndPath(species.id().getNamespace(),
                species.id().getPath() + "_spawn_egg");
        Item item = BuiltInRegistries.ITEM.get(iconId);
        return item == null ? Items.AIR : item;
    }

    private Component notebookText(String key, String fallback) {
        return translatedOrFallback("gui.aquanaut.notebook." + key, fallback);
    }

    private Component speciesName(NotebookSpecies species) {
        return translatedOrFallback(species.translationKey(), humanize(species.id().getPath()));
    }

    private Component categoryText(NotebookCategory category) {
        return translatedOrFallback(category.translationKey(), humanize(category.serializedName()));
    }

    private Component dietText(NotebookDiet diet) {
        return translatedOrFallback(diet.translationKey(), switch (diet) {
            case HERBIVOROUS -> "Herbivorous";
            case CARNIVOROUS -> "Carnivorous";
            case ENERGEVOROUS -> "Energivorous";
        });
    }

    private Component propertyLabel(NotebookPropertyKey key) {
        return translatedOrFallback(key.translationKey(), switch (key) {
            case MAX_HEALTH -> "Max Health";
            case EXCREMENT -> "Excrement";
            case DIET -> "Diet";
            case SIZE -> "Size";
            case VARIANT_COUNT -> "Variant Count";
        });
    }

    private Component tabTooltip(NotebookCategory category) {
        String key = "gui.aquanaut.notebook.tab." + category.serializedName();
        return translatedOrFallback(key, switch (category) {
            case FRIENDLY -> "Peaceful species and survey notes";
            case THREATENING -> "Predators and hazardous species";
            case TITAN -> "Titan-class encounters";
        });
    }

    private Component translatedOrFallback(String translationKey, String fallback) {
        Component translated = Component.translatable(translationKey);
        return translated.getString().equals(translationKey) ? Component.literal(fallback) : translated;
    }

    private String humanize(String value) {
        String[] parts = value.split("_");
        StringBuilder builder = new StringBuilder(value.length());
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.isEmpty() ? value : builder.toString();
    }

    private static boolean contains(NotebookLayout.TabBounds tab, double mouseX, double mouseY) {
        return mouseX >= tab.x() && mouseX < tab.x() + tab.width()
                && mouseY >= tab.y() && mouseY < tab.y() + tab.height();
    }
}
