package com.dexer.aquanaut.client.screen;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.core.ItemRegistry;
import com.dexer.aquanaut.network.CloseAquariumPayload;
import com.dexer.aquanaut.network.OpenAquariumPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Aquanaut.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
@SuppressWarnings("removal")
public final class AquariumScreenEvents {

    private static final int BUTTON_WIDTH = 20;
    private static final int BUTTON_HEIGHT = 18;
    private static Button currentButton;

    private AquariumScreenEvents() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();

        if (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen) {
            addAquariumOpenButton(screen, event);
        }

        if (screen instanceof AquariumScreen) {
            addAquariumCloseButton(screen, event);
        }
    }

    private static void addAquariumOpenButton(Screen screen, ScreenEvent.Init.Post event) {
        AbstractContainerScreen<?> containerScreen = (AbstractContainerScreen<?>) screen;
        int bx = containerScreen.getGuiLeft() + containerScreen.getXSize();
        int by = containerScreen.getGuiTop() + 26;

        Button button = new AquariumInventoryButton(bx, by, BUTTON_WIDTH, BUTTON_HEIGHT,
                btn -> PacketDistributor.sendToServer(new OpenAquariumPayload()));

        button.setTooltip(Tooltip.create(Component.translatable("gui.aquanaut.aquarium")));
        event.addListener(button);
        currentButton = button;
    }

    private static void addAquariumCloseButton(Screen screen, ScreenEvent.Init.Post event) {
        AbstractContainerScreen<?> containerScreen = (AbstractContainerScreen<?>) screen;
        int bx = containerScreen.getGuiLeft() + containerScreen.getXSize();
        int by = containerScreen.getGuiTop() + 26;

        Button button = new AquariumCloseButton(bx, by, BUTTON_WIDTH, BUTTON_HEIGHT,
                btn -> {
                    PacketDistributor.sendToServer(new CloseAquariumPayload());
                    net.minecraft.client.Minecraft.getInstance().setScreen(
                            new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
                });

        button.setTooltip(Tooltip.create(Component.translatable("gui.aquanaut.inventory")));
        event.addListener(button);
    }

    @SubscribeEvent
    public static void onContainerRender(ContainerScreenEvent.Render.Background event) {
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        if (!(screen instanceof InventoryScreen) && !(screen instanceof CreativeModeInventoryScreen)) {
            return;
        }

        if (currentButton != null) {
            currentButton.visible = true;
            currentButton.setX(screen.getGuiLeft() + screen.getXSize());
            currentButton.setY(screen.getGuiTop() + 26);
        }
    }

    private static class AquariumInventoryButton extends Button {

        private static final int WATER_BG = 0xCC2A4A6A;
        private static final int LIGHT = 0xFF4A7A9A;
        private static final int DARK = 0xFF1A2A3A;

        AquariumInventoryButton(int x, int y, int width, int height, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderButtonChrome(graphics, this, this.isHovered() ? 0xFF6ABAE0 : LIGHT, WATER_BG, DARK);
            renderButtonItem(graphics, openIcon(), this.getX(), this.getY());
        }
    }

    private static class AquariumCloseButton extends Button {

        private static final int WATER_BG = 0xCC6A2A2A;
        private static final int LIGHT = 0xFF9A4A4A;
        private static final int DARK = 0xFF3A1A1A;

        AquariumCloseButton(int x, int y, int width, int height, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderButtonChrome(graphics, this, this.isHovered() ? 0xFFE06A6A : LIGHT, WATER_BG, DARK);
            renderButtonItem(graphics, closeIcon(), this.getX(), this.getY());
        }
    }

    private static void renderButtonChrome(GuiGraphics graphics, Button button, int light, int fill, int dark) {
        int bx = button.getX();
        int by = button.getY();
        int bw = button.getWidth();
        int bh = button.getHeight();

        graphics.fill(bx, by, bx + bw, by + bh, fill);
        graphics.fill(bx + 1, by, bx + bw, by + 1, light);
        graphics.fill(bx, by, bx + 1, by + bh, light);
        graphics.fill(bx + bw - 1, by, bx + bw, by + bh, dark);
        graphics.fill(bx, by + bh - 1, bx + bw, by + bh, dark);
    }

    private static void renderButtonItem(GuiGraphics graphics, ItemStack stack, int buttonX, int buttonY) {
        graphics.renderItem(stack, buttonX + 2, buttonY + 1);
    }

    private static ItemStack openIcon() {
        return new ItemStack(ItemRegistry.SARDINE.get());
    }

    private static ItemStack closeIcon() {
        return new ItemStack(Items.CHEST);
    }
}
