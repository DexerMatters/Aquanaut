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

        write("lightning_generator.png", lightningSide(base));
        write("lightning_generator_front.png", lightningFront(base));

        write("bubble_machine.png", bubbleSide(base));
        write("bubble_machine_front.png", bubbleFront(base));

        write("swirl_generator.png", swirlSide(base));
        write("swirl_generator_front.png", swirlFront(base));

        write("torpedo_launcher.png", torpedoSide(base));
        write("torpedo_launcher_front.png", torpedoFront(base));

        write("shield_generator.png", shieldSide(base));
        write("shield_generator_top.png", shieldTop(base));
        write("air_supply.png", airSupplySide(base));
        write("air_supply_top.png", airSupplyTop(base));
        write("hard_shell_frame.png", hardShellFrame(base));
        write("gas_pipe_glass.png", gasPipeGlassWall());
        write("gas_pipe_clamp.png", gasPipeClamp(base));

        System.out.println("Generated 15 Aquanaut machine textures in " + BLOCK_TEXTURE_DIR);
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

    private static BufferedImage lightningSide(BufferedImage base) {
        BufferedImage img = copy(base);
        drawShellFrameBase(img);
        drawLightningSideHint(img);
        softMottle(img, 11, 0.012f);
        return img;
    }

    private static BufferedImage lightningFront(BufferedImage base) {
        BufferedImage img = copy(base);
        drawShellFrameBase(img);
        drawLightningEmitterBay(img);
        softMottle(img, 12, 0.018f);
        return img;
    }

    private static BufferedImage bubbleSide(BufferedImage base) {
        BufferedImage img = copy(base);
        drawShellFrameBase(img);
        drawBubbleSideHint(img);
        softMottle(img, 21, 0.012f);
        return img;
    }

    private static BufferedImage bubbleFront(BufferedImage base) {
        BufferedImage img = copy(base);
        drawShellFrameBase(img);
        drawBubbleEmitterBay(img);
        softMottle(img, 22, 0.018f);
        return img;
    }

    private static BufferedImage swirlSide(BufferedImage base) {
        BufferedImage img = copy(base);
        drawShellFrameBase(img);
        drawSwirlSideHint(img);
        softMottle(img, 31, 0.012f);
        return img;
    }

    private static BufferedImage swirlFront(BufferedImage base) {
        BufferedImage img = copy(base);
        drawShellFrameBase(img);
        drawSwirlTurbineBay(img);
        softMottle(img, 32, 0.018f);
        return img;
    }

    private static BufferedImage torpedoSide(BufferedImage base) {
        BufferedImage img = copy(base);
        drawShellFrameBase(img);
        drawTorpedoSideHint(img);
        softMottle(img, 41, 0.012f);
        return img;
    }

    private static BufferedImage torpedoFront(BufferedImage base) {
        BufferedImage img = copy(base);
        drawShellFrameBase(img);
        drawTorpedoTubeBay(img);
        softMottle(img, 42, 0.018f);
        return img;
    }

    private static BufferedImage shieldSide(BufferedImage base) {
        BufferedImage img = copy(base);
        drawShellFrameBase(img);
        drawShieldSideHint(img);
        softMottle(img, 51, 0.014f);
        return img;
    }

    private static BufferedImage shieldTop(BufferedImage base) {
        BufferedImage img = copy(base);
        drawShellFrameBase(img);
        drawShieldTopStation(img);
        softMottle(img, 52, 0.018f);
        return img;
    }

    private static BufferedImage airSupplySide(BufferedImage base) {
        BufferedImage img = copy(base);
        drawShellFrameBase(img);
        drawAirSideHint(img);
        softMottle(img, 61, 0.014f);
        return img;
    }

    private static BufferedImage airSupplyTop(BufferedImage base) {
        BufferedImage img = copy(base);
        drawShellFrameBase(img);
        drawAirTopStation(img);
        softMottle(img, 62, 0.018f);
        return img;
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
        int baseTone = rgba(93, 116, 124);
        int top = mix(baseTone, HIGHLIGHT, 0.24f);
        int side = mix(baseTone, HIGHLIGHT, 0.10f);
        int front = mix(baseTone, STEEL, 0.18f);
        int shadow = mix(baseTone, SHADOW, 0.24f);
        int deep = mix(baseTone, GRAPHITE, 0.32f);
        int accent = mix(CORE, HIGHLIGHT, 0.22f);

        // First 3x2 texels are the live palette sampled by the 1px framework rods.
        pixel(img, 0, 0, side);
        pixel(img, 1, 0, top);
        pixel(img, 2, 0, accent);
        pixel(img, 0, 1, front);
        pixel(img, 1, 1, shadow);
        pixel(img, 2, 1, deep);

        // The rest of the tile gives the texture a proper hard-shell look for particles/item preview.
        fillRect(img, 3, 3, 12, 12, rgba(0, 0, 0, 0));
        drawLine(img, 4, 3, 11, 3, top);
        drawLine(img, 3, 4, 3, 11, side);
        drawLine(img, 12, 4, 12, 11, shadow);
        drawLine(img, 4, 12, 11, 12, deep);
        drawLine(img, 5, 5, 10, 5, mix(top, accent, 0.30f));
        drawLine(img, 5, 10, 10, 10, mix(shadow, deep, 0.35f));
        drawLine(img, 5, 6, 5, 9, mix(side, accent, 0.22f));
        drawLine(img, 10, 6, 10, 9, mix(shadow, baseTone, 0.10f));
        pixel(img, 6, 6, accent);
        pixel(img, 9, 6, top);
        pixel(img, 6, 9, front);
        pixel(img, 9, 9, deep);
        pixel(img, 7, 7, mix(accent, HIGHLIGHT, 0.30f));
        pixel(img, 8, 8, mix(baseTone, SHADOW, 0.18f));
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

    private static void drawLightningSideHint(BufferedImage img) {
        fillRect(img, 4, 4, 11, 11, rgba(31, 43, 49));
        drawLine(img, 5, 4, 10, 4, mix(STEEL, HIGHLIGHT, 0.18f));
        drawLine(img, 5, 11, 10, 11, mix(STEEL, SHADOW, 0.16f));
        drawLine(img, 6, 5, 6, 10, mix(COPPER, STEEL, 0.48f));
        drawLine(img, 9, 5, 9, 10, mix(COPPER, STEEL, 0.48f));
        pixel(img, 7, 7, GLOW);
        pixel(img, 8, 8, PALE_GLOW);
        pixel(img, 7, 8, mix(AQUA, GLOW, 0.35f));
    }

    private static void drawBubbleSideHint(BufferedImage img) {
        fillRect(img, 4, 4, 11, 11, rgba(31, 43, 49));
        drawLine(img, 5, 4, 10, 4, mix(STEEL, HIGHLIGHT, 0.18f));
        drawLine(img, 5, 11, 10, 11, mix(STEEL, SHADOW, 0.16f));
        ring(img, 7, 7, 2, mix(GLASS, HIGHLIGHT, 0.40f));
        pixel(img, 7, 7, rgba(14, 20, 24));
        pixel(img, 8, 6, PALE_GLOW);
        pixel(img, 6, 8, BUBBLE);
    }

    private static void drawSwirlSideHint(BufferedImage img) {
        fillRect(img, 4, 4, 11, 11, rgba(31, 43, 49));
        drawLine(img, 5, 5, 10, 5, mix(STEEL, HIGHLIGHT, 0.16f));
        drawLine(img, 5, 7, 10, 7, mix(AQUA, STEEL, 0.26f));
        drawLine(img, 5, 9, 10, 9, mix(STEEL, SHADOW, 0.18f));
        drawLine(img, 6, 4, 6, 10, mix(STEEL, HIGHLIGHT, 0.08f));
        pixel(img, 8, 7, PALE_GLOW);
        pixel(img, 7, 8, mix(AQUA, GLOW, 0.30f));
    }

    private static void drawTorpedoSideHint(BufferedImage img) {
        fillRect(img, 4, 4, 11, 11, rgba(31, 43, 49));
        drawLine(img, 5, 4, 10, 4, mix(STEEL, HIGHLIGHT, 0.16f));
        drawLine(img, 5, 11, 10, 11, mix(STEEL, SHADOW, 0.16f));
        drawLine(img, 5, 7, 10, 7, mix(GRAPHITE, STEEL, 0.55f));
        drawLine(img, 6, 5, 6, 9, BRASS);
        drawLine(img, 9, 5, 9, 9, BRASS);
        pixel(img, 7, 7, HIGHLIGHT);
        pixel(img, 8, 7, mix(PALE_GLOW, STEEL, 0.45f));
    }

    private static void drawShieldSideHint(BufferedImage img) {
        fillRect(img, 4, 4, 11, 11, rgba(31, 43, 49));
        drawLine(img, 5, 5, 10, 5, mix(STEEL, HIGHLIGHT, 0.16f));
        drawLine(img, 5, 10, 10, 10, mix(STEEL, SHADOW, 0.16f));
        drawLine(img, 5, 7, 10, 7, mix(AQUA, GLOW, 0.32f));
        drawLine(img, 5, 8, 10, 8, mix(STEEL, AQUA, 0.18f));
        pixel(img, 7, 7, PALE_GLOW);
        pixel(img, 8, 7, PALE_GLOW);
    }

    private static void drawAirSideHint(BufferedImage img) {
        fillRect(img, 4, 4, 11, 11, rgba(31, 43, 49));
        drawLine(img, 6, 4, 9, 4, mix(STEEL, HIGHLIGHT, 0.16f));
        drawLine(img, 6, 11, 9, 11, mix(STEEL, SHADOW, 0.16f));
        fillRect(img, 6, 5, 9, 10, mix(STEEL, HIGHLIGHT, 0.20f));
        drawLine(img, 7, 6, 8, 6, PALE_GLOW);
        drawLine(img, 7, 9, 8, 9, GLOW);
        pixel(img, 6, 7, BRASS);
        pixel(img, 9, 7, BRASS);
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

    private static void frame(BufferedImage img, int x0, int y0, int x1, int y1, int dark, int light) {
        fillRect(img, x0, y0, x1, y1, dark);
        fillRect(img, x0 + 1, y0 + 1, x1 - 1, y1 - 1, rgba(44, 58, 66));
        highlightLine(img, x0 + 1, y0 + 1, x1 - 2, y0 + 1, light);
        highlightLine(img, x0 + 1, y0 + 1, x0 + 1, y1 - 2, light);
        shadowLine(img, x0 + 1, y1 - 1, x1 - 2, y1 - 1, SHADOW);
        shadowLine(img, x1 - 1, y0 + 1, x1 - 1, y1 - 2, SHADOW);
    }

    private static void panel(BufferedImage img, int x0, int y0, int x1, int y1, int dark, int mid, float midMix) {
        fillRect(img, x0, y0, x1, y1, dark);
        fillRect(img, x0 + 1, y0 + 1, x1 - 1, y1 - 1, mix(dark, mid, midMix));
        highlightLine(img, x0 + 1, y0 + 1, x1 - 2, y0 + 1, mix(mid, HIGHLIGHT, 0.35f));
        highlightLine(img, x0 + 1, y0 + 1, x0 + 1, y1 - 2, mix(mid, HIGHLIGHT, 0.25f));
        shadowLine(img, x0 + 1, y1 - 1, x1 - 2, y1 - 1, mix(dark, SHADOW, 0.6f));
        shadowLine(img, x1 - 1, y0 + 1, x1 - 1, y1 - 2, mix(dark, SHADOW, 0.6f));
    }

    private static void addRivets(BufferedImage img, int x0, int y0, int x1, int y1, int color) {
        pixel(img, x0 + 1, y0 + 1, color);
        pixel(img, x1 - 1, y0 + 1, color);
        pixel(img, x0 + 1, y1 - 1, color);
        pixel(img, x1 - 1, y1 - 1, color);
    }

    private static void seam(BufferedImage img, int x0, int y0, int x1, int y1, int color) {
        drawLine(img, x0, y0, x1, y1, color);
    }

    private static void highlightLine(BufferedImage img, int x0, int y0, int x1, int y1, int color) {
        drawLine(img, x0, y0, x1, y1, color);
    }

    private static void shadowLine(BufferedImage img, int x0, int y0, int x1, int y1, int color) {
        drawLine(img, x0, y0, x1, y1, color);
    }

    private static void softShellSide(BufferedImage img, int core, int accent) {
        panel(img, 4, 4, 11, 11, SHADOW, core, 0.12f);
        fillRect(img, 5, 5, 10, 10, rgba(38, 50, 56));
        drawLine(img, 5, 6, 10, 6, mix(core, HIGHLIGHT, 0.05f));
        drawLine(img, 5, 9, 10, 9, mix(core, SHADOW, 0.14f));
        drawLine(img, 6, 5, 6, 10, mix(core, HIGHLIGHT, 0.04f));
        drawLine(img, 9, 5, 9, 10, mix(core, SHADOW, 0.12f));
        drawLine(img, 6, 7, 9, 7, mix(core, accent, 0.05f));
        drawLine(img, 6, 8, 9, 8, mix(core, accent, 0.05f));
        pixel(img, 7, 7, mix(core, HIGHLIGHT, 0.08f));
        pixel(img, 8, 7, mix(core, HIGHLIGHT, 0.08f));
        pixel(img, 7, 8, mix(core, SHADOW, 0.08f));
        pixel(img, 8, 8, mix(core, SHADOW, 0.08f));
    }

    private static void softConduit(BufferedImage img, int x0, int y0, int x1, int y1, int color, int accent) {
        drawLine(img, x0, y0, x1, y1, mix(color, HIGHLIGHT, 0.18f));
        drawLine(img, x0, y0 + 1, x1, y1 + 1, mix(color, SHADOW, 0.26f));
        pixel(img, x0 + 1, y0, accent);
        pixel(img, x1 - 1, y1, accent);
    }

    private static void softBubbleSheen(BufferedImage img, int x0, int y0, int x1, int y1, int glass, int bubble) {
        drawLine(img, x0, y0, x1, y0 + 1, mix(glass, HIGHLIGHT, 0.16f));
        drawLine(img, x0, y1, x1, y1 - 1, mix(glass, SHADOW, 0.18f));
        pixel(img, x0 + 1, y0 + 1, bubble);
        pixel(img, x1 - 1, y0 + 2, bubble);
        pixel(img, x1 - 2, y1 - 1, bubble);
    }

    private static void softFlowRidge(BufferedImage img, int x0, int y0, int x1, int y1, int primary, int glow) {
        drawLine(img, x0, y0 + 1, x1 - 1, y0 + 1, mix(primary, HIGHLIGHT, 0.10f));
        drawLine(img, x0 + 1, y0 + 3, x1 - 2, y0 + 2, mix(primary, glow, 0.16f));
        drawLine(img, x0 + 2, y1 - 1, x1 - 1, y1 - 2, mix(primary, SHADOW, 0.22f));
    }

    private static void softBrace(BufferedImage img, int x0, int y0, int x1, int y1, int copper, int dark) {
        drawLine(img, x0 + 1, y0, x1 - 1, y0, mix(copper, HIGHLIGHT, 0.12f));
        drawLine(img, x0, y1 - 1, x1, y1 - 1, mix(copper, SHADOW, 0.20f));
        drawLine(img, x0 + 1, y0 + 2, x1 - 1, y1 - 2, dark);
        pixel(img, x0 + 1, y0 + 1, copper);
        pixel(img, x1 - 1, y1 - 1, copper);
    }

    private static void softWear(BufferedImage img, int x0, int y0, int x1, int y1, int shadow, int core) {
        drawLine(img, x0 + 1, y0 + 1, x1 - 2, y0 + 1, mix(core, HIGHLIGHT, 0.08f));
        drawLine(img, x0 + 1, y1 - 2, x1 - 2, y1 - 2, mix(core, shadow, 0.20f));
        drawLine(img, x0 + 2, y0 + 3, x1 - 3, y1 - 3, mix(core, shadow, 0.16f));
        drawLine(img, x0 + 1, y0 + 5, x1 - 2, y0 + 4, mix(core, HIGHLIGHT, 0.05f));
        drawLine(img, x0 + 2, y1 - 4, x1 - 1, y1 - 5, mix(core, shadow, 0.12f));
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

    private static void drawShieldFieldHub(BufferedImage img, int cx, int cy, int aqua, int glow, int highlight,
            int steel, int brass) {
        drawLine(img, cx - 3, cy - 3, cx + 4, cy - 3, steel);
        drawLine(img, cx - 3, cy + 3, cx + 4, cy + 3, steel);
        drawLine(img, cx - 3, cy - 3, cx - 3, cy + 3, steel);
        drawLine(img, cx + 4, cy - 3, cx + 4, cy + 3, steel);
        fillRect(img, cx - 2, cy - 2, cx + 3, cy + 2, rgba(20, 29, 35));
        drawLine(img, cx - 2, cy - 1, cx + 3, cy - 1, aqua);
        drawLine(img, cx - 2, cy + 1, cx + 3, cy + 1, glow);
        drawLine(img, cx - 2, cy - 2, cx - 2, cy + 2, brass);
        drawLine(img, cx + 3, cy - 2, cx + 3, cy + 2, brass);
        pixel(img, cx, cy, glow);
        pixel(img, cx + 1, cy, highlight);
        pixel(img, cx, cy - 1, highlight);
        pixel(img, cx + 1, cy - 1, glow);
        pixel(img, cx - 1, cy + 1, highlight);
        pixel(img, cx + 2, cy + 1, glow);
        pixel(img, cx - 1, cy - 2, brass);
        pixel(img, cx + 2, cy - 2, brass);
    }

    private static void drawShieldSideStation(BufferedImage img) {
        // Central shield column with softer side housings to read as a station.
        panel(img, 2, 2, 13, 13, rgba(24, 37, 43), rgba(48, 69, 79), 0.42f);
        drawLine(img, 3, 4, 12, 4, mix(STEEL, HIGHLIGHT, 0.30f));
        drawLine(img, 3, 11, 12, 11, mix(STEEL, SHADOW, 0.28f));
        drawLine(img, 4, 5, 4, 10, mix(STEEL, HIGHLIGHT, 0.12f));
        drawLine(img, 11, 5, 11, 10, mix(STEEL, SHADOW, 0.18f));
        fillRect(img, 5, 5, 10, 10, rgba(18, 26, 32));

        ring(img, 7, 8, 4, mix(AQUA, STEEL, 0.35f));
        ring(img, 7, 8, 3, mix(GLOW, AQUA, 0.45f));
        drawShieldFieldHub(img, 7, 8, AQUA, GLOW, HIGHLIGHT, STEEL, BRASS);

        drawLine(img, 2, 7, 4, 7, BRASS);
        drawLine(img, 10, 7, 13, 7, BRASS);
        pixel(img, 3, 6, mix(BRASS, HIGHLIGHT, 0.25f));
        pixel(img, 3, 8, mix(BRASS, SHADOW, 0.15f));
        pixel(img, 12, 6, mix(BRASS, SHADOW, 0.12f));
        pixel(img, 12, 8, mix(BRASS, HIGHLIGHT, 0.18f));

        drawLine(img, 5, 3, 5, 4, mix(STEEL, HIGHLIGHT, 0.10f));
        drawLine(img, 9, 3, 9, 4, mix(STEEL, HIGHLIGHT, 0.10f));
        drawLine(img, 5, 11, 5, 12, mix(STEEL, SHADOW, 0.14f));
        drawLine(img, 9, 11, 9, 12, mix(STEEL, SHADOW, 0.14f));
        pixel(img, 5, 5, HIGHLIGHT);
        pixel(img, 9, 5, HIGHLIGHT);
        pixel(img, 5, 10, SHADOW);
        pixel(img, 9, 10, SHADOW);
    }

    private static void drawShieldTopStation(BufferedImage img) {
        fillRect(img, 4, 4, 11, 11, rgba(20, 29, 35));
        ring(img, 7, 7, 4, mix(AQUA, STEEL, 0.28f));
        ring(img, 7, 7, 3, mix(GLOW, AQUA, 0.38f));
        fillRect(img, 6, 6, 8, 8, rgba(15, 22, 27));
        drawLine(img, 7, 3, 7, 5, BRASS);
        drawLine(img, 7, 9, 7, 11, BRASS);
        drawLine(img, 3, 7, 5, 7, BRASS);
        drawLine(img, 9, 7, 11, 7, BRASS);
        drawLine(img, 5, 5, 6, 6, STEEL);
        drawLine(img, 9, 5, 8, 6, STEEL);
        drawLine(img, 5, 9, 6, 8, STEEL);
        drawLine(img, 9, 9, 8, 8, STEEL);
        pixel(img, 7, 7, PALE_GLOW);
        pixel(img, 7, 6, GLOW);
        pixel(img, 8, 7, GLOW);
        pixel(img, 6, 7, GLOW);
        pixel(img, 7, 8, GLOW);
    }

    private static void drawAirSideStation(BufferedImage img) {
        // A compact life-support manifold: tank, pipes, gauges, and exhaust vents.
        panel(img, 2, 2, 13, 13, rgba(23, 35, 41), rgba(48, 67, 76), 0.35f);
        fillRect(img, 4, 4, 11, 11, rgba(20, 28, 34));

        drawLine(img, 4, 5, 11, 5, mix(STEEL, HIGHLIGHT, 0.18f));
        drawLine(img, 4, 10, 11, 10, mix(STEEL, SHADOW, 0.20f));
        drawLine(img, 5, 4, 5, 11, mix(STEEL, HIGHLIGHT, 0.08f));
        drawLine(img, 10, 4, 10, 11, mix(STEEL, SHADOW, 0.12f));

        fillRect(img, 6, 5, 9, 10, rgba(29, 40, 46));
        fillRect(img, 7, 5, 8, 10, mix(STEEL, HIGHLIGHT, 0.32f));
        drawLine(img, 6, 6, 9, 6, PALE_GLOW);
        drawLine(img, 6, 9, 9, 9, GLOW);
        drawLine(img, 5, 7, 6, 7, BRASS);
        drawLine(img, 9, 7, 10, 7, BRASS);
        pixel(img, 7, 7, PALE_GLOW);
        pixel(img, 8, 7, GLOW);

        drawLine(img, 3, 7, 4, 7, STEEL);
        drawLine(img, 11, 7, 12, 7, STEEL);
        drawLine(img, 3, 6, 3, 8, BRASS);
        drawLine(img, 12, 6, 12, 8, BRASS);
        pixel(img, 3, 7, HIGHLIGHT);
        pixel(img, 12, 7, HIGHLIGHT);

        drawLine(img, 5, 3, 10, 3, mix(AQUA, HIGHLIGHT, 0.28f));
        drawLine(img, 5, 12, 10, 12, mix(AQUA, SHADOW, 0.20f));
        pixel(img, 7, 3, GLOW);
        pixel(img, 8, 3, GLOW);
        pixel(img, 7, 12, SHADOW);
        pixel(img, 8, 12, SHADOW);
    }

    private static void drawAirTopStation(BufferedImage img) {
        fillRect(img, 4, 4, 11, 11, rgba(21, 30, 36));
        fillRect(img, 6, 5, 9, 10, mix(STEEL, HIGHLIGHT, 0.26f));
        drawLine(img, 7, 4, 8, 4, mix(STEEL, HIGHLIGHT, 0.20f));
        drawLine(img, 7, 11, 8, 11, mix(STEEL, SHADOW, 0.18f));
        drawLine(img, 4, 7, 6, 7, BRASS);
        drawLine(img, 9, 7, 11, 7, BRASS);
        drawLine(img, 7, 5, 8, 5, PALE_GLOW);
        drawLine(img, 7, 9, 8, 9, GLOW);
        pixel(img, 7, 7, PALE_GLOW);
        pixel(img, 8, 7, GLOW);
        pixel(img, 5, 6, HIGHLIGHT);
        pixel(img, 10, 6, HIGHLIGHT);
        pixel(img, 5, 8, SHADOW);
        pixel(img, 10, 8, SHADOW);
    }

    private static void drawAirManifold(BufferedImage img, int cx, int cy, int steel, int highlight, int brass,
            int pale, int glow) {
        drawLine(img, cx - 4, cy - 2, cx + 4, cy - 2, steel);
        drawLine(img, cx - 4, cy + 2, cx + 4, cy + 2, steel);
        drawLine(img, cx - 4, cy - 2, cx - 4, cy + 2, steel);
        drawLine(img, cx + 4, cy - 2, cx + 4, cy + 2, steel);
        fillRect(img, cx - 2, cy - 3, cx + 3, cy + 2, rgba(28, 39, 45));
        fillRect(img, cx - 1, cy - 2, cx + 2, cy + 1, mix(steel, highlight, 0.45f));
        drawLine(img, cx - 3, cy - 1, cx - 1, cy - 1, pale);
        drawLine(img, cx + 1, cy - 1, cx + 3, cy - 1, glow);
        drawLine(img, cx - 3, cy + 1, cx - 1, cy + 1, glow);
        drawLine(img, cx + 1, cy + 1, cx + 3, cy + 1, pale);
        drawLine(img, cx - 5, cy, cx - 3, cy, brass);
        drawLine(img, cx + 3, cy, cx + 5, cy, brass);
        pixel(img, cx, cy, highlight);
        pixel(img, cx + 1, cy, pale);
        pixel(img, cx - 1, cy, glow);
        pixel(img, cx, cy - 1, glow);
        pixel(img, cx, cy + 1, glow);
    }

    private static void drawLightningEmitterBay(BufferedImage img) {
        fillRect(img, 4, 3, 11, 12, rgba(18, 27, 33));
        drawLine(img, 5, 4, 10, 4, mix(STEEL, HIGHLIGHT, 0.18f));
        drawLine(img, 5, 11, 10, 11, mix(STEEL, SHADOW, 0.18f));
        drawLine(img, 5, 5, 5, 10, COPPER);
        drawLine(img, 10, 5, 10, 10, COPPER);
        drawLine(img, 6, 4, 9, 4, BRASS);
        drawLine(img, 6, 11, 9, 11, BRASS);
        drawLine(img, 6, 5, 6, 10, mix(STEEL, HIGHLIGHT, 0.12f));
        drawLine(img, 9, 5, 9, 10, mix(STEEL, HIGHLIGHT, 0.12f));
        fillRect(img, 7, 6, 8, 9, rgba(10, 15, 19));
        pixel(img, 7, 5, PALE_GLOW);
        pixel(img, 8, 6, GLOW);
        pixel(img, 7, 7, PALE_GLOW);
        pixel(img, 8, 8, GLOW);
        pixel(img, 7, 9, PALE_GLOW);
        pixel(img, 6, 7, GLOW);
        pixel(img, 9, 8, GLOW);
        pixel(img, 5, 7, HIGHLIGHT);
        pixel(img, 10, 8, HIGHLIGHT);
    }

    private static void drawBubbleEmitterBay(BufferedImage img) {
        fillRect(img, 4, 3, 11, 12, rgba(19, 28, 34));
        drawLine(img, 5, 10, 10, 10, mix(COPPER, WARM, 0.48f));
        ring(img, 7, 8, 3, mix(GLASS, HIGHLIGHT, 0.32f));
        fillRect(img, 6, 7, 8, 9, rgba(8, 13, 17));
        pixel(img, 7, 8, rgba(6, 10, 13));
        pixel(img, 7, 7, mix(GLASS, PALE_GLOW, 0.45f));
        pixel(img, 6, 8, mix(GLASS, GLOW, 0.28f));
        pixel(img, 8, 8, mix(GLASS, GLOW, 0.28f));
        ring(img, 7, 4, 1, BUBBLE);
        ring(img, 6, 2, 1, GLOW);
        ring(img, 9, 1, 1, PALE_GLOW);
        pixel(img, 7, 4, rgba(248, 252, 253));
        pixel(img, 6, 2, rgba(248, 252, 253));
        pixel(img, 9, 1, rgba(248, 252, 253));
        pixel(img, 8, 3, BUBBLE);
        pixel(img, 5, 5, mix(GLASS, HIGHLIGHT, 0.25f));
        pixel(img, 10, 6, mix(GLASS, HIGHLIGHT, 0.25f));
    }

    private static void drawSwirlTurbineBay(BufferedImage img) {
        fillRect(img, 4, 3, 11, 12, rgba(18, 27, 33));
        ring(img, 7, 8, 4, mix(STEEL, HIGHLIGHT, 0.20f));
        ring(img, 7, 8, 3, mix(AQUA, STEEL, 0.28f));
        fillRect(img, 6, 4, 8, 6, mix(AQUA, PALE_GLOW, 0.42f));
        fillRect(img, 9, 7, 11, 9, mix(CORE, GLOW, 0.34f));
        fillRect(img, 6, 10, 8, 12, mix(AQUA, PALE_GLOW, 0.42f));
        fillRect(img, 4, 7, 6, 9, mix(CORE, GLOW, 0.34f));
        drawLine(img, 6, 4, 8, 4, mix(AQUA, HIGHLIGHT, 0.20f));
        drawLine(img, 9, 7, 11, 7, mix(STEEL, HIGHLIGHT, 0.16f));
        drawLine(img, 6, 12, 8, 12, mix(AQUA, SHADOW, 0.18f));
        drawLine(img, 4, 7, 4, 9, mix(STEEL, HIGHLIGHT, 0.14f));
        fillRect(img, 6, 7, 8, 9, rgba(10, 16, 20));
        pixel(img, 7, 8, PALE_GLOW);
        pixel(img, 7, 7, GLOW);
        pixel(img, 8, 8, GLOW);
        pixel(img, 6, 8, GLOW);
    }

    private static void drawTorpedoTubeBay(BufferedImage img) {
        fillRect(img, 4, 3, 11, 12, rgba(18, 27, 33));
        drawLine(img, 5, 4, 10, 4, BRASS);
        drawLine(img, 5, 11, 10, 11, BRASS);
        drawLine(img, 5, 5, 5, 10, COPPER);
        drawLine(img, 10, 5, 10, 10, COPPER);
        fillRect(img, 6, 5, 9, 10, mix(STEEL, HIGHLIGHT, 0.18f));
        ring(img, 7, 8, 2, GRAPHITE);
        ring(img, 7, 8, 1, mix(STEEL, HIGHLIGHT, 0.20f));
        pixel(img, 7, 8, rgba(7, 11, 15));
        pixel(img, 7, 7, PALE_GLOW);
        pixel(img, 6, 8, GLOW);
        pixel(img, 8, 8, GLOW);
        drawLine(img, 4, 7, 5, 7, BRASS);
        drawLine(img, 10, 7, 11, 7, BRASS);
        drawLine(img, 6, 10, 9, 10, mix(DARK_STEEL, STEEL, 0.30f));
    }

    private static void drawLightningCavity(BufferedImage img, int cx, int cy, int copper, int brass, int glow,
            int pale) {
        fillRect(img, cx - 2, cy - 4, cx + 3, cy + 3, rgba(23, 31, 37));
        drawLine(img, cx - 2, cy - 4, cx + 3, cy - 4, copper);
        drawLine(img, cx - 2, cy + 3, cx + 3, cy + 3, brass);
        drawLine(img, cx - 2, cy - 4, cx - 2, cy + 3, brass);
        drawLine(img, cx + 3, cy - 4, cx + 3, cy + 3, copper);
        fillRect(img, cx - 1, cy - 2, cx + 2, cy + 1, rgba(15, 20, 26));
        pixel(img, cx, cy - 1, glow);
        pixel(img, cx + 1, cy - 1, pale);
        pixel(img, cx, cy, glow);
        pixel(img, cx + 1, cy, glow);
        pixel(img, cx - 1, cy, pale);
        pixel(img, cx, cy + 1, glow);
    }

    private static void lightningCrown(BufferedImage img, int cx, int cy, int copper, int brass, int glow) {
        drawLine(img, cx - 3, cy - 3, cx - 2, cy - 1, copper);
        drawLine(img, cx + 4, cy - 3, cx + 3, cy - 1, copper);
        drawLine(img, cx - 3, cy + 3, cx - 2, cy + 1, copper);
        drawLine(img, cx + 4, cy + 3, cx + 3, cy + 1, copper);
        pixel(img, cx - 2, cy - 3, brass);
        pixel(img, cx + 3, cy - 3, brass);
        pixel(img, cx - 2, cy + 3, brass);
        pixel(img, cx + 3, cy + 3, brass);
        drawLine(img, cx - 1, cy - 1, cx, cy, glow);
        drawLine(img, cx + 2, cy - 1, cx + 1, cy, glow);
        drawLine(img, cx - 1, cy + 1, cx, cy, glow);
        drawLine(img, cx + 2, cy + 1, cx + 1, cy, glow);
    }

    private static void lightningForks(BufferedImage img, int cx, int cy, int glow, int pale) {
        drawLine(img, cx - 2, cy - 1, cx - 4, cy - 3, glow);
        drawLine(img, cx + 2, cy - 1, cx + 4, cy - 3, glow);
        drawLine(img, cx - 2, cy + 1, cx - 4, cy + 3, glow);
        drawLine(img, cx + 2, cy + 1, cx + 4, cy + 3, glow);
        pixel(img, cx - 4, cy - 3, pale);
        pixel(img, cx + 4, cy - 3, pale);
        pixel(img, cx - 4, cy + 3, pale);
        pixel(img, cx + 4, cy + 3, pale);
    }

    private static void drawBubbleOutwardMouth(BufferedImage img, int cx, int cy, int glass, int bubble, int glow,
            int copper, int warm) {
        fillRect(img, cx - 2, cy - 3, cx + 3, cy + 2, DARK_STEEL);
        fillRect(img, cx - 1, cy - 2, cx + 2, cy + 1, glass);
        fillRect(img, cx, cy - 1, cx + 1, cy, rgba(17, 25, 31));
        drawLine(img, cx - 2, cy - 2, cx + 3, cy - 2, warm);
        drawLine(img, cx - 2, cy + 1, cx + 3, cy + 1, copper);
        drawLine(img, cx - 2, cy - 2, cx - 2, cy + 1, bubble);
        drawLine(img, cx + 3, cy - 2, cx + 3, cy + 1, glow);
        drawLine(img, cx - 1, cy, cx + 2, cy, PALE_GLOW);
        pixel(img, cx, cy, glow);
        pixel(img, cx + 1, cy, PALE_GLOW);
        pixel(img, cx, cy - 1, bubble);
        pixel(img, cx + 1, cy - 1, bubble);
    }

    private static void drawSwirlVortex(BufferedImage img, int cx, int cy, int aqua, int core, int glow,
            int pale) {
        fillRect(img, cx - 2, cy - 3, cx + 3, cy + 2, core);
        fillRect(img, cx - 1, cy - 2, cx + 2, cy + 1, rgba(19, 26, 31));
        drawLine(img, cx - 2, cy - 2, cx + 3, cy - 2, aqua);
        drawLine(img, cx + 3, cy - 2, cx + 3, cy + 1, glow);
        drawLine(img, cx + 2, cy + 2, cx - 1, cy + 2, pale);
        drawLine(img, cx - 2, cy + 1, cx - 2, cy - 1, glow);
        pixel(img, cx, cy, pale);
        pixel(img, cx + 1, cy, glow);
    }

    private static void drawSwirlVanes(BufferedImage img, int cx, int cy, int aqua, int highlight, int glow) {
        drawLine(img, cx - 3, cy - 2, cx - 1, cy - 3, aqua);
        drawLine(img, cx + 4, cy - 2, cx + 2, cy - 3, aqua);
        drawLine(img, cx - 3, cy + 1, cx - 1, cy + 3, glow);
        drawLine(img, cx + 4, cy + 1, cx + 2, cy + 3, glow);
        pixel(img, cx - 1, cy - 3, highlight);
        pixel(img, cx + 2, cy - 3, highlight);
        pixel(img, cx - 1, cy + 3, highlight);
        pixel(img, cx + 2, cy + 3, highlight);
    }

    private static void drawSwirlFan(BufferedImage img, int cx, int cy, int aqua, int core, int glow, int pale,
            int steel, int dark) {
        ring(img, cx, cy, 4, mix(aqua, steel, 0.35f));
        ring(img, cx, cy, 2, mix(glow, aqua, 0.50f));
        fillRect(img, cx - 1, cy - 1, cx + 1, cy + 1, rgba(18, 26, 31));
        fillRect(img, cx - 1, cy - 4, cx + 1, cy - 2, mix(aqua, pale, 0.62f));
        fillRect(img, cx + 2, cy - 1, cx + 4, cy + 1, mix(core, glow, 0.58f));
        fillRect(img, cx - 1, cy + 2, cx + 1, cy + 4, mix(aqua, pale, 0.62f));
        fillRect(img, cx - 4, cy - 1, cx - 2, cy + 1, mix(core, glow, 0.58f));

        drawLine(img, cx - 1, cy - 4, cx + 1, cy - 4, mix(aqua, pale, 0.55f));
        drawLine(img, cx + 4, cy - 1, cx + 4, cy + 1, mix(aqua, glow, 0.42f));
        drawLine(img, cx - 1, cy + 4, cx + 1, cy + 4, mix(aqua, pale, 0.55f));
        drawLine(img, cx - 4, cy - 1, cx - 4, cy + 1, mix(aqua, glow, 0.42f));

        drawLine(img, cx - 1, cy - 2, cx, cy - 1, mix(core, glow, 0.35f));
        drawLine(img, cx + 2, cy - 1, cx + 1, cy, mix(core, pale, 0.35f));
        drawLine(img, cx + 1, cy + 2, cx, cy + 1, mix(core, glow, 0.35f));
        drawLine(img, cx - 2, cy + 1, cx - 1, cy, mix(core, pale, 0.35f));

        pixel(img, cx, cy, pale);
        pixel(img, cx + 1, cy, glow);
        pixel(img, cx, cy + 1, glow);
        pixel(img, cx - 1, cy, aqua);
        pixel(img, cx, cy - 1, aqua);
        pixel(img, cx + 2, cy - 1, steel);
        pixel(img, cx - 2, cy + 1, steel);
        pixel(img, cx + 1, cy + 2, dark);
        pixel(img, cx - 1, cy - 2, dark);
    }

    private static void drawTorpedoBarrel(BufferedImage img, int cx, int cy, int body, int steel, int brass,
            int glow, int pale) {
        fillRect(img, cx - 2, cy - 3, cx + 3, cy + 2, DARK_STEEL);
        fillRect(img, cx - 1, cy - 2, cx + 2, cy + 1, steel);
        fillRect(img, cx, cy - 1, cx + 1, cy, body);
        drawLine(img, cx - 2, cy - 2, cx + 3, cy - 2, brass);
        drawLine(img, cx - 2, cy + 1, cx + 3, cy + 1, brass);
        drawLine(img, cx - 2, cy - 2, cx - 2, cy + 1, brass);
        drawLine(img, cx + 3, cy - 2, cx + 3, cy + 1, brass);
        pixel(img, cx, cy, glow);
        pixel(img, cx + 1, cy, pale);
    }

    private static void drawTorpedoShell(BufferedImage img, int cx, int cy, int copper, int dark, int highlight) {
        drawLine(img, cx - 2, cy - 3, cx - 4, cy - 2, copper);
        drawLine(img, cx + 3, cy - 3, cx + 5, cy - 2, copper);
        drawLine(img, cx - 2, cy + 2, cx - 4, cy + 3, copper);
        drawLine(img, cx + 3, cy + 2, cx + 5, cy + 3, copper);
        pixel(img, cx - 4, cy - 2, highlight);
        pixel(img, cx + 5, cy - 2, highlight);
        pixel(img, cx - 4, cy + 3, highlight);
        pixel(img, cx + 5, cy + 3, highlight);
    }

    private static void drawTorpedoLauncherCore(BufferedImage img, int cx, int cy, int body, int steel, int brass,
            int glow, int pale, int copper, int highlight) {
        fillRect(img, cx - 3, cy - 3, cx + 4, cy + 2, DARK_STEEL);
        fillRect(img, cx - 2, cy - 2, cx + 3, cy + 1, steel);
        fillRect(img, cx - 1, cy - 1, cx + 2, cy, body);

        torpedoTube(img, cx, cy, body, steel, brass, glow);
        drawLine(img, cx - 3, cy - 2, cx - 3, cy + 1, copper);
        drawLine(img, cx + 4, cy - 2, cx + 4, cy + 1, copper);
        drawLine(img, cx - 2, cy - 3, cx + 3, cy - 3, brass);
        drawLine(img, cx - 2, cy + 2, cx + 3, cy + 2, brass);
        finStrake(img, cx, cy - 1, copper, darkSteel(copper));
        barrelHighlight(img, cx, cy, highlight);
        pixel(img, cx, cy - 1, pale);
        pixel(img, cx + 1, cy, glow);
        pixel(img, cx - 1, cy, glow);
        pixel(img, cx, cy + 1, highlight);
    }

    private static int darkSteel(int color) {
        return mix(color, DARK_STEEL, 0.45f);
    }

    private static void drawLightningGlyph(BufferedImage img, int cx, int cy, int bright, int dim) {
        pixel(img, cx, cy - 3, bright);
        pixel(img, cx + 1, cy - 2, bright);
        pixel(img, cx, cy - 1, bright);
        pixel(img, cx - 1, cy, bright);
        pixel(img, cx, cy + 1, bright);
        pixel(img, cx + 1, cy + 2, bright);
        pixel(img, cx - 1, cy + 2, dim);
        pixel(img, cx, cy + 3, dim);
        pixel(img, cx + 1, cy + 1, dim);
        pixel(img, cx - 2, cy - 1, dim);
        pixel(img, cx + 2, cy, dim);
        pixel(img, cx + 1, cy - 1, dim);
    }

    private static void lightningChamber(BufferedImage img, int cx, int cy, int copper, int brass, int glow, int pale) {
        drawLine(img, cx - 3, cy - 2, cx - 1, cy - 4, copper);
        drawLine(img, cx + 3, cy - 2, cx + 1, cy - 4, copper);
        drawLine(img, cx - 3, cy + 2, cx - 1, cy + 4, copper);
        drawLine(img, cx + 3, cy + 2, cx + 1, cy + 4, copper);
        ring(img, cx, cy, 2, brass);
        pixel(img, cx, cy, glow);
        pixel(img, cx, cy - 1, pale);
        pixel(img, cx + 1, cy, glow);
        pixel(img, cx - 1, cy, glow);
    }

    private static void lightningArcs(BufferedImage img, int cx, int cy, int glow, int pale) {
        drawLine(img, cx - 4, cy - 1, cx - 2, cy - 3, glow);
        drawLine(img, cx + 4, cy - 1, cx + 2, cy - 3, glow);
        drawLine(img, cx - 4, cy + 1, cx - 2, cy + 3, glow);
        drawLine(img, cx + 4, cy + 1, cx + 2, cy + 3, glow);
        pixel(img, cx - 2, cy - 3, pale);
        pixel(img, cx + 2, cy - 3, pale);
        pixel(img, cx - 2, cy + 3, pale);
        pixel(img, cx + 2, cy + 3, pale);
    }

    private static void prongs(BufferedImage img, int cx, int cy, int copper, int brass) {
        drawLine(img, cx - 2, cy - 4, cx - 1, cy - 2, copper);
        drawLine(img, cx + 2, cy - 4, cx + 1, cy - 2, copper);
        drawLine(img, cx - 2, cy + 4, cx - 1, cy + 2, copper);
        drawLine(img, cx + 2, cy + 4, cx + 1, cy + 2, copper);
        pixel(img, cx - 2, cy - 4, brass);
        pixel(img, cx + 2, cy - 4, brass);
        pixel(img, cx - 2, cy + 4, brass);
        pixel(img, cx + 2, cy + 4, brass);
    }

    private static void bubbleCluster(BufferedImage img, int color) {
        ring(img, 6, 6, 1, color);
        ring(img, 10, 5, 1, color);
        ring(img, 11, 8, 1, color);
        pixel(img, 6, 6, rgba(248, 252, 253));
        pixel(img, 10, 5, rgba(246, 250, 251));
        pixel(img, 11, 8, rgba(246, 250, 251));
        pixel(img, 8, 3, rgba(255, 255, 255));
        pixel(img, 9, 9, rgba(255, 255, 255));
    }

    private static void drawBubbleHoleEmitter(BufferedImage img, int cx, int cy, int glass, int bubble, int glow,
            int pale, int copper, int warm) {
        drawLine(img, cx - 2, cy - 3, cx + 2, cy - 3, mix(glass, HIGHLIGHT, 0.30f));
        drawLine(img, cx - 3, cy - 2, cx - 3, cy + 1, mix(glass, SHADOW, 0.35f));
        drawLine(img, cx + 3, cy - 2, cx + 3, cy + 1, mix(glass, HIGHLIGHT, 0.28f));
        drawLine(img, cx - 2, cy + 2, cx + 2, cy + 2, mix(copper, warm, 0.45f));

        fillRect(img, cx - 1, cy - 1, cx + 1, cy + 1, rgba(10, 16, 20));
        ring(img, cx, cy, 2, mix(glass, HIGHLIGHT, 0.40f));
        pixel(img, cx, cy, rgba(7, 11, 15));
        pixel(img, cx, cy - 1, mix(glass, pale, 0.60f));
        pixel(img, cx - 1, cy, mix(glass, pale, 0.45f));
        pixel(img, cx + 1, cy, mix(glass, pale, 0.45f));

        ring(img, cx, cy - 3, 1, bubble);
        ring(img, cx - 2, cy - 4, 1, glow);
        ring(img, cx + 2, cy - 5, 1, pale);
        pixel(img, cx, cy - 3, rgba(248, 252, 253));
        pixel(img, cx - 2, cy - 4, rgba(248, 252, 253));
        pixel(img, cx + 2, cy - 5, rgba(248, 252, 253));
        pixel(img, cx, cy - 4, bubble);
        pixel(img, cx - 1, cy - 5, glow);
    }

    private static void bubbleBell(BufferedImage img, int cx, int cy, int glass, int bubble, int glow) {
        drawLine(img, cx, cy - 3, cx + 2, cy - 1, glass);
        drawLine(img, cx + 2, cy - 1, cx + 2, cy + 2, bubble);
        drawLine(img, cx + 2, cy + 2, cx - 1, cy + 3, glow);
        drawLine(img, cx - 1, cy + 3, cx - 2, cy + 1, glass);
        drawLine(img, cx - 2, cy + 1, cx - 1, cy - 1, bubble);
        pixel(img, cx, cy, glow);
        pixel(img, cx + 1, cy, bubble);
    }

    private static void bubbleNozzle(BufferedImage img, int cx, int cy, int copper, int warm, int glass) {
        drawLine(img, cx, cy + 1, cx, cy + 3, copper);
        drawLine(img, cx - 1, cy + 2, cx + 1, cy + 2, warm);
        pixel(img, cx, cy + 1, glass);
    }

    private static void swirlArms(BufferedImage img, int cx, int cy, int primary, int secondary) {
        pixel(img, cx, cy - 3, primary);
        pixel(img, cx + 1, cy - 2, primary);
        pixel(img, cx + 2, cy - 1, primary);
        pixel(img, cx + 2, cy, secondary);
        pixel(img, cx + 1, cy + 1, secondary);
        pixel(img, cx, cy + 2, secondary);
        pixel(img, cx - 1, cy + 2, primary);
        pixel(img, cx - 2, cy + 1, primary);
        pixel(img, cx - 2, cy, secondary);
        pixel(img, cx - 1, cy - 1, secondary);
    }

    private static void swirlCore(BufferedImage img, int cx, int cy, int color) {
        pixel(img, cx, cy, color);
        pixel(img, cx + 1, cy, color);
        pixel(img, cx, cy + 1, color);
    }

    private static void spiralStripe(BufferedImage img, int primary, int accent) {
        drawLine(img, 5, 5, 9, 5, primary);
        drawLine(img, 9, 5, 10, 7, accent);
        drawLine(img, 10, 7, 8, 9, primary);
        drawLine(img, 8, 9, 5, 9, accent);
        drawLine(img, 5, 9, 4, 7, primary);
    }

    private static void spiralVane(BufferedImage img, int cx, int cy, int primary, int accent, int glow) {
        pixel(img, cx, cy - 3, primary);
        pixel(img, cx + 1, cy - 2, primary);
        pixel(img, cx + 2, cy - 1, accent);
        pixel(img, cx + 2, cy, glow);
        pixel(img, cx + 1, cy + 1, accent);
        pixel(img, cx, cy + 2, primary);
        pixel(img, cx - 1, cy + 2, primary);
        pixel(img, cx - 2, cy + 1, accent);
        pixel(img, cx - 2, cy, glow);
        pixel(img, cx - 1, cy - 1, accent);
    }

    private static void ventBars(BufferedImage img, int startX, int startY, int width, int height, int color) {
        for (int y = 0; y < height; y++) {
            drawLine(img, startX, startY + y * 2, startX + width, startY + y * 2, color);
        }
    }

    private static void arrowBand(BufferedImage img, int cx, int cy, int body, int tip, int dir) {
        if (dir >= 0) {
            drawLine(img, cx - 3, cy, cx + 2, cy, body);
            drawLine(img, cx + 1, cy - 2, cx + 4, cy, tip);
            drawLine(img, cx + 1, cy + 2, cx + 4, cy, tip);
        } else {
            drawLine(img, cx + 3, cy, cx - 2, cy, body);
            drawLine(img, cx - 1, cy - 2, cx - 4, cy, tip);
            drawLine(img, cx - 1, cy + 2, cx - 4, cy, tip);
        }
        pixel(img, cx, cy, tip);
    }

    private static void finStrake(BufferedImage img, int cx, int cy, int copper, int dark) {
        drawLine(img, cx - 4, cy - 2, cx + 4, cy - 2, copper);
        drawLine(img, cx - 4, cy + 2, cx + 4, cy + 2, copper);
        drawLine(img, cx - 2, cy - 1, cx + 2, cy - 1, dark);
        drawLine(img, cx - 2, cy + 1, cx + 2, cy + 1, dark);
        pixel(img, cx, cy, copper);
    }

    private static void torpedoTube(BufferedImage img, int cx, int cy, int body, int steel, int brass, int glow) {
        ring(img, cx, cy, 3, body);
        ring(img, cx, cy, 2, steel);
        pixel(img, cx, cy - 2, brass);
        pixel(img, cx - 1, cy - 1, brass);
        pixel(img, cx + 1, cy - 1, brass);
        pixel(img, cx, cy, glow);
        pixel(img, cx - 1, cy, glow);
        pixel(img, cx + 1, cy, glow);
    }

    private static void barrelHighlight(BufferedImage img, int cx, int cy, int highlight) {
        pixel(img, cx, cy - 1, highlight);
        pixel(img, cx - 1, cy - 1, highlight);
        pixel(img, cx + 1, cy - 1, highlight);
        pixel(img, cx, cy + 1, highlight);
    }

    private static void shieldEmblem(BufferedImage img, int cx, int cy, int aqua, int glow, int highlight) {
        ring(img, cx, cy, 3, aqua);
        ring(img, cx, cy, 2, glow);
        pixel(img, cx, cy - 2, highlight);
        pixel(img, cx + 1, cy - 1, highlight);
        pixel(img, cx + 1, cy + 1, highlight);
        pixel(img, cx, cy + 2, highlight);
        pixel(img, cx - 1, cy + 1, highlight);
        pixel(img, cx - 1, cy - 1, highlight);
    }

    private static void tankBody(BufferedImage img, int cx, int cy, int steel, int highlight, int accent) {
        fillRect(img, cx - 2, cy - 4, cx + 2, cy + 4, steel);
        fillRect(img, cx - 1, cy - 3, cx + 1, cy + 3, mix(steel, highlight, 0.55f));
        pixel(img, cx, cy - 4, highlight);
        pixel(img, cx, cy + 4, accent);
        pixel(img, cx - 2, cy - 1, accent);
        pixel(img, cx + 2, cy + 1, accent);
    }

    private static void gauge(BufferedImage img, int x0, int y0, int x1, int y1, int pale, int glow) {
        ring(img, (x0 + x1) / 2, y0 + 1, 2, pale);
        pixel(img, (x0 + x1) / 2, y0 + 1, glow);
        drawLine(img, x0 + 1, y1 - 1, x1 - 1, y1 - 2, pale);
    }

    private static void ring(BufferedImage img, int cx, int cy, int radius, int color) {
        if (radius < 0) {
            return;
        }
        if (radius == 0) {
            pixel(img, cx, cy, color);
            return;
        }
        int x = radius;
        int y = 0;
        int err = 1 - x;
        while (x >= y) {
            pixel(img, cx + x, cy + y, color);
            pixel(img, cx + y, cy + x, color);
            pixel(img, cx - y, cy + x, color);
            pixel(img, cx - x, cy + y, color);
            pixel(img, cx - x, cy - y, color);
            pixel(img, cx - y, cy - x, color);
            pixel(img, cx + y, cy - x, color);
            pixel(img, cx + x, cy - y, color);
            y++;
            if (err < 0) {
                err += 2 * y + 1;
            } else {
                x--;
                err += 2 * (y - x) + 1;
            }
        }
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
