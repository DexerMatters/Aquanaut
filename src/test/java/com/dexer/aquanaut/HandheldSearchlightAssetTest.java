package com.dexer.aquanaut;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the wiring between the searchlight's art, its two states and the custom renderer.
 *
 * <p>
 * Nothing here pins the drawing itself -- the lamp may be repainted freely. What it pins are the
 * things that break silently: a sprite renamed out from under its model, the state models drifting
 * apart, the display transforms that seat the lamp in the hand, and the handle moving off the
 * pivot the item renderer rotates around. That last point is the one the whole held model exists
 * for: the display transforms pivot on model space {@code [8, 8, 8]}, so the grip has to be centred
 * there or the lamp will swing around the wrong point in the fist.
 */
public final class HandheldSearchlightAssetTest {

    private static final String ITEM = "handheld_searchlight";
    private static final String SPRITE = "aquanaut:item/" + ITEM;
    private static final String BURNING_SPRITE = SPRITE + "_on";
    private static final String MODEL_ATLAS = "aquanaut:item/" + ITEM + "_model";
    private static final String BURNING_MODEL_ATLAS = MODEL_ATLAS + "_on";

    private static final Path ASSETS = Path.of("src/main/resources/assets/aquanaut");
    private static final Path ITEM_MODELS = ASSETS.resolve("models/item");
    private static final Path ITEM_TEXTURES = ASSETS.resolve("textures/item");

    @Test
    public void bothSpritesAreDrawnAndFullyOpaque() throws IOException {
        BufferedImage off = readImage(ITEM + ".png");
        BufferedImage on = readImage(ITEM + "_on.png");

        assertTrue(differingPixels(off, on) > 8, "the burning sprite has to actually show a lit lamp");

        for (BufferedImage sprite : new BufferedImage[] { off, on }) {
            assertEquals(16, sprite.getWidth(), "an inventory sprite is 16 pixels wide");
            assertEquals(16, sprite.getHeight(), "an inventory sprite is 16 pixels tall");

            int opaque = 0;

            for (int y = 0; y < sprite.getHeight(); y++) {
                for (int x = 0; x < sprite.getWidth(); x++) {
                    int alpha = (sprite.getRGB(x, y) >>> 24) & 0xFF;

                    assertTrue(alpha == 0 || alpha == 255,
                            "the sprite carries a translucent pixel at " + x + "," + y
                                    + "; the lamp is drawn without a drop shadow");
                    if (alpha > 128) {
                        opaque++;
                    }
                }
            }

            assertTrue(opaque >= 32 && opaque <= 250,
                    "the sprite should be a drawn object, not an empty or flooded slot: " + opaque);
        }
    }

    @Test
    public void bothModelAtlasesExist() throws IOException {
        BufferedImage off = readImage(ITEM + "_model.png");
        BufferedImage on = readImage(ITEM + "_model_on.png");

        assertEquals(off.getWidth(), on.getWidth(), "the two states share one atlas layout");
        assertEquals(off.getHeight(), on.getHeight(), "the two states share one atlas layout");
        assertTrue(differingPixels(off, on) > 8, "the burning atlas has to actually show a lit lens");
    }

    @Test
    public void theItemModelIsTheCustomRendererBridge() throws IOException {
        JsonObject model = readJson(ITEM_MODELS.resolve(ITEM + ".json"));

        assertEquals("minecraft:builtin/entity", model.get("parent").getAsString(),
                "the item model delegates to HandheldSearchlightItemRenderer");
        assertEquals(SPRITE, model.getAsJsonObject("textures").get("particle").getAsString(),
                "the particle texture is the inventory sprite");
        assertFalse(model.has("overrides"),
                "the state swap lives in the renderer; an override here would be dead weight");
    }

    @Test
    public void theDisplayTransformsSeatTheLampInTheHand() throws IOException {
        JsonObject display = readJson(ITEM_MODELS.resolve(ITEM + ".json")).getAsJsonObject("display");
        assertNotNull(display, "the builtin/entity bridge must carry the display transforms");

        for (String hand : new String[] { "thirdperson_righthand", "thirdperson_lefthand" }) {
            JsonArray rotation = display.getAsJsonObject(hand).getAsJsonArray("rotation");

            assertEquals(90.0F, rotation.get(0).getAsFloat(), 1.0E-3F,
                    hand + " must stand the lamp upright in the fist");
            assertEquals(0.0F, rotation.get(2).getAsFloat(), 1.0E-3F,
                    hand + " must keep the lens along the facing direction; a Z twist hangs the lamp"
                            + " head-down, because the entity renderer already flips its Y axis");
        }

        for (String hand : new String[] { "firstperson_righthand", "firstperson_lefthand" }) {
            assertTrue(display.has(hand), "missing " + hand);
            float tilt = display.getAsJsonObject(hand).getAsJsonArray("rotation").get(2).getAsFloat();

            assertTrue(Math.abs(tilt) > 0.0F, hand + " should tilt the lamp away from the crosshair");
        }

        assertTrue(display.getAsJsonObject("ground").getAsJsonArray("translation").get(1).getAsFloat() > 0.0F,
                "the ground pose lifts the flange to the floor");
        assertEquals(180.0F,
                Math.abs(display.getAsJsonObject("fixed").getAsJsonArray("rotation").get(1).getAsFloat()),
                1.0E-3F, "the item frame shows the lens");
    }

    @Test
    public void theHeldModelsShareOneHandleAndDifferOnlyInTheLens() throws IOException {
        JsonObject off = readJson(ITEM_MODELS.resolve(ITEM + "_held.json"));
        JsonObject on = readJson(ITEM_MODELS.resolve(ITEM + "_held_on.json"));

        assertEquals(MODEL_ATLAS, off.getAsJsonObject("textures").get("body").getAsString());
        assertEquals(BURNING_MODEL_ATLAS, on.getAsJsonObject("textures").get("body").getAsString());
        assertEquals(off.getAsJsonArray("elements"), on.getAsJsonArray("elements"),
                "the lamp state must never move the geometry");
        assertFalse(off.has("display"),
                "the display transforms belong to the item model; a copy here would apply twice");
        assertEquals(14, off.getAsJsonArray("elements").size());
    }

    @Test
    public void theGripIsCentredOnTheDisplayPivot() throws IOException {
        JsonArray elements = readJson(ITEM_MODELS.resolve(ITEM + "_held.json")).getAsJsonArray("elements");

        JsonObject grip = null;

        for (int index = 0; index < elements.size(); index++) {
            JsonObject element = elements.get(index).getAsJsonObject();
            JsonArray from = element.getAsJsonArray("from");
            JsonArray to = element.getAsJsonArray("to");

            if (from.get(0).getAsInt() == 6 && from.get(1).getAsInt() == 6 && from.get(2).getAsInt() == 7
                    && to.get(0).getAsInt() == 10 && to.get(1).getAsInt() == 10 && to.get(2).getAsInt() == 9) {
                grip = element;
            }
        }

        assertNotNull(grip, "the rubber grip element has moved; the hand hold is no longer authored");
        JsonArray from = grip.getAsJsonArray("from");
        JsonArray to = grip.getAsJsonArray("to");

        for (int axis = 0; axis < 3; axis++) {
            assertEquals(8.0F, (from.get(axis).getAsFloat() + to.get(axis).getAsFloat()) / 2.0F, 1.0E-3F,
                    "the grip centre has to sit on the [8, 8, 8] display pivot on every axis");
        }

        int[] low = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE };
        int[] high = { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE };

        for (int index = 0; index < elements.size(); index++) {
            JsonObject element = elements.get(index).getAsJsonObject();

            for (int axis = 0; axis < 3; axis++) {
                low[axis] = Math.min(low[axis], element.getAsJsonArray("from").get(axis).getAsInt());
                high[axis] = Math.max(high[axis], element.getAsJsonArray("to").get(axis).getAsInt());
            }
        }

        assertArrayEquals(new int[] { 4, 5, -5 }, low, "the lamp's authored bounds have drifted");
        assertArrayEquals(new int[] { 12, 18, 11 }, high, "the lamp's authored bounds have drifted");
    }

    @Test
    public void theGuiModelsShowTheStateSprites() throws IOException {
        JsonObject off = readJson(ITEM_MODELS.resolve(ITEM + "_gui.json"));
        JsonObject on = readJson(ITEM_MODELS.resolve(ITEM + "_gui_on.json"));

        assertEquals("minecraft:item/generated", off.get("parent").getAsString());
        assertEquals("minecraft:item/generated", on.get("parent").getAsString());
        assertEquals(SPRITE, off.getAsJsonObject("textures").get("layer0").getAsString());
        assertEquals(BURNING_SPRITE, on.getAsJsonObject("textures").get("layer0").getAsString());
    }

    @Test
    public void theItemAndItsTooltipsAreNamedInBothLocales() throws IOException {
        for (String locale : new String[] { "en_us.json", "zh_cn.json" }) {
            JsonObject lang = readJson(ASSETS.resolve("lang").resolve(locale));

            for (String key : new String[] {
                    "item.aquanaut." + ITEM,
                    "tooltip.aquanaut." + ITEM + ".on",
                    "tooltip.aquanaut." + ITEM + ".off",
                    "tooltip.aquanaut." + ITEM + ".hint",
                    "tooltip.aquanaut." + ITEM + ".beam" }) {
                assertTrue(lang.has(key), "missing " + key + " in " + locale);
                assertTrue(!lang.get(key).getAsString().isBlank(), "blank " + key + " in " + locale);
            }
        }
    }

    private static BufferedImage readImage(String name) throws IOException {
        Path path = ITEM_TEXTURES.resolve(name);
        assertTrue(Files.isRegularFile(path), "missing texture: " + path);

        BufferedImage image = ImageIO.read(path.toFile());
        assertNotNull(image, "unreadable texture: " + path);

        return image;
    }

    private static int differingPixels(BufferedImage first, BufferedImage second) {
        if (first.getWidth() != second.getWidth() || first.getHeight() != second.getHeight()) {
            return Integer.MAX_VALUE;
        }

        int differing = 0;

        for (int y = 0; y < first.getHeight(); y++) {
            for (int x = 0; x < first.getWidth(); x++) {
                if (first.getRGB(x, y) != second.getRGB(x, y)) {
                    differing++;
                }
            }
        }

        return differing;
    }

    private static JsonObject readJson(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), "missing asset: " + path);
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
