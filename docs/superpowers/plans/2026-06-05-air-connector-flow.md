# Air Connector Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add shared air-connector abstractions for consumer and source blocks, connect them to gas pipes on their back side, and make pipe flow propagate locally with deterministic branch splitting.

**Architecture:** Introduce a shared `AbstractAirConnectorBlock` contract with two abstract specializations: one for air consumers (`bubble_machine`, `air_supply`) and one for air sources (`air_pump`). Reuse the existing `gas_pipe` node model, but move flow decisions into a small solver that classifies connected faces, resolves straight/corner/branch routes, and assigns flow from sources to sinks in a stable order.

**Tech Stack:** Java 21, NeoForge block registration, block entity sync/NBT, blockstate JSON models, existing client renderer.

---

### Task 1: Add failing flow-solver coverage

**Files:**
- Create: `src/test/java/com/dexer/aquanaut/common/pipe/AirFlowSolverTest.java`
- Create: `src/main/java/com/dexer/aquanaut/common/pipe/AirFlowSolver.java`

- [ ] **Step 1: Write the failing test**

```java
public static void main(String[] args) {
    AirFlowSolverTest test = new AirFlowSolverTest();
    test.cornerRoutesStayOnJoinedFaces();
    test.branchSplitsFlowDeterministically();
    test.sourceAndSinkBlocksAreClassifiedByType();
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mkdir -p /tmp/air-flow-test && javac -d /tmp/air-flow-test src/test/java/com/dexer/aquanaut/common/pipe/AirFlowSolverTest.java`

Expected: fail because `AirFlowSolver` does not exist yet.

- [ ] **Step 3: Write minimal implementation**

Implement `AirFlowSolver` as a pure-Java helper that accepts connected faces plus source/sink ports and returns deterministic route/flow assignments for straight, corner, and branch cases.

- [ ] **Step 4: Run test to verify it passes**

Run: `mkdir -p /tmp/air-flow-test && javac -d /tmp/air-flow-test src/main/java/com/dexer/aquanaut/common/pipe/AirFlowSolver.java src/test/java/com/dexer/aquanaut/common/pipe/AirFlowSolverTest.java && java -ea -cp /tmp/air-flow-test com.dexer.aquanaut.common.pipe.AirFlowSolverTest`

Expected: PASS

### Task 2: Add shared connector block hierarchy

**Files:**
- Create: `src/main/java/com/dexer/aquanaut/common/block/AbstractAirConnectorBlock.java`
- Create: `src/main/java/com/dexer/aquanaut/common/block/AbstractAirConsumerBlock.java`
- Create: `src/main/java/com/dexer/aquanaut/common/block/AbstractAirSourceBlock.java`
- Create: `src/main/java/com/dexer/aquanaut/common/block/AirPumpBlock.java`
- Create: `src/main/java/com/dexer/aquanaut/common/block/BubbleMachineBlock.java`
- Create: `src/main/java/com/dexer/aquanaut/common/block/AirSupplyBlock.java`
- Modify: `src/main/java/com/dexer/aquanaut/core/BlockRegistry.java`

- [ ] **Step 1: Write the failing test**

Add a small Java harness that verifies each concrete block reports the correct connection face and flow sign.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew compileJava`

Expected: fail until the new block classes exist and are wired into the registry.

- [ ] **Step 3: Write minimal implementation**

Implement a shared connector base with a back-side pipe connection contract, then specialize consumer and source behavior in the three concrete blocks.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew compileJava`

Expected: PASS

### Task 3: Wire pipe-node flow propagation

**Files:**
- Modify: `src/main/java/com/dexer/aquanaut/common/block/entity/AbstractPipeBlockEntity.java`
- Modify: `src/main/java/com/dexer/aquanaut/common/block/entity/GasPipeBlockEntity.java`
- Modify: `src/main/java/com/dexer/aquanaut/client/renderer/GasPipeBlockEntityRenderer.java`

- [ ] **Step 1: Write the failing test**

Extend `AirFlowSolverTest` with a case where a branch splits flow toward two joined exits and the renderer route helper follows the same path ordering.

- [ ] **Step 2: Run test to verify it fails**

Run: `mkdir -p /tmp/air-flow-test && javac -d /tmp/air-flow-test src/main/java/com/dexer/aquanaut/common/pipe/AirFlowSolver.java src/test/java/com/dexer/aquanaut/common/pipe/AirFlowSolverTest.java && java -ea -cp /tmp/air-flow-test com.dexer.aquanaut.common.pipe.AirFlowSolverTest`

Expected: fail until the solver returns route splits.

- [ ] **Step 3: Write minimal implementation**

Make each pipe node recompute local flow from adjacent connector blocks and neighboring pipe faces, then feed the resolved route list into the renderer so bubbles stay on joined branches only.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew compileJava`

Expected: PASS

### Task 4: Update blockstates and models for back-side clamps

**Files:**
- Modify: `src/main/resources/assets/aquanaut/models/block/template_station.json`
- Modify: `src/main/resources/assets/aquanaut/models/block/air_supply.json`
- Modify: `src/main/resources/assets/aquanaut/models/block/bubble_machine.json`
- Modify: `src/main/resources/assets/aquanaut/models/block/air_pump.json`
- Modify: `src/main/resources/assets/aquanaut/blockstates/air_supply.json`
- Modify: `src/main/resources/assets/aquanaut/blockstates/bubble_machine.json`
- Modify: `src/main/resources/assets/aquanaut/blockstates/air_pump.json`

- [ ] **Step 1: Write the failing test**

Reload resource JSONs mentally by checking that `air_supply` gains a back-face clamp path while remaining non-rotatable.

- [ ] **Step 2: Run test to verify it fails**

Run: `jq empty src/main/resources/assets/aquanaut/models/block/air_supply.json src/main/resources/assets/aquanaut/blockstates/air_supply.json`

- [ ] **Step 3: Write minimal implementation**

Add back-face clamp support to the shared station template and wire the three blocks to the correct back-side connection model.

- [ ] **Step 4: Run test to verify it passes**

Run: `jq empty src/main/resources/assets/aquanaut/models/block/air_supply.json src/main/resources/assets/aquanaut/blockstates/air_supply.json && ./gradlew compileJava`

---
