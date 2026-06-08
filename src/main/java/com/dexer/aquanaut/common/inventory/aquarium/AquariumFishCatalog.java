package com.dexer.aquanaut.common.inventory.aquarium;

import com.dexer.aquanaut.Aquanaut;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AquariumFishCatalog {

    private static volatile Map<ResourceLocation, AquariumFishSpec> byId;
    private static volatile Map<EntityType<?>, AquariumFishSpec> byEntityType;

    private AquariumFishCatalog() {
    }

    public static Collection<AquariumFishSpec> all() {
        ensureLoaded();
        return List.copyOf(byId.values());
    }

    public static List<String> commandSuggestions() {
        List<String> suggestions = new ArrayList<>();
        for (AquariumFishSpec spec : all()) {
            suggestions.add(spec.commandId());
        }
        suggestions.sort(String::compareTo);
        return suggestions;
    }

    public static Optional<AquariumFishSpec> byId(ResourceLocation id) {
        ensureLoaded();
        return Optional.ofNullable(byId.get(id));
    }

    public static Optional<AquariumFishSpec> resolve(String input) {
        if (!input.contains(":")) {
            return Optional.empty();
        }
        ResourceLocation id = ResourceLocation.tryParse(input);
        return id != null ? byId(id) : Optional.empty();
    }

    public static Optional<AquariumFishSpec> byEntityType(EntityType<?> type) {
        ensureLoaded();
        return Optional.ofNullable(byEntityType.get(type));
    }

    public static boolean isAquariumFish(EntityType<?> type) {
        return byEntityType(type).isPresent();
    }

    private static void ensureLoaded() {
        if (byId != null && byEntityType != null) {
            return;
        }
        synchronized (AquariumFishCatalog.class) {
            if (byId != null && byEntityType != null) {
                return;
            }

            Map<ResourceLocation, AquariumFishSpec> idMap = new LinkedHashMap<>();
            Map<EntityType<?>, AquariumFishSpec> entityMap = new LinkedHashMap<>();
            List<EntityType<?>> entityTypes = new ArrayList<>();
            for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
                entityTypes.add(type);
            }
            entityTypes.sort(Comparator.comparing(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()));

            for (EntityType<?> type : entityTypes) {
                ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
                if (id == null) {
                    continue;
                }
                if (!isAquaticMob(type)) {
                    continue;
                }

                int gridWidth;
                int gridHeight;
                float modelHeight;
                ResourceLocation modelLocation = ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
                        "geo/" + id.getPath() + ".geo.json");
                Optional<AquariumModelFootprint.Footprint> footprint = AquariumModelFootprint.resolve(
                        modelLocation.getNamespace(), modelLocation.getPath());
                if (footprint.isPresent()) {
                    modelHeight = footprint.get().height();
                    gridWidth = footprint.get().gridWidth();
                    gridHeight = footprint.get().gridHeight();
                } else {
                    modelHeight = type.getHeight() * 16.0F;
                    gridWidth = Math.max(1, Math.round(type.getWidth()));
                    gridHeight = Math.max(1, Math.round(type.getHeight()));
                }

                AquariumFishSpec spec = new AquariumFishSpec(id, type, modelLocation,
                        modelHeight, gridWidth, gridHeight);
                idMap.put(id, spec);
                entityMap.put(type, spec);
            }

            byId = Map.copyOf(idMap);
            byEntityType = Map.copyOf(entityMap);
        }
    }

    private static boolean isAquaticMob(EntityType<?> type) {
        MobCategory category = type.getCategory();
        return category == MobCategory.WATER_CREATURE || category == MobCategory.WATER_AMBIENT;
    }
}
