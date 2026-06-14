import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Generates semi-transparent jelly block sprites for Aquanaut.
 * Variants: light_red, light_cyan, white, light_golden.
 * Realistic jelly: translucent body, specular highlights, wobbly internal
 * refraction, and a light edge outline on the block face.
 */
public final class GenerateAquanautJellyTextures {
    private static final int SIZE = 16;
    private static final Path OUT_DIR = Paths.get(
            "src", "main", "resources", "assets", "aquanaut", "textures", "block");

    // ── Jelly palettes (RGB only, alpha applied per-pixel) ────────────
    private static final int[] JR_BODY  = {220, 150, 150};  // light red
    private static final int[] JR_HI    = {245, 200, 200};  // red highlight
    private static final int[] JR_DEEP  = {190, 110, 110};  // red deep
    private static final int[] JR_EDGE  = {235, 180, 180};  // red outline

    private static final int[] JC_BODY  = {150, 210, 220};  // light cyan
    private static final int[] JC_HI    = {200, 240, 245};  // cyan highlight
    private static final int[] JC_DEEP  = {110, 180, 195};  // cyan deep
    private static final int[] JC_EDGE  = {180, 225, 235};  // cyan outline

    private static final int[] JW_BODY  = {230, 230, 235};  // white
    private static final int[] JW_HI    = {248, 248, 252};  // white highlight
    private static final int[] JW_DEEP  = {210, 210, 218};  // white deep
    private static final int[] JW_EDGE  = {240, 240, 244};  // white outline

    private static final int[] JG_BODY  = {220, 200, 140};  // light golden
    private static final int[] JG_HI    = {245, 230, 185};  // golden highlight
    private static final int[] JG_DEEP  = {195, 170, 105};  // golden deep
    private static final int[] JG_EDGE  = {235, 215, 160};  // golden outline

    private static final float TWO_PI = 6.283185307f;

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUT_DIR);

        System.out.println("Generating jelly textures...");
        generateJelly("light_red_jelly_block",    JR_BODY, JR_HI, JR_DEEP, JR_EDGE);
        generateJelly("light_cyan_jelly_block",  JC_BODY, JC_HI, JC_DEEP, JC_EDGE);
        generateJelly("white_jelly_block",       JW_BODY, JW_HI, JW_DEEP, JW_EDGE);
        generateJelly("light_golden_jelly_block",JG_BODY, JG_HI, JG_DEEP, JG_EDGE);

        // Generate item textures (same as block, scaled appropriately)
        // Items use the same texture in 1.21.1
        System.out.println("Generated 4 jelly block textures in " + OUT_DIR);
    }

    private static void generateJelly(String name, int[] body, int[] hi, int[] deep, int[] edge)
            throws IOException {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                // ── Edge outline detection ──
                boolean isEdge = (x == 0 || x == 15 || y == 0 || y == 15);
                boolean isNearEdge = (x == 1 || x == 14 || y == 1 || y == 14);

                // ── Wobbly internal pattern (jelly refraction) ──
                float wave1 = sin01((x * 1.7f + y * 0.8f) * TWO_PI / 12f + 0.4f);
                float wave2 = sin01((x * 0.6f - y * 1.9f) * TWO_PI / 9f + 2.1f);
                float wave3 = sin01((x * 1.1f + y * 1.3f) * TWO_PI / 7f + 4.7f);
                float wobble = wave1 * 0.4f + wave2 * 0.35f + wave3 * 0.25f;

                // ── Specular highlight (glossy surface reflection) ──
                // Two bright spots simulating light sources reflecting off the jelly
                float dx1 = (x - 4.5f) * (x - 4.5f) + (y - 3.5f) * (y - 3.5f);
                float dx2 = (x - 10f) * (x - 10f) + (y - 11f) * (y - 11f);
                float highlight1 = Math.max(0f, 1f - dx1 / 28f);
                float highlight2 = Math.max(0f, 1f - dx2 / 40f);
                float specular = highlight1 * 0.55f + highlight2 * 0.30f;

                // ── Color computation ──
                int r, g, b, a;

                // Base alpha: more transparent with subtle variation
                float baseAlpha = 0.22f + wobble * 0.14f;  // 0.22–0.36
                float alpha = baseAlpha;

                // Mix body ↔ deep based on wobble
                float bodyMix = 0.3f + wobble * 0.7f;  // 0.3–1.0
                r = lerpComp(deep[0], body[0], bodyMix);
                g = lerpComp(deep[1], body[1], bodyMix);
                b = lerpComp(deep[2], body[2], bodyMix);

                // Apply specular highlight: brightens + increases opacity
                if (specular > 0.05f) {
                    float s = Math.min(specular, 1f);
                    r = lerpComp(r, hi[0], s * 0.9f);
                    g = lerpComp(g, hi[1], s * 0.9f);
                    b = lerpComp(b, hi[2], s * 0.9f);
                    alpha = Math.min(1f, alpha + s * 0.20f);  // highlight = slightly more opaque
                }

                // ── Edge outline: brighter, more opaque, defined border ──
                if (isEdge) {
                    // Solid bright outline at the very edge
                    r = edge[0];
                    g = edge[1];
                    b = edge[2];
                    alpha = 0.62f;
                } else if (isNearEdge) {
                    // Inner glow behind the outline — blends toward edge
                    float edgeFade = 0.45f;
                    r = lerpComp(r, edge[0], edgeFade);
                    g = lerpComp(g, edge[1], edgeFade);
                    b = lerpComp(b, edge[2], edgeFade);
                    alpha = Math.min(1f, alpha + 0.06f);
                }

                // Clamp
                r = clamp(r, 0, 255);
                g = clamp(g, 0, 255);
                b = clamp(b, 0, 255);
                a = clamp((int)(alpha * 255), 0, 255);

                img.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }

        // ── Additional: subtle internal speckles (air bubbles / impurities) ──
        scatterJelly(img, 41, 4, hi, 120, 0.40f);
        scatterJelly(img, 47, 2, edge, 140, 0.30f);
        scatterJelly(img, 53, 2, hi, 160, 0.25f);

        ImageIO.write(img, "png", OUT_DIR.resolve(name + ".png").toFile());
    }

    // ── Utility ──────────────────────────────────────────────────────

    private static float sin01(float x) {
        return (float)((Math.sin(x) + 1.0) * 0.5);
    }

    private static int lerpComp(int a, int b, float t) {
        float u = 1f - Math.max(0f, Math.min(1f, t));
        float v = Math.max(0f, Math.min(1f, t));
        return Math.round(a * u + b * v);
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static int hash(int n) {
        n = (n ^ (n >>> 13)) * 1274126177;
        return n ^ (n >>> 16);
    }

    private static void scatterJelly(BufferedImage img, int seed, int count, int[] color, int alpha, float strength) {
        for (int i = 0; i < count; i++) {
            int h = hash(seed + i * 31);
            int x = 1 + ((h & 0xFF) % (SIZE - 2));
            int y = 1 + (((h >>> 8) & 0xFF) % (SIZE - 2));
            int current = img.getRGB(x, y);
            int cr = (current >>> 16) & 0xFF;
            int cg = (current >>> 8) & 0xFF;
            int cb = current & 0xFF;
            int ca = (current >>> 24) & 0xFF;
            int nr = lerpComp(cr, color[0], strength);
            int ng = lerpComp(cg, color[1], strength);
            int nb = lerpComp(cb, color[2], strength);
            int na = Math.min(255, ca + (int)(alpha * strength));
            img.setRGB(x, y, (na << 24) | (nr << 16) | (ng << 8) | nb);
        }
    }
}
