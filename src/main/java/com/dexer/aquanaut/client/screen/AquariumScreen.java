package com.dexer.aquanaut.client.screen;

import com.dexer.aquanaut.common.inventory.aquarium.AquariumContainerMenu;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumFishEntry;
import com.dexer.aquanaut.client.renderer.AquariumPreviewRenderer;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumFishSpec;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumHealthTracker;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumInventoryData;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumInventoryHelper;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumPlacementMath;
import com.dexer.aquanaut.client.ClientAquariumData;
import com.dexer.aquanaut.network.AquariumFishTransferPayload;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;

public class AquariumScreen extends AbstractContainerScreen<AquariumContainerMenu> {

    private static final ResourceLocation VANILLA_SLOT = ResourceLocation.withDefaultNamespace("container/slot");
    private static final int CELL_SIZE = 18;
    private static final int GRID_LEFT_PADDING = 8;

    private static final int PANEL_FILL = 0xFFC2DBDB;
    private static final int PANEL_INNER = 0xFFB8D0D0;
    private static final int PANEL_LIGHT = 0xFFF2FFFF;
    private static final int PANEL_DARK = 0xFF5D7A7A;
    private static final int DIVIDER_COLOR = 0x55000000;
    private static final float SLOT_TINT_R = 0.78F;
    private static final float SLOT_TINT_G = 0.90F;
    private static final float SLOT_TINT_B = 0.90F;

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

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        drawFrame(graphics, x, y);
        drawAquariumWater(graphics, x, y);
        drawAquariumDividers(graphics, x, y);
        drawPlayerInventoryArea(graphics, x, y);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderAquariumFish(graphics, mouseX, mouseY);
        renderTitleOverlay(graphics);
        if (!renderFishTooltip(graphics, mouseX, mouseY)) {
            this.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x3F4F5F, false);
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        if (slot.getItem().isEmpty()) {
            return;
        }

        graphics.renderItem(slot.getItem(), slot.x, slot.y);
        graphics.renderItemDecorations(this.font, slot.getItem(), slot.x, slot.y);
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
            DragPreview preview = dragPreview(mouseX, mouseY);
            int targetIndex = preview.targetIndex();
            PacketDistributor.sendToServer(
                    new AquariumFishTransferPayload(this.draggedFishIndex, targetIndex));
            optimisticallyMoveFish(this.draggedFishIndex, targetIndex);
            clearDragState();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void optimisticallyMoveFish(int sourceIndex, int targetIndex) {
        if (targetIndex < 0 || sourceIndex == targetIndex) {
            return;
        }
        AquariumInventoryData data = ClientAquariumData.getAquarium();
        if (this.draggedFishSpec == null) {
            return;
        }
        if (!AquariumInventoryHelper.canPlaceAt(data, this.draggedFishSpec, targetIndex, sourceIndex)) {
            return;
        }
        if (this.draggedFishEntry == null) {
            return;
        }
        List<AquariumFishEntry> fishEntries = data.mutableCopy();
        fishEntries.set(sourceIndex, AquariumFishEntry.EMPTY);
        fishEntries.set(targetIndex, this.draggedFishEntry);
        ClientAquariumData.setFromData(new AquariumInventoryData(fishEntries));
    }

    private void drawFrame(GuiGraphics graphics, int x, int y) {
        int w = this.imageWidth;
        int h = this.imageHeight;

        graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, PANEL_FILL);
        graphics.fill(x + 3, y + 3, x + w - 3, y + h - 3, PANEL_INNER);

        graphics.fill(x + 2, y, x + w - 2, y + 1, PANEL_LIGHT);
        graphics.fill(x + 1, y + 1, x + w - 1, y + 2, PANEL_LIGHT);
        graphics.fill(x, y + 2, x + 1, y + h - 2, PANEL_LIGHT);
        graphics.fill(x + 1, y + 2, x + 2, y + h - 2, PANEL_LIGHT);

        graphics.fill(x + w - 2, y + 2, x + w - 1, y + h - 2, PANEL_DARK);
        graphics.fill(x + w - 1, y + 2, x + w, y + h - 2, PANEL_DARK);
        graphics.fill(x + 2, y + h - 2, x + w - 2, y + h - 1, PANEL_DARK);
        graphics.fill(x + 2, y + h - 1, x + w - 2, y + h, PANEL_DARK);

        graphics.fill(x + 1, y + 1, x + 2, y + 2, PANEL_LIGHT);
        graphics.fill(x + w - 2, y + 1, x + w - 1, y + 2, PANEL_LIGHT);
        graphics.fill(x + 1, y + h - 2, x + 2, y + h - 1, PANEL_DARK);
        graphics.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, PANEL_DARK);
    }

    private void drawAquariumWater(GuiGraphics graphics, int left, int top) {
        int gx = left + GRID_LEFT_PADDING;
        int gy = top + AquariumContainerMenu.AQUARIUM_GRID_Y;
        int cols = AquariumContainerMenu.AQUARIUM_COLS;
        int rows = AquariumContainerMenu.AQUARIUM_ROWS;
        int overflow = AquariumPreviewRenderer.VERTICAL_OVERFLOW;
        int waterTop = gy - overflow;

        TextureAtlasSprite waterSprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(ResourceLocation.withDefaultNamespace("block/water_still"));

        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        RenderSystem.setShaderColor(0.30F, 0.54F, 0.80F, 1.0F);

        for (int row = 0; row <= rows; row++) {
            for (int col = 0; col < cols; col++) {
                int sx = gx + col * 18;
                int sy = waterTop + row * 18;
                graphics.blit(sx, sy, 0, 18, 18, waterSprite);
            }
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawAquariumDividers(GuiGraphics graphics, int left, int top) {
        int gx = left + GRID_LEFT_PADDING;
        int gy = top + AquariumContainerMenu.AQUARIUM_GRID_Y;
        int cols = AquariumContainerMenu.AQUARIUM_COLS;
        int rows = AquariumContainerMenu.AQUARIUM_ROWS;
        int overflow = AquariumPreviewRenderer.VERTICAL_OVERFLOW;
        int waterTop = gy - overflow;
        int gridW = cols * CELL_SIZE;
        int gridH = rows * CELL_SIZE;
        int waterBottom = gy + gridH + overflow;

        int borderColor = 0x77000000;
        graphics.fill(gx, waterTop, gx + gridW, waterTop + 1, borderColor);
        graphics.fill(gx, waterBottom - 1, gx + gridW, waterBottom, borderColor);
        graphics.fill(gx, waterTop, gx + 1, waterBottom, borderColor);
        graphics.fill(gx + gridW - 1, waterTop, gx + gridW, waterBottom, borderColor);
    }

    private void drawPlayerInventoryArea(GuiGraphics graphics, int left, int top) {
        int gx = left + GRID_LEFT_PADDING;
        int mainGy = top + AquariumContainerMenu.MAIN_INV_Y;
        int cols = 9;

        drawSlotRow(graphics, gx, mainGy, cols);
        drawSlotRow(graphics, gx, mainGy + 18, cols);
        drawSlotRow(graphics, gx, mainGy + 36, cols);

        int hotbarGy = top + AquariumContainerMenu.HOTBAR_Y;
        drawSlotRow(graphics, gx, hotbarGy, cols);
    }

    private void drawSlotRow(GuiGraphics graphics, int gx, int gy, int cols) {
        RenderSystem.setShaderColor(SLOT_TINT_R, SLOT_TINT_G, SLOT_TINT_B, 1.0F);
        for (int col = 0; col < cols; col++) {
            graphics.blitSprite(VANILLA_SLOT, gx + col * 18 - 1, gy - 1, 18, 18);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderTitleOverlay(GuiGraphics graphics) {
        graphics.drawString(this.font, this.title, this.leftPos + this.titleLabelX, this.topPos + this.titleLabelY,
                0x3F4F5F, false);
    }

    private void renderAquariumFish(GuiGraphics graphics, int mouseX, int mouseY) {
        AquariumInventoryData data = aquariumData();

        for (int index = 0; index < AquariumInventoryData.SLOT_COUNT; index++) {
            if (index == this.draggedFishIndex) {
                continue;
            }

            AquariumFishEntry entry = AquariumInventoryHelper.fishEntryAt(data, index).orElse(null);
            AquariumFishSpec spec = AquariumInventoryHelper.fishAt(data, index).orElse(null);
            if (entry != null && spec != null) {
                renderFishAtIndex(graphics, index, entry, spec);
            }
        }

        if (this.draggedFishIndex >= 0 && this.draggedFishSpec != null) {
            DragPreview preview = dragPreview(mouseX, mouseY);
            if (preview.targetIndex() >= 0) {
                boolean canPlace = AquariumInventoryHelper.canPlaceAt(data, this.draggedFishSpec, preview.targetIndex(),
                        this.draggedFishIndex);
                renderPlacementPreview(graphics, preview.targetIndex(), this.draggedFishSpec, canPlace);
            }

            int drawX = (int) Math.round(preview.drawLeft());
            int drawY = (int) Math.round(preview.drawTop());
            AquariumPreviewRenderer.renderFish(graphics, drawX, drawY, this.draggedFishEntry, this.draggedFishSpec);
        }
    }

    private void renderFishAtIndex(GuiGraphics graphics, int index, AquariumFishEntry entry, AquariumFishSpec spec) {
        int x = slotLeft(index);
        int y = slotTop(index);
        AquariumPreviewRenderer.renderFish(graphics, x, y, entry, spec);
    }

    private void renderPlacementPreview(GuiGraphics graphics, int anchorIndex, AquariumFishSpec spec, boolean valid) {
        int fill = valid ? 0x6600C8FF : 0x66FF5555;
        int edge = valid ? 0xCCB8FFFF : 0xCCFF9999;
        List<Integer> coveredCells = AquariumPlacementMath.coveredCells(anchorIndex, spec.gridWidth(), spec.gridHeight(),
                AquariumContainerMenu.AQUARIUM_COLS, AquariumContainerMenu.AQUARIUM_ROWS);
        for (int cellIndex : coveredCells) {
            int x = slotLeft(cellIndex);
            int y = slotTop(cellIndex);
            graphics.fill(x + 1, y + 1, x + CELL_SIZE - 1, y + CELL_SIZE - 1, fill);
            graphics.fill(x, y, x + CELL_SIZE, y + 1, edge);
            graphics.fill(x, y + CELL_SIZE - 1, x + CELL_SIZE, y + CELL_SIZE, edge);
            graphics.fill(x, y, x + 1, y + CELL_SIZE, edge);
            graphics.fill(x + CELL_SIZE - 1, y, x + CELL_SIZE, y + CELL_SIZE, edge);
        }
    }

    private Optional<FishHit> fishAt(double mouseX, double mouseY) {
        AquariumInventoryData data = aquariumData();

        for (int index = AquariumInventoryData.SLOT_COUNT - 1; index >= 0; index--) {
            Optional<AquariumFishEntry> entry = AquariumInventoryHelper.fishEntryAt(data, index);
            Optional<AquariumFishSpec> spec = AquariumInventoryHelper.fishAt(data, index);
            if (entry.isEmpty() || spec.isEmpty()) {
                continue;
            }

            FishRect rect = fishRect(index, spec.get());
            if (rect.contains(mouseX, mouseY)) {
                return Optional.of(new FishHit(index, entry.get(), spec.get(), rect.x(), rect.y()));
            }
        }

        return Optional.empty();
    }

    private FishRect fishRect(int anchorIndex, AquariumFishSpec spec) {
        int x = slotLeft(anchorIndex);
        int y = slotTop(anchorIndex);
        return new FishRect(x, y, spec.gridWidth() * CELL_SIZE, spec.gridHeight() * CELL_SIZE);
    }

    private int aquariumAnchorAt(double left, double top) {
        int relX = (int) Math.floor(left - aquariumGridLeft());
        int relY = (int) Math.floor(top - aquariumGridTop());
        if (relX < 0 || relY < 0) {
            return -1;
        }

        int col = relX / CELL_SIZE;
        int row = relY / CELL_SIZE;
        if (col < 0 || col >= AquariumContainerMenu.AQUARIUM_COLS
                || row < 0 || row >= AquariumContainerMenu.AQUARIUM_ROWS) {
            return -1;
        }

        return row * AquariumContainerMenu.AQUARIUM_COLS + col;
    }

    private int aquariumAnchorAtSnapped(double left, double top) {
        double relX = left - aquariumGridLeft();
        double relY = top - aquariumGridTop();
        int col = (int) Math.round(relX / CELL_SIZE);
        int row = (int) Math.round(relY / CELL_SIZE);
        if (col < 0 || col >= AquariumContainerMenu.AQUARIUM_COLS
                || row < 0 || row >= AquariumContainerMenu.AQUARIUM_ROWS) {
            return -1;
        }

        return row * AquariumContainerMenu.AQUARIUM_COLS + col;
    }

    private int aquariumGridLeft() {
        return this.leftPos + GRID_LEFT_PADDING;
    }

    private int aquariumGridTop() {
        return this.topPos + AquariumContainerMenu.AQUARIUM_GRID_Y;
    }

    private int slotLeft(int index) {
        return aquariumGridLeft() + (index % AquariumContainerMenu.AQUARIUM_COLS) * CELL_SIZE;
    }

    private int slotTop(int index) {
        return aquariumGridTop() + (index / AquariumContainerMenu.AQUARIUM_COLS) * CELL_SIZE;
    }

    private AquariumInventoryData aquariumData() {
        return ClientAquariumData.getAquarium();
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

    private boolean renderFishTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.draggedFishIndex >= 0) {
            return false;
        }

        FishHit hit = fishAt(mouseX, mouseY).orElse(null);
        if (hit == null) {
            return false;
        }

        LivingEntity previewEntity = AquariumPreviewRenderer.getOrCreatePreviewEntity(hit.entry(), hit.spec());
        if (previewEntity == null) {
            return false;
        }

        Component name = previewEntity.getDisplayName();
        float health = AquariumHealthTracker.getHealth(hit.entry());
        float maxHealth = previewEntity.getMaxHealth();

        graphics.renderTooltip(this.font, List.of(
                name,
                healthTooltip(health, maxHealth)), Optional.empty(), mouseX, mouseY);
        return true;
    }

    private Component healthTooltip(float health, float maxHealth) {
        float clampedHealth = Mth.clamp(health, 0.0F, maxHealth);
        int totalHearts = Math.max(1, Mth.ceil(maxHealth * 0.5F));
        int filledHalfHearts = Mth.clamp(Mth.floor((health * 2.0F) + 1.0E-4F), 0, totalHearts * 2);
        int fullHearts = filledHalfHearts / 2;
        boolean hasHalfHeart = (filledHalfHearts & 1) == 1;
        int emptyHearts = totalHearts - fullHearts - (hasHalfHeart ? 1 : 0);

        MutableComponent line = Component.translatable("gui.aquanaut.aquarium.health").append(CommonComponents.SPACE);
        for (int index = 0; index < fullHearts; index++) {
            line.append(Component.literal("❤").withColor(0xFF5555));
        }
        if (hasHalfHeart) {
            line.append(Component.literal("½").withColor(0xFFAA55));
        }
        for (int index = 0; index < emptyHearts; index++) {
            line.append(Component.literal("♡").withColor(0x7F7F7F));
        }
        return line;
    }

    private DragPreview dragPreview(double mouseX, double mouseY) {
        double freeLeft = mouseX - this.dragOffsetX;
        double freeTop = mouseY - this.dragOffsetY;
        int targetIndex = aquariumAnchorAtSnapped(freeLeft, freeTop);
        if (targetIndex < 0) {
            return new DragPreview(freeLeft, freeTop, freeLeft, freeTop, -1);
        }

        return new DragPreview(
                freeLeft,
                freeTop,
                slotLeft(targetIndex),
                slotTop(targetIndex),
                targetIndex);
    }

    private record FishHit(int index, AquariumFishEntry entry, AquariumFishSpec spec, int x, int y) {
    }

    private record FishRect(int x, int y, int width, int height) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private record DragPreview(double freeLeft, double freeTop, double drawLeft, double drawTop, int targetIndex) {
    }
}
