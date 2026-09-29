package com.dexer.aquanaut.client.screen;

import java.util.Optional;

import com.dexer.aquanaut.client.photo.PhotoTextureCache;
import com.dexer.aquanaut.common.inventory.PhotoRinsingMenu;
import com.dexer.aquanaut.common.item.DevelopedPhotoItem.PhotoData;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The rinsing basin menu, drawn on the stock crafting-table container panel.
 *
 * <p>
 * The background is {@code textures/gui/container/crafting_table.png} blitted whole, so the border
 * is vanilla chrome byte for byte: the 1px black outline, the two-pixel white highlight down the
 * top and left, the matching {@code #555555} shadow down the bottom and right, and the
 * alpha-rounded corners. The crafting grid, arrow and result well baked into that texture are then
 * painted out with its own panel grey, leaving the border and the player-inventory wells intact.
 *
 * <p>
 * Wells are stamped for every slot the menu owns. The vanilla {@code container/slot} sprite was
 * verified pixel-identical to the baked well, so the input and output slots match the player
 * inventory exactly and the layout cannot drift out of step with where vanilla draws the items.
 *
 * <p>
 * The one piece of storytelling is the developer tray in the middle: the latent image from the
 * film in the input slot comes up top-down as the rinse advances, with a bright fixer line on the
 * boundary and slow bubbles rising through the chemical, so the progress is the photograph itself.
 */
public final class PhotoRinsingScreen extends AbstractContainerScreen<PhotoRinsingMenu> {
    /** Vanilla container panel, the same one the crafting table and chest use. 256x256 sheet. */
    private static final ResourceLocation PANEL_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/crafting_table.png");

    private static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot");

    /** Panel grey, sampled straight from the vanilla sheet. */
    private static final int PANEL = 0xFFC6C6C6;
    private static final int INSET_DARK = 0xFF373737;
    private static final int INSET_FIELD = 0xFF8B8B8B;
    private static final int BEVEL_LIGHT = 0xFFFFFFFF;
    private static final int LABEL = 0xFF404040;

    /** The developer tray. */
    private static final int TRAY_DARK = 0xFF10262B;
    private static final int TRAY_DEEP = 0xFF08161A;
    private static final int FIXER_LINE = 0xFFB9E6DE;
    private static final int BUBBLE = 0x669FD8D2;

    /** Tray rectangle inside the panel, kept clear of the border and the machine slots. */
    private static final int TRAY_X = 48;
    private static final int TRAY_Y = 20;
    private static final int TRAY_W = 80;
    private static final int TRAY_H = 45;

    /**
     * The machine band that replaces the crafting table's own furniture. Well inside the border,
     * which occupies x 0..2 / 173..175 and y 0..2 / 163..165.
     */
    private static final int MACHINE_LEFT = 6;
    private static final int MACHINE_TOP = 15;
    private static final int MACHINE_RIGHT = 170;
    private static final int MACHINE_BOTTOM = 73;

    public PhotoRinsingScreen(PhotoRinsingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Vanilla chrome, border included.
        graphics.blit(PANEL_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Paint out the crafting grid, arrow and result well with the panel's own grey.
        graphics.fill(x + MACHINE_LEFT, y + MACHINE_TOP, x + MACHINE_RIGHT, y + MACHINE_BOTTOM, PANEL);

        drawDevelopingTray(graphics, x + TRAY_X, y + TRAY_Y, TRAY_W, TRAY_H);
        drawSlotWells(graphics);
    }

    /** One well per slot, using the vanilla sprite so it matches the panel's baked wells. */
    private void drawSlotWells(GuiGraphics graphics) {
        for (Slot slot : this.menu.slots) {
            graphics.blitSprite(SLOT_SPRITE, this.leftPos + slot.x - 1, this.topPos + slot.y - 1, 18, 18);
        }
    }

    /**
     * The developer tray. The latent image is uncovered from the top as the rinse advances; a
     * fixer highlight rides the boundary and bubbles drift up through the chemical.
     */
    private void drawDevelopingTray(GuiGraphics graphics, int x, int y, int width, int height) {
        drawInset(graphics, x - 1, y - 1, width + 2, height + 2);
        graphics.fill(x, y, x + width, y + height, TRAY_DEEP);

        Optional<PhotoData> photo = this.menu.filmPhoto();
        float progress = this.menu.progressFraction();
        boolean showedImage = false;

        if (photo.isPresent() && this.minecraft != null) {
            PhotoTextureCache.Cached cached = PhotoTextureCache.get(this.minecraft, photo.get());
            if (cached != null) {
                int revealed = Mth.ceil(height * progress);
                graphics.enableScissor(x, y, x + width, y + revealed);
                graphics.blit(cached.location(), x, y, width, height,
                        0.0F, 0.0F, cached.width(), cached.height(), cached.width(), cached.height());
                graphics.disableScissor();

                // Chemical still sitting over the not-yet-fixed part of the sheet.
                if (revealed < height) {
                    graphics.fill(x, y + revealed, x + width, y + height, 0xB010262B);
                    graphics.fill(x, y + revealed - 1, x + width, y + revealed + 1, FIXER_LINE);
                    graphics.fill(x, y + revealed + 1, x + width, y + revealed + 2, 0x559FD8D2);
                }
                showedImage = true;
            }
        }

        if (!showedImage) {
            graphics.fill(x, y, x + width, y + height / 2, TRAY_DARK);
            graphics.fill(x, y + height / 2, x + width, y + height, TRAY_DEEP);
        }

        drawBubbles(graphics, x, y, width, height, progress);
    }

    /** The vanilla inset look, matching the slot wells: dark top/left, light bottom/right. */
    private static void drawInset(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, INSET_FIELD);
        graphics.fill(x, y, x + width, y + 1, INSET_DARK);
        graphics.fill(x, y, x + 1, y + height, INSET_DARK);
        graphics.fill(x, y + height - 1, x + width, y + height, BEVEL_LIGHT);
        graphics.fill(x + width - 1, y, x + width, y + height, BEVEL_LIGHT);
    }

    /** Slow bubbles: three columns drifting upward, faster while the rinse is running. */
    private static void drawBubbles(GuiGraphics graphics, int x, int y, int width, int height, float progress) {
        long time = Util.getMillis();
        float speed = 0.018F + progress * 0.022F;
        for (int i = 0; i < 3; i++) {
            float phase = (time * speed + i * 0.37F) % 1.0F;
            int bx = x + 10 + i * (width - 22) / 2;
            int by = y + height - 4 - Mth.floor(phase * (height - 9));
            int size = 1 + (i & 1);
            graphics.fill(bx, by, bx + size, by + size, BUBBLE);
            if (size > 1) {
                graphics.fill(bx, by, bx + 1, by + 1, 0x88C6EFEA);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, LABEL, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                LABEL, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
