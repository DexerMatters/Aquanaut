package com.dexer.aquanaut.common.light;

import java.util.Objects;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Stable, value-based ownership for a dynamic light. The owner is deliberately independent of
 * the provider so one future feature can expose more than one light channel.
 */
public sealed interface DynamicLightOwner
        permits DynamicLightOwner.Entity, DynamicLightOwner.Block, DynamicLightOwner.Named {

    record Entity(UUID id) implements DynamicLightOwner {
        public Entity {
            Objects.requireNonNull(id, "id");
        }
    }

    record Block(ResourceKey<Level> dimension, BlockPos position) implements DynamicLightOwner {
        public Block {
            Objects.requireNonNull(dimension, "dimension");
            position = Objects.requireNonNull(position, "position").immutable();
        }
    }

    record Named(ResourceLocation id) implements DynamicLightOwner {
        public Named {
            Objects.requireNonNull(id, "id");
        }
    }
}
