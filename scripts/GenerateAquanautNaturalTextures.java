import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Procedurally generates realistic natural block textures for Aquanaut.
 * Produces: coral_sand, nutrient_rich_mud, shale, limestone.
 *
 * - Coral sand: pale grey with brown undertone + coherent wave ripples
 *   that tile across block boundaries (like beach sand under water).
 * - Nutrient-rich mud: dark brown with strong pale/dark contrast.
 * - Shale & limestone: shared horizontal stratification for adjacent
 *   geological coherence.
 */
public final class GenerateAquanautNaturalTextures {
    private static final int SIZE = 16;
    private static final Path OUT_DIR = Paths.get(
            "src", "main", "resources", "assets", "aquanaut", "textures", "block");

    // ── Coral sand ────────────────────────────────────────────────────
    private static final int CS_BASE   = rgba(198, 192, 183);  // pale warm grey
    private static final int CS_LIGHT  = rgba(217, 212, 203);  // sunlit grain
    private static final int CS_PALE   = rgba(229, 224, 216);  // bleached shell chip
    private static final int CS_SHADOW = rgba(171, 164, 154);  // shadowed grain
    private static final int CS_BROWN  = rgba(176, 163, 145);  // warm brown mineral
    private static final int CS_PINK   = rgba(204, 180, 173);  // coral fragment
    private static final int CS_DARK   = rgba(150, 143, 133);  // darkest grain
    private static final int CS_WAVE_L = rgba(210, 205, 196);  // wave crest highlight
    private static final int CS_WAVE_D = rgba(180, 174, 165);  // wave trough shadow

    // ── Nutrient-rich mud (higher contrast: paler + darker) ────────────
    private static final int MUD_PALE    = rgba(105, 78, 50);   // pale dry highlight
    private static final int MUD_LIGHT   = rgba(80, 58, 37);    // raised clod
    private static final int MUD_BASE    = rgba(55, 38, 23);    // main dark brown
    private static final int MUD_DARK    = rgba(38, 26, 16);    // deep shadow
    private static final int MUD_DEEP    = rgba(24, 16, 10);    // near-black crevice
    private static final int MUD_MOIST   = rgba(44, 30, 19);    // damp patch
    private static final int MUD_SHINE   = rgba(98, 70, 43);    // wet surface glint
    private static final int MUD_ORGANIC = rgba(68, 48, 30);    // decaying matter

    // ── Shale ─────────────────────────────────────────────────────────
    private static final int SH_DEEP   = rgba(42, 41, 45);
    private static final int SH_DARK   = rgba(56, 55, 59);
    private static final int SH_MID    = rgba(76, 74, 78);
    private static final int SH_LIGHT  = rgba(96, 93, 96);
    private static final int SH_PALE   = rgba(112, 109, 111);
    private static final int SH_CARBON = rgba(38, 37, 41);
    private static final int SH_RUST   = rgba(135, 104, 82);
    private static final int SH_CRACK  = rgba(32, 32, 36);
    private static final int SH_SHEEN  = rgba(118, 114, 116);
    private static final int SH_SILT   = rgba(88, 86, 89);

    // ── Limestone ─────────────────────────────────────────────────────
    private static final int LS_DEEP   = rgba(162, 156, 147);
    private static final int LS_DARK   = rgba(175, 169, 160);
    private static final int LS_BASE   = rgba(202, 197, 189);
    private static final int LS_LIGHT  = rgba(220, 216, 209);
    private static final int LS_PALE   = rgba(238, 235, 229);
    private static final int LS_FOSSIL = rgba(190, 183, 173);
    private static final int LS_SPAR   = rgba(245, 242, 237);
    private static final int LS_PIT    = rgba(172, 166, 157);
    private static final int LS_WARM   = rgba(212, 205, 194);

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUT_DIR);

        generateCoralSand();
        generateNutrientRichMud();
        generateShale();
        generateLimestone();
        generateShaleTop();
        generateLimestoneTop();

        System.out.println("Generated 6 natural block textures in " + OUT_DIR);
    }

    // ══════════════════════════════════════════════════════════════════
    // CORAL SAND — distinct 45° diagonal wave ripples, clean & minimal noise
    // ══════════════════════════════════════════════════════════════════
    //
    // Wave ridges run diagonally (/// direction, crests ⊥ to (1,1)).
    // Period = 5 px → exactly 3 ridges per block, tiles on all edges.
    private static void generateCoralSand() throws IOException {
        BufferedImage img = blank();

        // ── Clean flat base: subtle warm grey, very little grain ──
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float n = noise2dTile(x, y, 7331, SIZE);
                int col;
                if (n < 0.15f)       col = lerp(CS_SHADOW, CS_BASE,  n / 0.15f);
                else if (n < 0.85f)  col = CS_BASE;
                else                 col = lerp(CS_BASE,   CS_LIGHT, (n - 0.85f) / 0.15f);
                img.setRGB(x, y, col);
            }
        }

        // ── Primary 45° diagonal wave ridges ──────────────────────────
        // phase = (x + y) / 5.0  →  3 full cycles per block, tiles perfectly.
        // Ridge profile is asymmetric (water-flow ripples: gentle stoss,
        // steeper lee slope).
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                // Subtle tileable wobble: sin*cos with period 16
                float wx = sin01(x * TWO_PI / 16f + 0.4f);
                float wy = sin01(y * TWO_PI / 16f + 1.9f);
                float wobble = (wx * wy - 0.5f) * 0.55f;  // subtle ±0.27px

                // Diagonal phase: lines ⊥ to (1,1)
                float phase = ((x + y + wobble) / 5.0f);
                float raw = sin01(phase * TWO_PI);

                // Asymmetric ridge (water-flow: gentle stoss, steep lee)
                float asym = 0.58f;
                float ridge;
                if (raw < asym) ridge = raw / asym;
                else            ridge = 1f - (raw - asym) / (1f - asym);

                int c = img.getRGB(x, y);
                if ((c >>> 24) == 0) continue;

                // Stronger ridge modulation for distinct wave pattern
                float str = 0.30f;
                if (ridge > 0.70f) {
                    c = mix(c, CS_WAVE_L, (ridge - 0.70f) * str * 4.0f);
                } else if (ridge < 0.20f) {
                    c = mix(c, CS_WAVE_D, (0.20f - ridge) * str * 4.0f);
                }
                img.setRGB(x, y, c);
            }
        }

        // ── Secondary faint cross-ripple (subtler, ⊥ direction) ──
        // Runs roughly ↙↗, phase = (x - y) / 8.0, also tiles.
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float phase2 = ((x - y + 16f) / 8.0f);
                float ridge2 = sin01(phase2 * TWO_PI);
                int c = img.getRGB(x, y);
                if ((c >>> 24) == 0) continue;

                float str2 = 0.04f;
                if (ridge2 > 0.78f) c = mix(c, CS_WAVE_L, (ridge2 - 0.78f) * str2);
                else if (ridge2 < 0.22f) c = mix(c, CS_WAVE_D, (0.22f - ridge2) * str2);
                img.setRGB(x, y, c);
            }
        }

        // ── Sparse shell / coral fragments ──
        scatter(img, 37,  5, CS_PALE,  0.65f);
        scatter(img, 53,  3, CS_BROWN,  0.50f);
        scatter(img, 59,  2, CS_PINK,   0.45f);
        scatter(img, 71,  3, CS_LIGHT,  0.55f);
        scatter(img, 97,  2, CS_PALE,   0.55f);

        // ── Rare darker mineral specks ──
        speckle(img, 67, 3, CS_DARK, 0.40f);

        // ── Edge-tile soften ──
        tileBlend(img, 0.003f, 131);

        write("coral_sand.png", img);
    }

    // ══════════════════════════════════════════════════════════════════
    // NUTRIENT-RICH MUD — paler highlights + deeper darks, high contrast
    // ══════════════════════════════════════════════════════════════════
    private static void generateNutrientRichMud() throws IOException {
        BufferedImage img = blank();

        // ── Base: strong contrast with wide tonal range ──
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float n1 = noise2dTile(x, y, 8447, SIZE);
                float n2 = noise2dTile(x * 3 + 1, y * 3 + 2, 8461, SIZE * 3) * 0.3f;
                float n3 = noise2dTile(x * 5 + 7, y * 5 + 3, 8521, SIZE * 5) * 0.15f;
                float n = n1 * 0.55f + n2 * 0.30f + n3 * 0.15f;

                int col;
                if (n < 0.12f)           col = MUD_DEEP;
                else if (n < 0.28f)      col = lerp(MUD_DEEP,  MUD_DARK,    (n - 0.12f) / 0.16f);
                else if (n < 0.50f)      col = lerp(MUD_DARK,  MUD_BASE,    (n - 0.28f) / 0.22f);
                else if (n < 0.70f)      col = lerp(MUD_BASE,  MUD_LIGHT,   (n - 0.50f) / 0.20f);
                else if (n < 0.88f)      col = lerp(MUD_LIGHT, MUD_PALE,    (n - 0.70f) / 0.18f);
                else                      col = MUD_PALE;

                img.setRGB(x, y, col);
            }
        }

        // ── Irregular organic clods (raised pale patches) ──
        organicBlob(img, 3,  4, 3, 2, MUD_PALE,    0.55f, 191);
        organicBlob(img, 11, 5, 2, 3, MUD_LIGHT,   0.50f, 193);
        organicBlob(img, 7, 13, 4, 2, MUD_PALE,    0.45f, 197);
        organicBlob(img, 1, 10, 2, 3, MUD_ORGANIC, 0.40f, 199);
        organicBlob(img, 12, 1, 3, 2, MUD_SHINE,   0.35f, 211);

        // ── Deep dark crevices ──
        organicBlob(img, 5, 7, 1, 2, MUD_DEEP,  0.60f, 223);
        organicBlob(img, 9, 3, 2, 1, MUD_DARK,  0.55f, 227);
        organicBlob(img, 13, 9, 1, 2, MUD_DEEP, 0.50f, 229);
        organicBlob(img, 2, 14, 2, 1, MUD_DARK, 0.50f, 233);

        // ── Moisture streaks (horizontal-ish wet lines) ──
        moistStreak(img, 2, 4,  12, 5,  MUD_MOIST, 0.40f, 241);
        moistStreak(img, 1, 11, 13, 10, MUD_MOIST, 0.35f, 251);

        // ── Wet glints ──
        scatter(img, 93,  5, MUD_SHINE,  0.55f);
        scatter(img, 97,  4, MUD_PALE,   0.50f);
        scatter(img, 101, 5, MUD_SHINE,  0.50f);
        scatter(img, 107, 3, MUD_LIGHT,  0.45f);

        // ── Tiny darkest specks ──
        speckle(img, 113, 7, MUD_DEEP, 0.50f);

        tileBlend(img, 0.005f, 261);

        write("nutrient_rich_mud.png", img);
    }

    // ══════════════════════════════════════════════════════════════════
    // SHALE — fissile sedimentary with organic stratification
    // ══════════════════════════════════════════════════════════════════
    private static void generateShale() throws IOException {
        BufferedImage img = blank();

        // ── Stratified base fill ──
        for (int y = 0; y < SIZE; y++) {
            float phase = strataPhase(y);
            int base;
            if      (phase < 0.14f) base = lerp(SH_DEEP,  SH_CARBON, phase / 0.14f);
            else if (phase < 0.30f) base = lerp(SH_CARBON, SH_DARK,  (phase - 0.14f) / 0.16f);
            else if (phase < 0.48f) base = lerp(SH_DARK,   SH_MID,   (phase - 0.30f) / 0.18f);
            else if (phase < 0.62f) base = lerp(SH_MID,    SH_LIGHT, (phase - 0.48f) / 0.14f);
            else if (phase < 0.78f) base = SH_LIGHT;
            else if (phase < 0.90f) base = lerp(SH_LIGHT,  SH_MID,   (phase - 0.78f) / 0.12f);
            else                    base = lerp(SH_DARK,    SH_DEEP,  (phase - 0.90f) / 0.10f);

            for (int x = 0; x < SIZE; x++) {
                float g1 = noise2dTile(x, y, 5077, SIZE);
                float g2 = noise2dTile(x * 2 + 3, y, 5089, SIZE * 2) * 0.35f;
                float grain = g1 * 0.65f + g2 * 0.35f;

                int col;
                if (grain < 0.12f)       col = mix(base, SH_DEEP,  0.55f);
                else if (grain < 0.30f)  col = mix(base, SH_DARK,  0.20f * grain);
                else if (grain < 0.70f)  col = base;
                else if (grain < 0.88f)  col = mix(base, SH_LIGHT, 0.30f * (grain - 0.70f));
                else                     col = mix(base, SH_SHEEN, 0.18f);

                img.setRGB(x, y, col);
            }
        }

        // ── Wavy perturbation to bands (natural dipping strata) ──
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float offset = smoothNoise2d(x * 0.4f, y, 5113) * 0.03f;
                int current = img.getRGB(x, y);
                if ((current >>> 24) != 0) {
                    // subtle horizontal variation along bands
                    if (noise2d(x * 2, y, 5147) > 0.85f) {
                        img.setRGB(x, y, mix(current, SH_SILT, 0.12f));
                    }
                }
            }
        }

        // ── Fissures / cracks ──
        crack(img, 3, 0,  3, 15, SH_CRACK, 0.60f, 311);
        crack(img, 9, 3,  9, 13, SH_CRACK, 0.40f, 313);
        crack(img, 13, 2, 12, 11, SH_DARK,  0.35f, 317);
        // Diagonal fracture
        crack(img, 6, 1, 8, 4, SH_CRACK, 0.45f, 331);
        crack(img, 10, 13, 13, 10, SH_DARK, 0.35f, 337);

        // ── Iron oxide staining ──
        rustStreak(img, 5, 3,  12, SH_RUST, 0.30f, 347);
        rustStreak(img, 2, 10, 10, SH_RUST, 0.25f, 353);

        // ── Flaked faces ──
        flakePatch(img, 4, 7, 7, 9, SH_LIGHT, 0.30f, 359);

        // ── Sheen specks ──
        scatterBand(img, 5, 7, 4, SH_SHEEN, 367);
        scatterBand(img, 3, 11, 3, SH_PALE, 373);

        tileBlend(img, 0.004f, 379);

        write("shale.png", img);
    }

    // ══════════════════════════════════════════════════════════════════
    // LIMESTONE — coherent banding with shale, fossil-rich, weathered
    // ══════════════════════════════════════════════════════════════════
    private static void generateLimestone() throws IOException {
        BufferedImage img = blank();

        // ── Stratified base (same phase function as shale) ──
        for (int y = 0; y < SIZE; y++) {
            float phase = strataPhase(y);
            int base;
            if      (phase < 0.14f) base = lerp(LS_DEEP, LS_DARK,  phase / 0.14f);
            else if (phase < 0.30f) base = lerp(LS_DARK, LS_BASE,  (phase - 0.14f) / 0.16f);
            else if (phase < 0.48f) base = lerp(LS_BASE, LS_LIGHT, (phase - 0.30f) / 0.18f);
            else if (phase < 0.62f) base = lerp(LS_LIGHT, LS_PALE, (phase - 0.48f) / 0.14f);
            else if (phase < 0.78f) base = LS_PALE;
            else if (phase < 0.90f) base = lerp(LS_PALE,  LS_LIGHT, (phase - 0.78f) / 0.12f);
            else                    base = lerp(LS_DARK,   LS_DEEP,  (phase - 0.90f) / 0.10f);

            for (int x = 0; x < SIZE; x++) {
                float g1 = noise2dTile(x, y, 6029, SIZE);
                float g2 = noise2dTile(x * 2 + 1, y, 6043, SIZE * 2) * 0.30f;
                float grain = g1 * 0.70f + g2 * 0.30f;

                int col;
                if (grain < 0.10f)       col = mix(base, LS_PIT,  0.50f);
                else if (grain < 0.35f)  col = base;
                else if (grain < 0.60f)  col = mix(base, LS_WARM, 0.25f * (grain - 0.35f));
                else if (grain < 0.85f)  col = mix(base, LS_LIGHT, 0.20f * (grain - 0.60f));
                else                     col = mix(base, LS_SPAR,  0.22f);

                img.setRGB(x, y, col);
            }
        }

        // ── Fossil inclusions (shell / crinoid cross-sections) ──
        crinoidFossil(img, 6,  8, LS_FOSSIL, 0.55f, 401);
        shellFossil(img,   11, 4, LS_FOSSIL, 0.45f, 409);
        crinoidFossil(img, 3,  13, LS_FOSSIL, 0.40f, 419);
        shellFossil(img,   13, 10, LS_FOSSIL, 0.35f, 429);

        // ── Calcite spar (crystalline sparkles) ──
        scatter(img, 401, 6, LS_SPAR,  0.55f);
        scatter(img, 409, 4, LS_PALE,  0.50f);
        scatter(img, 419, 5, LS_SPAR,  0.50f);
        scatter(img, 431, 3, LS_LIGHT, 0.45f);

        // ── Weathered pits ──
        pitCluster(img, 5, 11, 3, LS_PIT, 0.45f, 439);
        pitCluster(img, 10, 2, 2, LS_PIT, 0.40f, 443);

        // ── Warm patches ──
        warmPatch(img, 4,  5, 3, 2, LS_WARM, 0.25f, 449);
        warmPatch(img, 9, 10, 3, 3, LS_WARM, 0.22f, 457);

        tileBlend(img, 0.004f, 461);

        write("limestone.png", img);
    }

    // ══════════════════════════════════════════════════════════════════
    // SHALE TOP — plan view of the uppermost bedding surface
    // ══════════════════════════════════════════════════════════════════
    //
    // Shows the flat cleavage surface: thin parallel striations where
    // adjacent layers meet, subtle flaking, darker than the side view.
    private static void generateShaleTop() throws IOException {
        BufferedImage img = blank();

        // ── Base: tileable sinusoidal mottle ──
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float n1 = sin01((x * 3 + y * 1) * TWO_PI / 16f + 0.33f);
                float n2 = sin01((x * 1 - y * 4) * TWO_PI / 16f + 1.87f);
                float n3 = sin01((x * 5 + y * 2) * TWO_PI / 16f + 2.91f);
                float n = n1 * 0.45f + n2 * 0.30f + n3 * 0.25f;

                int col;
                if (n < 0.25f)       col = lerp(SH_DEEP, SH_DARK,  n / 0.25f);
                else if (n < 0.60f)  col = lerp(SH_DARK, SH_MID,   (n - 0.25f) / 0.35f);
                else                 col = lerp(SH_MID,   SH_LIGHT, (n - 0.60f) / 0.40f);
                img.setRGB(x, y, col);
            }
        }

        // ── Fine striations — layer edges viewed from above ──
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float phase = (x * 2f + y * 1f + 0.5f) / 16f;
                float lp = sin01(phase * TWO_PI);
                int c = img.getRGB(x, y);
                if ((c >>> 24) == 0) continue;
                if (lp > 0.84f)      c = mix(c, SH_PALE, (lp - 0.84f) * 0.32f);
                else if (lp < 0.16f) c = mix(c, SH_DEEP, (0.16f - lp) * 0.28f);
                img.setRGB(x, y, c);
            }
        }

        // ── Flake plates ──
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float n = sin01((x * 7 + y * 3) * TWO_PI / 16f + 0.77f)
                        * sin01((x * 2 - y * 5) * TWO_PI / 16f + 2.13f);
                int c = img.getRGB(x, y);
                if ((c >>> 24) == 0) continue;
                if (n > 0.72f) c = mix(c, SH_SHEEN, (n - 0.72f) * 0.50f);
                img.setRGB(x, y, c);
            }
        }

        // ── Mineral specks ──
        scatter(img, 271, 5, SH_PALE,  0.45f);
        scatter(img, 277, 4, SH_RUST,  0.35f);
        scatter(img, 281, 3, SH_SHEEN, 0.40f);

        tileBlend(img, 0.003f, 293);

        write("shale_top.png", img);
    }

    // ══════════════════════════════════════════════════════════════════
    // LIMESTONE TOP — plan view of the uppermost bedding surface
    // ══════════════════════════════════════════════════════════════════
    //
    // Shows the flat carbonate platform: massive appearance, subtle
    // mottling of mineral patches, fossil traces, calcite specks.
    private static void generateLimestoneTop() throws IOException {
        BufferedImage img = blank();

        // ── Base: tileable sinusoidal mottle ──
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float n1 = sin01((x * 2 - y * 3) * TWO_PI / 16f + 0.91f);
                float n2 = sin01((x * 4 + y * 1) * TWO_PI / 16f + 2.53f);
                float n3 = sin01((x * 1 + y * 5) * TWO_PI / 16f + 4.17f);
                float n = n1 * 0.40f + n2 * 0.35f + n3 * 0.25f;

                int col;
                if (n < 0.18f)       col = lerp(LS_DARK,  LS_BASE,  n / 0.18f);
                else if (n < 0.55f)  col = lerp(LS_BASE,  LS_LIGHT, (n - 0.18f) / 0.37f);
                else if (n < 0.85f)  col = lerp(LS_LIGHT, LS_PALE,  (n - 0.55f) / 0.30f);
                else                 col = LS_PALE;
                img.setRGB(x, y, col);
            }
        }

        // ── Subtle bedding striations ──
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float phase = (x * 1f + y * 2f + 0.7f) / 16f;
                float lp = sin01(phase * TWO_PI);
                int c = img.getRGB(x, y);
                if ((c >>> 24) == 0) continue;
                if (lp > 0.86f)      c = mix(c, LS_PALE, (lp - 0.86f) * 0.22f);
                else if (lp < 0.14f) c = mix(c, LS_DARK, (0.14f - lp) * 0.20f);
                img.setRGB(x, y, c);
            }
        }

        // ── Mottled mineral patches ──
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float n = sin01((x * 3 + y * 2) * TWO_PI / 16f + 1.41f)
                        + sin01((x * 5 - y * 3) * TWO_PI / 16f + 3.79f) * 0.5f;
                n /= 1.5f;
                int c = img.getRGB(x, y);
                if ((c >>> 24) == 0) continue;
                if (n > 0.76f)      c = mix(c, LS_WARM, (n - 0.76f) * 0.30f);
                else if (n < 0.24f) c = mix(c, LS_DARK, (0.24f - n) * 0.24f);
                img.setRGB(x, y, c);
            }
        }

        // ── Flat fossil impressions ──
        topFossilImprint(img, 5, 7,  LS_FOSSIL, 0.40f, 481);
        topFossilImprint(img, 11, 3, LS_FOSSIL, 0.35f, 487);
        topFossilImprint(img, 8, 13, LS_FOSSIL, 0.30f, 491);

        // ── Calcite specks ──
        scatter(img, 401, 5, LS_SPAR,  0.50f);
        scatter(img, 413, 4, LS_PALE,  0.45f);
        scatter(img, 421, 3, LS_LIGHT, 0.40f);

        // ── Small pits ──
        pitCluster(img, 9, 5, 2, LS_PIT, 0.35f, 499);
        pitCluster(img, 4, 12, 2, LS_PIT, 0.30f, 503);

        tileBlend(img, 0.003f, 509);

        write("limestone_top.png", img);
    }

    // ══════════════════════════════════════════════════════════════════
    // SHARED STRATIGRAPHY — identical band phase for shale + limestone
    // ══════════════════════════════════════════════════════════════════
    /** Shared stratigraphy for shale + limestone. Phase wraps so y=0≈y=15 (dark seam). */
    private static float strataPhase(int y) {
        float[] phases = {
            0.00f, 0.07f,   // 0-1:  deep seam
            0.16f, 0.24f,   // 2-3:  dark transition
            0.34f, 0.42f,   // 4-5:  medium
            0.50f, 0.58f,   // 6-7:  medium-light
            0.66f, 0.74f,   // 8-9:  light band
            0.82f, 0.86f,   // 10-11: upper transition
            0.90f, 0.93f,   // 12-13: darkening
            0.96f, 1.00f,   // 14-15: deep seam (wraps to dark)
        };
        return phases[Math.min(y, phases.length - 1)];
    }

    // ══════════════════════════════════════════════════════════════════
    // PRIMITIVES
    // ══════════════════════════════════════════════════════════════════

    private static void scatter(BufferedImage img, int seed, int count, int color, float strength) {
        for (int i = 0; i < count; i++) {
            int h = hash(seed + i * 31);
            // Avoid 1px edge zone so grains don't break tiling
            int x = 1 + ((h & 0xFF) % (SIZE - 2));
            int y = 1 + (((h >>> 8) & 0xFF) % (SIZE - 2));
            if ((img.getRGB(x, y) >>> 24) != 0) {
                img.setRGB(x, y, mix(img.getRGB(x, y), color, strength));
            }
        }
    }

    private static void speckle(BufferedImage img, int seed, int count, int color, float strength) {
        scatter(img, seed, count, color, strength);
    }

    private static void scatterBand(BufferedImage img, int y0, int y1, int count, int color, int seed) {
        for (int i = 0; i < count; i++) {
            int h = hash(seed + i * 37);
            int x = (h & 0xFF) % SIZE;
            int y = y0 + ((h >>> 8) & 0xFF) % Math.max(1, y1 - y0 + 1);
            if (y < SIZE && (img.getRGB(x, y) >>> 24) != 0) {
                img.setRGB(x, y, mix(img.getRGB(x, y), color, 0.50f));
            }
        }
    }

    /** Irregular organic blob with uneven edges. */
    private static void organicBlob(BufferedImage img, int cx, int cy, int rx, int ry,
                                     int color, float strength, int seed) {
        for (int dy = -ry - 1; dy <= ry + 1; dy++) {
            for (int dx = -rx - 1; dx <= rx + 1; dx++) {
                int x = cx + dx;
                int y = cy + dy;
                if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) continue;
                float ed = (dx * dx) / (float)((rx + 0.01f) * (rx + 0.01f))
                         + (dy * dy) / (float)((ry + 0.01f) * (ry + 0.01f));
                if (ed <= 1.25f) {
                    float fade = Math.max(0f, 1f - ed * 0.8f);
                    // irregular edge
                    if (ed > 0.8f && (hash(seed + x * 31 + y * 17) & 3) == 0) continue;
                    int current = img.getRGB(x, y);
                    if ((current >>> 24) != 0) {
                        img.setRGB(x, y, mix(current, color, strength * fade));
                    }
                }
            }
        }
    }

    /** Moisture streak — wobbly horizontal line. */
    private static void moistStreak(BufferedImage img, int x0, int y0, int x1, int y1,
                                     int color, float strength, int seed) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0, y = y0;
        while (true) {
            if (x >= 0 && x < SIZE && y >= 0 && y < SIZE) {
                int current = img.getRGB(x, y);
                if ((current >>> 24) != 0) {
                    // bleed to adjacent rows
                    img.setRGB(x, y, mix(current, color, strength));
                    if (y > 0) {
                        int above = img.getRGB(x, y - 1);
                        if ((above >>> 24) != 0) img.setRGB(x, y - 1, mix(above, color, strength * 0.35f));
                    }
                    if (y + 1 < SIZE) {
                        int below = img.getRGB(x, y + 1);
                        if ((below >>> 24) != 0) img.setRGB(x, y + 1, mix(below, color, strength * 0.30f));
                    }
                }
            }
            if (x == x1 && y == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 < dx)  { err += dx; y += sy; }
        }
    }

    /** Crack/fissure with irregular width. */
    private static void crack(BufferedImage img, int x0, int y0, int x1, int y1,
                               int color, float strength, int seed) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0, y = y0;
        while (true) {
            if (x >= 0 && x < SIZE && y >= 0 && y < SIZE) {
                int current = img.getRGB(x, y);
                if ((current >>> 24) != 0) {
                    float w = (hash(seed + y * 7 + x * 3) & 1) == 0 ? 1f : 0.6f;
                    img.setRGB(x, y, mix(current, color, strength * w));
                    // occasional widen
                    if ((hash(seed + 13 + y + x) & 3) == 0) {
                        int nx = x + ((hash(seed + 17 + y) & 1) * 2 - 1);
                        if (nx >= 0 && nx < SIZE) {
                            int nc = img.getRGB(nx, y);
                            if ((nc >>> 24) != 0) img.setRGB(nx, y, mix(nc, color, strength * 0.35f));
                        }
                    }
                }
            }
            if (x == x1 && y == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 < dx)  { err += dx; y += sy; }
        }
    }

    /** Rust staining along a layer. */
    private static void rustStreak(BufferedImage img, int x0, int y, int x1, int color, float strength, int seed) {
        int lo = Math.min(x0, x1), hi = Math.max(x0, x1);
        for (int x = lo; x <= hi; x++) {
            if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) continue;
            int c = img.getRGB(x, y);
            if ((c >>> 24) != 0) img.setRGB(x, y, mix(c, color, strength));
            if (y + 1 < SIZE) {
                int c2 = img.getRGB(x, y + 1);
                if ((c2 >>> 24) != 0) img.setRGB(x, y + 1, mix(c2, color, strength * 0.30f));
            }
        }
    }

    /** Flake patch. */
    private static void flakePatch(BufferedImage img, int x0, int y0, int x1, int y1,
                                    int color, float strength, int seed) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) continue;
                if ((hash(seed + 29 + x + y * 3) & 1) == 0) continue;
                int c = img.getRGB(x, y);
                if ((c >>> 24) != 0) img.setRGB(x, y, mix(c, color, strength));
            }
        }
    }

    /** Crinoid stem fossil — small ring-like imprint. */
    private static void crinoidFossil(BufferedImage img, int cx, int cy, int color, float strength, int seed) {
        // Ring shape
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int x = cx + dx, y = cy + dy;
                if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) continue;
                int c = img.getRGB(x, y);
                if ((c >>> 24) == 0) continue;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist > 0.7f && dist < 1.6f) {
                    img.setRGB(x, y, mix(c, color, strength));
                }
            }
        }
        // Center dot
        if ((img.getRGB(cx, cy) >>> 24) != 0)
            img.setRGB(cx, cy, mix(img.getRGB(cx, cy), LS_PALE, strength * 0.6f));
    }

    /** Shell cross-section fossil. */
    private static void shellFossil(BufferedImage img, int cx, int cy, int color, float strength, int seed) {
        // Curved half-circle shape
        int[][] shape = {
            {-1, -1}, {0, -1}, {1, -1},
            {-1,  0},          {1,  0},
                      {0,  1},
        };
        for (int[] off : shape) {
            int x = cx + off[0], y = cy + off[1];
            if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) continue;
            int c = img.getRGB(x, y);
            if ((c >>> 24) != 0)
                img.setRGB(x, y, mix(c, color, strength));
        }
    }

    /** Flat fossil impression as seen from above — spiral/coiled shape. */
    private static void topFossilImprint(BufferedImage img, int cx, int cy, int color, float strength, int seed) {
        // Small spiral shape — ammonite-like
        int[][] spiral = {
            {0, -2}, {1, -2},
            {2, -1}, {2, 0},
            {1, 1}, {0, 1},
            {-1, 0},
        };
        for (int[] off : spiral) {
            int x = cx + off[0], y = cy + off[1];
            if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) continue;
            int c = img.getRGB(x, y);
            if ((c >>> 24) != 0)
                img.setRGB(x, y, mix(c, color, strength + hash(seed + x + y * 7) * 0.03f));
        }
        // Center dot
        if (cx >= 0 && cx < SIZE && cy >= 0 && cy < SIZE) {
            int c = img.getRGB(cx, cy);
            if ((c >>> 24) != 0)
                img.setRGB(cx, cy, mix(c, LS_PALE, strength * 0.5f));
        }
    }

    /** Weathered pit cluster. */
    private static void pitCluster(BufferedImage img, int cx, int cy, int r, int color, float strength, int seed) {
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                int x = cx + dx, y = cy + dy;
                if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) continue;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist <= r && (hash(seed + x + y * 17) & 3) != 0) {
                    int c = img.getRGB(x, y);
                    if ((c >>> 24) != 0)
                        img.setRGB(x, y, mix(c, color, strength * (1f - dist / r) * 0.8f));
                }
            }
        }
    }

    /** Warm undertone patch. */
    private static void warmPatch(BufferedImage img, int x0, int y0, int w, int h, int color, float strength, int seed) {
        for (int y = y0; y < y0 + h && y < SIZE; y++) {
            for (int x = x0; x < x0 + w && x < SIZE; x++) {
                int c = img.getRGB(x, y);
                if ((c >>> 24) != 0)
                    img.setRGB(x, y, mix(c, color, strength));
            }
        }
    }

    /** Soft tile-blend at edges for seamless wrapping. */
    private static void tileBlend(BufferedImage img, float strength, int seed) {
        int bw = 3; // wider blend zone
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float ef = 1f;
                if (x < bw) ef = (float)x / bw;
                else if (x >= SIZE - bw) ef = (float)(SIZE - 1 - x) / bw;
                if (y < bw) ef *= (float)y / bw;
                else if (y >= SIZE - bw) ef *= (float)(SIZE - 1 - y) / bw;

                if (ef < 0.92f) {
                    int cur = img.getRGB(x, y);
                    if ((cur >>> 24) == 0) continue;
                    int wx = (x + SIZE / 2) % SIZE;
                    int wy = (y + SIZE / 2) % SIZE;
                    int nb = img.getRGB(wx, wy);
                    if ((nb >>> 24) != 0) {
                        img.setRGB(x, y, mix(cur, nb, (1f - ef) * strength * 4.0f));
                    }
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // NOISE
    // ══════════════════════════════════════════════════════════════════

    private static final float TWO_PI = 6.283185307f;

    /** Hash-based pseudo-random noise, range [0, 1). */
    private static float noise2d(int x, int y, int seed) {
        int n = x * 374761393 + y * 668265263 + seed * 1442695041;
        n = (n ^ (n >>> 13)) * 1274126177;
        n = n ^ (n >>> 16);
        return (n & 0x7FFFFFFF) / (float) 0x7FFFFFFF;
    }

    /** Tiling variant: blends near x-edges so texture wraps seamlessly at [0, wrapX). */
    private static float noise2dTile(int x, int y, int seed, int wrapX) {
        float edgeDist = Math.min(x, wrapX - 1 - x);
        if (edgeDist >= 2f) return noise2d(x, y, seed);
        // Blend with wrapped coordinate on the opposite side
        int wx = (x + wrapX / 2) % wrapX;
        float t = edgeDist / 2f; // 0 at edge, 1 at dist ≥ 2
        t = t * t * (3f - 2f * t); // smoothstep
        float here = noise2d(x, y, seed);
        float wrap = noise2d(wx, y, seed + 1);
        return here * t + wrap * (1f - t);
    }

    /** Smoother noise: bilinear interpolation of hash grid, still [0,1]. */
    private static float smoothNoise2d(float fx, float fy, int seed) {
        int ix = (int) Math.floor(fx);
        int iy = (int) Math.floor(fy);
        float tx = fx - ix, ty = fy - iy;
        // smoothstep
        float sx = tx * tx * (3f - 2f * tx);
        float sy = ty * ty * (3f - 2f * ty);

        float v00 = noise2d(ix,     iy,     seed);
        float v10 = noise2d(ix + 1, iy,     seed);
        float v01 = noise2d(ix,     iy + 1, seed);
        float v11 = noise2d(ix + 1, iy + 1, seed);

        float a = v00 + sx * (v10 - v00);
        float b = v01 + sx * (v11 - v01);
        return a + sy * (b - a);
    }

    private static float sin01(float x) {
        return (float) ((Math.sin(x) + 1.0) * 0.5);
    }

    private static int hash(int n) {
        n = (n ^ (n >>> 13)) * 1274126177;
        return n ^ (n >>> 16);
    }

    /** Linear interpolation between two colors. */
    private static int lerp(int a, int b, float t) {
        return mix(a, b, Math.max(0f, Math.min(1f, t)));
    }

    // ══════════════════════════════════════════════════════════════════
    // IMAGE I/O
    // ══════════════════════════════════════════════════════════════════

    private static BufferedImage blank() {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++)
            for (int x = 0; x < SIZE; x++)
                img.setRGB(x, y, 0xFF000000);
        return img;
    }

    private static void write(String name, BufferedImage img) throws IOException {
        ImageIO.write(img, "png", OUT_DIR.resolve(name).toFile());
    }

    // ══════════════════════════════════════════════════════════════════
    // COLOR MATH
    // ══════════════════════════════════════════════════════════════════

    private static int mix(int a, int b, float t) {
        float u = 1f - Math.max(0f, Math.min(1f, t));
        float v = Math.max(0f, Math.min(1f, t));
        int ar = (a >>> 16) & 0xFF, ag = (a >>> 8) & 0xFF, ab = a & 0xFF, aa = (a >>> 24) & 0xFF;
        int br = (b >>> 16) & 0xFF, bg = (b >>> 8) & 0xFF, bb = b & 0xFF, ba = (b >>> 24) & 0xFF;
        return ((Math.round(aa * u + ba * v) << 24)
              | (Math.round(ar * u + br * v) << 16)
              | (Math.round(ag * u + bg * v) << 8)
              |  Math.round(ab * u + bb * v));
    }

    private static int rgba(int r, int g, int b) {
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
