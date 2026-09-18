import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class GenerateAquanautMachineTextures {
    private static final int SIZE = 16;
    private static final Path BLOCK_TEXTURE_DIR = Paths.get(
            "src", "main", "resources", "assets", "aquanaut", "textures", "block");
    private static final Path BASE_TEXTURE = BLOCK_TEXTURE_DIR.resolve("polished_hard_shell_block.png");

    private static final int SHADOW = rgba(31, 52, 61);
    private static final int CORE = rgba(73, 116, 127);
    private static final int HIGHLIGHT = rgba(180, 220, 224);
    private static final int STEEL = rgba(104, 121, 130);
    private static final int DARK_STEEL = rgba(58, 71, 80);
    private static final int COPPER = rgba(132, 92, 54);
    private static final int BRASS = rgba(145, 114, 58);
    private static final int AQUA = rgba(118, 181, 188);
    private static final int GLOW = rgba(184, 231, 236);
    private static final int PALE_GLOW = rgba(211, 241, 244);
    private static final int GLASS = rgba(169, 214, 221);
    private static final int BUBBLE = rgba(223, 245, 248);
    private static final int WARM = rgba(177, 150, 80);
    private static final int GRAPHITE = rgba(40, 55, 64);

    public static void main(String[] args) throws Exception {
        BufferedImage base = readBase();
        Files.createDirectories(BLOCK_TEXTURE_DIR);

        write("hard_shell_frame.png", hardShellFrame(base));
        write("gas_pipe_glass.png", gasPipeGlassWall());
        write("gas_pipe_clamp.png", gasPipeClamp(base));

        System.out.println("Generated 3 block textures in "
                + BLOCK_TEXTURE_DIR);
    }

    private static BufferedImage readBase() throws IOException {
        if (!Files.exists(BASE_TEXTURE)) {
            throw new IOException("Missing base texture: " + BASE_TEXTURE);
        }
        return ImageIO.read(BASE_TEXTURE.toFile());
    }

    private static void write(String fileName, BufferedImage image) throws IOException {
        Path out = BLOCK_TEXTURE_DIR.resolve(fileName);
        ImageIO.write(image, "png", out.toFile());
    }

    private static BufferedImage hardShellFrame(BufferedImage base) {
        BufferedImage img = copy(base);
        drawShellFrameBase(img);
        softMottle(img, 71, 0.018f);
        return img;
    }

    private static BufferedImage gasPipeGlassWall() {
        BufferedImage img = clear();
        fillRect(img, 0, 0, 15, 15, rgba(0, 0, 0, 0));

        int bright = rgba(248, 253, 254);
        int soft = rgba(234, 244, 246);
        int dim = rgba(188, 208, 213);
        int deep = rgba(142, 164, 170);

        // Outer glints: closer to vanilla glass than the previous abstract motif.
        drawLine(img, 5, 1, 10, 1, bright);
        drawLine(img, 4, 2, 10, 2, soft);
        pixel(img, 3, 3, soft);
        pixel(img, 11, 2, dim);

        drawLine(img, 1, 5, 1, 10, bright);
        drawLine(img, 2, 4, 2, 10, soft);
        pixel(img, 3, 11, dim);

        drawLine(img, 5, 13, 10, 13, deep);
        drawLine(img, 6, 12, 11, 12, dim);
        pixel(img, 4, 13, dim);

        drawLine(img, 13, 5, 13, 10, deep);
        drawLine(img, 12, 6, 12, 11, dim);
        pixel(img, 13, 4, dim);

        // Core square sampled by the join planes and item cube.
        drawLine(img, 4, 4, 11, 4, soft);
        drawLine(img, 4, 5, 4, 11, bright);
        drawLine(img, 11, 5, 11, 11, dim);
        drawLine(img, 5, 11, 10, 11, deep);

        pixel(img, 6, 5, bright);
        pixel(img, 7, 5, bright);
        pixel(img, 8, 5, soft);
        pixel(img, 5, 6, soft);
        pixel(img, 6, 6, bright);
        pixel(img, 7, 6, soft);
        pixel(img, 8, 6, soft);
        pixel(img, 6, 7, soft);
        pixel(img, 7, 7, soft);
        pixel(img, 8, 7, dim);
        pixel(img, 7, 8, dim);
        pixel(img, 8, 8, deep);
        return img;
    }

    private static BufferedImage gasPipeClamp(BufferedImage base) {
        BufferedImage img = clear();
        int baseTone = rgba(88, 109, 118);
        int top = mix(baseTone, HIGHLIGHT, 0.26f);
        int side = mix(baseTone, STEEL, 0.16f);
        int front = mix(baseTone, GRAPHITE, 0.10f);
        int shadow = mix(baseTone, SHADOW, 0.28f);
        int deep = mix(baseTone, GRAPHITE, 0.38f);
        int accent = mix(CORE, HIGHLIGHT, 0.18f);
        int seam = mix(baseTone, HIGHLIGHT, 0.08f);

        // Small palette cells used by the 1px rods.
        pixel(img, 0, 0, side);
        pixel(img, 1, 0, top);
        pixel(img, 2, 0, accent);
        pixel(img, 0, 1, front);
        pixel(img, 1, 1, shadow);
        pixel(img, 2, 1, deep);

        // Banded atlas: the rods sample broad 8px strips instead of a single flat texel.
        fillRect(img, 0, 0, 15, 15, deep);
        fillRect(img, 1, 1, 14, 14, baseTone);

        drawLine(img, 1, 0, 14, 0, top);
        drawLine(img, 1, 1, 14, 1, mix(top, side, 0.72f));
        drawLine(img, 1, 2, 14, 2, side);
        drawLine(img, 1, 3, 14, 3, mix(side, accent, 0.22f));
        drawLine(img, 1, 4, 14, 4, front);
        drawLine(img, 1, 5, 14, 5, mix(front, shadow, 0.42f));
        drawLine(img, 1, 6, 14, 6, shadow);
        drawLine(img, 1, 7, 14, 7, mix(shadow, deep, 0.26f));

        drawLine(img, 2, 0, 2, 15, mix(top, seam, 0.40f));
        drawLine(img, 5, 0, 5, 15, mix(side, seam, 0.22f));
        drawLine(img, 8, 0, 8, 15, mix(front, seam, 0.24f));
        drawLine(img, 11, 0, 11, 15, mix(shadow, seam, 0.28f));
        drawLine(img, 13, 0, 13, 15, mix(deep, seam, 0.20f));

        drawLine(img, 0, 8, 15, 8, mix(baseTone, SHADOW, 0.12f));
        drawLine(img, 0, 9, 15, 9, mix(baseTone, HIGHLIGHT, 0.06f));
        drawLine(img, 0, 10, 15, 10, mix(baseTone, GRAPHITE, 0.12f));
        drawLine(img, 0, 11, 15, 11, mix(baseTone, STEEL, 0.10f));

        pixel(img, 2, 2, HIGHLIGHT);
        pixel(img, 5, 2, accent);
        pixel(img, 8, 2, HIGHLIGHT);
        pixel(img, 11, 2, accent);
        pixel(img, 13, 2, HIGHLIGHT);
        pixel(img, 3, 5, mix(accent, HIGHLIGHT, 0.35f));
        pixel(img, 6, 5, mix(front, HIGHLIGHT, 0.25f));
        pixel(img, 9, 5, mix(front, accent, 0.28f));
        pixel(img, 12, 5, mix(shadow, HIGHLIGHT, 0.18f));
        pixel(img, 2, 9, mix(shadow, GRAPHITE, 0.20f));
        pixel(img, 5, 9, mix(front, shadow, 0.26f));
        pixel(img, 8, 9, mix(baseTone, HIGHLIGHT, 0.12f));
        pixel(img, 11, 9, mix(deep, HIGHLIGHT, 0.18f));
        pixel(img, 13, 9, mix(deep, accent, 0.15f));
        pixel(img, 7, 7, mix(accent, HIGHLIGHT, 0.22f));
        pixel(img, 8, 8, mix(baseTone, SHADOW, 0.16f));
        pixel(img, 7, 11, mix(shadow, GRAPHITE, 0.20f));
        pixel(img, 8, 11, mix(front, SHADOW, 0.18f));

        softMottle(img, 93, 0.014f);
        return img;
    }

    private static void drawShellFrameBase(BufferedImage img) {
        fillRect(img, 0, 0, 15, 15, rgba(24, 36, 42));
        panel(img, 1, 1, 14, 14, rgba(32, 46, 53), rgba(64, 84, 93), 0.26f);
        fillRect(img, 2, 2, 13, 13, rgba(41, 58, 65));

        drawLine(img, 2, 2, 13, 2, mix(HIGHLIGHT, STEEL, 0.25f));
        drawLine(img, 2, 13, 13, 13, mix(SHADOW, STEEL, 0.25f));
        drawLine(img, 2, 2, 2, 13, mix(HIGHLIGHT, STEEL, 0.18f));
        drawLine(img, 13, 2, 13, 13, mix(SHADOW, STEEL, 0.20f));

        drawLine(img, 3, 3, 12, 3, mix(STEEL, HIGHLIGHT, 0.12f));
        drawLine(img, 3, 12, 12, 12, mix(STEEL, SHADOW, 0.12f));
        drawLine(img, 3, 3, 3, 12, mix(STEEL, HIGHLIGHT, 0.08f));
        drawLine(img, 12, 3, 12, 12, mix(STEEL, SHADOW, 0.10f));

        pixel(img, 3, 3, HIGHLIGHT);
        pixel(img, 12, 3, HIGHLIGHT);
        pixel(img, 3, 12, SHADOW);
        pixel(img, 12, 12, SHADOW);
        pixel(img, 7, 7, mix(CORE, SHADOW, 0.20f));
    }

    private static BufferedImage copy(BufferedImage src) {
        BufferedImage out = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                out.setRGB(x, y, src.getRGB(x, y));
            }
        }
        return out;
    }

    private static BufferedImage clear() {
        return new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
    }

    private static void panel(BufferedImage img, int x0, int y0, int x1, int y1, int dark, int mid, float midMix) {
        fillRect(img, x0, y0, x1, y1, dark);
        fillRect(img, x0 + 1, y0 + 1, x1 - 1, y1 - 1, mix(dark, mid, midMix));
        highlightLine(img, x0 + 1, y0 + 1, x1 - 2, y0 + 1, mix(mid, HIGHLIGHT, 0.35f));
        highlightLine(img, x0 + 1, y0 + 1, x0 + 1, y1 - 2, mix(mid, HIGHLIGHT, 0.25f));
        shadowLine(img, x0 + 1, y1 - 1, x1 - 2, y1 - 1, mix(dark, SHADOW, 0.6f));
        shadowLine(img, x1 - 1, y0 + 1, x1 - 1, y1 - 2, mix(dark, SHADOW, 0.6f));
    }

    private static void highlightLine(BufferedImage img, int x0, int y0, int x1, int y1, int color) {
        drawLine(img, x0, y0, x1, y1, color);
    }

    private static void shadowLine(BufferedImage img, int x0, int y0, int x1, int y1, int color) {
        drawLine(img, x0, y0, x1, y1, color);
    }

    private static void softMottle(BufferedImage img, int seed, float strength) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int color = img.getRGB(x, y);
                if ((color >>> 24) == 0) {
                    continue;
                }
                int h = hash2d(x, y, seed);
                int bucket = h & 0xFF;
                if (bucket < 5) {
                    img.setRGB(x, y, mix(color, SHADOW, strength));
                } else if (bucket < 8) {
                    img.setRGB(x, y, mix(color, HIGHLIGHT, strength * 0.65f));
                } else if (bucket < 10) {
                    img.setRGB(x, y, mix(color, STEEL, strength * 0.50f));
                }
            }
        }
    }

    private static int hash2d(int x, int y, int seed) {
        int n = x * 374761393 + y * 668265263 + seed * 1442695041;
        n = (n ^ (n >>> 13)) * 1274126177;
        return n ^ (n >>> 16);
    }

    private static void drawLine(BufferedImage img, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0;
        int y = y0;
        while (true) {
            pixel(img, x, y, color);
            if (x == x1 && y == y1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    private static void fillRect(BufferedImage img, int x0, int y0, int x1, int y1, int color) {
        int minX = Math.max(0, Math.min(x0, x1));
        int maxX = Math.min(SIZE - 1, Math.max(x0, x1));
        int minY = Math.max(0, Math.min(y0, y1));
        int maxY = Math.min(SIZE - 1, Math.max(y0, y1));
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                pixel(img, x, y, color);
            }
        }
    }

    private static void pixel(BufferedImage img, int x, int y, int color) {
        if (x < 0 || y < 0 || x >= SIZE || y >= SIZE) {
            return;
        }
        img.setRGB(x, y, color);
    }

    private static int mix(int a, int b, float t) {
        float u = 1.0f - t;
        int ar = (a >>> 16) & 0xFF;
        int ag = (a >>> 8) & 0xFF;
        int ab = a & 0xFF;
        int aa = (a >>> 24) & 0xFF;
        int br = (b >>> 16) & 0xFF;
        int bg = (b >>> 8) & 0xFF;
        int bb = b & 0xFF;
        int ba = (b >>> 24) & 0xFF;
        int r = Math.round(ar * u + br * t);
        int g = Math.round(ag * u + bg * t);
        int bl = Math.round(ab * u + bb * t);
        int al = Math.round(aa * u + ba * t);
        return (al << 24) | (r << 16) | (g << 8) | bl;
    }

    private static int rgba(int r, int g, int b) {
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int rgba(int r, int g, int b, int a) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
