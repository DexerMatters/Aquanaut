package com.dexer.aquanaut.common.pipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GasPipeNetworkSolverTest {
    public static void main(String[] args) {
        GasPipeNetworkSolverTest test = new GasPipeNetworkSolverTest();
        test.sourceFeedsSinglePipeExit();
        test.incomingFlowDoesNotBounceBackWhenAnotherExitExists();
        test.branchSplitsEquallyAcrossForwardExits();
        test.localSinkConsumesBeforeForwarding();
        test.mergeSumsIncomingFlowAndCapsOutput();
        test.distributedBranchKeepsSourceOutputConstant();
        test.distributedCornerTurnsWithoutGlobalRouting();
    }

    private void sourceFeedsSinglePipeExit() {
        GasPipeNetworkSolver.FlowResult result = GasPipeNetworkSolver.solve(new GasPipeNetworkSolver.NodeSnapshot(
                EnumSet.of(AirFlowSolver.Endpoint.EAST),
                Map.of(),
                Map.of(AirFlowSolver.Endpoint.WEST, 8),
                Map.of()));

        assertFaceFlow(result, AirFlowSolver.Endpoint.WEST, -8, "source intake");
        assertFaceFlow(result, AirFlowSolver.Endpoint.EAST, 8, "source output");
    }

    private void incomingFlowDoesNotBounceBackWhenAnotherExitExists() {
        GasPipeNetworkSolver.FlowResult result = GasPipeNetworkSolver.solve(new GasPipeNetworkSolver.NodeSnapshot(
                EnumSet.of(AirFlowSolver.Endpoint.WEST, AirFlowSolver.Endpoint.EAST),
                Map.of(AirFlowSolver.Endpoint.WEST, 8),
                Map.of(),
                Map.of()));

        assertFaceFlow(result, AirFlowSolver.Endpoint.WEST, -8, "incoming face remains entry");
        assertFaceFlow(result, AirFlowSolver.Endpoint.EAST, 8, "flow leaves through opposite face");
    }

    private void branchSplitsEquallyAcrossForwardExits() {
        GasPipeNetworkSolver.FlowResult result = GasPipeNetworkSolver.solve(new GasPipeNetworkSolver.NodeSnapshot(
                EnumSet.of(AirFlowSolver.Endpoint.WEST, AirFlowSolver.Endpoint.EAST, AirFlowSolver.Endpoint.SOUTH),
                Map.of(AirFlowSolver.Endpoint.WEST, 8),
                Map.of(),
                Map.of()));

        assertFaceFlow(result, AirFlowSolver.Endpoint.WEST, -8, "branch input");
        assertFaceFlow(result, AirFlowSolver.Endpoint.EAST, 4, "branch east split");
        assertFaceFlow(result, AirFlowSolver.Endpoint.SOUTH, 4, "branch south split");
    }

    private void localSinkConsumesBeforeForwarding() {
        GasPipeNetworkSolver.FlowResult result = GasPipeNetworkSolver.solve(new GasPipeNetworkSolver.NodeSnapshot(
                EnumSet.of(AirFlowSolver.Endpoint.WEST, AirFlowSolver.Endpoint.EAST),
                Map.of(AirFlowSolver.Endpoint.WEST, 12),
                Map.of(),
                Map.of(AirFlowSolver.Endpoint.SOUTH, 5)));

        assertFaceFlow(result, AirFlowSolver.Endpoint.WEST, -12, "sink case input");
        assertFaceFlow(result, AirFlowSolver.Endpoint.SOUTH, 5, "sink consumes locally");
        assertFaceFlow(result, AirFlowSolver.Endpoint.EAST, 7, "remaining flow goes forward");
    }

    private void mergeSumsIncomingFlowAndCapsOutput() {
        GasPipeNetworkSolver.FlowResult result = GasPipeNetworkSolver.solve(new GasPipeNetworkSolver.NodeSnapshot(
                EnumSet.of(AirFlowSolver.Endpoint.WEST, AirFlowSolver.Endpoint.EAST, AirFlowSolver.Endpoint.SOUTH),
                Map.of(
                        AirFlowSolver.Endpoint.WEST, 10,
                        AirFlowSolver.Endpoint.EAST, 10),
                Map.of(),
                Map.of()));

        assertFaceFlow(result, AirFlowSolver.Endpoint.WEST, -10, "merge west input");
        assertFaceFlow(result, AirFlowSolver.Endpoint.EAST, -10, "merge east input");
        assertFaceFlow(result, AirFlowSolver.Endpoint.SOUTH, 16, "merge output capped by pipe capacity");
    }

    private void distributedBranchKeepsSourceOutputConstant() {
        Network network = network(
                link("source", AirFlowSolver.Endpoint.EAST, "junction"),
                link("junction", AirFlowSolver.Endpoint.WEST, "source"),
                link("junction", AirFlowSolver.Endpoint.EAST, "sinkA"),
                link("sinkA", AirFlowSolver.Endpoint.WEST, "junction"),
                link("junction", AirFlowSolver.Endpoint.SOUTH, "sinkB"),
                link("sinkB", AirFlowSolver.Endpoint.NORTH, "junction"),
                source("source", AirFlowSolver.Endpoint.WEST, 12),
                sink("sinkA", AirFlowSolver.Endpoint.EAST, 6),
                sink("sinkB", AirFlowSolver.Endpoint.SOUTH, 6));

        Map<String, GasPipeNetworkSolver.FlowResult> result = simulate(network, 4);

        assertFaceFlow(result.get("source"), AirFlowSolver.Endpoint.WEST, -12, "distributed source input");
        assertFaceFlow(result.get("source"), AirFlowSolver.Endpoint.EAST, 12, "distributed source output stays constant");
        assertFaceFlow(result.get("junction"), AirFlowSolver.Endpoint.EAST, 6, "distributed east branch");
        assertFaceFlow(result.get("junction"), AirFlowSolver.Endpoint.SOUTH, 6, "distributed south branch");
    }

    private void distributedCornerTurnsWithoutGlobalRouting() {
        Network network = network(
                link("source", AirFlowSolver.Endpoint.EAST, "corner"),
                link("corner", AirFlowSolver.Endpoint.WEST, "source"),
                link("corner", AirFlowSolver.Endpoint.SOUTH, "sink"),
                link("sink", AirFlowSolver.Endpoint.NORTH, "corner"),
                source("source", AirFlowSolver.Endpoint.WEST, 8),
                sink("sink", AirFlowSolver.Endpoint.SOUTH, 8));

        Map<String, GasPipeNetworkSolver.FlowResult> result = simulate(network, 3);

        assertFaceFlow(result.get("corner"), AirFlowSolver.Endpoint.WEST, -8, "corner receives from source");
        assertFaceFlow(result.get("corner"), AirFlowSolver.Endpoint.SOUTH, 8, "corner forwards to sink");
    }

    private Map<String, GasPipeNetworkSolver.FlowResult> simulate(Network network, int steps) {
        Map<String, GasPipeNetworkSolver.FlowResult> current = new LinkedHashMap<>();
        for (String nodeId : network.nodeIds()) {
            current.put(nodeId, new GasPipeNetworkSolver.FlowResult(Collections.emptyMap()));
        }

        for (int step = 0; step < steps; step++) {
            Map<String, GasPipeNetworkSolver.FlowResult> next = new LinkedHashMap<>();
            for (String nodeId : network.nodeIds()) {
                EnumMap<AirFlowSolver.Endpoint, Integer> incomingPipeFlow = new EnumMap<>(AirFlowSolver.Endpoint.class);
                for (Map.Entry<AirFlowSolver.Endpoint, String> link : network.links().getOrDefault(nodeId, emptyLinks()).entrySet()) {
                    GasPipeNetworkSolver.FlowResult neighborState = current.get(link.getValue());
                    int amount = neighborState.faceFlows().getOrDefault(link.getKey().opposite(), 0);
                    if (amount > 0) {
                        incomingPipeFlow.put(link.getKey(), amount);
                    }
                }

                next.put(nodeId, GasPipeNetworkSolver.solve(new GasPipeNetworkSolver.NodeSnapshot(
                        EnumSet.copyOf(network.links().getOrDefault(nodeId, emptyLinks()).keySet()),
                        incomingPipeFlow,
                        network.sources().getOrDefault(nodeId, emptyInts()),
                        network.sinks().getOrDefault(nodeId, emptyInts()))));
            }
            current = next;
        }

        return current;
    }

    private void assertFaceFlow(GasPipeNetworkSolver.FlowResult result, AirFlowSolver.Endpoint face,
            int expected, String label) {
        int actual = result.faceFlows().getOrDefault(face, 0);
        if (actual != expected) {
            throw new AssertionError(label + " expected " + expected + " but was " + actual);
        }
    }

    private Network network(Object... parts) {
        Map<String, EnumMap<AirFlowSolver.Endpoint, String>> links = new LinkedHashMap<>();
        Map<String, EnumMap<AirFlowSolver.Endpoint, Integer>> sources = new LinkedHashMap<>();
        Map<String, EnumMap<AirFlowSolver.Endpoint, Integer>> sinks = new LinkedHashMap<>();
        for (Object part : parts) {
            if (part instanceof Link link) {
                links.computeIfAbsent(link.from(), key -> new EnumMap<>(AirFlowSolver.Endpoint.class))
                        .put(link.face(), link.to());
            } else if (part instanceof Terminal terminal) {
                Map<String, EnumMap<AirFlowSolver.Endpoint, Integer>> target = terminal.kind() == TerminalKind.SOURCE
                        ? sources
                        : sinks;
                target.computeIfAbsent(terminal.nodeId(), key -> new EnumMap<>(AirFlowSolver.Endpoint.class))
                        .put(terminal.face(), terminal.amount());
            }
        }
        return new Network(links, sources, sinks);
    }

    private Link link(String from, AirFlowSolver.Endpoint face, String to) {
        return new Link(from, face, to);
    }

    private Terminal source(String nodeId, AirFlowSolver.Endpoint face, int amount) {
        return new Terminal(nodeId, face, amount, TerminalKind.SOURCE);
    }

    private Terminal sink(String nodeId, AirFlowSolver.Endpoint face, int amount) {
        return new Terminal(nodeId, face, amount, TerminalKind.SINK);
    }

    private EnumMap<AirFlowSolver.Endpoint, String> emptyLinks() {
        return new EnumMap<>(AirFlowSolver.Endpoint.class);
    }

    private EnumMap<AirFlowSolver.Endpoint, Integer> emptyInts() {
        return new EnumMap<>(AirFlowSolver.Endpoint.class);
    }

    private record Network(
            Map<String, EnumMap<AirFlowSolver.Endpoint, String>> links,
            Map<String, EnumMap<AirFlowSolver.Endpoint, Integer>> sources,
            Map<String, EnumMap<AirFlowSolver.Endpoint, Integer>> sinks) {
        Set<String> nodeIds() {
            Set<String> nodeIds = new java.util.LinkedHashSet<>();
            nodeIds.addAll(links.keySet());
            nodeIds.addAll(sources.keySet());
            nodeIds.addAll(sinks.keySet());
            return nodeIds;
        }
    }

    private enum TerminalKind {
        SOURCE,
        SINK
    }

    private record Link(String from, AirFlowSolver.Endpoint face, String to) {
    }

    private record Terminal(String nodeId, AirFlowSolver.Endpoint face, int amount, TerminalKind kind) {
    }
}
