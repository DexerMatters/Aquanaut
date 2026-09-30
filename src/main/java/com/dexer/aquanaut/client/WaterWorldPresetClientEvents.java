package com.dexer.aquanaut.client;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.client.screen.SwimmingFishAnimator;
import com.dexer.aquanaut.client.screen.WaterWorldPresetScreen;
import com.dexer.aquanaut.core.WorldPresetRegistry;
import net.minecraft.Util;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterPresetEditorsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Client plumbing for the water world preset's entry on the create-world screen.
 *
 * <p>
 * The world type option is a {@link CycleButton} of {@link WorldCreationUiState.WorldTypeEntry}
 * values, so "the water world option" is whatever the button currently displays. While it shows
 * the water world, its label is tinted water blue (through NeoForge's
 * {@link AbstractWidget#setFGColor}) before the screen renders, and a small school of fish swims
 * across the button after it renders, clipped to the button's bounds. Selecting any other world
 * type restores the vanilla look on the next frame.
 *
 * <p>
 * The same event class registers the preset's {@link net.minecraft.client.gui.screens.worldselection.PresetEditor}
 * on the mod bus, which enables the "Customize" button and opens {@link WaterWorldPresetScreen}.
 */
@EventBusSubscriber(modid = Aquanaut.MODID, value = Dist.CLIENT)
public final class WaterWorldPresetClientEvents {

    /** Water-blue label for the world type option; RGB only — the widget ORs its own alpha in. */
    private static final int BUTTON_TEXT_COLOR = 0x3FB9F2;

    /** In-button aquarium: pixels are drawn at GUI scale 1 with a slightly translucent body. */
    private static final int BUTTON_FISH_SCALE = 1;
    private static final int BUTTON_FISH_COUNT = 3;
    private static final int BUTTON_BUBBLE_COUNT = 2;
    private static final int BUTTON_FISH_ALPHA = 216;
    private static final int BUTTON_INSET = 3;

    private WaterWorldPresetClientEvents() {
    }

    /** Mod bus: makes the "Customize" button on the create-world screen open the aquarium screen. */
    @SubscribeEvent
    public static void onRegisterPresetEditors(RegisterPresetEditorsEvent event) {
        event.register(WorldPresetRegistry.WATER_WORLD,
                (lastScreen, context) -> new WaterWorldPresetScreen(lastScreen));
    }

    /** Game bus, before the screen draws: tint the option's label while it shows the water world. */
    @SubscribeEvent
    public static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof CreateWorldScreen screen)) {
            return;
        }
        for (GuiEventListener child : screen.children()) {
            if (child instanceof CycleButton<?> button
                    && button.getValue() instanceof WorldCreationUiState.WorldTypeEntry entry) {
                if (isWaterWorld(entry)) {
                    button.setFGColor(BUTTON_TEXT_COLOR);
                } else {
                    button.clearFGColor();
                }
            }
        }
    }

    /** Game bus, after the screen draws: swim the fish over the water world option. */
    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof CreateWorldScreen screen)) {
            return;
        }
        for (GuiEventListener child : screen.children()) {
            if (child instanceof CycleButton<?> button
                    && button.getValue() instanceof WorldCreationUiState.WorldTypeEntry entry
                    && isWaterWorld(entry)
                    && button instanceof AbstractWidget widget
                    && widget.visible && widget.isActive()) {
                int x = widget.getX() + BUTTON_INSET;
                int y = widget.getY() + BUTTON_INSET;
                int width = widget.getWidth() - BUTTON_INSET * 2;
                int height = widget.getHeight() - BUTTON_INSET * 2;
                SwimmingFishAnimator.render(event.getGuiGraphics(), x, y, width, height,
                        BUTTON_FISH_SCALE, Util.getMillis(), BUTTON_FISH_COUNT, BUTTON_BUBBLE_COUNT,
                        BUTTON_FISH_ALPHA);
            }
        }
    }

    private static boolean isWaterWorld(WorldCreationUiState.WorldTypeEntry entry) {
        return entry.preset() != null && entry.preset().is(WorldPresetRegistry.WATER_WORLD);
    }
}
