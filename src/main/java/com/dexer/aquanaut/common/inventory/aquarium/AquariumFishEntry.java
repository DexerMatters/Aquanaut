package com.dexer.aquanaut.common.inventory.aquarium;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record AquariumFishEntry(String entityId, String entityData) {

    public static final AquariumFishEntry EMPTY = new AquariumFishEntry("", "");

    public static final Codec<AquariumFishEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("entity_id").forGetter(AquariumFishEntry::entityId),
            Codec.STRING.optionalFieldOf("entity_data", "").forGetter(AquariumFishEntry::entityData)
    ).apply(instance, AquariumFishEntry::new));

    public AquariumFishEntry {
        entityId = normalizeEntityId(entityId);
        entityData = entityId.isBlank() ? "" : normalizeEntityData(entityData);
    }

    public boolean isEmpty() {
        return entityId.isBlank();
    }

    public Optional<ResourceLocation> resourceId() {
        return this.isEmpty() ? Optional.empty() : Optional.of(ResourceLocation.parse(entityId));
    }

    public String cacheKey() {
        return this.entityId + "|" + this.entityData;
    }

    private static String normalizeEntityId(String rawEntityId) {
        if (rawEntityId == null || rawEntityId.isBlank()) {
            return "";
        }
        ResourceLocation id = ResourceLocation.tryParse(rawEntityId);
        return id == null ? "" : id.toString();
    }

    private static String normalizeEntityData(String rawEntityData) {
        return rawEntityData == null ? "" : rawEntityData;
    }
}
