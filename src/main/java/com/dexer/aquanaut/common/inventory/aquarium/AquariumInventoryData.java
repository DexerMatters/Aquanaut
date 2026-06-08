package com.dexer.aquanaut.common.inventory.aquarium;

import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record AquariumInventoryData(List<AquariumFishEntry> fishEntries) {

    public static final int SLOT_COUNT = 18;

    private static final Codec<AquariumInventoryData> PRIMARY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AquariumFishEntry.CODEC.listOf().fieldOf("fish_entries").forGetter(AquariumInventoryData::fishEntries)
    ).apply(instance, AquariumInventoryData::new));

    private static final Codec<AquariumInventoryData> ID_ONLY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().fieldOf("fish_ids").forGetter(data -> legacyIdsFromEntries(data.fishEntries()))
    ).apply(instance, fishIds -> new AquariumInventoryData(entriesFromFishIds(fishIds))));

    private static final Codec<AquariumInventoryData> STACK_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("stacks").forGetter(data -> legacyStacksFromEntries(data.fishEntries()))
    ).apply(instance, legacyStacks -> new AquariumInventoryData(entriesFromLegacyStacks(legacyStacks))));

    public static final Codec<AquariumInventoryData> CODEC = Codec.either(PRIMARY_CODEC, Codec.either(ID_ONLY_CODEC, STACK_CODEC)).xmap(
            either -> either.map(data -> data, nested -> nested.map(data -> data, data -> data)),
            Either::left);

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

    public Optional<ResourceLocation> fishIdAt(int index) {
        return entryAt(index).flatMap(AquariumFishEntry::resourceId);
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
        return new AquariumFishEntry(id.get().toString(), entry.entityData());
    }

    private static List<String> legacyIdsFromEntries(List<AquariumFishEntry> entries) {
        List<String> fishIds = new ArrayList<>(SLOT_COUNT);
        for (int index = 0; index < SLOT_COUNT; index++) {
            fishIds.add(index < entries.size() ? entries.get(index).entityId() : "");
        }
        return fishIds;
    }

    private static List<AquariumFishEntry> entriesFromFishIds(List<String> fishIds) {
        List<AquariumFishEntry> entries = new ArrayList<>(SLOT_COUNT);
        for (int index = 0; index < SLOT_COUNT; index++) {
            String fishId = index < fishIds.size() ? fishIds.get(index) : "";
            entries.add(new AquariumFishEntry(fishId, ""));
        }
        return entries;
    }

    private static List<AquariumFishEntry> entriesFromLegacyStacks(List<ItemStack> legacyStacks) {
        List<AquariumFishEntry> resolved = new ArrayList<>(SLOT_COUNT);
        for (int index = 0; index < SLOT_COUNT; index++) {
            ItemStack stack = index < legacyStacks.size() ? legacyStacks.get(index) : ItemStack.EMPTY;
            resolved.add(entryFromLegacyStack(stack));
        }
        return resolved;
    }

    private static AquariumFishEntry entryFromLegacyStack(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof SpawnEggItem spawnEgg)) {
            return AquariumFishEntry.EMPTY;
        }

        EntityType<?> type = spawnEgg.getType(stack);
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id == null || AquariumFishCatalog.byId(id).isEmpty()) {
            return AquariumFishEntry.EMPTY;
        }
        return new AquariumFishEntry(id.toString(), "");
    }

    private static List<ItemStack> legacyStacksFromEntries(List<AquariumFishEntry> entries) {
        List<ItemStack> stacks = new ArrayList<>(SLOT_COUNT);
        for (int index = 0; index < SLOT_COUNT; index++) {
            AquariumFishEntry entry = index < entries.size() ? entries.get(index) : AquariumFishEntry.EMPTY;
            stacks.add(legacyStackFromEntry(entry));
        }
        return stacks;
    }

    private static ItemStack legacyStackFromEntry(AquariumFishEntry entry) {
        Optional<ResourceLocation> fishId = entry.resourceId();
        if (fishId.isEmpty()) {
            return ItemStack.EMPTY;
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(fishId.get());
        if (type == null) {
            return ItemStack.EMPTY;
        }

        SpawnEggItem spawnEgg = SpawnEggItem.byId(type);
        return spawnEgg == null ? ItemStack.EMPTY : new ItemStack(spawnEgg);
    }
}
