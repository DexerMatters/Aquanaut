package com.dexer.aquanaut.common.gaze;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record GazeInstance(ResourceLocation id, GazeType type, int level) {
    public static final Codec<GazeInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(GazeInstance::id),
            GazeType.CODEC.fieldOf("type").forGetter(GazeInstance::type),
            Codec.INT.fieldOf("level").forGetter(GazeInstance::level)
    ).apply(instance, GazeInstance::new));

    public GazeInstance {
        Objects.requireNonNull(id, "Gaze id must not be null");
        Objects.requireNonNull(type, "Gaze type must not be null");
        if (level < 1) {
            throw new IllegalArgumentException("Gaze level must be >= 1, got " + level);
        }
    }
}
