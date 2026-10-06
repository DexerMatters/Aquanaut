package com.dexer.aquanaut.client.screen;

import com.dexer.aquanaut.client.ClientAquariumData;
import com.dexer.aquanaut.client.renderer.AquariumPreviewRenderer;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumContainerMenu;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumFishEntry;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumFishSpec;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumInventoryData;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumInventoryHelper;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumPlacementMath;
import com.dexer.aquanaut.network.AquariumFishTransferPayload;
import com.dexer.aquanaut.network.CloseAquariumPayload;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The aquarium: the second page of the player inventory.
 *
 * <p>The panel is built exactly like a vanilla container - black outline, two pixel light bevel down
 * the top and left, matching dark bevel down the bottom and right, and the same 18 pixel slot wells
 * in the same places as the player inventory - so the only thing that reads as new is the tank that
 * took over the crafting area. The tabs hanging off the bottom-right corner are the vanilla
 * creative-inventory tabs, which is what lets the screen swap with the inventory without looking
 * like a different screen.
 *
 * <p>Fish are live entities rendered into the tank: they are dragged with the mouse, snapped to the
 * {@value AquariumContainerMenu#AQUARIUM_COLS}x{@value AquariumContainerMenu#AQUARIUM_ROWS} grid and
 * clamped to the tank, so a two cell fish cannot be dropped half out of the water.
 */
public class AquariumScreen extends AbstractContainerScreen<AquariumContainerMenu> {

    /** Vanilla sheet the rounded panel corners are lifted from, so they stay byte identical. */
    private static final ResourceLocation PANEL_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png");
    private static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot");
    private static final ResourceLocation WATER_SPRITE = ResourceLocation.withDefaultNamespace("block/water_still");

    private static final int PANEL_COLOR = 0xFFC6C6C6;
    private static final int OUTLINE = 0xFF000000;
    private static final int EDGE_LIGHT = 0xFFFFFFFF;
    private static final int EDGE_DARK = 0xFF555555;
    private static final int WELL_SHADE = 0xFF373737;
    private static final int GLASS_HIGHLIGHT = 0xFFE9F8FD;
    private static final int LABEL_COLOR = 0xFF404040;

    private static final int WATER_SHALLOW = 0xFF3E7FA3;
    private static final int WATER_MID = 0xFF1D4864;
    private static final int WATER_DEEP = 0xFF0A1E2E;
    private static final int BUBBLE_COLOR = 0x7FD6F2FF;
    private static final int BUBBLE_CORE = 0xBFEFFBFF;
    private static final int BUBBLE_COUNT = 7;

    private static final int PLACE_FILL_OK = 0x4433E1A0;
    private static final int PLACE_EDGE_OK = 0xCC8CF5C8;
    private static final int PLACE_FILL_BAD = 0x44FF5555;
    private static final int PLACE_EDGE_BAD = 0xCCFF9A9A;
    private static final int HOVER_FILL = 0x22FFFFFF;
    private static final int HOVER_EDGE = 0x88FFFFFF;

    private static final int CELL = 18;
    private static final int TANK_INSET = 3;

    private static final int GRID_LEFT = AquariumContainerMenu.AQUARIUM_GRID_X;
    private static final int GRID_TOP = AquariumContainerMenu.AQUARIUM_GRID_Y;
    private static final int GRID_WIDTH = AquariumContainerMenu.AQUARIUM_COLS * CELL;
    private static final int GRID_HEIGHT = AquariumContainerMenu.AQUARIUM_ROWS * CELL;

    /** The water spans the grid plus the overhang fish are allowed to poke into, top and bottom. */
    private static final int WATER_LEFT = GRID_LEFT;
    private static final int WATER_TOP = GRID_TOP - AquariumPreviewRenderer.VERTICAL_OVERFLOW;
    private static final int WATER_WIDTH = GRID_WIDTH;
    private static final int WATER_HEIGHT = GRID_HEIGHT + 2 * AquariumPreviewRenderer.VERTICAL_OVERFLOW;

    private int draggedFishIndex = -1;
    private AquariumFishEntry draggedFishEntry;
    private AquariumFishSpec draggedFishSpec;
    private int dragOffsetX;
    private int dragOffsetY;

    public AquariumScreen(AquariumContainerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    /**
     * Swaps back to the player inventory, on both sides: the server drops the aquarium menu and the
     * client forgets it, so a carried stack is not stranded on a container that no longer exists.
     *
     * <p>The menu is dropped by assigning the field rather than through
     * {@code LocalPlayer#clientSideCloseContainer}, which closes the screen on its way out: that grabs
     * the mouse for a frame and snaps the pointer to the middle of the window before the inventory
     * opens again. Switching tabs has to leave the pointer where the player left it.
     */
    public static void returnToInventory() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        PacketDistributor.sendToServer(new CloseAquariumPayload());
        player.containerMenu = player.inventoryMenu;
        minecraft.setScreen(new InventoryScreen(player));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        drawPanel(graphics);
        drawTank(graphics);
        drawPlayerSlots(graphics);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTankLife(graphics, mouseX, mouseY);
        if (!renderFishTooltip(graphics, mouseX, mouseY)) {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        Component capacity = Component.translatable("gui.aquanaut.aquarium.capacity",
                fishCount(), AquariumInventoryData.SLOT_COUNT);
        graphics.drawString(this.font, capacity, this.imageWidth - 8 - this.font.width(capacity),
                this.titleLabelY, LABEL_COLOR, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Optional<FishHit> hit = fishAt(mouseX, mouseY);
            if (hit.isPresent()) {
                beginDrag(hit.get(), mouseX, mouseY);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggedFishIndex >= 0) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.draggedFishIndex >= 0) {
            int targetIndex = dragPreview(mouseX, mouseY).targetIndex();
            PacketDistributor.sendToServer(
                    new AquariumFishTransferPayload(this.draggedFishIndex, targetIndex));
            optimisticallyMoveFish(this.draggedFishIndex, targetIndex);
            clearDragState();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    // ---------------------------------------------------------------- panel

    private void drawPanel(GuiGraphics graphics) {
        int x = this.leftPos;
        int y = this.topPos;
        int right = x + this.imageWidth;
        int bottom = y + this.imageHeight;

        graphics.fill(x, y, right, bottom, PANEL_COLOR);
        graphics.fill(x, y, right, y + 1, OUTLINE);
        graphics.fill(x, y, x + 1, bottom, OUTLINE);
        graphics.fill(right - 1, y, right, bottom, OUTLINE);
        graphics.fill(x, bottom - 1, right, bottom, OUTLINE);
        graphics.fill(x + 1, y + 1, right - 1, y + 3, EDGE_LIGHT);
        graphics.fill(x + 1, y + 1, x + 3, bottom - 1, EDGE_LIGHT);
        graphics.fill(x + 1, bottom - 3, right - 1, bottom - 1, EDGE_DARK);
        graphics.fill(right - 3, y + 1, right - 1, bottom - 1, EDGE_DARK);

        graphics.blit(PANEL_TEXTURE, x, y, 0, 0, 4, 4);
        graphics.blit(PANEL_TEXTURE, right - 4, y, 172, 0, 4, 4);
        graphics.blit(PANEL_TEXTURE, x, bottom - 4, 0, 162, 4, 4);
        graphics.blit(PANEL_TEXTURE, right - 4, bottom - 4, 172, 162, 4, 4);
    }

    private void drawPlayerSlots(GuiGraphics graphics) {
        int x = this.leftPos + GRID_LEFT;
        for (int row = 0; row < 3; row++) {
            drawSlotRow(graphics, x, this.topPos + AquariumContainerMenu.MAIN_INV_Y + row * CELL);
        }
        drawSlotRow(graphics, x, this.topPos + AquariumContainerMenu.HOTBAR_Y);
    }

    private void drawSlotRow(GuiGraphics graphics, int x, int y) {
        for (int col = 0; col < AquariumContainerMenu.AQUARIUM_COLS; col++) {
            graphics.blitSprite(SLOT_SPRITE, x + col * CELL - 1, y - 1, CELL, CELL);
        }
    }

    // ----------------------------------------------------------------- tank

    private void drawTank(GuiGraphics graphics) {
        int x = tankLeft();
        int y = tankTop();
        int width = WATER_WIDTH + 2 * TANK_INSET;
        int height = WATER_HEIGHT + 2 * TANK_INSET;

        // A window cut into the panel: black rim, shadow along the top and left, glass along the
        // bottom and right, exactly the recipe a vanilla slot well uses at a larger size.
        graphics.fill(x, y, x + width, y + height, OUTLINE);
        graphics.fill(x + 1, y + 1, x + width - 1, y + TANK_INSET, WELL_SHADE);
        graphics.fill(x + 1, y + 1, x + TANK_INSET, y + height - 1, WELL_SHADE);
        graphics.fill(x + 1, y + height - TANK_INSET, x + width - 1, y + height - 1, GLASS_HIGHLIGHT);
        graphics.fill(x + width - TANK_INSET, y + 1, x + width - 1, y + height - 1, GLASS_HIGHLIGHT);

        drawWater(graphics);
    }

    /** Depth gradient, then the vanilla water sprite scrolling over it, then bubbles. */
    private void drawWater(GuiGraphics graphics) {
        int x = waterLeft();
        int y = waterTop();
        // The water sheet is tiled in 18 pixel cells, so the last row and column overhang the tank:
        // the scissor trims them instead of letting them spill onto the glass.
        graphics.enableScissor(x, y, x + WATER_WIDTH, y + WATER_HEIGHT);

        int bands = 12;
        for (int band = 0; band < bands; band++) {
            int from = y + band * WATER_HEIGHT / bands;
            int to = y + (band + 1) * WATER_HEIGHT / bands;
            graphics.fill(x, from, x + WATER_WIDTH, to, waterBand(band, bands));
        }

        TextureAtlasSprite water = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(WATER_SPRITE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        // The vanilla water sheet is a grey overlay, so it is tinted and kept faint: the gradient
        // below carries the depth and the sheet only adds the moving caustics.
        RenderSystem.setShaderColor(0.42F, 0.68F, 0.86F, 0.30F);
        for (int row = 0; row * CELL < WATER_HEIGHT; row++) {
            for (int col = 0; col * CELL < WATER_WIDTH; col++) {
                graphics.blit(x + col * CELL, y + row * CELL, 0, CELL, CELL, water);
            }
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();

        drawBubbles(graphics);
        graphics.fill(x, y, x + WATER_WIDTH, y + 1, 0x66CFEFFF);
        graphics.disableScissor();
    }

    private static int waterBand(int band, int bands) {
        float t = (float) band / (bands - 1);
        return lerpColor(WATER_SHALLOW, t < 0.5F ? WATER_MID : WATER_DEEP, t < 0.5F ? t * 2.0F : (t - 0.5F) * 2.0F);
    }

    private static int lerpColor(int from, int to, float t) {
        int red = (int) Mth.lerp(t, (from >> 16) & 0xFF, (to >> 16) & 0xFF);
        int green = (int) Mth.lerp(t, (from >> 8) & 0xFF, (to >> 8) & 0xFF);
        int blue = (int) Mth.lerp(t, from & 0xFF, to & 0xFF);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    /** Bubbles rise on fixed, index derived paths so the tank never flickers between frames. */
    private void drawBubbles(GuiGraphics graphics) {
        long time = Util.getMillis();
        int x = waterLeft();
        int y = waterTop();
        int spanX = WATER_WIDTH - 10;
        int spanY = WATER_HEIGHT - 6;
        for (int i = 0; i < BUBBLE_COUNT; i++) {
            int period = 2600 + (i % 4) * 430;
            float phase = (time + i * 811L) % period / (float) period;
            int bubbleX = x + 5 + (i * 53 + 11) % spanX;
            int bubbleY = y + spanY - (int) (phase * spanY);
            int size = i % 3 == 0 ? 2 : 1;
            int alpha = (int) (0x7F * (1.0F - phase * 0.7F));
            int color = (alpha << 24) | (BUBBLE_COLOR & 0xFFFFFF);
            graphics.fill(bubbleX, bubbleY, bubbleX + size, bubbleY + size, color);
            if (size > 1) {
                graphics.fill(bubbleX, bubbleY, bubbleX + 1, bubbleY + 1, BUBBLE_CORE);
            }
        }
    }

    // ----------------------------------------------------------------- fish

    private void renderTankLife(GuiGraphics graphics, int mouseX, int mouseY) {
        AquariumInventoryData data = aquariumData();

        graphics.enableScissor(waterLeft(), waterTop(), waterLeft() + WATER_WIDTH, waterTop() + WATER_HEIGHT);
        for (int index = 0; index < AquariumInventoryData.SLOT_COUNT; index++) {
            if (index == this.draggedFishIndex) {
                continue;
            }
            AquariumFishEntry entry = AquariumInventoryHelper.fishEntryAt(data, index).orElse(null);
            AquariumFishSpec spec = AquariumInventoryHelper.fishAt(data, index).orElse(null);
            if (entry != null && spec != null) {
                AquariumPreviewRenderer.renderFish(graphics, slotLeft(index), slotTop(index), entry, spec);
            }
        }
        graphics.disableScissor();

        if (this.draggedFishIndex < 0) {
            fishAt(mouseX, mouseY).ifPresent(hit -> drawCellHighlight(graphics, hit.index(), hit.spec(),
                    HOVER_FILL, HOVER_EDGE));
        } else {
            renderDrag(graphics, mouseX, mouseY, data);
        }
    }

    private void renderDrag(GuiGraphics graphics, int mouseX, int mouseY, AquariumInventoryData data) {
        if (this.draggedFishSpec == null) {
            return;
        }
        DragPreview preview = dragPreview(mouseX, mouseY);
        if (preview.targetIndex() >= 0) {
            boolean allowed = AquariumInventoryHelper.canPlaceAt(data, this.draggedFishSpec,
                    preview.targetIndex(), this.draggedFishIndex);
            drawCellHighlight(graphics, preview.targetIndex(), this.draggedFishSpec,
                    allowed ? PLACE_FILL_OK : PLACE_FILL_BAD, allowed ? PLACE_EDGE_OK : PLACE_EDGE_BAD);
        }

        // The fish in hand follows the pointer and is allowed to leave the tank, so it is not clipped.
        AquariumPreviewRenderer.renderFish(graphics, (int) Math.round(preview.drawLeft()),
                (int) Math.round(preview.drawTop()), this.draggedFishEntry, this.draggedFishSpec);
    }

    private void drawCellHighlight(GuiGraphics graphics, int anchorIndex, AquariumFishSpec spec, int fill, int edge) {
        for (int cellIndex : coveredCells(anchorIndex, spec)) {
            int x = slotLeft(cellIndex);
            int y = slotTop(cellIndex);
            graphics.fill(x, y, x + CELL, y + CELL, fill);
            graphics.fill(x, y, x + CELL, y + 1, edge);
            graphics.fill(x, y + CELL - 1, x + CELL, y + CELL, edge);
            graphics.fill(x, y, x + 1, y + CELL, edge);
            graphics.fill(x + CELL - 1, y, x + CELL, y + CELL, edge);
        }
    }

    private List<Integer> coveredCells(int anchorIndex, AquariumFishSpec spec) {
        return AquariumPlacementMath.coveredCells(anchorIndex, spec.gridWidth(), spec.gridHeight(),
                AquariumContainerMenu.AQUARIUM_COLS, AquariumContainerMenu.AQUARIUM_ROWS);
    }

    private void optimisticallyMoveFish(int sourceIndex, int targetIndex) {
        if (targetIndex < 0 || sourceIndex == targetIndex || this.draggedFishSpec == null
                || this.draggedFishEntry == null) {
            return;
        }
        AquariumInventoryData data = ClientAquariumData.getAquarium();
        if (!AquariumInventoryHelper.canPlaceAt(data, this.draggedFishSpec, targetIndex, sourceIndex)) {
            return;
        }
        List<AquariumFishEntry> entries = data.mutableCopy();
        entries.set(sourceIndex, AquariumFishEntry.EMPTY);
        entries.set(targetIndex, this.draggedFishEntry);
        ClientAquariumData.setFromData(new AquariumInventoryData(entries));
    }

    // ------------------------------------------------------------- tooltips

    private boolean renderFishTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.draggedFishIndex >= 0) {
            return false;
        }
        FishHit hit = fishAt(mouseX, mouseY).orElse(null);
        if (hit == null) {
            return false;
        }

        LivingEntity preview = AquariumPreviewRenderer.getOrCreatePreviewEntity(hit.entry(), hit.spec());
        if (preview == null) {
            return false;
        }

        graphics.renderTooltip(this.font,
                List.of(preview.getDisplayName(), healthLine(preview.getHealth(), preview.getMaxHealth())),
                Optional.empty(), mouseX, mouseY);
        return true;
    }

    private Component healthLine(float health, float maxHealth) {
        int hearts = Math.max(1, Mth.ceil(maxHealth * 0.5F));
        int halves = Mth.clamp(Mth.floor(health * 2.0F + 1.0E-4F), 0, hearts * 2);

        MutableComponent line = Component.translatable("gui.aquanaut.aquarium.health").append(CommonComponents.SPACE);
        for (int index = 0; index < hearts; index++) {
            int remaining = halves - index * 2;
            if (remaining >= 2) {
                line.append(Component.literal("❤").withColor(0xFF5555));
            } else if (remaining == 1) {
                line.append(Component.literal("❤").withColor(0xFF9955));
            } else {
                line.append(Component.literal("♡").withColor(0x7F7F7F));
            }
        }
        return line.append(Component.literal(" " + trim(health) + "/" + trim(maxHealth)).withColor(0xAAAAAA));
    }

    private static String trim(float value) {
        return value == Math.floor(value) ? Integer.toString((int) value) : String.format(Locale.ROOT, "%.1f", value);
    }

    // ---------------------------------------------------------------- hits

    private Optional<FishHit> fishAt(double mouseX, double mouseY) {
        AquariumInventoryData data = aquariumData();
        for (int index = AquariumInventoryData.SLOT_COUNT - 1; index >= 0; index--) {
            AquariumFishEntry entry = AquariumInventoryHelper.fishEntryAt(data, index).orElse(null);
            AquariumFishSpec spec = AquariumInventoryHelper.fishAt(data, index).orElse(null);
            if (entry == null || spec == null) {
                continue;
            }
            int x = slotLeft(index);
            int y = slotTop(index);
            int width = spec.gridWidth() * CELL;
            int height = spec.gridHeight() * CELL;
            if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
                return Optional.of(new FishHit(index, entry, spec, x, y));
            }
        }
        return Optional.empty();
    }

    private void beginDrag(FishHit hit, double mouseX, double mouseY) {
        this.draggedFishIndex = hit.index();
        this.draggedFishEntry = hit.entry();
        this.draggedFishSpec = hit.spec();
        this.dragOffsetX = (int) Math.round(mouseX - hit.x());
        this.dragOffsetY = (int) Math.round(mouseY - hit.y());
    }

    private void clearDragState() {
        this.draggedFishIndex = -1;
        this.draggedFishEntry = null;
        this.draggedFishSpec = null;
        this.dragOffsetX = 0;
        this.dragOffsetY = 0;
    }

    private DragPreview dragPreview(double mouseX, double mouseY) {
        double freeLeft = mouseX - this.dragOffsetX;
        double freeTop = mouseY - this.dragOffsetY;
        int targetIndex = anchorAt(freeLeft, freeTop);
        if (targetIndex < 0) {
            return new DragPreview(freeLeft, freeTop, freeLeft, freeTop, -1);
        }
        return new DragPreview(freeLeft, freeTop, slotLeft(targetIndex), slotTop(targetIndex), targetIndex);
    }

    private int anchorAt(double left, double top) {
        int col = (int) Math.round((left - gridLeft()) / CELL);
        int row = (int) Math.round((top - gridTop()) / CELL);
        if (col < 0 || col >= AquariumContainerMenu.AQUARIUM_COLS
                || row < 0 || row >= AquariumContainerMenu.AQUARIUM_ROWS) {
            return -1;
        }
        return row * AquariumContainerMenu.AQUARIUM_COLS + col;
    }

    // -------------------------------------------------------------- helpers

    private AquariumInventoryData aquariumData() {
        return ClientAquariumData.getAquarium();
    }

    private int fishCount() {
        AquariumInventoryData data = aquariumData();
        int count = 0;
        for (int index = 0; index < AquariumInventoryData.SLOT_COUNT; index++) {
            if (AquariumInventoryHelper.fishEntryAt(data, index).isPresent()) {
                count++;
            }
        }
        return count;
    }

    private int gridLeft() {
        return this.leftPos + GRID_LEFT;
    }

    private int gridTop() {
        return this.topPos + GRID_TOP;
    }

    private int slotLeft(int index) {
        return gridLeft() + index % AquariumContainerMenu.AQUARIUM_COLS * CELL;
    }

    private int slotTop(int index) {
        return gridTop() + index / AquariumContainerMenu.AQUARIUM_COLS * CELL;
    }

    private int tankLeft() {
        return this.leftPos + WATER_LEFT - TANK_INSET;
    }

    private int tankTop() {
        return this.topPos + WATER_TOP - TANK_INSET;
    }

    private int waterLeft() {
        return this.leftPos + WATER_LEFT;
    }

    private int waterTop() {
        return this.topPos + WATER_TOP;
    }

    private record FishHit(int index, AquariumFishEntry entry, AquariumFishSpec spec, int x, int y) {
    }

    private record DragPreview(double freeLeft, double freeTop, double drawLeft, double drawTop, int targetIndex) {
    }
}
