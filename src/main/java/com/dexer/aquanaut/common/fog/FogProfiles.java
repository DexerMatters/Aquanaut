package com.dexer.aquanaut.common.fog;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.dexer.aquanaut.Aquanaut;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The live {@link FogTable}, replaced on every resource reload.
 *
 * <p>Read from {@code assets/aquanaut/fog_profile.json} — a client-side visual table, so it
 * lives with the other client assets and a resource pack can retune any ocean simply by
 * shipping its own file. Files from several packs layer in load order, each contributing
 * only the entries it names.</p>
 *
 * <p>A reload that finds no file keeps the current table: the built-in defaults are always a
 * complete, usable table, so there is nothing to fall back to and nothing to reset.</p>
 */
public final class FogProfiles {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_NAME = "fog_profile.json";
    private static volatile FogTable table = FogTable.defaults();

    private FogProfiles() {
    }

    public static FogTable get() {
        return table;
    }

    public static PreparableReloadListener reloadListener() {
        return (stage, resourceManager, prepProfiler, reloadProfiler, backgroundExecutor, gameExecutor) -> stage
                .wait(null)
                .thenRunAsync(() -> reload(resourceManager), gameExecutor);
    }

    public static synchronized void reload(ResourceManager resourceManager) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, FILE_NAME);
        // Every pack's copy of the same file, lowest priority first: each one layers over the
        // last, so an addon pack can add or retune single oceans with a partial file.
        List<Resource> stack = resourceManager.getResourceStack(location);
        if (stack.isEmpty()) {
            LOGGER.warn("No {} found; keeping the previous fog table", location);
            return;
        }

        FogTable loaded = FogTable.defaults();
        for (Resource resource : stack) {
            try (InputStream inputStream = resource.open();
                    InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                loaded = loaded.withOverrides(json);
            } catch (IOException | IllegalStateException | UnsupportedOperationException e) {
                LOGGER.error("Failed to load fog profile from {}", resource.sourcePackId(), e);
            }
        }
        table = loaded;
        // Logged because a silent fallback is indistinguishable from a pack that was ignored —
        // which is exactly how the first version of this loader failed.
        LOGGER.info("Fog profile loaded from {} pack resource(s); {} biomes declared",
                stack.size(), loaded.biomeIds().size());
    }

    /** Test hook: install a table directly. */
    public static void install(FogTable replacement) {
        table = replacement == null ? FogTable.defaults() : replacement;
    }
}