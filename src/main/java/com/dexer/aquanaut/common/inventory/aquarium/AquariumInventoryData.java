package com.dexer.aquanaut.common.inventory.aquarium;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record AquariumInventoryData(List<AquariumFishEntry> fishEntries) {

    public static final int SLOT_COUNT = 18;

    public static final Codec<AquariumInventoryData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AquariumFishEntry.CODEC.listOf().fieldOf("fish_entries").forGetter(AquariumInventoryData::fishEntries)
    ).apply(instance, AquariumInventoryData::new));

    public static final AquariumInventoryData EMPTY = new AquariumInventoryData(
            Collections.nCopies(SLOT_COUNT, AquariumFishEntry.EMPTY));

    public AquariumInventoryData {
        List<AquariumFishEntry> padded = new ArrayList<>(fishEntries);
        while (padded.size() < SLOT_COUNT) {
            padded.add(AquariumFishEntry.EMPTY);
        }
        if (padded.size() > SLOT_COUNT) {
            padded = new ArrayList<>(padded.subList(0, SLOT_COUNT));
        }

        List<AquariumFishEntry> normalized = new ArrayList<>(SLOT_COUNT);
        for (AquariumFishEntry entry : padded) {
            normalized.add(normalizeEntry(entry));
        }
        fishEntries = List.copyOf(normalized);
    }

    public Optional<AquariumFishEntry> entryAt(int index) {
        validateIndex(index);
        AquariumFishEntry entry = fishEntries.get(index);
        return entry.isEmpty() ? Optional.empty() : Optional.of(entry);
    }

    public AquariumInventoryData withEntry(int index, AquariumFishEntry entry) {
        validateIndex(index);
        List<AquariumFishEntry> copy = new ArrayList<>(fishEntries);
        copy.set(index, normalizeEntry(entry));
        return new AquariumInventoryData(copy);
    }

    public AquariumInventoryData withoutFish(int index) {
        return withEntry(index, AquariumFishEntry.EMPTY);
    }

    public List<AquariumFishEntry> mutableCopy() {
        return new ArrayList<>(fishEntries);
    }

    public String serialize() {
        return CODEC.encodeStart(JsonOps.INSTANCE, this)
                .result()
                .orElseThrow(() -> new IllegalStateException("failed to encode aquarium inventory"))
                .toString();
    }

    public static AquariumInventoryData deserialize(String serialized) {
        if (serialized == null || serialized.isBlank()) {
            return EMPTY;
        }
        try {
            return CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(serialized))
                    .result()
                    .orElse(EMPTY);
        } catch (Exception ignored) {
            return EMPTY;
        }
    }

    private static void validateIndex(int index) {
        if (index < 0 || index >= SLOT_COUNT) {
            throw new IndexOutOfBoundsException("Aquarium slot index out of bounds: " + index);
        }
    }

    private static AquariumFishEntry normalizeEntry(AquariumFishEntry entry) {
        if (entry == null || entry.isEmpty()) {
            return AquariumFishEntry.EMPTY;
        }
        Optional<ResourceLocation> id = entry.resourceId();
        if (id.isEmpty() || AquariumFishCatalog.byId(id.get()).isEmpty()) {
            return AquariumFishEntry.EMPTY;
        }
        return new AquariumFishEntry(id.get().toString(), entry.health(), entry.entityData());
    }
}
