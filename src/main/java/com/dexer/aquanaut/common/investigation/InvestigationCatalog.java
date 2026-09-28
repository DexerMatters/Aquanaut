package com.dexer.aquanaut.common.investigation;

import com.dexer.aquanaut.Aquanaut;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Registry for investigation content. Add-ons may register nodes and links during common setup.
 * The registry rejects dangling links and cycles when it is first queried.
 */
public final class InvestigationCatalog {
    private static final Map<ResourceLocation, InvestigationNode> NODES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, InvestigationLink> LINKS = new LinkedHashMap<>();
    private static boolean validated;

    static {
        ResourceLocation wreck = id("sunken_station");
        ResourceLocation glass = id("fractured_viewport");
        ResourceLocation spores = id("luminous_spores");
        ResourceLocation log = id("last_log");
        ResourceLocation signal = id("abyssal_signal");
        ResourceLocation trench = id("silent_trench");

        registerNode(new InvestigationNode(wreck,
                key("node.sunken_station.title"), key("node.sunken_station.summary"),
                key("node.sunken_station.question"), 70, 255, 3,
                InvestigationNode.Tone.PAPER, List.of()));
        registerNode(new InvestigationNode(glass,
                key("node.fractured_viewport.title"), key("node.fractured_viewport.summary"),
                key("node.fractured_viewport.question"), 305, 80, 3,
                InvestigationNode.Tone.BLUEPRINT, List.of(wreck)));
        registerNode(new InvestigationNode(spores,
                key("node.luminous_spores.title"), key("node.luminous_spores.summary"),
                key("node.luminous_spores.question"), 320, 420, 3,
                InvestigationNode.Tone.WARNING, List.of(wreck)));
        registerNode(new InvestigationNode(log,
                key("node.last_log.title"), key("node.last_log.summary"),
                key("node.last_log.question"), 565, 245, 3,
                InvestigationNode.Tone.PAPER, List.of(glass)));
        registerNode(new InvestigationNode(signal,
                key("node.abyssal_signal.title"), key("node.abyssal_signal.summary"),
                key("node.abyssal_signal.question"), 800, 105, 3,
                InvestigationNode.Tone.BLUEPRINT, List.of(log)));
        registerNode(new InvestigationNode(trench,
                key("node.silent_trench.title"), key("node.silent_trench.summary"),
                key("node.silent_trench.question"), 805, 420, 3,
                InvestigationNode.Tone.WARNING, List.of(log, spores)));

        registerLink(link("impact", wreck, glass));
        registerLink(link("sample", wreck, spores));
        registerLink(link("timestamp", glass, log));
        registerLink(link("frequency", log, signal));
        registerLink(link("migration", spores, trench));
        registerLink(link("coordinates", log, trench));
    }

    private InvestigationCatalog() {
    }

    public static synchronized void registerNode(InvestigationNode node) {
        requireMutable();
        if (NODES.putIfAbsent(node.id(), node) != null) {
            throw new IllegalArgumentException("duplicate investigation node: " + node.id());
        }
    }

    public static synchronized void registerLink(InvestigationLink link) {
        requireMutable();
        if (LINKS.putIfAbsent(link.id(), link) != null) {
            throw new IllegalArgumentException("duplicate investigation link: " + link.id());
        }
    }

    public static Collection<InvestigationNode> nodes() {
        validate();
        return List.copyOf(NODES.values());
    }

    public static Collection<InvestigationLink> links() {
        validate();
        return List.copyOf(LINKS.values());
    }

    public static Optional<InvestigationNode> node(ResourceLocation id) {
        validate();
        return Optional.ofNullable(NODES.get(id));
    }

    public static Optional<InvestigationLink> link(ResourceLocation id) {
        validate();
        return Optional.ofNullable(LINKS.get(id));
    }

    private static InvestigationLink link(String name, ResourceLocation from, ResourceLocation to) {
        return new InvestigationLink(id(name), from, to, key("link." + name + ".title"),
                key("link." + name + ".detail"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, path);
    }

    private static String key(String suffix) {
        return "gui.aquanaut.investigation." + suffix;
    }

    private static void requireMutable() {
        if (validated) {
            throw new IllegalStateException("investigation catalog is already in use");
        }
    }

    private static synchronized void validate() {
        if (validated) {
            return;
        }
        for (InvestigationLink link : LINKS.values()) {
            if (!NODES.containsKey(link.from()) || !NODES.containsKey(link.to())) {
                throw new IllegalStateException("investigation link has a missing endpoint: " + link.id());
            }
        }
        for (InvestigationNode node : NODES.values()) {
            for (ResourceLocation prerequisite : node.prerequisites()) {
                if (!NODES.containsKey(prerequisite)) {
                    throw new IllegalStateException("investigation node has a missing prerequisite: " + node.id());
                }
            }
        }

        Map<ResourceLocation, List<ResourceLocation>> outgoing = new LinkedHashMap<>();
        Map<ResourceLocation, Integer> indegree = new LinkedHashMap<>();
        for (ResourceLocation id : NODES.keySet()) {
            outgoing.put(id, new ArrayList<>());
            indegree.put(id, 0);
        }
        for (InvestigationLink link : LINKS.values()) {
            outgoing.get(link.from()).add(link.to());
            indegree.put(link.to(), indegree.get(link.to()) + 1);
        }
        ArrayDeque<ResourceLocation> roots = new ArrayDeque<>();
        indegree.forEach((id, degree) -> {
            if (degree == 0) roots.add(id);
        });
        Set<ResourceLocation> visited = new LinkedHashSet<>();
        while (!roots.isEmpty()) {
            ResourceLocation id = roots.removeFirst();
            visited.add(id);
            for (ResourceLocation next : outgoing.get(id)) {
                int degree = indegree.compute(next, (ignored, old) -> old - 1);
                if (degree == 0) roots.add(next);
            }
        }
        if (visited.size() != NODES.size()) {
            throw new IllegalStateException("investigation graph must be acyclic");
        }
        validated = true;
    }
}
