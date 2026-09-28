package com.dexer.aquanaut.common.investigation;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Server-owned, world-wide state backing every physical investigation board. */
final class InvestigationSavedData extends SavedData {
    static final String FILE_ID = "aquanaut_investigation";
    static final Factory<InvestigationSavedData> FACTORY = new Factory<>(
            InvestigationSavedData::new, InvestigationSavedData::load, DataFixTypes.LEVEL);

    private InvestigationProgress progress;

    private InvestigationSavedData() {
        // A new development world starts with enough varied progress to exercise every visual state.
        Map<net.minecraft.resources.ResourceLocation, Integer> stars = new LinkedHashMap<>();
        InvestigationCatalog.nodes().forEach(node -> {
            int value = switch (node.id().getPath()) {
                case "sunken_station" -> 3;
                case "fractured_viewport" -> 2;
                case "luminous_spores", "last_log" -> 1;
                default -> 0;
            };
            if (value > 0) stars.put(node.id(), value);
        });
        Set<net.minecraft.resources.ResourceLocation> links = new LinkedHashSet<>();
        InvestigationCatalog.links().stream().limit(3).forEach(link -> links.add(link.id()));
        progress = new InvestigationProgress(stars, links);
        setDirty();
    }

    private InvestigationSavedData(InvestigationProgress progress) {
        this.progress = progress;
    }

    InvestigationProgress snapshot() {
        return progress;
    }

    boolean setStars(InvestigationNode node, int stars) {
        int clamped = Math.clamp(stars, 0, node.maxStars());
        if (progress.stars(node) == clamped) return false;
        Map<net.minecraft.resources.ResourceLocation, Integer> updated = new LinkedHashMap<>(progress.nodeStars());
        if (clamped == 0) updated.remove(node.id());
        else updated.put(node.id(), clamped);
        progress = new InvestigationProgress(updated, progress.discoveredLinks());
        setDirty();
        return true;
    }

    boolean discover(InvestigationLink link) {
        if (progress.isDiscovered(link)) return false;
        Set<net.minecraft.resources.ResourceLocation> updated = new LinkedHashSet<>(progress.discoveredLinks());
        updated.add(link.id());
        progress = new InvestigationProgress(progress.nodeStars(), updated);
        setDirty();
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("Progress", progress.serialize());
        return tag;
    }

    private static InvestigationSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        return new InvestigationSavedData(InvestigationProgress.deserialize(tag.getString("Progress")));
    }
}
