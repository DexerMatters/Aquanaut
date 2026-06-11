package com.dexer.aquanaut.common.notebook;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class NotebookCatalog {

    private static final Logger LOGGER = Logger.getLogger(NotebookCatalog.class.getName());

    private static volatile Map<ResourceLocation, NotebookSpecies> byId = Map.of();
    private static volatile Map<ResourceLocation, ResourceLocation> aliasToCanonical = Map.of();
    private static volatile Map<NotebookCategory, List<NotebookSpecies>> byCategory = Map.of(
            NotebookCategory.FRIENDLY, List.of(),
            NotebookCategory.THREATENING, List.of(),
            NotebookCategory.TITAN, List.of());

    private NotebookCatalog() {
    }

    public static PreparableReloadListener reloadListener() {
        return (stage, resourceManager, prepProfiler, reloadProfiler, backgroundExecutor, gameExecutor) -> stage
                .wait(null)
                .thenRunAsync(() -> reload(resourceManager), gameExecutor);
    }

    public static synchronized void reload(ResourceManager resourceManager) {
        Map<ResourceLocation, NotebookSpecies> previousById = byId;
        Map<ResourceLocation, ResourceLocation> previousAliases = aliasToCanonical;
        Map<NotebookCategory, List<NotebookSpecies>> previousCategories = byCategory;

        Map<ResourceLocation, NotebookSpecies> loadedById = new LinkedHashMap<>();
        Map<ResourceLocation, ResourceLocation> loadedAliases = new LinkedHashMap<>();
        Map<NotebookCategory, List<NotebookSpecies>> loadedCategories = new EnumMap<>(NotebookCategory.class);
        for (NotebookCategory category : NotebookCategory.values()) {
            loadedCategories.put(category, new ArrayList<>());
        }

        Map<ResourceLocation, Resource> resources = resourceManager.listResources("notebook/waterlife",
                id -> id.getPath().endsWith(".json"));
        List<ResourceLocation> ids = new ArrayList<>(resources.keySet());
        ids.sort(Comparator.comparing(ResourceLocation::toString));

        for (ResourceLocation location : ids) {
            Resource resource = resources.get(location);
            if (resource == null) {
                continue;
            }

            try (InputStream inputStream = resource.open();
                    InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                NotebookSpecies species = parseSpecies(json, location);
                loadedById.put(species.id(), species);
                loadedCategories.get(species.category()).add(species);
                for (ResourceLocation alias : species.aliases()) {
                    loadedAliases.put(alias, species.id());
                }
            } catch (IOException | IllegalStateException | UnsupportedOperationException e) {
                LOGGER.log(Level.SEVERE, "Failed to load notebook species from " + location, e);
            }
        }

        if (loadedById.isEmpty()) {
            LOGGER.warning("Notebook reload produced no entries from resource manager; trying development fallback");
            Path devPath = Path.of("src/main/resources/data/aquanaut/notebook/waterlife");
            if (Files.isDirectory(devPath)) {
                reloadFromDirectory(devPath);
                return;
            }

            LOGGER.warning("Notebook reload kept previous catalog because no notebook entries could be loaded");
            byId = previousById;
            aliasToCanonical = previousAliases;
            byCategory = previousCategories;
            return;
        }

        for (List<NotebookSpecies> speciesList : loadedCategories.values()) {
            speciesList.sort(Comparator.comparing(species -> species.id().toString()));
        }

        byId = Map.copyOf(loadedById);
        aliasToCanonical = Map.copyOf(loadedAliases);
        Map<NotebookCategory, List<NotebookSpecies>> finalCategories = new EnumMap<>(NotebookCategory.class);
        for (Map.Entry<NotebookCategory, List<NotebookSpecies>> entry : loadedCategories.entrySet()) {
            finalCategories.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        byCategory = Map.copyOf(finalCategories);
        LOGGER.info("Loaded " + byId.size() + " notebook species entries");
    }

    public static void ensureLoaded() {
        if (!byId.isEmpty()) {
            return;
        }

        Path devPath = Path.of("src/main/resources/data/aquanaut/notebook/waterlife");
        if (Files.isDirectory(devPath)) {
            reloadFromDirectory(devPath);
        }
    }

    public static synchronized void reloadFromDirectory(Path directory) {
        Map<ResourceLocation, NotebookSpecies> previousById = byId;
        Map<ResourceLocation, ResourceLocation> previousAliases = aliasToCanonical;
        Map<NotebookCategory, List<NotebookSpecies>> previousCategories = byCategory;

        Map<ResourceLocation, NotebookSpecies> loadedById = new LinkedHashMap<>();
        Map<ResourceLocation, ResourceLocation> loadedAliases = new LinkedHashMap<>();
        Map<NotebookCategory, List<NotebookSpecies>> loadedCategories = new EnumMap<>(NotebookCategory.class);
        for (NotebookCategory category : NotebookCategory.values()) {
            loadedCategories.put(category, new ArrayList<>());
        }

        try {
            List<Path> files = Files.walk(directory)
                    .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".json"))
                    .sorted()
                    .toList();
            for (Path path : files) {
                try (InputStream inputStream = Files.newInputStream(path);
                        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    NotebookSpecies species = parseSpecies(json, null);
                    loadedById.put(species.id(), species);
                    loadedCategories.get(species.category()).add(species);
                    for (ResourceLocation alias : species.aliases()) {
                        loadedAliases.put(alias, species.id());
                    }
                } catch (IOException | IllegalStateException | UnsupportedOperationException e) {
                    LOGGER.log(Level.SEVERE, "Failed to load notebook species from " + path, e);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to walk notebook species directory " + directory, e);
        }

        if (loadedById.isEmpty()) {
            LOGGER.warning("Notebook reload from directory produced no entries; keeping previous catalog");
            byId = previousById;
            aliasToCanonical = previousAliases;
            byCategory = previousCategories;
            return;
        }

        for (List<NotebookSpecies> speciesList : loadedCategories.values()) {
            speciesList.sort(Comparator.comparing(species -> species.id().toString()));
        }

        byId = Map.copyOf(loadedById);
        aliasToCanonical = Map.copyOf(loadedAliases);
        Map<NotebookCategory, List<NotebookSpecies>> finalCategories = new EnumMap<>(NotebookCategory.class);
        for (Map.Entry<NotebookCategory, List<NotebookSpecies>> entry : loadedCategories.entrySet()) {
            finalCategories.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        byCategory = Map.copyOf(finalCategories);
        LOGGER.info("Loaded " + byId.size() + " notebook species entries from " + directory);
    }

    public static Collection<NotebookSpecies> all() {
        ensureLoaded();
        return List.copyOf(byId.values());
    }

    public static List<NotebookSpecies> byCategory(NotebookCategory category) {
        ensureLoaded();
        return byCategory.getOrDefault(category, List.of());
    }

    public static List<NotebookSpecies> visibleByCategory(NotebookCategory category, NotebookProgress progress) {
        ensureLoaded();
        List<NotebookSpecies> result = new ArrayList<>();
        for (NotebookSpecies species : byCategory(category)) {
            if (progress.stageFor(species.id()).isAtLeast(NotebookResearchStage.ENCOUNTERED)) {
                result.add(species);
            }
        }
        return List.copyOf(result);
    }

    public static Optional<NotebookSpecies> resolve(ResourceLocation id) {
        ensureLoaded();
        ResourceLocation canonical = canonicalId(id);
        return Optional.ofNullable(byId.get(canonical));
    }

    public static ResourceLocation canonicalId(ResourceLocation id) {
        ensureLoaded();
        return aliasToCanonical.getOrDefault(id, id);
    }

    private static NotebookSpecies parseSpecies(JsonObject json, ResourceLocation sourceLocation) {
        ResourceLocation id = readResourceLocation(json, "species_id");
        ResourceLocation entityId = json.has("entity_id") ? readResourceLocation(json, "entity_id") : id;
        NotebookCategory category = NotebookCategory.fromSerializedName(readString(json, "category"));
        NotebookDiet diet = NotebookDiet.fromSerializedName(readString(json, "diet"));

        List<ResourceLocation> aliases = new ArrayList<>();
        JsonArray aliasArray = readArray(json, "aliases");
        for (JsonElement element : aliasArray) {
            aliases.add(ResourceLocation.parse(element.getAsString()));
        }

        List<NotebookBlock> blocks = new ArrayList<>();
        JsonArray blockArray = readArray(json, "blocks");
        for (JsonElement element : blockArray) {
            JsonObject blockJson = element.getAsJsonObject();
            NotebookBlockKind kind = NotebookBlockKind.fromSerializedName(readString(blockJson, "type"));
            NotebookResearchStage stage = NotebookResearchStage.fromSerializedName(readString(blockJson, "stage"));
            if (kind == NotebookBlockKind.PARAGRAPH) {
                blocks.add(NotebookBlock.paragraph(stage, readString(blockJson, "text")));
                continue;
            }

            NotebookComponentKind componentKind = NotebookComponentKind.fromSerializedName(
                    readString(blockJson, "component_kind"));
            ResourceLocation resource = ResourceLocation.parse(readString(blockJson, "resource"));
            String caption = blockJson.has("caption") ? readString(blockJson, "caption") : "";
            blocks.add(NotebookBlock.component(stage, componentKind, resource, caption));
        }

        if (sourceLocation != null) {
            ResourceLocation expectedId = inferIdFromPath(sourceLocation);
            if (expectedId != null && !expectedId.equals(id)) {
                LOGGER.warning("Notebook file " + sourceLocation + " declared species_id " + id
                        + " but file path suggests " + expectedId);
            }
        }

        return new NotebookSpecies(id, entityId, category, diet, aliases, blocks);
    }

    private static ResourceLocation inferIdFromPath(ResourceLocation sourceLocation) {
        String path = sourceLocation.getPath();
        int index = path.lastIndexOf('/');
        String fileName = index >= 0 ? path.substring(index + 1) : path;
        if (!fileName.endsWith(".json")) {
            return null;
        }
        String stripped = fileName.substring(0, fileName.length() - 5);
        return ResourceLocation.fromNamespaceAndPath(sourceLocation.getNamespace(), stripped);
    }

    private static JsonArray readArray(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonArray()) {
            return new JsonArray();
        }
        return json.getAsJsonArray(key);
    }

    private static String readString(JsonObject json, String key) {
        if (!json.has(key)) {
            throw new IllegalStateException("Missing notebook species field: " + key);
        }
        return json.get(key).getAsString();
    }

    private static ResourceLocation readResourceLocation(JsonObject json, String key) {
        return ResourceLocation.parse(readString(json, key));
    }
}
