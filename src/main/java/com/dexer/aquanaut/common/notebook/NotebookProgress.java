package com.dexer.aquanaut.common.notebook;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

public record NotebookProgress(Map<ResourceLocation, NotebookResearchStage> speciesStages) {

    public static final Codec<NotebookProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(ResourceLocation.CODEC, NotebookResearchStageCodec.CODEC)
                    .fieldOf("species_stages")
                    .forGetter(NotebookProgress::speciesStages)
    ).apply(instance, NotebookProgress::new));

    public static final NotebookProgress EMPTY = new NotebookProgress(Map.of());

    public NotebookProgress {
        Map<ResourceLocation, NotebookResearchStage> normalized = new LinkedHashMap<>();
        if (speciesStages != null) {
            for (Map.Entry<ResourceLocation, NotebookResearchStage> entry : speciesStages.entrySet()) {
                ResourceLocation canonical = NotebookCatalog.canonicalId(entry.getKey());
                NotebookResearchStage stage = entry.getValue() == null ? NotebookResearchStage.UNSEEN : entry.getValue();
                NotebookResearchStage current = normalized.get(canonical);
                if (current == null || stage.isAtLeast(current)) {
                    normalized.put(canonical, stage);
                }
            }
        }
        speciesStages = Map.copyOf(normalized);
    }

    public NotebookResearchStage stageFor(ResourceLocation speciesId) {
        ResourceLocation canonical = NotebookCatalog.canonicalId(speciesId);
        return speciesStages.getOrDefault(canonical, NotebookResearchStage.UNSEEN);
    }

    public boolean hasUnlocked(ResourceLocation speciesId, NotebookResearchStage stage) {
        return stageFor(speciesId).isAtLeast(stage);
    }

    public NotebookProgress withStage(ResourceLocation speciesId, NotebookResearchStage stage) {
        ResourceLocation canonical = NotebookCatalog.canonicalId(speciesId);
        NotebookResearchStage current = speciesStages.get(canonical);
        if (current != null && current.isAtLeast(stage)) {
            return this;
        }

        Map<ResourceLocation, NotebookResearchStage> copy = new LinkedHashMap<>(speciesStages);
        copy.put(canonical, stage);
        return new NotebookProgress(copy);
    }

    public String serialize() {
        return CODEC.encodeStart(JsonOps.INSTANCE, this)
                .result()
                .orElseThrow(() -> new IllegalStateException("failed to encode notebook progress"))
                .toString();
    }

    public static NotebookProgress deserialize(String serialized) {
        if (serialized == null || serialized.isBlank()) {
            return EMPTY;
        }
        try {
            return CODEC.parse(JsonOps.INSTANCE, com.google.gson.JsonParser.parseString(serialized))
                    .result()
                    .orElse(EMPTY);
        } catch (Exception ignored) {
            return EMPTY;
        }
    }
}
