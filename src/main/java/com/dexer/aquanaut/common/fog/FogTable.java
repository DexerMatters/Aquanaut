package com.dexer.aquanaut.common.fog;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The whole fog table: how far the eye reaches in each ocean.
 *
 * <p>Biome colours are deliberately absent. While the camera is submerged the authoritative
 * colour is the world's own fog state — the biome's {@code water_fog_color}, already
 * modified by the abyss ramp — so a colour lives in exactly one place, its biome JSON, and
 * a pack that reads vanilla fog gets the same colour the veil is drawn with.</p>
 *
 * <p>{@link #parse} layers a data file over {@link #defaults()}, so a partial or missing
 * file is still a working table.</p>
 */
public final class FogTable {
    private static final String MODID = "aquanaut";

    private final FogVisibility fallback;
    private final Map<ResourceLocation, FogVisibility> biomes;

    public FogTable(FogVisibility fallback, Map<ResourceLocation, FogVisibility> biomes) {
        this.fallback = Objects.requireNonNull(fallback, "fallback");
        this.biomes = Map.copyOf(biomes);
    }

    public FogVisibility fallback() {
        return fallback;
    }

    /** The visibility profile of a biome, or the fallback when that biome declares none. */
    public FogVisibility visibility(ResourceLocation biome) {
        FogVisibility profile = biomes.get(biome);
        return profile != null ? profile : fallback;
    }

    /** Whether this biome declares its own profile (rather than falling back). */
    public boolean declares(ResourceLocation biome) {
        return biomes.containsKey(biome);
    }

    public Set<ResourceLocation> biomeIds() {
        return biomes.keySet();
    }

    /** A biome weighted by how much it influences a point — the mixture between two oceans. */
    public record WeightedBiome(ResourceLocation biome, double weight) {
    }

    /**
     * The visibility of a point that sits between several oceans: each sample's profile
     * averaged by weight. Blending here rather than picking the nearest biome is what makes a
     * boundary a gradient instead of a step.
     */
    public FogVisibility blendedVisibility(List<WeightedBiome> samples) {
        if (samples == null || samples.isEmpty()) {
            return fallback;
        }
        double total = 0.0D;
        double near = 0.0D;
        double far = 0.0D;
        double cast = 0.0D;
        for (WeightedBiome sample : samples) {
            double weight = Math.max(0.0D, sample.weight());
            if (weight <= 0.0D) {
                continue;
            }
            FogVisibility profile = visibility(sample.biome());
            total += weight;
            near += profile.nearPlane() * weight;
            far += profile.farPlane() * weight;
            cast += profile.castStrength() * weight;
        }
        if (total <= 0.0D) {
            return fallback;
        }
        return new FogVisibility((float) (near / total), (float) (far / total),
                (float) (cast / total));
    }

    /** The table used before any data file is read, and if none ever is. */
    public static FogTable defaults() {
        Map<ResourceLocation, FogVisibility> biomes = new LinkedHashMap<>();
        biomes.put(id("middle_level_ocean"), new FogVisibility(-4.0F, 128.0F, 0.30F));
        biomes.put(id("coral_forest"), new FogVisibility(-4.0F, 120.0F, 0.26F));
        biomes.put(id("jelly_jungle"), new FogVisibility(-2.0F, 64.0F, 0.48F));
        biomes.put(id("mud_zone"), new FogVisibility(-3.0F, 72.0F, 0.62F));
        biomes.put(id("brine_mirror_gorge"), new FogVisibility(-6.0F, 160.0F, 0.20F));
        biomes.put(id("brimstone_caldera"), new FogVisibility(-3.0F, 80.0F, 0.55F));
        biomes.put(id("crystal_nest"), new FogVisibility(-5.0F, 144.0F, 0.28F));

        return new FogTable(new FogVisibility(-4.0F, 96.0F, 0.45F), biomes);
    }

    /** Read a table from JSON, layered over {@link #defaults()} so a partial file is valid. */
    public static FogTable parse(JsonObject root) {
        return defaults().withOverrides(root);
    }

    /**
     * A copy of this table with whatever {@code root} declares replaced, and everything it
     * leaves out untouched — so several data packs can each contribute only their own
     * entries and still layer in a defined order.
     */
    public FogTable withOverrides(JsonObject root) {
        if (root == null) {
            return this;
        }
        FogVisibility newFallback = readVisibility(root.get("default"), fallback);

        Map<ResourceLocation, FogVisibility> newBiomes = new LinkedHashMap<>(biomes);
        JsonObject biomeBlock = asObject(root.get("biomes"));
        if (biomeBlock != null) {
            for (Map.Entry<String, JsonElement> entry : biomeBlock.entrySet()) {
                ResourceLocation biome = ResourceLocation.tryParse(entry.getKey());
                if (biome == null) {
                    continue;
                }
                newBiomes.put(biome, readVisibility(entry.getValue(),
                        newBiomes.getOrDefault(biome, newFallback)));
            }
        }
        return new FogTable(newFallback, newBiomes);
    }

    private static FogVisibility readVisibility(JsonElement element, FogVisibility fallback) {
        JsonObject object = asObject(element);
        if (object == null) {
            return fallback;
        }
        float near = readFloat(object, "near", fallback.nearPlane());
        float far = readFloat(object, "far", fallback.farPlane());
        float cast = readFloat(object, "cast", fallback.castStrength());
        return new FogVisibility(near, far, cast);
    }

    private static JsonObject asObject(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static float readFloat(JsonObject object, String key, float fallback) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsFloat();
        } catch (NumberFormatException | UnsupportedOperationException e) {
            return fallback;
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
