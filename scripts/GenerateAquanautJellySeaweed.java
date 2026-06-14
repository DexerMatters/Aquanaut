import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Generates seaweed-wrapped jelly textures.
 * Realistic vine-like seaweed strands twining around the jelly cube face,
 * wrapping in from edges with organic thickness variation, subtle 3D shading,
 * and partial translucency showing the jelly beneath.
 */
public final class GenerateAquanautJellySeaweed {
    private static final int S = 16;
    private static final Path OUT_DIR = Paths.get(
            "src", "main", "resources", "assets", "aquanaut", "textures", "block");

    // ── Seaweed vine palette (dark muted green tones) ─────────────────
    private static final int SW_LEAF   = rgba(52, 102, 58);   // main leaf
    private static final int SW_LIGHT  = rgba(72, 128, 78);   // highlight edge
    private static final int SW_SHADOW = rgba(34, 76, 40);    // shadow side
    private static final int SW_DARK   = rgba(22, 54, 28);    // deep shadow / stem
    private static final int SW_PALE   = rgba(88, 145, 92);   // bright tip highlight

    // ── Jelly file names ─────────────────────────────────────────────
    private static final String[] JELLY_FILES = {
        "light_red_jelly_block.png",
        "light_cyan_jelly_block.png",
        "white_jelly_block.png",
        "light_golden_jelly_block.png",
    };
    private static final String[] OUT_NAMES = {
        "light_red_jelly_block_seaweed.png",
        "light_cyan_jelly_block_seaweed.png",
        "white_jelly_block_seaweed.png",
        "light_golden_jelly_block_seaweed.png",
    };

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < JELLY_FILES.length; i++) {
            BufferedImage jelly = ImageIO.read(OUT_DIR.resolve(JELLY_FILES[i]).toFile());
            BufferedImage out = wrapWithSeaweed(jelly);
            ImageIO.write(out, "png", OUT_DIR.resolve(OUT_NAMES[i]).toFile());
        }
        System.out.println("Generated 4 seaweed-wrapped jelly textures");
    }

    /**
     * Draws realistic vine-like seaweed strands wrapping around the
     * jelly block face — stems entering from edges/corners, branching,
     * with organic thickness and subtle 3D shading.
     */
    private static BufferedImage wrapWithSeaweed(BufferedImage jelly) {
        BufferedImage out = copyImg(jelly);

        // ── Vine strands — each is a bezier-like curve path ──
        // Format per strand: {x0, y0, cx1, cy1, cx2, cy2, x1, y1, thickness}
        // Entry point → control points → exit point
        int[][] strands = {
            // Bottom edge → upper-right (main wrap)
            { 2, 15,   1, 10,   4, 6,    5, 1,   2 },
            // Left edge → right edge (horizontal band)
            { 0, 8,    4, 7,   10, 9,   15, 5,   2 },
            // Top-right corner wrap
            {12, 0,   12, 5,   11, 9,    9, 14,  2 },
            // Thin accent vine
            { 5, 15,   3, 12,   2, 8,     0, 3,   1 },
            // Upper-left diagonal
            { 1, 0,    6, 1,    8, 5,    10, 3,   2 },
            // Right-side vertical wrap
            {15, 2,   13, 6,   14, 10,   15, 14,  1 },
        };

        // ── Draw each strand ──
        for (int[] s : strands) {
            drawVineStrand(out, s[0], s[1], s[2], s[3], s[4], s[5], s[6], s[7], s[8]);
        }

        // ── Tiny leaf nodes / buds along strands ──
        leafNode(out,  4, 8,  2);
        leafNode(out, 10, 8,  2);
        leafNode(out, 11, 4,  1);
        leafNode(out,  8, 2,  2);
        leafNode(out,  3, 13, 2);
        leafNode(out,  7, 13, 1);
        leafNode(out, 13, 11, 1);

        return out;
    }

    // ══════════════════════════════════════════════════════════════════
    // VINE DRAWING
    // ══════════════════════════════════════════════════════════════════

    /** Draw a cubic bezier vine strand with organic thickness. */
    private static void drawVineStrand(BufferedImage img,
            float x0, float y0, float cx1, float cy1,
            float cx2, float cy2, float x1, float y1, int thick) {
        int steps = 28;
        float prevX = x0, prevY = y0;
        for (int i = 1; i <= steps; i++) {
            float t = (float) i / steps;
            // Cubic bezier
            float u = 1f - t;
            float x = u*u*u*x0 + 3*u*u*t*cx1 + 3*u*t*t*cx2 + t*t*t*x1;
            float y = u*u*u*y0 + 3*u*u*t*cy1 + 3*u*t*t*cy2 + t*t*t*y1;

            // Organic thickness variation
            float thickness = thick * (0.7f + 0.3f * sin01(t * 6.28f + 0.7f));
            // Thinner at ends (entering/exiting the block)
            float endFade = Math.min(t * 1.8f, (1f - t) * 1.8f);
            endFade = Math.min(1f, endFade);
            thickness *= endFade;

            drawThickLine(img, prevX, prevY, x, y, thickness);
            prevX = x;
            prevY = y;
        }
    }

    /** Draw a thick anti-aliased line segment between two points. */
    private static void drawThickLine(BufferedImage img, float x0, float y0, float x1, float y1, float thick) {
        float dx = x1 - x0, dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01f) return;

        // Perpendicular direction
        float nx = -dy / len, ny = dx / len;

        // Draw a filled thick line by iterating over a bounding box
        int minX = (int) Math.floor(Math.min(x0, x1) - thick - 1);
        int maxX = (int) Math.ceil(Math.max(x0, x1) + thick + 1);
        int minY = (int) Math.floor(Math.min(y0, y1) - thick - 1);
        int maxY = (int) Math.ceil(Math.max(y0, y1) + thick + 1);

        for (int py = Math.max(0, minY); py <= Math.min(S - 1, maxY); py++) {
            for (int px = Math.max(0, minX); px <= Math.min(S - 1, maxX); px++) {
                // Distance from point to line segment
                float dist = pointToSegmentDist(px, py, x0, y0, x1, y1);

                if (dist <= thick + 0.6f) {
                    // Anti-aliased edge
                    float alpha;
                    if (dist <= thick - 0.5f) {
                        alpha = 1f;  // solid core
                    } else {
                        alpha = 1f - (dist - (thick - 0.5f));  // soft edge
                    }

                    // 3D shading: top-left side lit, bottom-right shadowed
                    float dot = (nx * 0.6f + ny * -0.8f);  // light from upper-left
                    dot = dot * 0.5f + 0.5f;  // remap to 0-1

                    int vineColor;
                    if (dot > 0.65f)      vineColor = SW_LIGHT;
                    else if (dot > 0.40f) vineColor = SW_LEAF;
                    else if (dot > 0.25f) vineColor = SW_SHADOW;
                    else                  vineColor = SW_DARK;

                    blendPixel(img, px, py, vineColor, alpha * 0.95f);
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // LEAF NODES
    // ══════════════════════════════════════════════════════════════════

    /** Draw a small leaf/bud cluster at a position. */
    private static void leafNode(BufferedImage img, int cx, int cy, int size) {
        int[][] offsets = {
            {-1, -1}, {0, -2}, {1, -1},
            {-2,  0}, {0,  0}, {2,  0},
            {-1,  1}, {0,  2}, {1,  1},
        };
        for (int[] off : offsets) {
            int x = cx + off[0], y = cy + off[1];
            if (x < 0 || x >= S || y < 0 || y >= S) continue;
            float dist = (float) Math.sqrt(off[0] * off[0] + off[1] * off[1]);
            float alpha = dist <= size * 0.7f ? 1f : Math.max(0f, 1f - (dist - size * 0.7f));
            int color = dist <= size * 0.3f ? SW_LIGHT : (dist <= size * 0.55f ? SW_LEAF : SW_PALE);
            blendPixel(img, x, y, color, alpha * 0.85f);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // UTILITY
    // ══════════════════════════════════════════════════════════════════

    private static float pointToSegmentDist(float px, float py, float x0, float y0, float x1, float y1) {
        float dx = x1 - x0, dy = y1 - y0;
        float lenSq = dx * dx + dy * dy;
        if (lenSq < 0.0001f) {
            float ddx = px - x0, ddy = py - y0;
            return (float) Math.sqrt(ddx * ddx + ddy * ddy);
        }
        float t = ((px - x0) * dx + (py - y0) * dy) / lenSq;
        t = Math.max(0f, Math.min(1f, t));
        float projX = x0 + t * dx, projY = y0 + t * dy;
        float ddx = px - projX, ddy = py - projY;
        return (float) Math.sqrt(ddx * ddx + ddy * ddy);
    }

    /** Blend a vine color onto the image with alpha. */
    private static void blendPixel(BufferedImage img, int x, int y, int vineRGB, float alpha) {
        int cur = img.getRGB(x, y);
        int cr = (cur >>> 16) & 0xFF, cg = (cur >>> 8) & 0xFF, cb = cur & 0xFF;
        int ca = (cur >>> 24) & 0xFF;

        int vr = (vineRGB >>> 16) & 0xFF, vg = (vineRGB >>> 8) & 0xFF, vb = vineRGB & 0xFF;

        float a = Math.min(1f, alpha);
        int nr = Math.round(cr * (1f - a) + vr * a);
        int ng = Math.round(cg * (1f - a) + vg * a);
        int nb = Math.round(cb * (1f - a) + vb * a);
        int na = Math.min(255, ca + Math.round(255 * a * 0.85f));

        // Clamp
        nr = Math.max(0, Math.min(255, nr));
        ng = Math.max(0, Math.min(255, ng));
        nb = Math.max(0, Math.min(255, nb));

        img.setRGB(x, y, (na << 24) | (nr << 16) | (ng << 8) | nb);
    }

    private static BufferedImage copyImg(BufferedImage src) {
        BufferedImage out = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < S; y++)
            for (int x = 0; x < S; x++)
                out.setRGB(x, y, src.getRGB(x, y));
        return out;
    }

    private static float sin01(float x) {
        return (float) ((Math.sin(x) + 1.0) * 0.5);
    }

    private static int rgba(int r, int g, int b) {
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
