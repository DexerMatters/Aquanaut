package com.dexer.aquanaut.common.worldgen;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class JellyJungleStemForestConnections {
    private JellyJungleStemForestConnections() {
    }

    static ConnectionMap forPlan(JellyJungleStemForestPlanner.Plan plan) {
        Map<JellyJungleStemForestPlanner.Pos, ConnectionState> states = new LinkedHashMap<>();
        Set<JellyJungleStemForestPlanner.Pos> stems = plan.stemPositions();
        for (JellyJungleStemForestPlanner.Pos pos : stems) {
            states.put(pos, new ConnectionState(
                    stems.contains(pos.offset(0, 0, -1)),
                    stems.contains(pos.offset(0, 0, 1)),
                    stems.contains(pos.offset(1, 0, 0)),
                    stems.contains(pos.offset(-1, 0, 0)),
                    stems.contains(pos.offset(0, 1, 0)),
                    stems.contains(pos.offset(0, -1, 0))));
        }
        return new ConnectionMap(Map.copyOf(states));
    }

    record ConnectionMap(Map<JellyJungleStemForestPlanner.Pos, ConnectionState> states) {
    }

    record ConnectionState(boolean north, boolean south, boolean east, boolean west, boolean up, boolean down) {
    }
}
