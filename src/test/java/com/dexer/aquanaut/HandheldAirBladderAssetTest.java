package com.dexer.aquanaut;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the wiring between the air bladders' art and their code.
 *
 * <p>
 * The small bladder's art is authored by hand and the large one is derived from
 * it, so nothing here pins geometry, element counts or image sizes: the checks
 * are the things that break silently when an asset is renamed, re-exported to a
 * different sheet or dropped, plus the renderer and lang wiring both items need.
 */
public final class HandheldAirBladderAssetTest {

    /** Each bladder item, and the tooltip key its class adds. */
    private static final List<ItemAssets> ITEMS = List.of(
            new ItemAssets("handheld_air_bladder", "tooltip.aquanaut.handheld_air_bladder.floats"),
            new ItemAssets("large_handheld_air_bladder", "tooltip.aquanaut.large_handheld_air_bladder.ascends"));

    private static final Path ASSETS = Path.of("src/main/resources/assets/aquanaut");
    private static final Path ITEM_MODELS = ASSETS.resolve("models/item");
    private static final Path ITEM_TEXTURES = ASSETS.resolve("textures/item");

    private record ItemAssets(String id, String tooltipKey) {
    }

    /** Vanilla clamps element coordinates to these extents when loading a model. */
    private static final float MIN_EXTENT = -16.0F;
    private static final float MAX_EXTENT = 32.0F;

    @Test
    public void heldModelsSampleTheirTextureInsideTheUvSpace() throws IOException {
        for (ItemAssets item : ITEMS) {
            assertHeldModel(item.id());
        }
    }

    private static void assertHeldModel(String item) throws IOException {
        JsonObject model = readJson(ITEM_MODELS.resolve(item + "_held.json"));
        String texture = model.getAsJsonObject("textures").get("bladder").getAsString();
        assertTrue(Files.isRegularFile(textureFile(texture)),
                item + " samples a texture that does not exist: " + texture);

        JsonArray elements = model.getAsJsonArray("elements");
        assertTrue(elements.size() > 1,
                "the bladder is a modelled object, not one cube: found " + elements.size() + " elements");

        for (JsonElement entry : elements) {
            JsonObject element = entry.getAsJsonObject();
            JsonArray from = element.getAsJsonArray("from");
            JsonArray to = element.getAsJsonArray("to");
            for (int axis = 0; axis < 3; axis++) {
                float low = from.get(axis).getAsFloat();
                float high = to.get(axis).getAsFloat();
                assertTrue(low < high, "degenerate element on axis " + axis + ": " + element);
                assertTrue(low >= MIN_EXTENT && high <= MAX_EXTENT,
                        "element leaves the legal model extent: " + element);
            }

            JsonObject faces = element.getAsJsonObject("faces");
            assertTrue(faces.size() >= 5, "element without a full face set: " + element);
            for (String direction : faces.keySet()) {
                JsonObject face = faces.getAsJsonObject(direction);
                assertEquals("#bladder", face.get("texture").getAsString(),
                        direction + " face must sample the model texture");
                JsonArray uv = face.getAsJsonArray("uv");
                assertEquals(4, uv.size(), direction + " face needs four uv values");
                for (JsonElement value : uv) {
                    float coordinate = value.getAsFloat();
                    assertTrue(coordinate >= 0.0F && coordinate <= 16.0F,
                            direction + " face samples outside the texture: " + uv);
                }
                // mirrored rects (u0 > u1) are legal and are how faces are
                // flipped, so only an empty rect is an error
                assertTrue(uv.get(0).getAsFloat() != uv.get(2).getAsFloat()
                                && uv.get(1).getAsFloat() != uv.get(3).getAsFloat(),
                        direction + " face collapses to a line: " + uv);
            }
        }
    }

    @Test
    public void guiModelsUseTheirSprite() throws IOException {
        for (ItemAssets item : ITEMS) {
            JsonObject gui = readJson(ITEM_MODELS.resolve(item.id() + "_gui.json"));
            assertEquals("minecraft:item/generated", gui.get("parent").getAsString());
            assertEquals("aquanaut:item/" + item.id(),
                    gui.getAsJsonObject("textures").get("layer0").getAsString());
        }
    }

    @Test
    public void mainModelsDelegateToTheItemRenderer() throws IOException {
        for (ItemAssets item : ITEMS) {
            JsonObject model = readJson(ITEM_MODELS.resolve(item.id() + ".json"));
            assertEquals("minecraft:builtin/entity", model.get("parent").getAsString(),
                    "a flat sprite for the slot and elements in the hand need a custom renderer: " + item.id());

            JsonObject display = model.getAsJsonObject("display");
            for (String context : new String[] {
                    "firstperson_righthand", "firstperson_lefthand",
                    "thirdperson_righthand", "thirdperson_lefthand", "ground", "fixed" }) {
                assertNotNull(display.get(context), "missing display transform on " + item.id() + ": " + context);
                JsonObject transform = display.getAsJsonObject(context);
                assertEquals(3, transform.getAsJsonArray("rotation").size(), context);
                assertEquals(3, transform.getAsJsonArray("translation").size(), context);
                assertEquals(3, transform.getAsJsonArray("scale").size(), context);
            }
        }
    }

    @Test
    public void texturesArePresentAndDrawn() throws IOException {
        BufferedImage sprite = ImageIO.read(ITEM_TEXTURES.resolve(ITEMS.get(0).id() + ".png").toFile());
        assertNotNull(sprite, "missing inventory sprite");
        assertEquals(16, sprite.getWidth());
        assertEquals(16, sprite.getHeight());

        int opaque = 0;
        for (int y = 0; y < sprite.getHeight(); y++) {
            for (int x = 0; x < sprite.getWidth(); x++) {
                if (((sprite.getRGB(x, y) >>> 24) & 0xFF) > 128) {
                    opaque++;
                }
            }
        }
        assertTrue(opaque >= 32 && opaque <= 250,
                "the sprite should be a drawn object, not an empty or flooded slot: " + opaque);

        BufferedImage atlas = ImageIO.read(textureFile("aquanaut:item/handheld_air_bladder_model").toFile());
        assertNotNull(atlas, "missing model texture");
        assertTrue(atlas.getWidth() > sprite.getWidth() && atlas.getHeight() > sprite.getHeight(),
                "the model texture should be a sheet the element uv's are mapped into, found "
                        + atlas.getWidth() + "x" + atlas.getHeight());
    }

    @Test
    public void itemsAndTheirFlotationAreNamedInBothLocales() throws IOException {
        for (String locale : new String[] { "en_us.json", "zh_cn.json" }) {
            JsonObject lang = readJson(ASSETS.resolve("lang").resolve(locale));
            for (ItemAssets item : ITEMS) {
                for (String key : new String[] { "item.aquanaut." + item.id(), item.tooltipKey() }) {
                    assertTrue(lang.has(key), "missing " + key + " in " + locale);
                    assertTrue(!lang.get(key).getAsString().isBlank(), "blank " + key + " in " + locale);
                }
            }
        }
    }

    /** {@code aquanaut:item/foo} -> {@code assets/aquanaut/textures/item/foo.png}. */
    private static Path textureFile(String reference) {
        String[] parts = reference.split(":", 2);
        return ASSETS.getParent().resolve(parts[0]).resolve("textures").resolve(parts[1] + ".png");
    }

    private static JsonObject readJson(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), "missing asset: " + path);
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
