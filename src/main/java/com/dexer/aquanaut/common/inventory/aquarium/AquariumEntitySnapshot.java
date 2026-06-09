package com.dexer.aquanaut.common.inventory.aquarium;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class AquariumEntitySnapshot {

    private AquariumEntitySnapshot() {
    }

    public static Optional<AquariumFishEntry> createDefault(Level level, AquariumFishSpec spec) {
        Entity entity = spec.entityType().create(level);
        if (entity == null) {
            return Optional.empty();
        }
        return snapshot(entity);
    }

    public static Optional<AquariumFishEntry> snapshot(Entity entity) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id == null || AquariumFishCatalog.byId(id).isEmpty()) {
            return Optional.empty();
        }

        float health = entity instanceof LivingEntity le ? le.getHealth() : 20.0F;

        CompoundTag tag = new CompoundTag();
        entity.save(tag);
        tag.remove("id");
        tag.remove("Pos");
        tag.remove("Motion");
        tag.remove("Rotation");
        tag.remove("UUID");
        tag.remove("Passengers");
        tag.remove("Leash");
        tag.remove("OnGround");
        tag.remove("FallDistance");
        tag.remove("PortalCooldown");
        tag.remove("Health");

        return Optional.of(new AquariumFishEntry(id, health, tag.toString()));
    }

    public static Optional<Entity> createEntity(Level level, AquariumFishEntry entry) {
        Optional<ResourceLocation> id = entry.resourceId();
        if (id.isEmpty()) {
            return Optional.empty();
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id.get());
        if (type == null) {
            return Optional.empty();
        }

        Entity entity = type.create(level);
        if (entity == null) {
            return Optional.empty();
        }

        if (!entry.entityData().isBlank()) {
            try {
                CompoundTag tag = TagParser.parseTag(entry.entityData());
                entity.load(tag);
            } catch (Exception ignored) {
            }
        }
        if (entity instanceof LivingEntity le && entry.health() > 0.0F) {
            le.setHealth(Math.min(entry.health(), le.getMaxHealth()));
        }

        return Optional.of(entity);
    }

    public static Optional<LivingEntity> createLivingEntity(Level level, AquariumFishEntry entry) {
        Optional<Entity> entity = createEntity(level, entry);
        if (entity.isPresent() && entity.get() instanceof LivingEntity livingEntity) {
            return Optional.of(livingEntity);
        }
        return Optional.empty();
    }
}
