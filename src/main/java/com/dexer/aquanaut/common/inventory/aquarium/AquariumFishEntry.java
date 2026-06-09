package com.dexer.aquanaut.common.inventory.aquarium;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record AquariumFishEntry(String entityId, float health, String entityData) {

    public static final AquariumFishEntry EMPTY = new AquariumFishEntry("", 0.0F, "");

    public static final Codec<AquariumFishEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("entity_id").forGetter(AquariumFishEntry::entityId),
            Codec.FLOAT.fieldOf("health").forGetter(AquariumFishEntry::health),
            Codec.STRING.optionalFieldOf("entity_data", "").forGetter(AquariumFishEntry::entityData)
    ).apply(instance, AquariumFishEntry::new));

    public AquariumFishEntry {
        entityId = normalizeEntityId(entityId);
        entityData = entityId.isBlank() ? "" : (entityData == null ? "" : entityData);
    }

    public AquariumFishEntry(ResourceLocation entityId, float health, String entityData) {
        this(entityId.toString(), health, entityData);
    }

    public boolean isEmpty() {
        return entityId.isBlank();
    }

    public Optional<ResourceLocation> resourceId() {
        return this.isEmpty() ? Optional.empty() : Optional.of(ResourceLocation.parse(entityId));
    }

    public AquariumFishEntry withHealth(float newHealth) {
        return new AquariumFishEntry(entityId, newHealth, entityData);
    }

    public String cacheKey() {
        return this.entityId + "|" + this.health + "|" + this.entityData;
    }

    private static String normalizeEntityId(String rawEntityId) {
        if (rawEntityId == null || rawEntityId.isBlank()) {
            return "";
        }
        ResourceLocation id = ResourceLocation.tryParse(rawEntityId);
        return id == null ? "" : id.toString();
    }
}
