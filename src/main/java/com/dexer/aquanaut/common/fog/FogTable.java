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
 * The whole fog table: how far the eye reaches in each ocean, and the profile of every
 * medium the world cannot describe by itself.
 *
 * <p>Biome colours are deliberately absent. While the camera is submerged the authoritative
 * colour is the world's own fog state — the biome's {@code water_fog_color}, already
 * modified by the abyss ramp — so a colour lives in exactly one place, its biome JSON, and
 * a pack that reads vanilla fog gets the same colour the veil is drawn with. Only media the
 * world has no colour for (sulfuric acid) bring one here.</p>
 *
 * <p>{@link #parse} layers a data file over {@link #defaults()}, so a partial or missing
 * file is still a working table.</p>
 */
public final class FogTable {
    /** Id of the acid medium inside the table's {@code mediums} block. */
    public static final String ACID = "acid";

    private static final String MODID = "aquanaut";

    private final FogVisibility fallback;
    private final Map<ResourceLocation, FogVisibility> biomes;
    private final Map<String, FogMediumProfile> mediums;

    public FogTable(FogVisibility fallback, Map<ResourceLocation, FogVisibility> biomes,
                    Map<String, FogMediumProfile> mediums) {
        this.fallback = Objects.requireNonNull(fallback, "fallback");
        this.biomes = Map.copyOf(biomes);
        this.mediums = Map.copyOf(mediums);
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

    /** A medium profile such as {@link #ACID}, or {@code null} when the table has none. */
    public FogMediumProfile medium(String id) {
        return mediums.get(id);
    }

    /** The acid profile, falling back to the built-in one if a data file dropped it. */
    public FogMediumProfile acid() {
        FogMediumProfile acid = mediums.get(ACID);
        return acid != null ? acid : defaults().mediums.get(ACID);
    }

    public Set<String> mediumIds() {
        return mediums.keySet();
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
        biomes.put(id("brine_mirror_gorge"), new FogVisibility(-6.0F, 160.0F, 0.20F));
        biomes.put(id("brimstone_caldera"), new FogVisibility(-3.0F, 80.0F, 0.55F));

        Map<String, FogMediumProfile> mediums = new LinkedHashMap<>();
        // The acid's surface tint (0xFFD8D466) is chosen for a thin film of liquid; a screen
        // full of acid fog needs the same hue knocked back to something a body can swim in.
        mediums.put(ACID, new FogMediumProfile(0x6E7A2A, new FogVisibility(1.0F, 22.0F, 0.85F)));
        return new FogTable(new FogVisibility(-4.0F, 96.0F, 0.45F), biomes, mediums);
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

        Map<String, FogMediumProfile> newMediums = new LinkedHashMap<>(mediums);
        JsonObject mediumBlock = asObject(root.get("mediums"));
        if (mediumBlock != null) {
            for (Map.Entry<String, JsonElement> entry : mediumBlock.entrySet()) {
                FogMediumProfile current = newMediums.get(entry.getKey());
                if (current == null && !ACID.equals(entry.getKey())) {
                    // A medium this mod knows nothing about: only a file that names its
                    // colour may introduce it, and it starts from the acid's murk.
                    if (asObject(entry.getValue()) == null
                            || !entry.getValue().getAsJsonObject().has("color")) {
                        continue;
                    }
                    current = defaults().mediums.get(ACID);
                }
                if (current != null) {
                    newMediums.put(entry.getKey(), readMedium(entry.getValue(), current));
                }
            }
        }
        return new FogTable(newFallback, newBiomes, newMediums);
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

    private static FogMediumProfile readMedium(JsonElement element, FogMediumProfile fallback) {
        JsonObject object = asObject(element);
        if (object == null) {
            return fallback;
        }
        int rgb = object.has("color") ? readColor(object.get("color"), fallback.rgb()) : fallback.rgb();
        return new FogMediumProfile(rgb, readVisibility(object, fallback.visibility()));
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

    /** Accepts {@code "#RRGGBB"}, {@code "RRGGBB"} or a bare decimal/0x integer. */
    private static int readColor(JsonElement element, int fallback) {
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            if (element.getAsJsonPrimitive().isNumber()) {
                return element.getAsInt() & 0xFFFFFF;
            }
            String text = element.getAsString().trim();
            if (text.startsWith("#")) {
                text = text.substring(1);
            } else if (text.startsWith("0x") || text.startsWith("0X")) {
                return Integer.parseInt(text.substring(2), 16) & 0xFFFFFF;
            }
            return Integer.parseInt(text, 16) & 0xFFFFFF;
        } catch (NumberFormatException | UnsupportedOperationException e) {
            return fallback;
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}