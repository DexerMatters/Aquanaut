package com.dexer.aquanaut;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the wiring every mob effect depends on and none of it reports: an effect registered without
 * an icon draws as a missing-texture square, and one without a language entry prints its raw
 * translation key in the HUD. Neither shows up in a compile, so they are pinned here instead.
 *
 * <p>
 * The icons are also held to the house style the narcosis icon set: an 18x18 sprite with a
 * transparent border and the mod's dark navy outline. The drawing itself is free to change -- what
 * is pinned is that a painted, outlined sprite exists at the name the registry uses.
 */
public final class MobEffectAssetTest {

    /** Vanilla renders effect icons from an 18x18 sprite, one pixel of bleed around the art. */
    private static final int ICON_SIZE = 18;

    /** The outline colour shared by the mod's effect icons. */
    private static final int OUTLINE = 0xFF112A3D;

    private static final Set<String> AQUATIC_EFFECTS = Set.of("charged", "narcosis", "pellucid");

    private static final Path ASSETS = Path.of("src/main/resources/assets/aquanaut");
    private static final Path ICONS = ASSETS.resolve("textures/mob_effect");
    private static final Path LANG = ASSETS.resolve("lang");
    private static final Path REGISTRY = Path.of("src/main/java/com/dexer/aquanaut/core/MobEffectRegistry.java");
    private static final Path PELLUCID_SOURCE = Path
            .of("src/main/java/com/dexer/aquanaut/common/effect/PellucidMobEffect.java");

    private static final Pattern REGISTERED_EFFECT = Pattern.compile("MOB_EFFECTS\\.register\\(\"([a-z0-9_]+)\"");

    @Test
    public void everyRegisteredEffectHasAnIconAndANameInBothLanguages() throws IOException {
        Set<String> registered = registeredEffectNames();

        assertTrue(registered.containsAll(AQUATIC_EFFECTS),
                "the three-tier aquatic effects must stay registered: " + AQUATIC_EFFECTS);
        assertTrue(registered.size() >= AQUATIC_EFFECTS.size(), "no effect may drop out of the registry");

        for (String locale : new String[] { "en_us.json", "zh_cn.json" }) {
            JsonObject lang = readJson(LANG.resolve(locale));

            for (String effect : registered) {
                String key = "effect.aquanaut." + effect;
                assertTrue(lang.has(key), "missing " + key + " in " + locale);
                assertTrue(!lang.get(key).getAsString().isBlank(), "blank " + key + " in " + locale);
            }
        }
    }

    @Test
    public void everyEffectIconIsAnOutlinedSpriteAtTheVanillaSize() throws IOException {
        for (String effect : registeredEffectNames()) {
            Path path = ICONS.resolve(effect + ".png");
            assertTrue(Files.isRegularFile(path), "missing effect icon: " + path);

            BufferedImage icon = ImageIO.read(path.toFile());
            assertNotNull(icon, "unreadable effect icon: " + path);
            assertEquals(ICON_SIZE, icon.getWidth(), effect + " icon width");
            assertEquals(ICON_SIZE, icon.getHeight(), effect + " icon height");

            int painted = 0;
            boolean outlined = false;

            for (int y = 0; y < ICON_SIZE; y++) {
                for (int x = 0; x < ICON_SIZE; x++) {
                    int pixel = icon.getRGB(x, y);
                    if ((pixel >>> 24) != 0) {
                        painted++;
                    }
                    if (pixel == OUTLINE) {
                        outlined = true;
                    }
                }
            }

            assertTrue(painted > 0, effect + " icon is blank");
            assertTrue(painted < ICON_SIZE * ICON_SIZE, effect + " icon fills the whole sprite, leaving no border");
            assertTrue(outlined, effect + " icon is missing the mod's dark outline");
        }
    }

    /**
     * Pellucid is meant to be felt, not listed: it carries haste on its own attribute modifiers
     * rather than granting vanilla effects, so the HUD shows one icon and a player who already has
     * haste or night vision keeps the instance -- and the icon -- they had.
     */
    @Test
    public void pellucidCarriesItsOwnHasteInsteadOfGrantingVanillaEffects() throws IOException {
        assertTrue(Files.isRegularFile(PELLUCID_SOURCE), "missing effect class: " + PELLUCID_SOURCE);
        String source = Files.readString(PELLUCID_SOURCE, StandardCharsets.UTF_8);

        assertFalse(source.contains("MobEffectInstance"),
                "pellucid must not hand the player a second effect to carry");
        assertTrue(source.contains("addAttributeModifier"),
                "pellucid's haste has to come from its own attribute modifiers");
    }

    /** Names as the registry declares them, read from source: the registry cannot boot in a unit test. */
    private static Set<String> registeredEffectNames() throws IOException {        assertTrue(Files.isRegularFile(REGISTRY), "missing registry: " + REGISTRY);

        Set<String> names = new LinkedHashSet<>();
        Matcher matcher = REGISTERED_EFFECT.matcher(Files.readString(REGISTRY, StandardCharsets.UTF_8));

        while (matcher.find()) {
            names.add(matcher.group(1));
        }

        return names;
    }

    private static JsonObject readJson(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), "missing asset: " + path);
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
