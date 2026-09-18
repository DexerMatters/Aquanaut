package com.dexer.aquanaut;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the assets added with {@code models.zip}: geometry, textures, animation clips, spawn eggs,
 * notebook entries and the dissection table.
 *
 * <p>
 * The pipeline is script driven, so a rename or a forgotten export is easy to miss by eye. These
 * checks fail loudly instead: every animated bone must exist in its geometry, every clip referenced
 * by the entity classes must exist, every notebook paragraph must be translated in both languages,
 * and the spawn egg sprite must be inside the same statistical envelope as the shipped eggs.
 */
public final class NewSpeciesAssetTest {

    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final Path ASSETS = RESOURCES.resolve("assets/aquanaut");
    private static final Path DATA = RESOURCES.resolve("data/aquanaut");

    private static final Set<String> SPECIES = Set.of(
            "vamprey", "oresucker", "flagellonautilus", "skeleton_carp", "golden_carp", "silver_carp",
            "gentlefish", "slimmy", "ionfin", "opticichthus", "gemini_jellyfish", "ecofish",
            "pale_abyss_hydra", "three_headed_shark");

    /** Clips the entity classes actually reference. */
    private static final Map<String, List<String>> EXPECTED_CLIPS = Map.ofEntries(
            Map.entry("vamprey", List.of("swim", "open", "close", "charge")),
            Map.entry("oresucker", List.of("swim")),
            Map.entry("flagellonautilus", List.of("swim", "attack")),
            Map.entry("skeleton_carp", List.of("swim")),
            Map.entry("golden_carp", List.of("swim")),
            Map.entry("silver_carp", List.of("swim")),
            Map.entry("gentlefish", List.of("swim")),
            Map.entry("slimmy", List.of("swim")),
            Map.entry("ionfin", List.of("swim")),
            Map.entry("opticichthus", List.of("swim", "target")),
            Map.entry("gemini_jellyfish", List.of("swim")),
            Map.entry("ecofish", List.of("swim")),
            Map.entry("pale_abyss_hydra", List.of("swim", "attack")),
            Map.entry("three_headed_shark", List.of("swim", "charge", "attack")));

    private static final Set<String> GLOWING = Set.of(
            "vamprey", "ionfin", "opticichthus", "gemini_jellyfish");

    /**
     * Cube counts per model, as {@code boxUV:perFace:mirrored}.
     *
     * <p>
     * This is the fingerprint of the exporter's output. The models are exported by Blockbench itself
     * (File &gt; Export through the GeckoLib plugin's format), so uniform cubes keep the compact
     * cube-level UV form, per-face cubes carry their six rectangles, and a mirrored auto-UV cube is
     * flagged with {@code "mirror": true} exactly like the shipped Aquanaut models. A regression in
     * the export (or a hand edit) changes these numbers, which should fail loudly.
     */
    private static final Map<String, String> CUBE_UV_CENSUS = Map.ofEntries(
            Map.entry("vamprey", "8:3:1"),
            Map.entry("oresucker", "6:5:0"),
            Map.entry("flagellonautilus", "62:1:2"),
            Map.entry("skeleton_carp", "6:11:0"),
            Map.entry("golden_carp", "6:11:0"),
            Map.entry("silver_carp", "6:11:0"),
            Map.entry("gentlefish", "2:9:0"),
            Map.entry("slimmy", "27:3:7"),
            Map.entry("ionfin", "8:14:1"),
            Map.entry("opticichthus", "10:8:1"),
            Map.entry("gemini_jellyfish", "80:2:13"),
            Map.entry("ecofish", "156:84:0"),
            Map.entry("pale_abyss_hydra", "145:0:0"),
            Map.entry("three_headed_shark", "20:14:4"),
            Map.entry("dissection_table", "27:0:0"),
            Map.entry("dissection_table_x2", "36:0:0"),
            Map.entry("dissection_table_x4", "42:0:0"));

    @Test
    void everySpeciesShipsGeometryTextureAndAnimation() {
        for (String slug : SPECIES) {
            Path geo = ASSETS.resolve("geo/" + slug + ".geo.json");
            Path texture = ASSETS.resolve("textures/entity/" + slug + ".png");
            Path animation = ASSETS.resolve("animations/" + slug + ".animation.json");

            assertTrue(Files.isRegularFile(geo), "missing geometry for " + slug);
            assertTrue(Files.isRegularFile(texture), "missing entity texture for " + slug);
            assertTrue(Files.isRegularFile(animation), "missing animation for " + slug);

            JsonObject geometry = readJson(geo);
            assertEquals("1.12.0", geometry.get("format_version").getAsString(),
                    slug + " geometry must use the Bedrock 1.12.0 format");

            JsonObject description = geometry.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                    .getAsJsonObject("description");
            assertEquals("geometry.unknown", description.get("identifier").getAsString());
            assertTrue(description.get("texture_width").getAsInt() > 0, slug + " texture width");

            if (GLOWING.contains(slug)) {
                assertTrue(Files.isRegularFile(ASSETS.resolve("textures/entity/" + slug + "_glowmask.png")),
                        "missing glowmask for " + slug);
            }
        }
    }

    @Test
    void animationBonesExistInTheirGeometry() {
        for (String slug : SPECIES) {
            Set<String> bones = geometryBones(slug);
            for (Map.Entry<String, JsonObject> clip : animationClips(slug).entrySet()) {
                for (String bone : clip.getValue().getAsJsonObject("bones").keySet()) {
                    assertTrue(bones.contains(bone),
                            slug + "." + clip.getKey() + " animates unknown bone " + bone);
                }
            }
        }
    }

    @Test
    void everyAnimationShipsTheClipsTheEntitiesAskFor() {
        for (Map.Entry<String, List<String>> expected : EXPECTED_CLIPS.entrySet()) {
            Set<String> clips = animationClips(expected.getKey()).keySet();
            assertTrue(clips.containsAll(expected.getValue()),
                    expected.getKey() + " is missing clips " + expected.getValue() + " (has " + clips + ")");
        }
    }

    @Test
    void spawnEggsShipASpriteAndAModel() {
        for (String slug : SPECIES) {
            assertTrue(Files.isRegularFile(ASSETS.resolve("models/item/" + slug + "_spawn_egg.json")),
                    "missing spawn egg item model for " + slug);
            Path sprite = ASSETS.resolve("textures/item/" + slug + "_spawn_egg.png");
            assertTrue(Files.isRegularFile(sprite), "missing spawn egg sprite for " + slug);

            EggStats stats = eggStats(sprite);
            assertTrue(stats.opaque >= 120 && stats.opaque <= 148,
                    slug + " spawn egg has " + stats.opaque + " opaque pixels, expected the shipped 120..148 egg");
            assertTrue(stats.colours >= 45 && stats.colours <= 130,
                    slug + " spawn egg has " + stats.colours + " distinct colours, expected the shipped 45..130");
            assertTrue(stats.relativeSpread >= 0.10 && stats.relativeSpread <= 0.60,
                    slug + " spawn egg shading spread is " + stats.relativeSpread + ", expected 0.10..0.60");
        }
    }

    @Test
    void opticichthusShipsItsLanceSprites() {
        Path beam = ASSETS.resolve("textures/entity/opticichthus_beam.png");
        Path flare = ASSETS.resolve("textures/entity/opticichthus_flare.png");
        assertTrue(Files.isRegularFile(beam), "missing opticichthus beam texture");
        assertTrue(Files.isRegularFile(flare), "missing opticichthus flare texture");
        assertTrue(Files.isRegularFile(ASSETS.resolve("textures/entity/opticichthus_beam.png.mcmeta")),
                "the beam is scrolled along its length, so its texture must be allowed to tile");

        // Both sprites are deliberately tiny: nearest-filtered pixel art, not a smooth modern glow.
        PngImage beamImage = readPng(beam, 16, 64);
        PngImage flareImage = readPng(flare, 16, 32);

        // The ribbon maps u across its width, so the painted profile is the beam's cross-section. It
        // must be a hard stair-step of a handful of tones, mirrored about the centre line.
        Set<Integer> alphas = new LinkedHashSet<>();
        for (int y = 0; y < beamImage.height; y++) {
            for (int x = 0; x < beamImage.width; x++) {
                alphas.add(beamImage.pixel(x, y) & 0xFF);
                assertEquals(beamImage.pixel(x, y), beamImage.pixel(beamImage.width - 1 - x, y),
                        "beam cross-section must be symmetric at x=" + x + ", y=" + y);
            }
        }
        assertTrue(alphas.size() <= 7,
                "beam must be painted from a small palette, found " + alphas.size() + " alpha levels");
        assertTrue(alphas.containsAll(List.of(40, 96, 152, 208, 255)),
                "beam must be stepped through its whole tone ramp, found " + alphas);

        int edges = 0;
        for (int y = 0; y < beamImage.height; y++) {
            edges = Math.max(edges, Math.max(beamImage.pixel(0, y) & 0xFF,
                    beamImage.pixel(beamImage.width - 1, y) & 0xFF));
        }
        assertEquals(0, edges, "beam profile must fade to nothing at the ribbon edges");
        for (int y = 0; y < beamImage.height; y++) {
            for (int x = 5; x <= 10; x++) {
                assertEquals(255, beamImage.pixel(x, y) & 0xFF,
                        "beam core must be a solid block at x=" + x + ", y=" + y);
            }
        }

        // The flare is billboarded pixel art in two frames (a plus burst and a 45-degree burst): an
        // opaque blocky core, mirror symmetric on both axes, no ink on the sprite border, and the
        // frames must actually differ or the animation would be invisible.
        int previousInk = -1;
        for (int frame = 0; frame < 2; frame++) {
            int offset = frame * 16;
            for (int y = 6; y <= 9; y++) {
                for (int x = 6; x <= 9; x++) {
                    assertEquals(255, flareImage.pixel(x, y + offset) & 0xFF,
                            "flare frame " + frame + " core must be an opaque block at " + x + "," + y);
                }
            }
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    assertEquals(flareImage.pixel(x, y + offset), flareImage.pixel(15 - x, y + offset),
                            "flare frame " + frame + " must mirror horizontally at " + x + "," + y);
                    assertEquals(flareImage.pixel(x, y + offset), flareImage.pixel(x, 15 - y + offset),
                            "flare frame " + frame + " must mirror vertically at " + x + "," + y);
                }
            }
            for (int i = 0; i < 16; i++) {
                int border = Math.max(
                        Math.max(flareImage.pixel(i, offset) & 0xFF, flareImage.pixel(i, 15 + offset) & 0xFF),
                        Math.max(flareImage.pixel(0, i + offset) & 0xFF,
                                flareImage.pixel(15, i + offset) & 0xFF));
                assertEquals(0, border, "flare frame " + frame + " must not touch its sprite border");
            }

            int ink = 0;
            Set<Integer> tones = new LinkedHashSet<>();
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int pixel = flareImage.pixel(x, y + offset);
                    if ((pixel & 0xFF) > 0) {
                        ink++;
                        tones.add(pixel >>> 8);
                    }
                }
            }
            assertTrue(ink >= 40 && ink <= 160,
                    "flare frame " + frame + " must be a lean pixel star, found " + ink + " lit pixels");
            assertTrue(tones.size() >= 3,
                    "flare frame " + frame + " must use its palette, found " + tones.size() + " tones");
            if (frame == 1) {
                assertTrue(ink != previousInk, "the flare frames must differ, otherwise they do not animate");
            }
            previousInk = ink;
        }
    }

    @Test
    void notebookEntriesAreRegisteredAndTranslated() {
        JsonObject english = readJson(ASSETS.resolve("lang/en_us.json"));
        JsonObject chinese = readJson(ASSETS.resolve("lang/zh_cn.json"));

        for (String slug : SPECIES) {
            Path entry = DATA.resolve("notebook/waterlife/" + slug + ".json");
            assertTrue(Files.isRegularFile(entry), "missing notebook entry for " + slug);

            JsonObject notebook = readJson(entry);
            assertEquals("aquanaut:" + slug, notebook.get("species_id").getAsString());
            assertEquals("aquanaut:" + slug, notebook.get("entity_id").getAsString());

            String first = "gui.aquanaut.notebook.waterlife." + slug + ".paragraph_1";
            String second = "gui.aquanaut.notebook.waterlife." + slug + ".paragraph_2";
            for (String key : List.of(first, second)) {
                assertTrue(english.has(key), "en_us.json is missing " + key);
                assertTrue(chinese.has(key), "zh_cn.json is missing " + key);
            }
            assertTrue(english.has("entity.aquanaut." + slug), "missing entity name for " + slug);
            assertTrue(chinese.has("entity.aquanaut." + slug), "missing Chinese entity name for " + slug);
            assertTrue(english.has("item.aquanaut." + slug + "_spawn_egg"), "missing egg name for " + slug);
            assertTrue(chinese.has("item.aquanaut." + slug + "_spawn_egg"), "missing Chinese egg name for " + slug);

            List<String> paragraphKeys = new ArrayList<>();
            for (JsonElement block : notebook.getAsJsonArray("blocks")) {
                JsonObject object = block.getAsJsonObject();
                if (object.get("type").getAsString().equals("paragraph")) {
                    paragraphKeys.add(object.get("text").getAsString());
                }
            }
            assertTrue(paragraphKeys.containsAll(List.of(first, second)),
                    "notebook blocks for " + slug + " must reference both paragraphs");
        }
    }

    @Test
    void bothLanguageFilesCarryTheSameKeys() {
        JsonObject english = readJson(ASSETS.resolve("lang/en_us.json"));
        JsonObject chinese = readJson(ASSETS.resolve("lang/zh_cn.json"));

        Set<String> englishKeys = new LinkedHashSet<>(english.keySet());
        Set<String> chineseKeys = new LinkedHashSet<>(chinese.keySet());
        englishKeys.removeAll(chineseKeys);
        chineseKeys.removeAll(english.keySet());
        assertTrue(englishKeys.isEmpty(), "keys missing from zh_cn.json: " + englishKeys);
        assertTrue(chineseKeys.isEmpty(), "keys missing from en_us.json: " + chineseKeys);
    }

    @Test
    void dissectionTableAssetsAreComplete() {
        for (String suffix : List.of("", "_x2", "_x4")) {
            assertTrue(Files.isRegularFile(ASSETS.resolve("geo/dissection_table" + suffix + ".geo.json")),
                    "missing dissection_table" + suffix + " geometry");
            assertTrue(Files.isRegularFile(ASSETS.resolve("textures/block/dissection_table" + suffix + ".png")),
                    "missing dissection_table" + suffix + " texture");
        }

        assertTrue(Files.isRegularFile(ASSETS.resolve("blockstates/dissection_table.json")),
                "missing dissection table blockstate");
        assertTrue(Files.isRegularFile(ASSETS.resolve("models/block/dissection_table.json")),
                "missing dissection table block model");
        assertTrue(Files.isRegularFile(ASSETS.resolve("models/item/dissection_table.json")),
                "missing dissection table item model");
        assertTrue(Files.isRegularFile(ASSETS.resolve("textures/item/dissection_table.png")),
                "missing dissection table item icon");
        assertTrue(Files.isRegularFile(DATA.resolve("loot_table/blocks/dissection_table.json")),
                "missing dissection table loot table");
        assertTrue(Files.isRegularFile(DATA.resolve("recipes/dissection_table.json")),
                "missing dissection table recipe");

        JsonObject pickaxe = readJson(RESOURCES.resolve("data/minecraft/tags/block/mineable/pickaxe.json"));
        boolean tagged = false;
        for (JsonElement value : pickaxe.getAsJsonArray("values")) {
            if (value.getAsString().equals("aquanaut:dissection_table")) {
                tagged = true;
            }
        }
        assertTrue(tagged, "dissection table must be mineable with a pickaxe");
    }

    @Test
    void geometryUvsUseValidBedrockRectangles() {
        for (String slug : CUBE_UV_CENSUS.keySet()) {
            JsonObject description = readJson(ASSETS.resolve("geo/" + slug + ".geo.json"))
                    .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                    .getAsJsonObject("description");
            int textureWidth = description.get("texture_width").getAsInt();
            int textureHeight = description.get("texture_height").getAsInt();

            int box = 0;
            int perFace = 0;
            int mirrored = 0;
            for (JsonObject cube : cubes(slug)) {
                JsonElement uv = cube.get("uv");
                if (uv.isJsonArray()) {
                    box++;
                    assertEquals(2, uv.getAsJsonArray().size(),
                            slug + " cube-level UV must be a [u, v] pair");
                    if (cube.has("mirror")) {
                        assertTrue(cube.get("mirror").getAsBoolean(),
                                slug + " must not disable the mirror flag explicitly");
                        mirrored++;
                    }
                    continue;
                }

                // The exporter writes "mirror" only for the compact cube-level form; a per-face cube
                // carries the mirrored rectangles directly.
                assertTrue(!cube.has("mirror"),
                        slug + " cube " + cube.get("origin") + " must not mix per-face UVs with the"
                                + " geometry mirror flag");
                perFace++;
                JsonObject faces = uv.getAsJsonObject();
                assertEquals(Set.of("north", "east", "south", "west", "up", "down"), faces.keySet(),
                        slug + " cube " + cube.get("origin") + " must carry all six faces");
                for (Map.Entry<String, JsonElement> face : faces.entrySet()) {
                    JsonObject payload = face.getValue().getAsJsonObject();
                    int x = payload.getAsJsonArray("uv").get(0).getAsInt();
                    int y = payload.getAsJsonArray("uv").get(1).getAsInt();
                    int width = payload.getAsJsonArray("uv_size").get(0).getAsInt();
                    int height = payload.getAsJsonArray("uv_size").get(1).getAsInt();
                    int left = Math.min(x, x + width);
                    int top = Math.min(y, y + height);
                    int right = Math.max(x, x + width);
                    int bottom = Math.max(y, y + height);
                    assertTrue(left >= 0 && top >= 0 && right <= textureWidth && bottom <= textureHeight,
                            slug + " cube " + cube.get("origin") + " face " + face.getKey()
                                    + " samples " + right + "x" + bottom + " outside the declared "
                                    + textureWidth + "x" + textureHeight + " texture");
                    if (payload.has("uv_rotation")) {
                        int rotation = payload.get("uv_rotation").getAsInt();
                        assertTrue(rotation == 0 || rotation == 90 || rotation == 180 || rotation == 270,
                                slug + " has invalid uv_rotation " + rotation);
                    }
                }
            }

            assertEquals(CUBE_UV_CENSUS.get(slug), box + ":" + perFace + ":" + mirrored,
                    slug + " cube UV census drifted (box UV : per-face : mirrored)");
        }
    }

    // -- helpers ----------------------------------------------------------

    private static List<JsonObject> cubes(String slug) {
        JsonObject geometry = readJson(ASSETS.resolve("geo/" + slug + ".geo.json"))
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        List<JsonObject> cubes = new ArrayList<>();
        collectCubes(geometry.getAsJsonArray("bones"), cubes);
        return cubes;
    }

    private static void collectCubes(Iterable<JsonElement> bones, List<JsonObject> cubes) {
        for (JsonElement element : bones) {
            JsonObject bone = element.getAsJsonObject();
            if (bone.has("cubes")) {
                for (JsonElement cube : bone.getAsJsonArray("cubes")) {
                    cubes.add(cube.getAsJsonObject());
                }
            }
            if (bone.has("bones")) {
                collectCubes(bone.getAsJsonArray("bones"), cubes);
            }
        }
    }

    private static Set<String> geometryBones(String slug) {
        JsonObject geometry = readJson(ASSETS.resolve("geo/" + slug + ".geo.json"))
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        Set<String> bones = new LinkedHashSet<>();
        for (JsonElement bone : geometry.getAsJsonArray("bones")) {
            bones.add(bone.getAsJsonObject().get("name").getAsString());
        }
        return bones;
    }

    private static Map<String, JsonObject> animationClips(String slug) {
        JsonObject animations = readJson(ASSETS.resolve("animations/" + slug + ".animation.json"))
                .getAsJsonObject("animations");
        Map<String, JsonObject> clips = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> clip : animations.entrySet()) {
            clips.put(clip.getKey(), clip.getValue().getAsJsonObject());
        }
        return clips;
    }

    private static JsonObject readJson(Path path) {
        try {
            return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException error) {
            throw new AssertionError("failed to read " + path, error);
        }
    }

    private record EggStats(int opaque, int colours, double relativeSpread) {
    }

    /** Minimal PNG reader: enough to measure a 16x16 sprite without pulling in a client library. */
    private static EggStats eggStats(Path path) {
        try {
            byte[] data = Files.readAllBytes(path);
            assertTrue(data.length > 8 && (data[0] & 0xFF) == 0x89 && data[1] == 'P' && data[2] == 'N'
                    && data[3] == 'G', path + " is not a PNG");
            PngImage image = PngImage.read(data);
            assertEquals(16, image.width, path + " must be a 16x16 sprite");
            assertEquals(16, image.height, path + " must be a 16x16 sprite");

            Set<Integer> colours = new LinkedHashSet<>();
            List<Double> luminances = new ArrayList<>();
            for (int y = 0; y < image.height; y++) {
                for (int x = 0; x < image.width; x++) {
                    int alpha = image.pixel(x, y) & 0xFF;
                    if (alpha <= 16) {
                        continue;
                    }
                    int rgb = image.pixel(x, y) >>> 8;
                    colours.add(rgb);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    luminances.add(0.2126 * r + 0.7152 * g + 0.0722 * b);
                }
            }

            double mean = luminances.stream().mapToDouble(Double::doubleValue).average().orElse(0.0D);
            double variance = luminances.stream()
                    .mapToDouble(value -> (value - mean) * (value - mean))
                    .average().orElse(0.0D);
            double spread = mean <= 0.0D ? 0.0D : Math.sqrt(variance) / mean;
            return new EggStats(luminances.size(), colours.size(), spread);
        } catch (IOException error) {
            throw new AssertionError("failed to read " + path, error);
        }
    }

    private static PngImage readPng(Path path, int width, int height) {
        try {
            PngImage image = PngImage.read(Files.readAllBytes(path));
            assertEquals(width, image.width, path + " must be " + width + "px wide");
            assertEquals(height, image.height, path + " must be " + height + "px tall");
            return image;
        } catch (IOException error) {
            throw new AssertionError("failed to read " + path, error);
        }
    }

    /** Tiny RGBA PNG decoder for the non-interlaced 8-bit sprites this project ships. */
    private record PngImage(int width, int height, int[] pixels) {
        int pixel(int x, int y) {
            return this.pixels[y * this.width + x];
        }

        static PngImage read(byte[] data) {
            int offset = 8;
            int width = 0;
            int height = 0;
            byte[] idat = new byte[0];
            while (offset < data.length) {
                int length = readInt(data, offset);
                String type = new String(data, offset + 4, 4, StandardCharsets.US_ASCII);
                int payload = offset + 8;
                switch (type) {
                    case "IHDR" -> {
                        width = readInt(data, payload);
                        height = readInt(data, payload + 4);
                        int bitDepth = data[payload + 8] & 0xFF;
                        int colourType = data[payload + 9] & 0xFF;
                        int interlace = data[payload + 12] & 0xFF;
                        if (bitDepth != 8 || colourType != 6 || interlace != 0) {
                            throw new AssertionError("only 8-bit RGBA non-interlaced PNGs are supported");
                        }
                    }
                    case "IDAT" -> {
                        byte[] chunk = new byte[length];
                        System.arraycopy(data, payload, chunk, 0, length);
                        byte[] merged = new byte[idat.length + chunk.length];
                        System.arraycopy(idat, 0, merged, 0, idat.length);
                        System.arraycopy(chunk, 0, merged, idat.length, chunk.length);
                        idat = merged;
                    }
                    default -> {
                    }
                }
                offset = payload + length + 4;
            }

            byte[] raw = inflate(idat);
            int stride = width * 4;
            int[] pixels = new int[width * height];
            byte[] previous = new byte[stride];
            byte[] current = new byte[stride];
            int position = 0;
            for (int y = 0; y < height; y++) {
                int filter = raw[position++] & 0xFF;
                System.arraycopy(raw, position, current, 0, stride);
                position += stride;
                for (int i = 0; i < stride; i++) {
                    int left = i >= 4 ? current[i - 4] & 0xFF : 0;
                    int up = previous[i] & 0xFF;
                    int upLeft = i >= 4 ? previous[i - 4] & 0xFF : 0;
                    switch (filter) {
                        case 1 -> current[i] = (byte) (current[i] + left);
                        case 2 -> current[i] = (byte) (current[i] + up);
                        case 3 -> current[i] = (byte) (current[i] + ((left + up) >> 1));
                        case 4 -> {
                            int p = left + up - upLeft;
                            int pa = Math.abs(p - left);
                            int pb = Math.abs(p - up);
                            int pc = Math.abs(p - upLeft);
                            int predictor = pa <= pb && pa <= pc ? left : (pb <= pc ? up : upLeft);
                            current[i] = (byte) (current[i] + predictor);
                        }
                        default -> {
                        }
                    }
                }
                for (int x = 0; x < width; x++) {
                    int r = current[x * 4] & 0xFF;
                    int g = current[x * 4 + 1] & 0xFF;
                    int b = current[x * 4 + 2] & 0xFF;
                    int a = current[x * 4 + 3] & 0xFF;
                    pixels[y * width + x] = (r << 24) | (g << 16) | (b << 8) | a;
                }
                byte[] swap = previous;
                previous = current;
                current = swap;
            }
            return new PngImage(width, height, pixels);
        }

        private static int readInt(byte[] data, int offset) {
            return ((data[offset] & 0xFF) << 24) | ((data[offset + 1] & 0xFF) << 16)
                    | ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
        }

        private static byte[] inflate(byte[] data) {
            try {
                java.util.zip.Inflater inflater = new java.util.zip.Inflater();
                inflater.setInput(data);
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(data.length * 4);
                byte[] buffer = new byte[4096];
                while (!inflater.finished()) {
                    int read = inflater.inflate(buffer);
                    if (read == 0) {
                        break;
                    }
                    out.write(buffer, 0, read);
                }
                inflater.end();
                return out.toByteArray();
            } catch (java.util.zip.DataFormatException error) {
                throw new AssertionError("failed to inflate PNG data", error);
            }
        }
    }
}
