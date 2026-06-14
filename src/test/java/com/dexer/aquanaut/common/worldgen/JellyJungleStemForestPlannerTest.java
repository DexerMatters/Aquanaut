package com.dexer.aquanaut.common.worldgen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public final class JellyJungleStemForestPlannerTest {
    @Test
    void plannerCreatesATallRootedTrunkBeforeBranching() {
        JellyJungleStemForestPlanner.Plan plan = JellyJungleStemForestPlanner.plan(1234L);

        assertTrue(plan.trunkHeight() >= 12, "stem forest should grow a tall main trunk");
        for (int y = 0; y < plan.trunkHeight(); y++) {
            assertTrue(plan.stemPositions().contains(JellyJungleStemForestPlanner.originAt(y)),
                    "main trunk should stay continuous from the root");
        }
    }

    @Test
    void plannerKeepsHorizontalBranchesLongEnoughToReadAsBranches() {
        JellyJungleStemForestPlanner.Plan plan = JellyJungleStemForestPlanner.plan(9876L);

        boolean sawHorizontal = false;
        for (JellyJungleStemForestPlanner.Segment segment : plan.segments()) {
            if (segment.axis() == JellyJungleStemForestPlanner.Axis.VERTICAL) {
                continue;
            }

            sawHorizontal = true;
            assertTrue(segment.length() >= 2,
                    "horizontal branches should be at least two blocks long");
        }

        assertTrue(sawHorizontal, "stem forest should include horizontal branches");
        assertTrue(!plan.foliageTargets().isEmpty(), "stem forest should carry seaweed foliage");
    }

    @Test
    void stemPlacementDerivesDirectionalConnectionsFromNeighbors() {
        JellyJungleStemForestPlanner.Plan plan = JellyJungleStemForestPlanner.plan(1234L);
        JellyJungleStemForestConnections.ConnectionMap connections = JellyJungleStemForestConnections.forPlan(plan);

        JellyJungleStemForestConnections.ConnectionState root = connections.states()
                .get(JellyJungleStemForestPlanner.originAt(0));
        JellyJungleStemForestConnections.ConnectionState trunk = connections.states()
                .get(JellyJungleStemForestPlanner.originAt(1));

        assertTrue(root.up(), "root stem should connect upward into the trunk");
        assertTrue(!root.down(), "root stem should not claim a downward stem connection");
        assertTrue(trunk.up(), "interior trunk stem should connect upward");
        assertTrue(trunk.down(), "interior trunk stem should connect downward");

        boolean foundHorizontalBranchConnection = connections.states().values().stream()
                .anyMatch(state -> state.north() || state.south() || state.east() || state.west());
        assertTrue(foundHorizontalBranchConnection,
                "branching stem states should carry horizontal connections");
    }
}
