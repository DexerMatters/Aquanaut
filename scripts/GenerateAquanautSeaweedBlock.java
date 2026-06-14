import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Generates a tileable seaweed leaf block texture.
 *
 * The texture is built from many wrapped seaweed leaves on a toroidal 16x16
 * canvas, so opposite edges match and adjacent blocks connect cleanly.
 * Pixels are always either fully opaque or fully transparent.
 */
public final class GenerateAquanautSeaweedBlock {
    private static final int S = 16;
    private static final Path OUT_DIR = Paths.get(
            "src", "main", "resources", "assets", "aquanaut", "textures", "block");

    private static final int SW_DEEP = 0x173321;
    private static final int SW_DARK = 0x235032;
    private static final int SW_SHADE = 0x2E6842;
    private static final int SW_MID = 0x3B7E4F;
    private static final int SW_LEAF = 0x4C9660;
    private static final int SW_BRIGHT = 0x63AB70;
    private static final int SW_SUN = 0x78BD7D;
    private static final int SW_PALE = 0x92CC90;
    private static final int SW_STEM = 0x43512A;

    private static final Leaf[] LEAVES = {
            new Leaf(1.0f, 1.8f, radians(71f), 18.0f, 2.2f, 1.65f, -0.03f),
            new Leaf(5.0f, 1.0f, radians(93f), 18.5f, -1.8f, 1.95f, 0.05f),
            new Leaf(8.9f, 1.4f, radians(107f), 18.0f, 1.7f, 1.85f, 0.01f),
            new Leaf(13.4f, 1.2f, radians(82f), 17.0f, -1.5f, 1.40f, -0.04f),
            new Leaf(2.1f, 5.8f, radians(31f), 16.0f, 1.4f, 1.15f, -0.02f),
            new Leaf(6.3f, 6.0f, radians(148f), 14.5f, -1.2f, 1.00f, 0.02f),
            new Leaf(10.8f, 6.6f, radians(38f), 15.5f, 1.1f, 1.05f, 0.03f),
            new Leaf(3.2f, 10.9f, radians(79f), 15.5f, -1.4f, 1.30f, -0.01f),
            new Leaf(8.0f, 11.3f, radians(95f), 16.5f, 1.6f, 1.75f, 0.04f),
            new Leaf(12.7f, 10.9f, radians(116f), 15.0f, -1.1f, 1.25f, -0.02f),
            new Leaf(6.0f, 13.7f, radians(14f), 12.5f, 0.9f, 0.85f, -0.03f),
            new Leaf(11.7f, 14.3f, radians(167f), 12.0f, -0.8f, 0.82f, 0.00f),
    };

    private static final Bud[] BUDS = {
            new Bud(3.4f, 3.7f, 1.25f),
            new Bud(7.7f, 4.4f, 1.10f),
            new Bud(11.8f, 3.3f, 1.00f),
            new Bud(5.2f, 8.4f, 1.05f),
            new Bud(9.8f, 8.1f, 1.15f),
            new Bud(2.8f, 12.3f, 1.05f),
            new Bud(13.0f, 11.7f, 0.95f),
            new Bud(8.5f, 13.2f, 1.10f),
    };

    private GenerateAquanautSeaweedBlock() {
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUT_DIR);
        BufferedImage image = generate();
        ImageIO.write(image, "png", OUT_DIR.resolve("seaweed.png").toFile());
        System.out.println("Generated seaweed block texture");
    }

    private static BufferedImage generate() {
        BufferedImage image = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);

        for (Leaf leaf : LEAVES) {
            drawLeaf(image, leaf);
        }
        for (Bud bud : BUDS) {
            drawBud(image, bud);
        }
        addStemAccents(image);
        carveInteriorHoles(image);
        validateAlpha(image);
        return image;
    }

    private static void drawLeaf(BufferedImage image, Leaf leaf) {
        final int steps = 26;
        float[] xs = new float[steps];
        float[] ys = new float[steps];
        float[] halfWidths = new float[steps];

        float dirX = (float) Math.cos(leaf.angle);
        float dirY = (float) Math.sin(leaf.angle);
        float sideX = -dirY;
        float sideY = dirX;

        for (int i = 0; i < steps; i++) {
            float u = i / (float) (steps - 1);
            float along = (u - 0.5f) * leaf.length;
            float curl = (float) Math.sin(u * Math.PI) * leaf.bend;
            float ripple = (float) Math.sin((u * 2.0f + leaf.accent * 3.0f) * Math.PI) * 0.28f;
            xs[i] = leaf.cx + dirX * along + sideX * (curl + ripple * 0.45f);
            ys[i] = leaf.cy + dirY * along + sideY * (curl + ripple * 0.45f);
            halfWidths[i] = leaf.width * (0.16f + 0.84f * (float) Math.pow(Math.sin(u * Math.PI), 0.72));
        }

        for (int i = 0; i < steps; i++) {
            int prev = i == 0 ? i : i - 1;
            int next = i == steps - 1 ? i : i + 1;
            float tx = wrapDelta(xs[next], xs[prev]);
            float ty = wrapDelta(ys[next], ys[prev]);
            float len = (float) Math.sqrt(tx * tx + ty * ty);
            if (len < 0.0001f) {
                continue;
            }
            tx /= len;
            ty /= len;
            stampLeaf(image, xs[i], ys[i], tx, ty, halfWidths[i], leaf.accent);
        }
    }

    private static void stampLeaf(BufferedImage image, float cx, float cy, float tx, float ty, float half, float accent) {
        float nx = -ty;
        float ny = tx;

        for (int py = 0; py < S; py++) {
            for (int px = 0; px < S; px++) {
                float dx = wrapDelta(px + 0.5f, cx);
                float dy = wrapDelta(py + 0.5f, cy);
                float along = dx * tx + dy * ty;
                float across = dx * nx + dy * ny;

                float reachAlong = half * 0.95f + 0.24f;
                if (Math.abs(along) > reachAlong || Math.abs(across) > half + 0.36f) {
                    continue;
                }

                float normalized = (along * along) / (reachAlong * reachAlong)
                        + (across * across) / ((half + 0.08f) * (half + 0.08f));
                if (normalized > 1.0f) {
                    continue;
                }

                float radial = Math.min(1.0f, Math.abs(across) / Math.max(0.001f, half));
                float light = 0.56f + 0.26f * (-nx * 0.78f - ny * 0.36f) - radial * 0.33f + accent;
                int color = pickLeafColor(light, radial);
                paintOpaque(image, px, py, color, 0.72f + (1.0f - radial) * 0.28f);
            }
        }
    }

    private static void drawBud(BufferedImage image, Bud bud) {
        for (int py = 0; py < S; py++) {
            for (int px = 0; px < S; px++) {
                float dx = wrapDelta(px + 0.5f, bud.cx);
                float dy = wrapDelta(py + 0.5f, bud.cy);
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist > bud.radius + 0.25f) {
                    continue;
                }

                float inner = dist / Math.max(0.001f, bud.radius);
                int color;
                if (inner < 0.25f) {
                    color = SW_PALE;
                } else if (inner < 0.48f) {
                    color = SW_SUN;
                } else if (inner < 0.72f) {
                    color = SW_BRIGHT;
                } else {
                    color = SW_LEAF;
                }
                paintOpaque(image, px, py, color, 0.80f);
            }
        }
    }

    private static void addStemAccents(BufferedImage image) {
        for (Leaf leaf : LEAVES) {
            float dirX = (float) Math.cos(leaf.angle);
            float dirY = (float) Math.sin(leaf.angle);
            float startX = leaf.cx - dirX * leaf.length * 0.28f;
            float startY = leaf.cy - dirY * leaf.length * 0.28f;
            float endX = leaf.cx + dirX * leaf.length * 0.36f;
            float endY = leaf.cy + dirY * leaf.length * 0.36f;

            for (int py = 0; py < S; py++) {
                for (int px = 0; px < S; px++) {
                    float dist = torusDistanceToSegment(px + 0.5f, py + 0.5f, startX, startY, endX, endY);
                    if (dist < 0.16f) {
                        paintOpaque(image, px, py, SW_STEM, 0.38f);
                    }
                }
            }
        }
    }

    private static void carveInteriorHoles(BufferedImage image) {
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                if ((image.getRGB(x, y) >>> 24) == 0) {
                    continue;
                }
                int neighbors = opaqueNeighborCount(image, x, y);
                if (neighbors < 6) {
                    continue;
                }
                int h = hash(x * 97 + y * 131 + 7013);
                if ((h & 15) == 0 || ((h >>> 5) & 31) == 9) {
                    image.setRGB(x, y, 0x00000000);
                }
            }
        }
    }

    private static int opaqueNeighborCount(BufferedImage image, int x, int y) {
        int count = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                int sx = wrapIndex(x + dx);
                int sy = wrapIndex(y + dy);
                if ((image.getRGB(sx, sy) >>> 24) != 0) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int pickLeafColor(float light, float radial) {
        float tone = light - radial * 0.18f;
        if (tone > 0.86f) {
            return SW_PALE;
        }
        if (tone > 0.74f) {
            return SW_SUN;
        }
        if (tone > 0.61f) {
            return SW_BRIGHT;
        }
        if (tone > 0.47f) {
            return SW_LEAF;
        }
        if (tone > 0.35f) {
            return SW_MID;
        }
        if (tone > 0.24f) {
            return SW_SHADE;
        }
        if (tone > 0.14f) {
            return SW_DARK;
        }
        return SW_DEEP;
    }

    private static void paintOpaque(BufferedImage image, int x, int y, int rgb, float strength) {
        strength = Math.max(0.0f, Math.min(1.0f, strength));
        int current = image.getRGB(x, y);
        if ((current >>> 24) == 0) {
            image.setRGB(x, y, 0xFF000000 | rgb);
            return;
        }

        int cr = (current >>> 16) & 0xFF;
        int cg = (current >>> 8) & 0xFF;
        int cb = current & 0xFF;
        int nr = (rgb >>> 16) & 0xFF;
        int ng = (rgb >>> 8) & 0xFF;
        int nb = rgb & 0xFF;

        int r = clamp(Math.round(cr * (1.0f - strength) + nr * strength));
        int g = clamp(Math.round(cg * (1.0f - strength) + ng * strength));
        int b = clamp(Math.round(cb * (1.0f - strength) + nb * strength));
        image.setRGB(x, y, 0xFF000000 | (r << 16) | (g << 8) | b);
    }

    private static float torusDistanceToSegment(float px, float py, float x0, float y0, float x1, float y1) {
        float dx = wrapDelta(x1, x0);
        float dy = wrapDelta(y1, y0);
        float ox = wrapDelta(px, x0);
        float oy = wrapDelta(py, y0);
        float lenSq = dx * dx + dy * dy;
        if (lenSq < 0.0001f) {
            return (float) Math.sqrt(ox * ox + oy * oy);
        }
        float t = (ox * dx + oy * dy) / lenSq;
        t = Math.max(0.0f, Math.min(1.0f, t));
        float projX = dx * t;
        float projY = dy * t;
        float ddx = ox - projX;
        float ddy = oy - projY;
        return (float) Math.sqrt(ddx * ddx + ddy * ddy);
    }

    private static float wrapDelta(float a, float b) {
        float d = a - b;
        while (d <= -S * 0.5f) {
            d += S;
        }
        while (d > S * 0.5f) {
            d -= S;
        }
        return d;
    }

    private static int wrapIndex(int v) {
        int m = v % S;
        return m < 0 ? m + S : m;
    }

    private static int hash(int value) {
        value = (value ^ (value >>> 13)) * 1274126177;
        return value ^ (value >>> 16);
    }

    private static void validateAlpha(BufferedImage image) {
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                int alpha = (image.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha != 0 && alpha != 0xFF) {
                    throw new IllegalStateException("Unexpected non-binary alpha at " + x + "," + y);
                }
            }
        }
    }

    private static float radians(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static final class Leaf {
        private final float cx;
        private final float cy;
        private final float angle;
        private final float length;
        private final float bend;
        private final float width;
        private final float accent;

        private Leaf(float cx, float cy, float angle, float length, float bend, float width, float accent) {
            this.cx = cx;
            this.cy = cy;
            this.angle = angle;
            this.length = length;
            this.bend = bend;
            this.width = width;
            this.accent = accent;
        }
    }

    private static final class Bud {
        private final float cx;
        private final float cy;
        private final float radius;

        private Bud(float cx, float cy, float radius) {
            this.cx = cx;
            this.cy = cy;
            this.radius = radius;
        }
    }
}
