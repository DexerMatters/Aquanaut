package com.dexer.aquanaut.client.screen;

import net.minecraft.client.gui.GuiGraphics;

/**
 * A tiny pixel-art aquarium for GUI surfaces: a school of fish crossing a rectangle, tails
 * wagging, plus rising bubbles. Everything is drawn with immediate-mode fills from hardcoded
 * bitmaps, so there is no texture to ship and the scene stays crisp at any pixel scale.
 *
 * <p>
 * Used by the water world preset in two places: as a subtle overlay inside the world type
 * cycle button on the create-world screen (scale 1), and as the centerpiece of the water
 * world customize screen (scale 3).
 */
public final class SwimmingFishAnimator {

    /** Fish bitmap size in pixels; frames below must all be exactly this shape. */
    private static final int FISH_W = 10;
    private static final int FISH_H = 6;

    /**
     * Tail-wag cycle for a right-facing fish. {@code b}=body, {@code l}=belly, {@code d}=dorsal
     * and pelvic fins, {@code e}=eye, {@code t}=tail; anything else is transparent.
     */
    private static final String[][] FRAMES = {
            {
                    "....dd....",
                    "tt.bbbb...",
                    ".tbbbebb..",
                    "..bbbbbb..",
                    "...llll...",
                    "....ll...."
            },
            {
                    "....dd....",
                    "...bbbb...",
                    "ttbbbebb..",
                    "ttbbbbbb..",
                    "...llll...",
                    "....ll...."
            },
            {
                    "....dd....",
                    "...bbbb...",
                    "..bbbebb..",
                    ".tbbbbbb..",
                    "tt.llll...",
                    "....ll...."
            }
    };

    /** body / fins / belly / eye */
    private record Palette(int body, int fin, int belly, int eye) {
    }

    /** Mostly blues and cyans, with one clownfish-orange accent so the school reads at a glance. */
    private static final Palette[] PALETTES = {
            new Palette(0x4FC3F7, 0x1E88E5, 0xB3E5FC, 0x0D2B45),
            new Palette(0x26C6DA, 0x00838F, 0xB2EBF2, 0x0D2B45),
            new Palette(0x5C6BC0, 0x283593, 0x9FA8DA, 0x0D1B3E),
            new Palette(0xFFA726, 0xE65100, 0xFFE0B2, 0x3E2723)
    };

    private static final int BUBBLE_COLOR = 0xCDEFFF;

    private SwimmingFishAnimator() {
    }

    /**
     * Draws the school clipped to the given rectangle.
     *
     * @param scale       pixel size of one bitmap pixel; 1 for the world type button
     * @param millis      wall-clock time (e.g. {@code Util.getMillis()}); drives all motion
     * @param fishCount   how many fish swim across the rectangle
     * @param bubbleCount how many bubble streams rise through it
     * @param alpha       0-255 opacity applied to fish and bubbles
     */
    public static void render(GuiGraphics graphics, int x, int y, int width, int height,
                              int scale, long millis, int fishCount, int bubbleCount, int alpha) {
        if (width <= 0 || height <= 0 || scale <= 0) {
            return;
        }
        graphics.enableScissor(x, y, x + width, y + height);

        int pad = (FISH_W + 2) * scale;
        double path = width + pad * 2.0;
        for (int i = 0; i < fishCount; i++) {
            double speed = 0.010 + 0.007 * ((i * 5 + 2) % 4) / 3.0;
            double u = positiveMod(millis * speed + i * path * 0.37, path * 2.0);
            boolean facingRight = u < path;
            double along = facingRight ? u : path * 2.0 - u;
            int fx = (int) Math.round(x - pad + along);

            int usable = Math.max(0, height - FISH_H * scale - 2 * scale);
            double lane = positiveMod(i * 0.6180339887, 1.0);
            int bob = (int) Math.round(Math.sin(millis / 420.0 + i * 2.1) * scale);
            int fy = y + scale + (int) Math.round(usable * lane) + bob;

            int frame = (int) positiveMod(millis / 130.0 + i * 2.0, FRAMES.length);
            drawFish(graphics, fx, fy, scale, facingRight, FRAMES[frame], PALETTES[i % PALETTES.length], alpha);
        }

        int bubbleSize = Math.max(1, Math.round(scale * 0.6F));
        for (int b = 0; b < bubbleCount; b++) {
            double rise = height + 6.0 * scale;
            double u = positiveMod(millis * (0.008 + 0.006 * ((b * 3) % 5) / 4.0) + b * 53.0, rise);
            int drift = (int) Math.round(Math.sin(millis / 500.0 + b * 1.7) * 1.5 * scale);
            int span = Math.max(1, width - 6 * scale);
            int bx = x + 3 * scale + (b * 37 + 11) % span + drift;
            int by = (int) Math.round(y + height + 2 * scale - u);
            int fade = (int) (alpha * Math.min(1.0, (rise - u) / (rise * 0.35)));
            fade = Math.max(24, Math.min(alpha, fade));
            graphics.fill(bx, by, bx + bubbleSize, by + bubbleSize, (fade << 24) | BUBBLE_COLOR);
        }

        graphics.disableScissor();
    }

    private static void drawFish(GuiGraphics graphics, int x, int y, int scale, boolean facingRight,
                                 String[] frame, Palette palette, int alpha) {
        for (int row = 0; row < FISH_H; row++) {
            String line = frame[row];
            for (int col = 0; col < FISH_W; col++) {
                char c = line.charAt(facingRight ? col : FISH_W - 1 - col);
                int rgb = switch (c) {
                    case 'b' -> palette.body();
                    case 'd', 't' -> palette.fin();
                    case 'l' -> palette.belly();
                    case 'e' -> palette.eye();
                    default -> -1;
                };
                if (rgb < 0) {
                    continue;
                }
                int px = x + col * scale;
                int py = y + row * scale;
                graphics.fill(px, py, px + scale, py + scale, (alpha << 24) | rgb);
            }
        }
    }

    private static double positiveMod(double value, double modulus) {
        double m = value % modulus;
        return m < 0.0 ? m + modulus : m;
    }
}
