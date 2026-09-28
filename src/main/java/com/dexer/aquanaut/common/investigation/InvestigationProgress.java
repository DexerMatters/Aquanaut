package com.dexer.aquanaut.common.investigation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Immutable network snapshot of the world-wide investigation progress. */
public record InvestigationProgress(Map<ResourceLocation, Integer> nodeStars, Set<ResourceLocation> discoveredLinks) {
    public static final Codec<InvestigationProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.intRange(0, 5))
                    .fieldOf("node_stars").forGetter(InvestigationProgress::nodeStars),
            ResourceLocation.CODEC.listOf().fieldOf("discovered_links")
                    .forGetter(progress -> List.copyOf(progress.discoveredLinks()))
    ).apply(instance, (stars, links) -> new InvestigationProgress(stars, new LinkedHashSet<>(links))));

    public static final InvestigationProgress EMPTY = new InvestigationProgress(Map.of(), Set.of());

    public InvestigationProgress {
        nodeStars = nodeStars == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(nodeStars));
        discoveredLinks = discoveredLinks == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(discoveredLinks));
    }

    public int stars(InvestigationNode node) {
        return Math.clamp(nodeStars.getOrDefault(node.id(), 0), 0, node.maxStars());
    }

    public boolean isDiscovered(InvestigationNode node) {
        return stars(node) > 0;
    }

    public boolean isDiscovered(InvestigationLink link) {
        return discoveredLinks.contains(link.id());
    }

    public String serialize() {
        return CODEC.encodeStart(JsonOps.INSTANCE, this).result()
                .orElseThrow(() -> new IllegalStateException("failed to encode investigation progress"))
                .toString();
    }

    public static InvestigationProgress deserialize(String serialized) {
        if (serialized == null || serialized.isBlank()) return EMPTY;
        try {
            return CODEC.parse(JsonOps.INSTANCE, com.google.gson.JsonParser.parseString(serialized))
                    .result().orElse(EMPTY);
        } catch (RuntimeException ignored) {
            return EMPTY;
        }
    }
}
