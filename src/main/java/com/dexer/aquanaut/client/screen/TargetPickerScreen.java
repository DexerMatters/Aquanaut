package com.dexer.aquanaut.client.screen;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;

/**
 * A list of things a held item can be pointed at, chosen by holding the right button and releasing on
 * the entry you want.
 *
 * <p>
 * This is a real {@link Screen} rather than a HUD overlay, and it has to be: during normal play the
 * mouse is <em>grabbed</em>, so the mouse position never changes and an overlay has nothing to select
 * with. Opening a screen releases the mouse, which is the whole mechanism behind "move the mouse to
 * choose".
 *
 * <p>
 * Two gestures share it, because they are the same gesture: the submarine compass picks a tagged
 * marker to point at, and the drone controller picks a drone to link to. They differ only in what is
 * listed and what the choice is sent as, which is exactly the pair of things the caller supplies —
 * {@link Source} says whose list it is (so each opener closes only its own), and the confirm callback
 * does the sending.
 *
 * <p>
 * That a choice has one consequence worth knowing about: while a screen is open the game stops running
 * {@code Minecraft.handleKeybinds} and {@code MouseHandler} releases every key binding, so an item
 * that was being held stops being held. The compass opens this list in the middle of an item-use hold
 * and would otherwise stay stuck in its "using" pose forever, so {@link #removed()} releases it
 * explicitly. For the controller — which opens the list from a raw button and holds no item use — that
 * is a no-op.
 *
 * <p>
 * The layout is a plain centred list, because a radial wheel only looks well when it is full: with the
 * usual two or three entries it flings them to opposite edges of the screen and buries the world
 * behind a large plate. Each row is a colour chip, the entry's name, and — when the list mixes kinds —
 * the kind it is right-aligned, so "which of these is the drone" is answered without reading the whole
 * list.
 */
public class TargetPickerScreen extends Screen {

    /** Whose list this is. Each opener only ever opens — and only ever closes — its own. */
    public enum Source {
        COMPASS,
        CONTROLLER
    }

    /**
     * One entry in the list.
     *
     * @param id    what to report when this entry is chosen, or {@code null} for the compass's
     *              "nearest" row
     * @param label the entry's own name
     * @param color the colour it is written in
     * @param kind  a short word for what kind of thing it is, or {@code null} to leave the kind
     *              column out of the whole list
     */
    public record Option(@Nullable UUID id, Component label, int color, @Nullable Component kind) {
    }

    private static final int ROW_HEIGHT = 13;
    private static final int HEADER_HEIGHT = 15;
    private static final int FOOTER_HEIGHT = 12;
    private static final int LIST_TOP_PAD = 3;
    private static final int LIST_BOTTOM_PAD = 3;
    private static final int CHIP_SIZE = 6;
    private static final int CHIP_LEFT_PAD = 5;
    private static final int TEXT_GAP = 5;
    /** Gap between an entry's name and the kind written after it. */
    private static final int KIND_GAP = 8;
    private static final int RIGHT_PAD = 8;
    private static final int MIN_PANEL_WIDTH = 108;
    private static final int MAX_PANEL_WIDTH = 210;

    private static final int BACKDROP = 0x99000000;
    private static final int PANEL = 0xF00E1216;
    private static final int PANEL_EDGE = 0xFF3A4045;
    private static final int ACCENT = 0xFFC9A24E;
    private static final int ROW_ON = 0xFF212A31;
    private static final int ROW_EDGE = 0xFF585F65;
    private static final int TEXT = 0xFFE8EFED;
    private static final int TEXT_DIM = 0xFF9AA6B2;

    /** Colour of the compass's "nearest" entry, which has no tag of its own. */
    public static final int NEAREST_COLOR = 0xFFC9A24E;

    private final Source source;
    private final Component hint;
    private final List<Option> options;
    private final Consumer<UUID> onConfirm;
    private final boolean showKinds;
    private final int initialHighlighted;

    private int highlighted;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;

    /**
     * @param onConfirm handed the chosen id, or {@code null} when the "nearest" row was chosen
     */
    public TargetPickerScreen(Source source, Component title, Component hint, List<Option> options,
            int initialHighlighted, Consumer<UUID> onConfirm) {
        super(title);
        this.source = source;
        this.hint = hint;
        this.options = List.copyOf(options);
        this.onConfirm = onConfirm;
        // The column appears only when the list actually mixes kinds; a list of drones does not need
        // to say "drone" ten times.
        this.showKinds = options.stream().anyMatch(option -> option.kind() != null);
        this.initialHighlighted = Math.max(0, Math.min(initialHighlighted, options.size() - 1));
        this.highlighted = this.initialHighlighted;
    }

    public Source source() {
        return this.source;
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = panelWidth(font);
        panelHeight = HEADER_HEIGHT + LIST_TOP_PAD + options.size() * ROW_HEIGHT
                + LIST_BOTTOM_PAD + FOOTER_HEIGHT;
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** A plain dim: this is an in-game gesture, so it should not blur the world like a menu. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, BACKDROP);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        updateHighlight(mouseY);
        drawPanel(graphics);
    }

    /** Hovering a row chooses it; moving off the list keeps the last choice instead of clearing it. */
    private void updateHighlight(double mouseY) {
        int first = listTop();
        int row = (int) Math.floor((mouseY - first) / ROW_HEIGHT);
        if (mouseY < first || row < 0 || row >= options.size()) {
            return;
        }
        highlighted = row;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // Releasing the right button is the gesture that opened the list, so it confirms.
        if (button == 1) {
            commitAndClose();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // A left click also confirms, for anyone who lets go of the right button first.
        if (button == 0) {
            commitAndClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void commitAndClose() {
        if (!options.isEmpty()) {
            onConfirm.accept(options.get(Math.floorMod(highlighted, options.size())).id());
        }
        onClose();
    }

    /**
     * Ends the compass's "using" hold on the way out. Vanilla would normally do this from
     * {@code handleKeybinds}, but that does not run while a screen is open and the item-use state is
     * left stale, so the hold has to be cancelled here or the compass never lowers. The controller
     * holds no item use, and this quietly does nothing for it.
     */
    @Override
    public void removed() {
        super.removed();
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player != null && player.isUsingItem() && minecraft.gameMode != null) {
            minecraft.gameMode.releaseUsingItem(player);
        }
    }

    // ------------------------------------------------------------------
    // layout
    // ------------------------------------------------------------------

    private int listTop() {
        return panelTop + HEADER_HEIGHT + LIST_TOP_PAD;
    }

    /**
     * Wide enough for the longest thing the panel has to draw — a row, its kind, the title or the
     * footer hint — so nothing is ever squeezed or spills past the frame.
     */
    private int panelWidth(Font font) {
        int overNames = 0;
        int overKinds = 0;
        for (Option option : options) {
            overNames = Math.max(overNames, font.width(option.label()));
            if (option.kind() != null) {
                overKinds = Math.max(overKinds, font.width(option.kind()));
            }
        }
        int overHeadings = Math.max(font.width(title), font.width(hint));

        int overRows = CHIP_LEFT_PAD + CHIP_SIZE + TEXT_GAP + overNames;
        if (showKinds) {
            overRows += KIND_GAP + overKinds;
        }
        int needed = Math.max(overRows, CHIP_LEFT_PAD + 1 + overHeadings);
        return Mth.clamp(needed + RIGHT_PAD, MIN_PANEL_WIDTH, MAX_PANEL_WIDTH);
    }

    // ------------------------------------------------------------------
    // drawing
    // ------------------------------------------------------------------

    private void drawPanel(GuiGraphics graphics) {
        int left = panelLeft;
        int top = panelTop;
        int right = left + panelWidth;
        int bottom = top + panelHeight;

        graphics.fill(left, top, right, bottom, PANEL);
        graphics.fill(left, top, right, top + 1, PANEL_EDGE);
        graphics.fill(left, bottom - 1, right, bottom, PANEL_EDGE);
        graphics.fill(left, top, left + 1, bottom, PANEL_EDGE);
        graphics.fill(right - 1, top, right, bottom, PANEL_EDGE);

        graphics.drawString(font, title, left + CHIP_LEFT_PAD + 1, top + 4, TEXT, false);
        graphics.fill(left + 1, top + HEADER_HEIGHT - 1, right - 1, top + HEADER_HEIGHT, ACCENT);

        int listTop = listTop();
        for (int i = 0; i < options.size(); i++) {
            drawRow(graphics, listTop + i * ROW_HEIGHT, i);
        }

        graphics.drawString(font, hint, left + CHIP_LEFT_PAD + 1, bottom - FOOTER_HEIGHT + 2, TEXT_DIM,
                false);
    }

    private void drawRow(GuiGraphics graphics, int rowTop, int index) {
        Option option = options.get(index);
        boolean selected = index == highlighted;
        int rowBottom = rowTop + ROW_HEIGHT - 1;
        int left = panelLeft;
        int right = panelLeft + panelWidth;

        if (selected) {
            graphics.fill(left + 1, rowTop, right - 1, rowBottom, ROW_ON);
            graphics.fill(left + 1, rowTop, right - 1, rowTop + 1, ROW_EDGE);
            graphics.fill(left + 1, rowBottom - 1, right - 1, rowBottom, ROW_EDGE);
        }

        int chipTop = rowTop + (ROW_HEIGHT - 1 - CHIP_SIZE) / 2;
        int chipLeft = left + CHIP_LEFT_PAD;
        graphics.fill(chipLeft, chipTop, chipLeft + CHIP_SIZE, chipTop + CHIP_SIZE, option.color());

        graphics.drawString(font, option.label(), chipLeft + CHIP_SIZE + TEXT_GAP, rowTop + 3,
                selected ? TEXT : TEXT_DIM, false);

        if (showKinds && option.kind() != null) {
            // Flush right, so the kinds line up in a column however long the names are.
            graphics.drawString(font, option.kind(), right - RIGHT_PAD - font.width(option.kind()),
                    rowTop + 3, TEXT_DIM, false);
        }
    }
}
