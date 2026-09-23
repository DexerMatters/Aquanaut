package com.dexer.aquanaut.client.drone;

import java.util.Locale;

import com.dexer.aquanaut.common.PressureHelper;
import com.dexer.aquanaut.common.drone.SubmarineDroneService;
import com.dexer.aquanaut.common.entity.SubmarineDroneEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * The drone's field monitor: a small camera window in the corner of the screen, with its instruments
 * drawn <em>on</em> the picture, the way a real first-person-view drone feed carries its telemetry.
 *
 * <h3>An overlay, not a window</h3>
 * The operator keeps their own view. The drone's camera is laid over it as a nearly solid screen — a
 * monitor, not a tint on the world they are standing in — and nothing at all is drawn over the rest of
 * the screen. The window is small — a fraction of the screen width — and anchored in the bottom-right
 * corner, where it clears the crosshair, the hotbar and the status bars, exactly where a
 * picture-in-picture belongs.
 *
 * <h3>On-screen display</h3>
 * Real FPV gear prints its numbers over the image, not in a panel beside it: a recording telltale and
 * the link in the top corners, depth and heading along the bottom, speed and pitch under those, and a
 * warning across the top when something is actually wrong. Every figure is real — depth and pressure
 * from {@link PressureHelper}, speed from the hull's own velocity, heading and pitch from its
 * rotation, and the link from how far the operator has let the drone get from
 * {@link SubmarineDroneService#CONTROL_RANGE the edge of the radio range} — a range the drone itself
 * polices, so the readout degrades and then the link is gone rather than sitting at nothing forever.
 */
public final class DroneHudRenderer {

    // ── layout ────────────────────────────────────────────────────────────────
    private static final int MARGIN = 6;
    private static final int LINE = 10;

    /** Screen rows left clear at the bottom for the hotbar and the status bars. */
    private static final int BOTTOM_RESERVE = 24;

    /** Window width as a fraction of the screen, clamped to a band that stays small. */
    private static final float WIDTH_FRACTION = 0.26F;
    private static final int MIN_WIDTH = 140;
    private static final int MAX_WIDTH = 200;
    private static final int MIN_SAFE_WIDTH = 96;

    /** Legend lines under the window. */
    private static final int LEGEND_LINES = 4;

    /** Above this fraction of the control range the link is called degraded. */
    private static final float LINK_WARN = 0.75F;

    // ── palette ───────────────────────────────────────────────────────────────
    /** The window's bezel, and the backing shown when there is no picture. */
    private static final int BEZEL = 0x99050A0E;
    /** The dark chip behind each piece of on-screen text, so it reads over anything. */
    private static final int CHIP = 0x8C000000;
    private static final int TEXT = 0xFFE2FAFF;
    private static final int TEXT_DIM = 0x998FB4C4;
    private static final int LEGEND = 0x778FB4C4;
    private static final int GOOD = 0xFF7BE39B;
    private static final int WARN = 0xFFF2C14E;
    private static final int BAD = 0xFFE86A6A;
    private static final int LIVE = 0xFFE05C5C;

    private DroneHudRenderer() {
    }

    public static void render(GuiGraphics graphics, Minecraft minecraft) {
        SubmarineDroneEntity drone = ClientDroneState.drone();
        if (drone == null || minecraft.level == null) {
            return;
        }
        Font font = minecraft.font;
        int guiWidth = graphics.guiWidth();
        int guiHeight = graphics.guiHeight();
        if (guiWidth < 220) {
            return;
        }

        int width = Mth.clamp(Math.round(guiWidth * WIDTH_FRACTION), MIN_WIDTH, MAX_WIDTH);
        // Never reach past the middle of the screen, because that is where the crosshair lives. On a
        // narrow screen the window gets narrower rather than covering the player's aim.
        int clearWidth = guiWidth - MARGIN - (guiWidth / 2 + 5);
        if (width > clearWidth) {
            width = Math.max(MIN_SAFE_WIDTH, clearWidth);
        }

        int feedHeight = Math.max(56, Math.round(width * 9.0F / 16.0F));
        int legendHeight = LEGEND_LINES * LINE;
        int x = guiWidth - MARGIN - width;
        int bottom = guiHeight - MARGIN - BOTTOM_RESERVE;
        int legendTop = bottom - legendHeight;
        int feedTop = legendTop - 2 - feedHeight;
        if (feedTop < MARGIN) {
            // A screen too short for the window is a screen the window would dominate.
            return;
        }

        float pressure = PressureHelper.getPressure(drone);
        int depthRange = Math.max(1, minecraft.level.getSeaLevel() - minecraft.level.getMinBuildHeight());
        float depth = pressure * depthRange;
        float speed = (float) drone.getDeltaMovement().length() * 20.0F;
        float heading = normaliseHeading(drone.getYRot());
        float pitch = -drone.getXRot();
        float signal = signalQuality(minecraft.player, drone);

        drawWindow(graphics, minecraft, font, x, feedTop, width, feedHeight);
        drawOsd(graphics, font, x, feedTop, width, feedHeight, depth, pressure, speed, heading, pitch,
                signal);
        drawLegend(graphics, font, x, legendTop, width);
    }

    // ------------------------------------------------------------------
    // the window
    // ------------------------------------------------------------------

    private static void drawWindow(GuiGraphics graphics, Minecraft minecraft, Font font, int x, int y,
            int width, int height) {
        // A thin bezel, so the viewport reads as a screen set into the corner of the HUD.
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, BEZEL);

        boolean hasPicture = DroneFeedRenderer.isAvailable()
                && DroneFeedRenderer.verdict() != DroneFeedRenderer.Verdict.BLACK;
        if (hasPicture) {
            DroneFeedRenderer.draw(graphics, x, y, width, height);
            return;
        }

        String key = DroneFeedRenderer.hasFailed()
                ? "hud.aquanaut.drone.feed.failed"
                : DroneFeedRenderer.isAvailable()
                        ? "hud.aquanaut.drone.feed.black"
                        : "hud.aquanaut.drone.feed.waiting";
        String label = Component.translatable(key).getString();
        graphics.fill(x, y, x + width, y + height, BEZEL);
        graphics.drawString(font, label, x + (width - font.width(label)) / 2, y + height / 2 - 4,
                TEXT_DIM, false);
    }

    // ------------------------------------------------------------------
    // the on-screen display
    // ------------------------------------------------------------------

    private static void drawOsd(GuiGraphics graphics, Font font, int x, int y, int width, int height,
            float depth, float pressure, float speed, float heading, float pitch, float signal) {
        boolean submerged = pressure > 0.0F;

        // Top left: the recording telltale.
        drawRecording(graphics, font, x + 4, y + 3);

        // Top right: the link.
        String link = Component.translatable("hud.aquanaut.drone.link",
                Math.round(signal * 100.0F)).getString();
        int linkWidth = font.width(link);
        int right = x + width - 4;
        graphics.fill(right - linkWidth - 4, y + 2, right + 2, y + 12, CHIP);
        graphics.drawString(font, link, right - linkWidth, y + 3, signalColor(signal), false);

        // Top centre: one warning, only when something is actually wrong.
        String warning = warning(pressure, signal, submerged);
        if (warning != null) {
            int warningWidth = font.width(warning);
            int centre = x + width / 2;
            graphics.fill(centre - warningWidth / 2 - 3, y + 13, centre + warningWidth / 2 + 3, y + 23, CHIP);
            graphics.drawString(font, warning, centre - warningWidth / 2, y + 14,
                    warningColour(signal), false);
        }

        // Bottom: two rows of figures, left and right, the way a real OSD lays them out.
        int rowTop = y + height - 22;
        osdLeft(graphics, font, "hud.aquanaut.drone.depth.short",
                submerged ? String.format(Locale.ROOT, "%.1fm", depth) : "--", x + 4, rowTop,
                submerged ? TEXT : TEXT_DIM);
        osdRight(graphics, font, "hud.aquanaut.drone.heading.short",
                String.format(Locale.ROOT, "%03d", Math.round(heading)), x + width - 4, rowTop, TEXT);

        int lowerRow = rowTop + LINE;
        osdLeft(graphics, font, "hud.aquanaut.drone.speed.short",
                String.format(Locale.ROOT, "%.1fm/s", speed), x + 4, lowerRow, TEXT);
        osdRight(graphics, font, "hud.aquanaut.drone.pitch.short",
                String.format(Locale.ROOT, "%+.0f", pitch), x + width - 4, lowerRow, TEXT);
    }

    /** The recording telltale: a red dot and the word, on a chip. */
    private static void drawRecording(GuiGraphics graphics, Font font, int x, int y) {
        String rec = Component.translatable("hud.aquanaut.drone.feed.live").getString();
        graphics.fill(x - 2, y - 1, x + font.width(rec) + 6, y + 10, CHIP);
        graphics.fill(x, y + 3, x + 3, y + 6, LIVE);
        graphics.drawString(font, rec, x + 5, y, TEXT, false);
    }

    /** A labelled figure, chip-backed, flush left. */
    private static void osdLeft(GuiGraphics graphics, Font font, String labelKey, String value, int x,
            int y, int colour) {
        String label = Component.translatable(labelKey).getString() + " ";
        int width = font.width(label) + font.width(value);
        graphics.fill(x - 2, y - 1, x + width + 2, y + 10, CHIP);
        graphics.drawString(font, label, x, y, TEXT_DIM, false);
        graphics.drawString(font, value, x + font.width(label), y, colour, false);
    }

    /** A labelled figure, chip-backed, flush right. */
    private static void osdRight(GuiGraphics graphics, Font font, String labelKey, String value,
            int rightX, int y, int colour) {
        String label = Component.translatable(labelKey).getString() + " ";
        int valueWidth = font.width(value);
        int x = rightX - font.width(label) - valueWidth;
        graphics.fill(x - 2, y - 1, rightX + 2, y + 10, CHIP);
        graphics.drawString(font, label, x, y, TEXT_DIM, false);
        graphics.drawString(font, value, x + font.width(label), y, colour, false);
    }

    /** The single most useful thing the pilot can be told, or nothing at all. */
    private static String warning(float pressure, float signal, boolean submerged) {
        // There is no "signal lost" line to write: a link that reaches nothing is dropped, so the panel
        // stops existing instead of reporting it. What is left is the range where it still works.
        if (!submerged) {
            return Component.translatable("hud.aquanaut.drone.warning.airborne").getString();
        }
        if (pressure > 0.85F) {
            return Component.translatable("hud.aquanaut.drone.warning.crush").getString();
        }
        if (signal < LINK_WARN) {
            return Component.translatable("hud.aquanaut.drone.warning.signal.weak").getString();
        }
        return null;
    }

    /** Red for the one state that ends the flight, amber for the ones that merely shorten it. */
    private static int warningColour(float signal) {
        return signal <= 0.0F ? BAD : WARN;
    }

    // ------------------------------------------------------------------
    // legend
    // ------------------------------------------------------------------

    private static void drawLegend(GuiGraphics graphics, Font font, int x, int y, int width) {
        drawLegendLine(graphics, font, "hud.aquanaut.drone.controls.attitude", x, y, width);
        drawLegendLine(graphics, font, "hud.aquanaut.drone.controls.throttle", x, y + LINE, width);
        drawLegendLine(graphics, font, "hud.aquanaut.drone.controls.headlight", x, y + 2 * LINE, width);
        drawLegendLine(graphics, font, "hud.aquanaut.drone.controls.link", x, y + 3 * LINE, width);
    }

    private static void drawLegendLine(GuiGraphics graphics, Font font, String key, int x, int y,
            int width) {
        String line = Component.translatable(key).getString();
        graphics.drawString(font, font.plainSubstrByWidth(line, width), x, y, LEGEND, false);
    }

    // ------------------------------------------------------------------
    // signals
    // ------------------------------------------------------------------

    private static int signalColor(float signal) {
        if (signal <= 0.0F) {
            return BAD;
        }
        return signal < LINK_WARN ? WARN : GOOD;
    }

    private static float normaliseHeading(float yaw) {
        return (yaw % 360.0F + 360.0F) % 360.0F;
    }

    /**
     * Link quality as a fraction of the control range, fading with distance in the usual inverse
     * way: full strength until the drone is well out, then falling away to nothing at the edge.
     */
    private static float signalQuality(Player player, SubmarineDroneEntity drone) {
        if (player == null) {
            return 0.0F;
        }
        double distance = Math.sqrt(player.distanceToSqr(drone));
        if (distance >= SubmarineDroneService.CONTROL_RANGE) {
            return 0.0F;
        }
        float linear = (float) (1.0D - distance / SubmarineDroneService.CONTROL_RANGE);
        return Mth.clamp(linear * linear, 0.0F, 1.0F);
    }
}
