package com.dexer.aquanaut.common.investigation;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InvestigationGraphTest {
    @Test
    void exampleBoardIsADirectedAcyclicGraphWithValidEndpoints() {
        Set<ResourceLocation> nodes = new HashSet<>();
        Map<ResourceLocation, Integer> indegree = new HashMap<>();
        Map<ResourceLocation, Set<ResourceLocation>> outgoing = new HashMap<>();
        InvestigationCatalog.nodes().forEach(node -> {
            nodes.add(node.id());
            indegree.put(node.id(), 0);
            outgoing.put(node.id(), new HashSet<>());
        });

        InvestigationCatalog.links().forEach(link -> {
            assertTrue(nodes.contains(link.from()), link.id() + " source");
            assertTrue(nodes.contains(link.to()), link.id() + " target");
            assertFalse(link.from().equals(link.to()), link.id() + " self loop");
            outgoing.get(link.from()).add(link.to());
            indegree.compute(link.to(), (ignored, degree) -> degree + 1);
        });

        ArrayDeque<ResourceLocation> roots = new ArrayDeque<>();
        indegree.forEach((id, degree) -> {
            if (degree == 0) roots.add(id);
        });
        int visited = 0;
        while (!roots.isEmpty()) {
            ResourceLocation current = roots.removeFirst();
            visited++;
            for (ResourceLocation next : outgoing.get(current)) {
                int degree = indegree.compute(next, (ignored, old) -> old - 1);
                if (degree == 0) roots.add(next);
            }
        }
        assertEquals(nodes.size(), visited, "example investigation graph contains a cycle");
    }
}
