package com.dexer.aquanaut.client;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.diving.DivingEquipmentSlotType;
import com.dexer.aquanaut.common.diving.inventory.DivingEquipmentContainer;
import com.dexer.aquanaut.common.diving.inventory.DivingEquipmentMenuSlot;
import com.dexer.aquanaut.common.diving.inventory.DivingInventoryLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Renders an Aquanaut equipment side panel next to the vanilla inventory.
 */
@EventBusSubscriber(modid = Aquanaut.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientInventoryPanelEvents {

    private static final ResourceLocation VANILLA_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot");
    private static final ResourceLocation MASK_SLOT_SPRITE = ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID,
            "container/slot/mask");
    private static final ResourceLocation TANK_SLOT_SPRITE = ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID,
            "container/slot/tank");
    private static final ResourceLocation FLIPPERS_SLOT_SPRITE = ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID,
            "container/slot/flippers");

    private static final int PANEL_WIDTH = 30;
    private static final int PANEL_GAP = 6;

    private static final int SLOT_SIZE = 18;
    private static final int SLOT_ICON_SIZE = 16;
    private static final int PANEL_SLOT_TOP_PADDING = 8;
    private static final int PANEL_SLOT_BOTTOM_PADDING = 8;
    private static final int PANEL_HEIGHT = PANEL_SLOT_TOP_PADDING + (SLOT_SIZE * 3) + PANEL_SLOT_BOTTOM_PADDING;

    private static final int PANEL_FILL = 0xFFC2DBDB;
    private static final int PANEL_INNER = 0xFFB8D0D0;
    private static final int PANEL_LIGHT = 0xFFF2FFFF;
    private static final int PANEL_DARK = 0xFF5D7A7A;
    private static final int SLOT_BACKDROP = 0xFF4A6161;
    private static final float SLOT_TINT_R = 0.78F;
    private static final float SLOT_TINT_G = 0.90F;
    private static final float SLOT_TINT_B = 0.90F;

    private ClientInventoryPanelEvents() {
    }

    /**
     * ContainerScreenEvent.Render.Background is the modern NeoForge hook for
     * adding background elements to container screens.
     */
    @SubscribeEvent
    public static void onContainerBackground(ContainerScreenEvent.Render.Background event) {
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        if (!isSupportedInventoryScreen(screen)) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        drawEquipmentPanel(graphics, screen.getGuiLeft(), screen.getGuiTop());
        drawSlotBackgrounds(graphics, screen, false);
    }

    @SubscribeEvent
    public static void onContainerForeground(ContainerScreenEvent.Render.Foreground event) {
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        if (!isSupportedInventoryScreen(screen)) {
            return;
        }

        drawEmptySlotIcons(event.getGuiGraphics(), screen, true);
    }

    @SubscribeEvent
    public static void onCreativeInventoryRender(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof CreativeModeInventoryScreen creativeScreen)
                || !(screen instanceof AbstractContainerScreen<?> containerScreen)
                || !creativeScreen.isInventoryOpen()) {
            return;
        }

        drawEquipmentPanel(event.getGuiGraphics(), containerScreen.getGuiLeft(), containerScreen.getGuiTop());
        drawSlotBackgrounds(event.getGuiGraphics(), containerScreen, false);
        drawEmptySlotIcons(event.getGuiGraphics(), containerScreen, false);
    }

    private static void drawEquipmentPanel(GuiGraphics graphics, int inventoryLeft, int inventoryTop) {

        int panelX = inventoryLeft - PANEL_GAP - PANEL_WIDTH;
        int panelY = inventoryTop;

        drawPanel(graphics, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);
    }

    private static void drawEmptySlotIcons(GuiGraphics graphics, AbstractContainerScreen<?> screen,
            boolean translatedToGuiOrigin) {
        Slot maskSlot = findSlot(screen, DivingEquipmentSlotType.MASK);
        Slot tankSlot = findSlot(screen, DivingEquipmentSlotType.TANK);
        Slot flippersSlot = findSlot(screen, DivingEquipmentSlotType.FLIPPERS);

        drawSlotContents(graphics, screen, maskSlot, MASK_SLOT_SPRITE, translatedToGuiOrigin);
        drawSlotContents(graphics, screen, tankSlot, TANK_SLOT_SPRITE, translatedToGuiOrigin);
        drawSlotContents(graphics, screen, flippersSlot, FLIPPERS_SLOT_SPRITE, translatedToGuiOrigin);
    }

    private static void drawSlotBackgrounds(GuiGraphics graphics, AbstractContainerScreen<?> screen,
            boolean translatedToGuiOrigin) {
        Slot maskSlot = findSlot(screen, DivingEquipmentSlotType.MASK);
        Slot tankSlot = findSlot(screen, DivingEquipmentSlotType.TANK);
        Slot flippersSlot = findSlot(screen, DivingEquipmentSlotType.FLIPPERS);

        drawSlotBackground(graphics, screen, maskSlot, translatedToGuiOrigin);
        drawSlotBackground(graphics, screen, tankSlot, translatedToGuiOrigin);
        drawSlotBackground(graphics, screen, flippersSlot, translatedToGuiOrigin);
    }

    private static boolean isSupportedInventoryScreen(AbstractContainerScreen<?> screen) {
        if (screen instanceof InventoryScreen) {
            return true;
        }

        return screen instanceof CreativeModeInventoryScreen creativeScreen && creativeScreen.isInventoryOpen();
    }

    private static Slot findSlot(AbstractContainerScreen<?> screen, DivingEquipmentSlotType slotType) {
        for (Slot slot : screen.getMenu().slots) {
            if (matchesDivingSlot(slot, slotType)) {
                return slot;
            }
        }
        return null;
    }

    private static boolean matchesDivingSlot(Slot slot, DivingEquipmentSlotType slotType) {
        if (slot instanceof DivingEquipmentMenuSlot divingSlot) {
            return divingSlot.slotType() == slotType;
        }

        if (slot.container instanceof DivingEquipmentContainer) {
            return slot.getContainerSlot() == slotType.toIndex()
                    || (slot.x == DivingInventoryLayout.SLOT_X && slot.y == DivingInventoryLayout.slotY(slotType));
        }

        return false;
    }

    private static void drawSlotBackground(GuiGraphics graphics, AbstractContainerScreen<?> screen, Slot slot,
            boolean translatedToGuiOrigin) {
        if (slot == null) {
            return;
        }

        int slotX = slot.x - 1;
        int slotY = slot.y - 1;
        if (!translatedToGuiOrigin) {
            slotX += screen.getGuiLeft();
            slotY += screen.getGuiTop();
        }

        graphics.fill(slotX + 1, slotY + 1, slotX + SLOT_SIZE - 1, slotY + SLOT_SIZE - 1, SLOT_BACKDROP);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(SLOT_TINT_R, SLOT_TINT_G, SLOT_TINT_B, 1.0F);
        graphics.blitSprite(VANILLA_SLOT_SPRITE, slotX, slotY, SLOT_SIZE, SLOT_SIZE);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawSlotContents(GuiGraphics graphics, AbstractContainerScreen<?> screen, Slot slot,
            ResourceLocation iconSprite, boolean translatedToGuiOrigin) {
        if (slot == null) {
            return;
        }

        int iconX = slot.x;
        int iconY = slot.y;
        ItemStack stack = slot.getItem();

        if (!translatedToGuiOrigin) {
            iconX += screen.getGuiLeft();
            iconY += screen.getGuiTop();
        }

        if (!stack.isEmpty()) {
            graphics.renderItem(stack, iconX, iconY);
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                graphics.renderItemDecorations(minecraft.font, stack, iconX, iconY);
            }
            return;
        }

        graphics.blitSprite(iconSprite, iconX, iconY, SLOT_ICON_SIZE, SLOT_ICON_SIZE);
    }

    private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        // Core fill with stronger corner cut so the panel does not read as a plain
        // rectangle.
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, PANEL_FILL);
        graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, PANEL_INNER);

        // Beveled frame segments (2px chamfered corners, vanilla-like).
        graphics.fill(x + 2, y, x + width - 2, y + 1, PANEL_LIGHT);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, PANEL_LIGHT);
        graphics.fill(x, y + 2, x + 1, y + height - 2, PANEL_LIGHT);
        graphics.fill(x + 1, y + 2, x + 2, y + height - 2, PANEL_LIGHT);

        graphics.fill(x + width - 2, y + 2, x + width - 1, y + height - 2, PANEL_DARK);
        graphics.fill(x + width - 1, y + 2, x + width, y + height - 2, PANEL_DARK);
        graphics.fill(x + 2, y + height - 2, x + width - 2, y + height - 1, PANEL_DARK);
        graphics.fill(x + 2, y + height - 1, x + width - 2, y + height, PANEL_DARK);

        // Corner bridge pixels for diagonal transitions.
        graphics.fill(x + 1, y + 1, x + 2, y + 2, PANEL_LIGHT);
        graphics.fill(x + width - 2, y + 1, x + width - 1, y + 2, PANEL_LIGHT);
        graphics.fill(x + 1, y + height - 2, x + 2, y + height - 1, PANEL_DARK);
        graphics.fill(x + width - 2, y + height - 2, x + width - 1, y + height - 1, PANEL_DARK);
    }

}