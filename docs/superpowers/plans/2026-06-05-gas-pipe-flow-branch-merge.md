# Gas Pipe Flow Branch and Merge Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make gas pipe flow split by downstream demand at branches and accumulate naturally at merges, while capping the resulting throughput by pipe capacity.

**Architecture:** Keep the current connected-component discovery in `GasPipeBlockEntity`, but replace the shortest-path allocation in `GasPipeNetworkSolver` with a two-pass conservation solver. A backward pass computes per-edge demand weights from sinks, a forward pass pushes source supply through the same graph, then each node caps its outgoing total to capacity and redistributes by weight so branches split predictably and merges sum upstream contributions. The renderer will continue consuming the per-face flow map already stored on pipe block entities.

**Tech Stack:** Java 21, NeoForge block entities, existing `GasPipeNetworkSolver` and `PipeFlowLayout` helpers, JUnit-free local harness tests run through `main`.

---

### Task 1: Capture branch and merge regression cases

**Files:**
- Modify: `src/test/java/com/dexer/aquanaut/common/pipe/GasPipeNetworkSolverTest.java`

- [ ] **Step 1: Write the failing test**

```java
private void mergeAccumulatesAndCapsAtPipeCapacity() {
    GasPipeNetworkSolver.NetworkSnapshot snapshot = snapshot(
            links(
                    link("leftSource", AirFlowSolver.Endpoint.EAST, "merge"),
                    link("merge", AirFlowSolver.Endpoint.WEST, "leftSource"),
                    link("rightSource", AirFlowSolver.Endpoint.WEST, "merge"),
                    link("merge", AirFlowSolver.Endpoint.EAST, "rightSource"),
                    link("merge", AirFlowSolver.Endpoint.EAST, "sink"),
                    link("sink", AirFlowSolver.Endpoint.WEST, "merge")),
            terminal("leftSource", AirFlowSolver.Endpoint.WEST, 8, GasPipeNetworkSolver.TerminalRole.SOURCE),
            terminal("rightSource", AirFlowSolver.Endpoint.EAST, 8, GasPipeNetworkSolver.TerminalRole.SOURCE),
            terminal("sink", AirFlowSolver.Endpoint.EAST, 12, GasPipeNetworkSolver.TerminalRole.SINK));

    GasPipeNetworkSolver.FlowResult result = GasPipeNetworkSolver.solve(snapshot);

    assertFaceFlow(result, "leftSource", AirFlowSolver.Endpoint.WEST, -8, "left source input");
    assertFaceFlow(result, "leftSource", AirFlowSolver.Endpoint.EAST, 8, "left source output");
    assertFaceFlow(result, "rightSource", AirFlowSolver.Endpoint.EAST, -8, "right source input");
    assertFaceFlow(result, "rightSource", AirFlowSolver.Endpoint.WEST, 8, "right source output");
    assertFaceFlow(result, "merge", AirFlowSolver.Endpoint.WEST, -8, "merge incoming from left");
    assertFaceFlow(result, "merge", AirFlowSolver.Endpoint.EAST, 8, "merge incoming from right");
    assertFaceFlow(result, "merge", AirFlowSolver.Endpoint.SOUTH, 12, "merge downstream capped output");
    assertFaceFlow(result, "sink", AirFlowSolver.Endpoint.WEST, -12, "sink input");
    assertFaceFlow(result, "sink", AirFlowSolver.Endpoint.EAST, 12, "sink face");
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test --tests com.dexer.aquanaut.common.pipe.GasPipeNetworkSolverTest`
Expected: FAIL because the current shortest-path solver does not accumulate merge throughput by capacity.

- [ ] **Step 3: Add the test to `main`**

```java
test.mergeAccumulatesAndCapsAtPipeCapacity();
```

- [ ] **Step 4: Commit the test-only change**

```bash
git add src/test/java/com/dexer/aquanaut/common/pipe/GasPipeNetworkSolverTest.java
git commit -m "test: cover gas pipe merge throughput"
```

### Task 2: Replace shortest-path allocation with conservation routing

**Files:**
- Modify: `src/main/java/com/dexer/aquanaut/common/pipe/GasPipeNetworkSolver.java`

- [ ] **Step 1: Write the minimal solver rewrite**

```java
public static FlowResult solve(NetworkSnapshot snapshot) {
    Objects.requireNonNull(snapshot, "snapshot");

    Map<String, EnumMap<AirFlowSolver.Endpoint, Integer>> faceFlows = new LinkedHashMap<>();
    Map<String, EnumMap<AirFlowSolver.Endpoint, String>> links = normalizedLinks(snapshot.links());
    Map<String, EnumMap<AirFlowSolver.Endpoint, Integer>> capacityByNode = nodeCapacity(links);
    List<Terminal> terminals = normalizedTerminals(snapshot.terminals());

    Map<String, Integer> supplyByNode = new LinkedHashMap<>();
    Map<String, Integer> demandByNode = new LinkedHashMap<>();
    for (Terminal terminal : terminals) {
        if (terminal.role() == TerminalRole.SOURCE) {
            supplyByNode.merge(terminal.nodeId(), terminal.strength(), Integer::sum);
        } else {
            demandByNode.merge(terminal.nodeId(), terminal.strength(), Integer::sum);
        }
    }

    Map<String, Integer> downstreamDemand = computeDownstreamDemand(links, demandByNode);
    Map<EdgeKey, Integer> edgeFlow = new LinkedHashMap<>();

    for (String nodeId : orderedNodes(links)) {
        int incoming = incomingFlow(nodeId, edgeFlow);
        int supply = supplyByNode.getOrDefault(nodeId, 0);
        int demand = demandByNode.getOrDefault(nodeId, 0);
        int available = incoming + supply;
        int capacity = capacityByNode.getOrDefault(nodeId, new EnumMap<>(AirFlowSolver.Endpoint.class)).size();
        int cappedAvailable = Math.min(available, capacity <= 0 ? available : capacity);

        distributeThroughNode(nodeId, cappedAvailable, demand, downstreamDemand, links, edgeFlow, faceFlows);
        settleTerminalFaces(nodeId, supply, demand, faceFlows);
    }

    pruneZeroEntries(faceFlows);
    return new FlowResult(Collections.unmodifiableMap(faceFlows));
}
```

- [ ] **Step 2: Run compile plus the focused harness**

Run:
`./gradlew compileJava`
`javac -d /tmp/gas-pipe-network-test src/main/java/com/dexer/aquanaut/common/pipe/AirFlowSolver.java src/main/java/com/dexer/aquanaut/common/pipe/GasPipeNetworkSolver.java src/test/java/com/dexer/aquanaut/common/pipe/GasPipeNetworkSolverTest.java && java -ea -cp /tmp/gas-pipe-network-test com.dexer.aquanaut.common.pipe.GasPipeNetworkSolverTest`

Expected: the new merge test passes, and the existing straight/branch/corner tests still pass.

- [ ] **Step 3: Add a focused unit for split weighting**

```java
private void branchSplitsByDownstreamDemand() {
    GasPipeNetworkSolver.NetworkSnapshot snapshot = snapshot(
            links(
                    link("source", AirFlowSolver.Endpoint.WEST, "junction"),
                    link("junction", AirFlowSolver.Endpoint.EAST, "sinkA"),
                    link("junction", AirFlowSolver.Endpoint.SOUTH, "sinkB"),
                    link("sinkA", AirFlowSolver.Endpoint.WEST, "junction"),
                    link("sinkB", AirFlowSolver.Endpoint.NORTH, "junction")),
            terminal("source", AirFlowSolver.Endpoint.WEST, 12, GasPipeNetworkSolver.TerminalRole.SOURCE),
            terminal("sinkA", AirFlowSolver.Endpoint.EAST, 8, GasPipeNetworkSolver.TerminalRole.SINK),
            terminal("sinkB", AirFlowSolver.Endpoint.SOUTH, 4, GasPipeNetworkSolver.TerminalRole.SINK));

    GasPipeNetworkSolver.FlowResult result = GasPipeNetworkSolver.solve(snapshot);

    assertFaceFlow(result, "junction", AirFlowSolver.Endpoint.EAST, 8, "preferred larger demand branch");
    assertFaceFlow(result, "junction", AirFlowSolver.Endpoint.SOUTH, 4, "smaller demand branch");
}
```

- [ ] **Step 4: Commit the solver change**

```bash
git add src/main/java/com/dexer/aquanaut/common/pipe/GasPipeNetworkSolver.java
git commit -m "feat: conserve gas pipe flow through branches and merges"
```

### Task 3: Verify renderer stays aligned with the new per-face flows

**Files:**
- Modify: `src/main/java/com/dexer/aquanaut/client/renderer/PipeFlowLayout.java`
- Modify: `src/main/java/com/dexer/aquanaut/client/renderer/GasPipeBlockEntityRenderer.java`
- Test: `src/test/java/com/dexer/aquanaut/client/renderer/PipeFlowLayoutTest.java`

- [ ] **Step 1: Keep the renderer consuming the solved face map**

```java
Map<PipeFlowLayout.Endpoint, Integer> faceFlows = new EnumMap<>(PipeFlowLayout.Endpoint.class);
for (Map.Entry<Direction, Integer> entry : blockEntity.getFaceFlows().entrySet()) {
    faceFlows.merge(endpoint(entry.getKey()), entry.getValue(), Integer::sum);
}
List<PipeFlowLayout.RouteFlow> routes = PipeFlowLayout.routes(faceFlows);
```

- [ ] **Step 2: Run the existing renderer harness**

Run: `javac -d /tmp/pipe-flow-layout-test src/main/java/com/dexer/aquanaut/client/renderer/PipeFlowLayout.java src/test/java/com/dexer/aquanaut/client/renderer/PipeFlowLayoutTest.java && java -ea -cp /tmp/pipe-flow-layout-test com.dexer.aquanaut.client.renderer.PipeFlowLayoutTest`

Expected: PASS, with no changes needed unless the new solver exposes a new route shape.

- [ ] **Step 3: Commit renderer-only adjustments if needed**

```bash
git add src/main/java/com/dexer/aquanaut/client/renderer/PipeFlowLayout.java src/main/java/com/dexer/aquanaut/client/renderer/GasPipeBlockEntityRenderer.java src/test/java/com/dexer/aquanaut/client/renderer/PipeFlowLayoutTest.java
git commit -m "feat: align gas pipe bubble rendering with solved face flows"
```

