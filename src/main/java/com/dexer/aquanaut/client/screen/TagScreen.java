package com.dexer.aquanaut.client.screen;

import com.dexer.aquanaut.common.entity.AbstractTaggableEntity;
import com.dexer.aquanaut.common.entity.TagEditor;
import com.dexer.aquanaut.common.entity.TagRules;
import com.dexer.aquanaut.network.TagPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * The tag editor: pick a colour, type a name, and see exactly what the nameplate will look like
 * before you commit to it.
 *
 * <p>
 * Deliberately small and vanilla-shaped — a title, a live preview on a plate, one text field, a grid
 * of swatches and two buttons — because this is a two-field form, not a destination. The palette and
 * the preview plate echo the markers' own instrument colours so the panel reads as part of the same
 * object rather than a bolted-on dialog.
 *
 * <p>
 * The screen only ever <em>proposes</em> a tag: it sends the request and closes. Duplicate names are
 * flagged live against the markers the client knows about, but the server re-checks every rule, so
 * that hint is convenience rather than enforcement.
 */
public class TagScreen extends Screen {

    private static final int PANEL_WIDTH = 196;
    private static final int PANEL_HEIGHT = 167;
    private static final int PADDING = 12;
    private static final int INNER_WIDTH = PANEL_WIDTH - 2 * PADDING;

    private static final int SWATCH_COLUMNS = 6;
    private static final int SWATCH_SIZE = 16;
    /** Wide enough that a selected swatch's two-ring outline still clears its neighbours. */
    private static final int SWATCH_GAP = 6;
    private static final int GRID_WIDTH = SWATCH_COLUMNS * SWATCH_SIZE + (SWATCH_COLUMNS - 1) * SWATCH_GAP;

    private static final int PANEL = 0xF00E1216;
    private static final int PANEL_EDGE = 0xFF3A4045;
    private static final int PANEL_HIGHLIGHT = 0xFF585F65;
    private static final int PLATE = 0xFF05080A;
    private static final int TITLE = 0xFFE8EFED;
    private static final int LABEL = 0xFF9AA6B2;
    private static final int ACCENT = 0xFFC9A24E;
    private static final int WARNING = 0xFFE8705C;

    private static final int TITLE_Y = 10;
    private static final int PREVIEW_Y = 26;
    private static final int PREVIEW_HEIGHT = 24;
    private static final int LABEL_Y = 56;
    private static final int FIELD_Y = 67;
    private static final int FIELD_HEIGHT = 18;
    private static final int GRID_Y = 93;
    private static final int BUTTON_Y = 135;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 82;

    private final int entityId;
    private final String initialName;

    private EditBox nameField;
    private int selectedColor;
    private boolean nameTaken;
    private int panelLeft;
    private int panelTop;

    public TagScreen(int entityId, String currentName, int currentColor) {
        super(Component.translatable("gui.aquanaut.tag.title"));
        this.entityId = entityId;
        this.initialName = currentName;
        this.selectedColor = currentColor;
    }

    /** Installed by the client during setup; keeps the entity out of client-only code. */
    public static void install() {
        TagEditor.install(TagScreen::openFor);
    }

    private static void openFor(AbstractTaggableEntity entity) {
        Minecraft.getInstance().setScreen(
                new TagScreen(entity.getId(), entity.getTagName(), entity.getTagColor()));
    }

    @Override
    protected void init() {
        super.init();
        panelLeft = (width - PANEL_WIDTH) / 2;
        panelTop = (height - PANEL_HEIGHT) / 2;

        nameField = new EditBox(font, panelLeft + PADDING, panelTop + FIELD_Y,
                INNER_WIDTH, FIELD_HEIGHT, Component.translatable("gui.aquanaut.tag.name"));
        nameField.setMaxLength(TagRules.MAX_NAME_LENGTH);
        nameField.setValue(initialName);
        nameField.setResponder(value -> nameTaken = isNameTakenLocally(value));
        addRenderableWidget(nameField);
        setInitialFocus(nameField);
        nameTaken = isNameTakenLocally(initialName);

        int buttonLeft = panelLeft + PADDING;
        addRenderableWidget(Button.builder(Component.translatable("gui.aquanaut.tag.apply"),
                button -> applyAndClose())
                .bounds(buttonLeft, panelTop + BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"),
                button -> onClose())
                .bounds(buttonLeft + INNER_WIDTH - BUTTON_WIDTH, panelTop + BUTTON_Y,
                        BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    /**
     * The panel is painted from {@code renderBackground} on purpose. {@code Screen.render} draws the
     * background first and the widgets on top of it, so a panel drawn from {@code render} would sit
     * over the text field and the buttons.
     */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        int left = panelLeft;
        int top = panelTop;
        int right = left + PANEL_WIDTH;
        int bottom = top + PANEL_HEIGHT;
        int innerLeft = left + PADDING;
        int centerX = left + PANEL_WIDTH / 2;

        graphics.fill(left, top, right, bottom, PANEL);
        graphics.fill(left, top, right, top + 1, PANEL_EDGE);
        graphics.fill(left, bottom - 1, right, bottom, PANEL_EDGE);
        graphics.fill(left, top, left + 1, bottom, PANEL_EDGE);
        graphics.fill(right - 1, top, right, bottom, PANEL_EDGE);
        graphics.fill(innerLeft, top + TITLE_Y + 12, innerLeft + INNER_WIDTH, top + TITLE_Y + 13, ACCENT);

        graphics.drawCenteredString(font, title, centerX, top + TITLE_Y, TITLE);

        drawPreview(graphics, innerLeft, top + PREVIEW_Y, centerX);

        graphics.drawString(font, Component.translatable("gui.aquanaut.tag.name"),
                innerLeft, top + LABEL_Y, LABEL, false);
        if (nameTaken) {
            Component warning = Component.translatable("gui.aquanaut.tag.taken");
            graphics.drawString(font, warning,
                    innerLeft + INNER_WIDTH - font.width(warning), top + LABEL_Y, WARNING, false);
        }

        drawSwatches(graphics, mouseX, mouseY);
    }

    /** The plate shows the real label in the real colour, so there is nothing to imagine. */
    private void drawPreview(GuiGraphics graphics, int innerLeft, int previewTop, int centerX) {
        int innerRight = innerLeft + INNER_WIDTH;
        int previewBottom = previewTop + PREVIEW_HEIGHT;
        int color = TagRules.toArgb(selectedColor);
        graphics.fill(innerLeft, previewTop, innerRight, previewBottom, PLATE);
        graphics.fill(innerLeft, previewTop, innerRight, previewTop + 1, color);
        graphics.fill(innerLeft, previewBottom - 1, innerRight, previewBottom, color);
        graphics.fill(innerLeft, previewTop, innerLeft + 1, previewBottom, color);
        graphics.fill(innerRight - 1, previewTop, innerRight, previewBottom, color);

        String preview = TagRules.normalizeName(nameField.getValue());
        if (preview.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("gui.aquanaut.tag.empty"),
                    centerX, previewTop + 8, LABEL);
        } else {
            graphics.drawCenteredString(font, preview, centerX, previewTop + 8, color);
        }
    }

    private void drawSwatches(GuiGraphics graphics, int mouseX, int mouseY) {
        int hovered = swatchAt(mouseX, mouseY);
        for (int i = 0; i < TagRules.TAG_COLORS.length; i++) {
            int x = swatchX(i);
            int y = swatchY(i);
            // Compare on the palette value and paint with the ARGB one: selectedColor is stored the
            // way the palette and the network use it, so the two are not interchangeable.
            int palette = TagRules.TAG_COLORS[i];
            graphics.fill(x, y, x + SWATCH_SIZE, y + SWATCH_SIZE, TagRules.toArgb(palette));

            if (palette == selectedColor) {
                // Outlines, not fills: filling the swatch's rectangle here would paint over the very
                // colour it is supposed to be marking. Black outside, white inside, so the ring
                // reads against a swatch of any colour.
                strokeRect(graphics, x - 2, y - 2, x + SWATCH_SIZE + 2, y + SWATCH_SIZE + 2, 1, 0xFF000000);
                strokeRect(graphics, x - 1, y - 1, x + SWATCH_SIZE + 1, y + SWATCH_SIZE + 1, 1, 0xFFFFFFFF);
            } else if (i == hovered) {
                strokeRect(graphics, x - 1, y - 1, x + SWATCH_SIZE + 1, y + SWATCH_SIZE + 1, 1, PANEL_HIGHLIGHT);
            } else {
                graphics.fill(x, y, x + SWATCH_SIZE, y + 1, PANEL_HIGHLIGHT);
                graphics.fill(x, y + SWATCH_SIZE - 1, x + SWATCH_SIZE, y + SWATCH_SIZE, PANEL_EDGE);
                graphics.fill(x, y, x + 1, y + SWATCH_SIZE, PANEL_HIGHLIGHT);
                graphics.fill(x + SWATCH_SIZE - 1, y, x + SWATCH_SIZE, y + SWATCH_SIZE, PANEL_EDGE);
            }
        }
    }

    /**
     * Draws a rectangular outline. {@code GuiGraphics} only has a filled rectangle, so an outline is
     * four thin fills — using a single {@code fill} for a "ring" silently paints a solid block.
     */
    private static void strokeRect(GuiGraphics graphics, int left, int top, int right, int bottom,
            int thickness, int color) {
        graphics.fill(left, top, right, top + thickness, color);
        graphics.fill(left, bottom - thickness, right, bottom, color);
        graphics.fill(left, top + thickness, left + thickness, bottom - thickness, color);
        graphics.fill(right - thickness, top + thickness, right, bottom - thickness, color);
    }

    private int swatchX(int index) {
        int gridLeft = panelLeft + PADDING + (INNER_WIDTH - GRID_WIDTH) / 2;
        return gridLeft + (index % SWATCH_COLUMNS) * (SWATCH_SIZE + SWATCH_GAP);
    }

    private int swatchY(int index) {
        return panelTop + GRID_Y + (index / SWATCH_COLUMNS) * (SWATCH_SIZE + SWATCH_GAP);
    }

    private int swatchAt(double mouseX, double mouseY) {
        for (int i = 0; i < TagRules.TAG_COLORS.length; i++) {
            int x = swatchX(i);
            int y = swatchY(i);
            if (mouseX >= x && mouseX < x + SWATCH_SIZE && mouseY >= y && mouseY < y + SWATCH_SIZE) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int swatch = swatchAt(mouseX, mouseY);
            if (swatch >= 0) {
                selectedColor = TagRules.TAG_COLORS[swatch];
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean enter = keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER;
        if (enter && nameField.isFocused()) {
            applyAndClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void applyAndClose() {
        String name = TagRules.normalizeName(nameField.getValue());
        if (!TagRules.isValidName(name) || nameTaken) {
            // Refuse locally too, so an obviously bad tag never leaves the client.
            nameField.setFocused(true);
            return;
        }
        PacketDistributor.sendToServer(new TagPayload(entityId, name, selectedColor));
        onClose();
    }

    /**
     * Best-effort duplicate hint from the markers the client currently knows about. The server has
     * the final say, so a miss here is corrected rather than tolerated.
     */
    private boolean isNameTakenLocally(String rawName) {
        String name = TagRules.normalizeName(rawName);
        if (name.isEmpty()) {
            return false;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.level() == null) {
            return false;
        }
        AABB around = player.getBoundingBox().inflate(256.0D);
        for (AbstractTaggableEntity other : player.level().getEntitiesOfClass(AbstractTaggableEntity.class, around)) {
            if (other.getId() != entityId && TagRules.sameName(other.getTagName(), name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
