package com.dexer.aquanaut;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the wiring between the worn diving equipment art and the renderer.
 *
 * <p>
 * The {@code textures/equipment/*_on_body.png} files are UV-unwrapped box faces:
 * the game wraps them around cuboids on the player's head, back, and feet. The
 * UV regions therefore have to match the box geometry in
 * {@code DivingEquipmentRenderLayer}, and that table is generated from the
 * renderer source itself so the two cannot drift apart silently.
 */
public final class DivingEquipmentWornTextureTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/aquanaut");
    private static final Path EQUIPMENT_TEXTURES = ASSETS.resolve("textures/equipment");
    private static final Path RENDERER =
            Path.of("src/main/java/com/dexer/aquanaut/client/renderer/DivingEquipmentRenderLayer.java");
    private static final Path ITEM_REGISTRY =
            Path.of("src/main/java/com/dexer/aquanaut/core/ItemRegistry.java");

    /** Side length of every on-body texture, in texels. */
    private static final int ATLAS = 64;
    /** Texels per model unit in the on-body textures. */
    private static final int TEXELS_PER_UNIT = 1;

    /** One cuboid of a worn piece, as declared in the renderer. */
    private record BoxDef(String name, float x0, float y0, float z0,
            float w, float h, float d, int u, int v, boolean mirrorX) {
    }

    /** Face rectangle in atlas texels: u, v, width, height. */
    private record Rect(int u, int v, int w, int h) {
        boolean contains(int x, int y) {
            return x >= u && x < u + w && y >= v && y < v + h;
        }
    }

    @Test
    public void everyRegisteredDivingItemHasWornArt() throws IOException {
        Map<String, String> registered = registeredDivingItems();
        assertTrue(registered.size() >= 19, "expected the diving gear set, found " + registered);
        for (Map.Entry<String, String> entry : registered.entrySet()) {
            Path texture = EQUIPMENT_TEXTURES.resolve(entry.getKey() + "_on_body.png");
            assertTrue(Files.isRegularFile(texture),
                    entry.getValue() + " item " + entry.getKey() + " has no worn texture at " + texture);
        }
    }

    @Test
    public void rendererUvRegionsFitTheAtlasWithoutOverlap() throws IOException {
        for (String category : List.of("mask", "tank", "flipper")) {
            List<BoxDef> boxes = rendererBoxes(category);
            assertTrue(!boxes.isEmpty(), "no " + category + " boxes found in " + RENDERER);
            List<Rect> placed = new ArrayList<>();
            for (BoxDef box : boxes) {
                for (Rect rect : faceRects(box)) {
                    assertTrue(rect.u() >= 0 && rect.v() >= 0
                                    && rect.u() + rect.w() <= ATLAS && rect.v() + rect.h() <= ATLAS,
                            category + "/" + box.name() + " region " + rect + " leaves the atlas");
                    for (Rect other : placed) {
                        boolean overlap = rect.u() < other.u() + other.w() && other.u() < rect.u() + rect.w()
                                && rect.v() < other.v() + other.h() && other.v() < rect.v() + rect.h();
                        assertTrue(!overlap, category + "/" + box.name() + " region " + rect
                                + " overlaps " + other);
                    }
                    placed.add(rect);
                }
            }
        }
    }

    @Test
    public void wornTexturesFillExactlyTheFacesTheRendererSamples() throws IOException {
        Map<String, String> registered = registeredDivingItems();
        for (Map.Entry<String, String> entry : registered.entrySet()) {
            String item = entry.getKey();
            String category = entry.getValue();
            BufferedImage image = ImageIO.read(EQUIPMENT_TEXTURES.resolve(item + "_on_body.png").toFile());
            assertNotNull(image, item + " worn texture could not be read");
            assertEquals(ATLAS, image.getWidth(), item + " worn texture width");
            assertEquals(ATLAS, image.getHeight(), item + " worn texture height");

            List<Rect> faces = new ArrayList<>();
            List<String> faceKeys = new ArrayList<>();
            for (BoxDef box : rendererBoxes(category)) {
                faces.addAll(faceRects(box));
                faceKeys.addAll(List.of(box.name() + "/top", box.name() + "/bottom",
                        box.name() + "/-x", box.name() + "/+z", box.name() + "/+x", box.name() + "/-z"));
            }
            int[] painted = new int[faces.size()];
            for (int y = 0; y < ATLAS; y++) {
                for (int x = 0; x < ATLAS; x++) {
                    int faceIndex = -1;
                    for (int i = 0; i < faces.size(); i++) {
                        if (faces.get(i).contains(x, y)) {
                            faceIndex = i;
                            break;
                        }
                    }
                    int alpha = image.getRGB(x, y) >>> 24;
                    if (faceIndex >= 0) {
                        // translucent glass and see-through windows are allowed,
                        // but a face the renderer samples must not be blank
                        if (alpha > 0) {
                            painted[faceIndex]++;
                        }
                    } else {
                        assertEquals(0, alpha, item + " paints outside the sampled faces at " + x + "," + y);
                    }
                }
            }
            for (int i = 0; i < faces.size(); i++) {
                // the helmet visor's back face is intentionally open so the
                // player's face shows through the glass
                if (faceKeys.get(i).endsWith("VISOR/-z")) {
                    continue;
                }
                assertTrue(painted[i] > 0, item + " leaves face " + faceKeys.get(i) + " blank");
            }
        }
    }

    /**
     * The face unwrap of one box, laid out the way Minecraft unwraps boxes for
     * {@code CubeListBuilder.texOffs}: top and bottom next to each other, then
     * -x, +z, +x, -z in a row below.
     */
    private static List<Rect> faceRects(BoxDef box) {
        int dw = texels(box.d());
        int hw = texels(box.w());
        int hh = texels(box.h());
        return List.of(
                new Rect(box.u() + dw, box.v(), hw, dw),
                new Rect(box.u() + dw + hw, box.v(), hw, dw),
                new Rect(box.u(), box.v() + dw, dw, hh),
                new Rect(box.u() + dw, box.v() + dw, hw, hh),
                new Rect(box.u() + dw + hw, box.v() + dw, dw, hh),
                new Rect(box.u() + dw + hw + dw, box.v() + dw, hw, hh));
    }

    private static int texels(float units) {
        int value = Math.round(units * TEXELS_PER_UNIT);
        assertEquals(units * TEXELS_PER_UNIT, value, 1.0E-3,
                "box size " + units + " does not land on whole texels");
        return value;
    }

    /** Reads the box tables out of the renderer source, so art and code cannot drift. */
    private static List<BoxDef> rendererBoxes(String category) throws IOException {
        String source = Files.readString(RENDERER).replaceAll("\\s+", " ");
        Matcher matcher = Pattern.compile("(\\w+)\\s*=\\s*new Box\\(([^)]*)\\)").matcher(source);
        List<BoxDef> boxes = new ArrayList<>();
        while (matcher.find()) {
            String name = matcher.group(1);
            if (!name.toUpperCase().startsWith(category.toUpperCase() + "_")) {
                continue;
            }
            String[] args = matcher.group(2).split(",");
            assertEquals(9, args.length, "box " + name + " should have 9 arguments");
            boxes.add(new BoxDef(name,
                    number(args[0]), number(args[1]), number(args[2]),
                    number(args[3]), number(args[4]), number(args[5]),
                    (int) number(args[6]), (int) number(args[7]),
                    Boolean.parseBoolean(args[8].trim())));
        }
        return boxes;
    }

    private static float number(String arg) {
        return Float.parseFloat(arg.trim().replace("F", ""));
    }

    /** The diving equipment items registered in {@code ItemRegistry}, mapped to their piece. */
    private static Map<String, String> registeredDivingItems() throws IOException {
        String source = Files.readString(ITEM_REGISTRY);
        Matcher matcher = Pattern.compile("(\\w+)Item\\(\\s*\"([a-z_]+)\"").matcher(source);
        Map<String, String> registered = new LinkedHashMap<>();
        while (matcher.find()) {
            String piece = switch (matcher.group(1)) {
                case "mask" -> "mask";
                case "tank" -> "tank";
                case "flippers" -> "flipper";
                default -> null;
            };
            if (piece != null) {
                registered.put(matcher.group(2), piece);
            }
        }
        return registered;
    }
}
