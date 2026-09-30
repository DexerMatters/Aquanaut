package com.dexer.aquanaut.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The customize screen behind the water world preset, opened from the create-world screen's
 * "Customize" button through {@code RegisterPresetEditorsEvent}.
 *
 * <p>
 * The preset is fixed data — there is nothing to configure — so instead of a settings form this
 * is an aquarium: a deep blue water column with drifting light shafts, a school of fish and
 * rising bubbles, plus a short description of what the world type contains.
 */
public class WaterWorldPresetScreen extends Screen {

    private static final int GRADIENT_BANDS = 32;
    private static final int SURFACE_COLOR = 0x0E6FA8;
    private static final int MID_COLOR = 0x084C7C;
    private static final int ABYSS_COLOR = 0x032B4A;
    private static final int SHAFT_COLOR = 0xBFE9FF;

    private static final int TITLE_COLOR = 0xFFEAF7FF;
    private static final int SUMMARY_COLOR = 0xFF74C7F5;
    private static final int DESCRIPTION_COLOR = 0xFFC4DDEE;
    private static final int HINT_COLOR = 0xFF8FB4CC;

    private static final int FISH_SCALE = 3;
    private static final int FISH_COUNT = 7;
    private static final int BUBBLE_COUNT = 10;

    private final Screen lastScreen;

    public WaterWorldPresetScreen(Screen lastScreen) {
        super(Component.translatable("generator.aquanaut.water_world"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 27, 200, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        long millis = Util.getMillis();
        Component summary = Component.translatable("gui.aquanaut.water_world.screen.summary")
                .copy().withStyle(ChatFormatting.ITALIC);
        Component description = Component.translatable("gui.aquanaut.water_world.screen.description");
        Component hint = Component.translatable("gui.aquanaut.water_world.screen.hint")
                .copy().withStyle(ChatFormatting.ITALIC);

        int wrapWidth = Math.min(this.width - 40, 360);
        var descriptionLines = this.font.split(description, wrapWidth);
        int descriptionBottom = this.height - 52;
        int descriptionTop = descriptionBottom - descriptionLines.size() * (this.font.lineHeight + 2);
        int aquariumTop = 58;
        int aquariumBottom = Math.max(aquariumTop + FISH_SCALE * 8, descriptionTop - 10);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, TITLE_COLOR);
        guiGraphics.drawCenteredString(this.font, summary, this.width / 2, 20 + this.font.lineHeight + 8, SUMMARY_COLOR);

        SwimmingFishAnimator.render(guiGraphics,
                20, aquariumTop, this.width - 40, aquariumBottom - aquariumTop,
                FISH_SCALE, millis, FISH_COUNT, BUBBLE_COUNT, 255);

        int lineY = descriptionTop;
        for (var line : descriptionLines) {
            guiGraphics.drawCenteredString(this.font, line, this.width / 2, lineY, DESCRIPTION_COLOR);
            lineY += this.font.lineHeight + 2;
        }
        guiGraphics.drawCenteredString(this.font, hint, this.width / 2, this.height - 46, HINT_COLOR);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        long millis = Util.getMillis();

        // Water column: bright near the surface, near black in the abyss.
        for (int band = 0; band < GRADIENT_BANDS; band++) {
            float t = (float) band / (GRADIENT_BANDS - 1);
            int color = t < 0.55F
                    ? lerpColor(SURFACE_COLOR, MID_COLOR, t / 0.55F)
                    : lerpColor(MID_COLOR, ABYSS_COLOR, (t - 0.55F) / 0.45F);
            int y0 = band * this.height / GRADIENT_BANDS;
            int y1 = (band + 1) * this.height / GRADIENT_BANDS;
            guiGraphics.fill(0, y0, this.width, y1, 0xFF000000 | color);
        }

        // Slow light shafts fading with depth.
        for (int shaft = 0; shaft < 6; shaft++) {
            double drift = Math.sin(millis / 9000.0 + shaft * 2.0) * 24.0;
            int x = (int) (shaft * this.width / 6.0 + drift + this.width * 0.04);
            int shaftWidth = 12 + (shaft % 3) * 8;
            for (int segment = 0; segment < 8; segment++) {
                int alpha = 26 - segment * 3;
                if (alpha <= 0) {
                    break;
                }
                int y0 = segment * this.height / 8;
                int y1 = (segment + 1) * this.height / 8;
                guiGraphics.fill(x, y0, x + shaftWidth, y1, (alpha << 24) | SHAFT_COLOR);
            }
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static int lerpColor(int from, int to, float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        int r = (int) Mth.lerp(t, (from >> 16) & 0xFF, (to >> 16) & 0xFF);
        int g = (int) Mth.lerp(t, (from >> 8) & 0xFF, (to >> 8) & 0xFF);
        int b = (int) Mth.lerp(t, from & 0xFF, to & 0xFF);
        return (r << 16) | (g << 8) | b;
    }
}
