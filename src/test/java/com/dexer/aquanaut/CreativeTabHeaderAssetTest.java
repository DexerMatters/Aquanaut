package com.dexer.aquanaut;

import com.dexer.aquanaut.common.inventory.CreativeTabHeader;
import com.dexer.aquanaut.core.BiomeRegistry;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the wiring between the natural tab's headers, their banner images and their names.
 *
 * <p>
 * The tab declares its headers in code ({@code CreativeTabHeader.biome(...)} and
 * {@code CreativeTabHeader.of(...)}), and a header whose banner is missing or misspelled does not
 * crash: it draws a stretched nothing and a raw translation key. So this walks the headers the tab
 * actually declares, resolves each through the same factory the tab uses, and pins the things that
 * break silently -- that the tab names real biome constants, that the title is the biome's (or the
 * header's) translation key, that the key exists in both shipped languages, and that the banner
 * behind the header's texture is a real, opaque, full-row image of its own.
 */
public final class CreativeTabHeaderAssetTest {

    /** One full row of the picker grid: nine 18px slot cells. */
    private static final int BANNER_WIDTH = 162;
    private static final int BANNER_HEIGHT = 18;

    private static final Path ROOT = Path.of("src/main/resources");
    private static final Path ASSETS = ROOT.resolve("assets/aquanaut");
    private static final Path LANG = ASSETS.resolve("lang");
    private static final Path SOURCES = Path.of("src/main/java/com/dexer/aquanaut");

    private static final Path ITEM_REGISTRY = SOURCES.resolve("core/ItemRegistry.java");

    private static final Pattern BIOME_HEADER = Pattern
            .compile("CreativeTabHeader\\.biome\\(BiomeRegistry\\.([A-Z_]+)\\)");
    private static final Pattern NAMED_HEADER = Pattern.compile("CreativeTabHeader\\.of\\(\"([a-z_]+)\"\\)");

    @Test
    public void everyDeclaredHeaderHasAWholeRowBannerAndANameInBothLanguages() throws IOException {
        JsonObject en = readJson(LANG.resolve("en_us.json"));
        JsonObject zh = readJson(LANG.resolve("zh_cn.json"));
        Set<Long> seenBanners = new HashSet<>();

        for (CreativeTabHeader header : declaredHeaders()) {
            checkHeader(header, en, zh, seenBanners);
        }
    }

    private static void checkHeader(CreativeTabHeader header, JsonObject en, JsonObject zh, Set<Long> seenBanners)
            throws IOException {
        String titleKey = header.title().getString();
        assertTrue(en.has(titleKey), "en_us.json is missing the header title " + titleKey);
        assertTrue(zh.has(titleKey), "zh_cn.json is missing the header title " + titleKey);

        Path banner = ASSETS.resolve(header.background().getPath());
        assertTrue(Files.isRegularFile(banner), "the header banner is missing: " + banner);
        BufferedImage image = ImageIO.read(banner.toFile());
        assertEquals(BANNER_WIDTH, image.getWidth(), "a banner is one whole grid row wide: " + banner);
        assertEquals(BANNER_HEIGHT, image.getHeight(), "a banner is one grid row tall: " + banner);

        long checksum = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                assertEquals(255, pixel >>> 24, "a banner is drawn over slot cells and must be opaque: " + banner);
                checksum = checksum * 31 + pixel;
            }
        }
        assertTrue(seenBanners.add(checksum), "each header should have its own banner: " + banner);
    }

    /**
     * Resolves every header the tab declares through the factory the tab itself uses, so a rename
     * that leaves the tab and the assets out of sync fails here instead of in the inventory.
     */
    private static List<CreativeTabHeader> declaredHeaders() throws IOException {
        String tabSource = Files.readString(ITEM_REGISTRY, StandardCharsets.UTF_8);
        List<CreativeTabHeader> headers = new ArrayList<>();

        Matcher biomes = BIOME_HEADER.matcher(tabSource);
        while (biomes.find()) {
            String constant = biomes.group(1);

            ResourceKey<Biome> biome;
            try {
                @SuppressWarnings("unchecked")
                ResourceKey<Biome> reflected = (ResourceKey<Biome>) BiomeRegistry.class.getField(constant).get(null);
                biome = reflected;
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("the tab names BiomeRegistry." + constant
                        + " but BiomeRegistry declares no such biome", e);
            }

            CreativeTabHeader header = CreativeTabHeader.biome(biome);
            String name = biome.location().getPath();
            assertEquals("biome." + biome.location().getNamespace() + "." + name, header.title().getString(),
                    "a biome header is titled by the biome's translation key");
            assertEquals("textures/gui/creative_tab/" + name + ".png", header.background().getPath(),
                    "a biome header takes the banner named after the biome");
            headers.add(header);
        }

        Matcher named = NAMED_HEADER.matcher(tabSource);
        while (named.find()) {
            String name = named.group(1);
            CreativeTabHeader header = CreativeTabHeader.of(name);
            assertEquals("gui.aquanaut.creative_header." + name, header.title().getString(),
                    "a named header is titled by its translation key");
            assertEquals("textures/gui/creative_tab/" + name + ".png", header.background().getPath(),
                    "a named header takes the banner named after it");
            headers.add(header);
        }

        assertFalse(headers.isEmpty(), "no headers found in ItemRegistry; has the declaration syntax changed?");
        return headers;
    }

    private static JsonObject readJson(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
