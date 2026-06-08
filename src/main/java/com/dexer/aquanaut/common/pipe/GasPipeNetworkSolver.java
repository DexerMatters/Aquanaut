package com.dexer.aquanaut.common.pipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class GasPipeNetworkSolver {
    public static final int PIPE_CAPACITY = 16;

    private static final List<AirFlowSolver.Endpoint> ORDER = List.of(
            AirFlowSolver.Endpoint.WEST,
            AirFlowSolver.Endpoint.NORTH,
            AirFlowSolver.Endpoint.DOWN,
            AirFlowSolver.Endpoint.EAST,
            AirFlowSolver.Endpoint.SOUTH,
            AirFlowSolver.Endpoint.UP);

    private GasPipeNetworkSolver() {
    }

    public static FlowResult solve(NodeSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");

        EnumMap<AirFlowSolver.Endpoint, Integer> faceFlows = new EnumMap<>(AirFlowSolver.Endpoint.class);
        int available = 0;

        for (AirFlowSolver.Endpoint endpoint : ORDER) {
            int incoming = Math.max(0, snapshot.incomingPipeFlow().getOrDefault(endpoint, 0));
            if (incoming > 0) {
                faceFlows.merge(endpoint, -incoming, Integer::sum);
                available += incoming;
            }

            int source = Math.max(0, snapshot.sourceInputs().getOrDefault(endpoint, 0));
            if (source > 0) {
                faceFlows.merge(endpoint, -source, Integer::sum);
                available += source;
            }
        }

        for (AirFlowSolver.Endpoint endpoint : ORDER) {
            int demand = Math.max(0, snapshot.sinkDemands().getOrDefault(endpoint, 0));
            if (demand <= 0 || available <= 0) {
                continue;
            }
            int consumed = Math.min(demand, available);
            faceFlows.merge(endpoint, consumed, Integer::sum);
            available -= consumed;
        }

        int forwardBudget = Math.min(available, PIPE_CAPACITY);
        if (forwardBudget > 0) {
            List<AirFlowSolver.Endpoint> exits = forwardExits(snapshot.pipeFaces(), snapshot.incomingPipeFlow());
            if (!exits.isEmpty()) {
                int base = forwardBudget / exits.size();
                int remainder = forwardBudget % exits.size();
                for (int i = 0; i < exits.size(); i++) {
                    int amount = base + (i < remainder ? 1 : 0);
                    if (amount > 0) {
                        faceFlows.merge(exits.get(i), amount, Integer::sum);
                    }
                }
            }
        }

        faceFlows.entrySet().removeIf(entry -> entry.getValue() == 0);
        return new FlowResult(Collections.unmodifiableMap(faceFlows));
    }

    private static List<AirFlowSolver.Endpoint> forwardExits(Set<AirFlowSolver.Endpoint> pipeFaces,
            Map<AirFlowSolver.Endpoint, Integer> incomingPipeFlow) {
        if (pipeFaces.isEmpty()) {
            return List.of();
        }

        List<AirFlowSolver.Endpoint> orderedFaces = new ArrayList<>();
        for (AirFlowSolver.Endpoint endpoint : ORDER) {
            if (pipeFaces.contains(endpoint)) {
                orderedFaces.add(endpoint);
            }
        }
        if (orderedFaces.size() <= 1) {
            return List.copyOf(orderedFaces);
        }

        LinkedHashSet<AirFlowSolver.Endpoint> blockedFaces = new LinkedHashSet<>();
        for (AirFlowSolver.Endpoint endpoint : orderedFaces) {
            if (incomingPipeFlow.getOrDefault(endpoint, 0) > 0) {
                blockedFaces.add(endpoint);
            }
        }

        List<AirFlowSolver.Endpoint> forward = new ArrayList<>();
        for (AirFlowSolver.Endpoint endpoint : orderedFaces) {
            if (!blockedFaces.contains(endpoint)) {
                forward.add(endpoint);
            }
        }
        return forward.isEmpty() ? List.copyOf(orderedFaces) : List.copyOf(forward);
    }

    public record NodeSnapshot(
            Set<AirFlowSolver.Endpoint> pipeFaces,
            Map<AirFlowSolver.Endpoint, Integer> incomingPipeFlow,
            Map<AirFlowSolver.Endpoint, Integer> sourceInputs,
            Map<AirFlowSolver.Endpoint, Integer> sinkDemands) {
        public NodeSnapshot {
            pipeFaces = Set.copyOf(pipeFaces);
            incomingPipeFlow = copyIntMap(incomingPipeFlow);
            sourceInputs = copyIntMap(sourceInputs);
            sinkDemands = copyIntMap(sinkDemands);
        }
    }

    public record FlowResult(Map<AirFlowSolver.Endpoint, Integer> faceFlows) {
        public FlowResult {
            faceFlows = Map.copyOf(faceFlows);
        }
    }

    private static Map<AirFlowSolver.Endpoint, Integer> copyIntMap(Map<AirFlowSolver.Endpoint, Integer> input) {
        EnumMap<AirFlowSolver.Endpoint, Integer> copy = new EnumMap<>(AirFlowSolver.Endpoint.class);
        for (Map.Entry<AirFlowSolver.Endpoint, Integer> entry : input.entrySet()) {
            int amount = entry.getValue() == null ? 0 : entry.getValue();
            if (amount > 0) {
                copy.put(entry.getKey(), amount);
            }
        }
        return copy;
    }
}
