package com.dexer.aquanaut.client.renderer;

import java.util.EnumSet;
import java.util.EnumMap;
import java.util.Map;
import java.util.List;

public final class PipeFlowLayoutTest {
    public static void main(String[] args) {
        PipeFlowLayoutTest test = new PipeFlowLayoutTest();
        test.positiveStraightXUsesSingleWestToEastRoute();
        test.positiveCornerNorthEastTurnsTowardEast();
        test.negativeCornerNorthEastTurnsTowardNorth();
        test.positiveHorizontalCrossUsesStraightPairs();
        test.positiveSplitReusesSingleEntryForMultipleExits();
        test.routesNeverUseUnconnectedEndpoints();
        test.signedFaceFlowsPreferOppositeFaces();
        test.signedFaceFlowsSplitBranchesDeterministically();
    }

    private void positiveStraightXUsesSingleWestToEastRoute() {
        List<PipeFlowLayout.Route> routes = PipeFlowLayout.routes(
                endpoints(PipeFlowLayout.Endpoint.WEST, PipeFlowLayout.Endpoint.EAST), 8);

        assertEquals(1, routes.size(), "positive straight X route count");
        assertRoute(routes.getFirst(), PipeFlowLayout.Endpoint.WEST, PipeFlowLayout.Endpoint.EAST,
                "positive straight X");
    }

    private void positiveCornerNorthEastTurnsTowardEast() {
        List<PipeFlowLayout.Route> routes = PipeFlowLayout.routes(
                endpoints(PipeFlowLayout.Endpoint.NORTH, PipeFlowLayout.Endpoint.EAST), 8);

        assertEquals(1, routes.size(), "positive north-east corner route count");
        assertRoute(routes.getFirst(), PipeFlowLayout.Endpoint.NORTH, PipeFlowLayout.Endpoint.EAST,
                "positive north-east corner");
    }

    private void negativeCornerNorthEastTurnsTowardNorth() {
        List<PipeFlowLayout.Route> routes = PipeFlowLayout.routes(
                endpoints(PipeFlowLayout.Endpoint.NORTH, PipeFlowLayout.Endpoint.EAST), -8);

        assertEquals(1, routes.size(), "negative north-east corner route count");
        assertRoute(routes.getFirst(), PipeFlowLayout.Endpoint.EAST, PipeFlowLayout.Endpoint.NORTH,
                "negative north-east corner");
    }

    private void positiveHorizontalCrossUsesStraightPairs() {
        List<PipeFlowLayout.Route> routes = PipeFlowLayout.routes(
                endpoints(PipeFlowLayout.Endpoint.WEST, PipeFlowLayout.Endpoint.EAST,
                        PipeFlowLayout.Endpoint.NORTH, PipeFlowLayout.Endpoint.SOUTH),
                8);

        assertEquals(2, routes.size(), "positive horizontal cross route count");
        assertContainsRoute(routes, PipeFlowLayout.Endpoint.WEST, PipeFlowLayout.Endpoint.EAST,
                "positive horizontal cross");
        assertContainsRoute(routes, PipeFlowLayout.Endpoint.NORTH, PipeFlowLayout.Endpoint.SOUTH,
                "positive horizontal cross");
    }

    private void positiveSplitReusesSingleEntryForMultipleExits() {
        List<PipeFlowLayout.Route> routes = PipeFlowLayout.routes(
                endpoints(PipeFlowLayout.Endpoint.WEST, PipeFlowLayout.Endpoint.EAST,
                        PipeFlowLayout.Endpoint.SOUTH),
                8);

        assertEquals(2, routes.size(), "positive split route count");
        assertContainsRoute(routes, PipeFlowLayout.Endpoint.WEST, PipeFlowLayout.Endpoint.EAST,
                "positive split");
        assertContainsRoute(routes, PipeFlowLayout.Endpoint.WEST, PipeFlowLayout.Endpoint.SOUTH,
                "positive split");
    }

    private void routesNeverUseUnconnectedEndpoints() {
        for (int mask = 1; mask < 64; mask++) {
            EnumSet<PipeFlowLayout.Endpoint> connections = endpointsFromMask(mask);
            for (int flow : new int[] { -8, 8 }) {
                List<PipeFlowLayout.Route> routes = PipeFlowLayout.routes(connections, flow);
                for (PipeFlowLayout.Route route : routes) {
                    assertEndpointConnected(connections, route.start(), "route start", mask, flow);
                    assertEndpointConnected(connections, route.end(), "route end", mask, flow);
                }
            }
        }
    }

    private void signedFaceFlowsPreferOppositeFaces() {
        List<PipeFlowLayout.RouteFlow> routes = PipeFlowLayout.routes(faceFlows(
                faceFlow(PipeFlowLayout.Endpoint.WEST, -8),
                faceFlow(PipeFlowLayout.Endpoint.EAST, 8)));

        assertEquals(1, routes.size(), "signed straight route count");
        assertRoute(routes.getFirst(), PipeFlowLayout.Endpoint.WEST, PipeFlowLayout.Endpoint.EAST, 8,
                "signed straight");
    }

    private void signedFaceFlowsSplitBranchesDeterministically() {
        List<PipeFlowLayout.RouteFlow> routes = PipeFlowLayout.routes(faceFlows(
                faceFlow(PipeFlowLayout.Endpoint.WEST, -8),
                faceFlow(PipeFlowLayout.Endpoint.EAST, 4),
                faceFlow(PipeFlowLayout.Endpoint.SOUTH, 4)));

        assertEquals(2, routes.size(), "signed split route count");
        assertContainsRoute(routes, PipeFlowLayout.Endpoint.WEST, PipeFlowLayout.Endpoint.EAST, 4,
                "signed split east");
        assertContainsRoute(routes, PipeFlowLayout.Endpoint.WEST, PipeFlowLayout.Endpoint.SOUTH, 4,
                "signed split south");
    }

    private void assertEndpointConnected(EnumSet<PipeFlowLayout.Endpoint> connections, PipeFlowLayout.Endpoint endpoint,
            String label, int mask, int flow) {
        if (endpoint == PipeFlowLayout.Endpoint.CENTER) {
            return;
        }
        if (!connections.contains(endpoint)) {
            throw new AssertionError(
                    label + " " + endpoint + " is not connected for mask " + mask + " flow " + flow);
        }
    }

    private void assertContainsRoute(List<PipeFlowLayout.Route> routes, PipeFlowLayout.Endpoint start,
            PipeFlowLayout.Endpoint end, String label) {
        for (PipeFlowLayout.Route route : routes) {
            if (route.start() == start && route.end() == end) {
                return;
            }
        }
        throw new AssertionError(label + " missing route " + start + " -> " + end + " from " + routes);
    }

    private void assertContainsRoute(List<PipeFlowLayout.RouteFlow> routes, PipeFlowLayout.Endpoint start,
            PipeFlowLayout.Endpoint end, int amount, String label) {
        for (PipeFlowLayout.RouteFlow route : routes) {
            if (route.route().start() == start && route.route().end() == end && route.amount() == amount) {
                return;
            }
        }
        throw new AssertionError(label + " missing route " + start + " -> " + end + " @ " + amount + " from "
                + routes);
    }

    private void assertRoute(PipeFlowLayout.Route route, PipeFlowLayout.Endpoint expectedStart,
            PipeFlowLayout.Endpoint expectedEnd, String label) {
        if (route.start() != expectedStart || route.end() != expectedEnd) {
            throw new AssertionError(label + " expected " + expectedStart + " -> " + expectedEnd
                    + " but got " + route.start() + " -> " + route.end());
        }
    }

    private void assertRoute(PipeFlowLayout.RouteFlow route, PipeFlowLayout.Endpoint expectedStart,
            PipeFlowLayout.Endpoint expectedEnd, int expectedAmount, String label) {
        if (route.route().start() != expectedStart || route.route().end() != expectedEnd
                || route.amount() != expectedAmount) {
            throw new AssertionError(label + " expected " + expectedStart + " -> " + expectedEnd + " @ "
                    + expectedAmount + " but got " + route.route().start() + " -> " + route.route().end()
                    + " @ " + route.amount());
        }
    }

    private void assertEquals(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + " expected " + expected + " but was " + actual);
        }
    }

    @SafeVarargs
    private final EnumSet<PipeFlowLayout.Endpoint> endpoints(PipeFlowLayout.Endpoint... endpoints) {
        EnumSet<PipeFlowLayout.Endpoint> set = EnumSet.noneOf(PipeFlowLayout.Endpoint.class);
        for (PipeFlowLayout.Endpoint endpoint : endpoints) {
            set.add(endpoint);
        }
        return set;
    }

    private EnumSet<PipeFlowLayout.Endpoint> endpointsFromMask(int mask) {
        PipeFlowLayout.Endpoint[] order = {
                PipeFlowLayout.Endpoint.NORTH,
                PipeFlowLayout.Endpoint.SOUTH,
                PipeFlowLayout.Endpoint.EAST,
                PipeFlowLayout.Endpoint.WEST,
                PipeFlowLayout.Endpoint.UP,
                PipeFlowLayout.Endpoint.DOWN
        };
        EnumSet<PipeFlowLayout.Endpoint> set = EnumSet.noneOf(PipeFlowLayout.Endpoint.class);
        for (int i = 0; i < order.length; i++) {
            if ((mask & (1 << i)) != 0) {
                set.add(order[i]);
            }
        }
        return set;
    }

    @SafeVarargs
    private final Map<PipeFlowLayout.Endpoint, Integer> faceFlows(Map.Entry<PipeFlowLayout.Endpoint, Integer>... flows) {
        EnumMap<PipeFlowLayout.Endpoint, Integer> map = new EnumMap<>(PipeFlowLayout.Endpoint.class);
        for (Map.Entry<PipeFlowLayout.Endpoint, Integer> flow : flows) {
            map.put(flow.getKey(), flow.getValue());
        }
        return map;
    }

    private Map.Entry<PipeFlowLayout.Endpoint, Integer> faceFlow(PipeFlowLayout.Endpoint face, int amount) {
        return Map.entry(face, amount);
    }
}
