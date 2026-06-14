# Seaweed Stem Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a new water-only `seaweed_stem` block that uses pipe-style directional connections, connects to the existing seaweed family, and renders as a sleek organic 6x6 stem.

**Architecture:** Implement the stem as a dedicated `AbstractPipeBlock` subclass so it inherits the existing six-direction connection state and waterlogging behavior. Override connection checks so it only links to `seaweed_stem` and the seaweed family blocks, and override the collision/voxel shape so the stem reads as a narrower organic column rather than the default pipe footprint.

**Tech Stack:** Java, NeoForge block registration, Minecraft JSON block models, Python texture generation, JUnit 5.

---

### Task 1: Lock the stem connection rules with a focused test

**Files:**
- Create: `src/test/java/com/dexer/aquanaut/common/block/SeaweedStemBlockTest.java`
- Create: `src/main/java/com/dexer/aquanaut/common/block/SeaweedStemRules.java`

- [ ] **Step 1: Write the failing test**

```java
package com.dexer.aquanaut.common.block;

import org.junit.jupiter.api.Test;

public final class SeaweedStemBlockTest {
    @Test
    void connectsOnlyToStemClass() {
        assertTrue(SeaweedStemRules.connectsToSameStem(SeaweedStemBlock.class),
                "stem should connect to itself");
        assertFalse(SeaweedStemRules.connectsToSameStem(GasPipeBlock.class),
                "stem should not connect to gas pipes");
    }

    @Test
    void connectsToSeaweedFamilyClasses() {
        assertTrue(SeaweedStemRules.connectsToSeaweedFamily(SeaweedBlock.class),
                "stem should connect to seaweed");
        assertTrue(SeaweedStemRules.connectsToSeaweedFamily(DroopingSeaweedBlock.class),
                "stem should connect to drooping seaweed");
    }

    @Test
    void rejectsUnrelatedClasses() {
        assertFalse(SeaweedStemRules.connectsToSeaweedFamily(StoneBlock.class),
                "stem should not connect to unrelated blocks");
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean value, String message) {
        if (value) {
            throw new AssertionError(message);
        }
    }

    private static final class SeaweedStemBlock {
    }

    private static final class SeaweedBlock {
    }

    private static final class DroopingSeaweedBlock {
    }

    private static final class GasPipeBlock {
    }

    private static final class StoneBlock {
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test --tests com.dexer.aquanaut.common.block.SeaweedStemBlockTest`
Expected: compilation failure because `SeaweedStemRules` does not exist yet.

- [ ] **Step 3: Implement the minimum code to make it pass**

Add `SeaweedStemBlock` and the overridden connection rules in the production code.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew test --tests com.dexer.aquanaut.common.block.SeaweedStemBlockTest`
Expected: PASS.

### Task 2: Register the block and wire its assets

**Files:**
- Modify: `src/main/java/com/dexer/aquanaut/core/BlockRegistry.java`
- Modify: `src/main/java/com/dexer/aquanaut/core/ItemRegistry.java`
- Create: `src/main/resources/assets/aquanaut/blockstates/seaweed_stem.json`
- Create: `src/main/resources/assets/aquanaut/models/block/seaweed_stem_center.json`
- Create: `src/main/resources/assets/aquanaut/models/block/seaweed_stem_arm.json`
- Create: `src/main/resources/assets/aquanaut/models/item/seaweed_stem.json`
- Create: `src/main/resources/data/aquanaut/loot_table/blocks/seaweed_stem.json`
- Modify: `src/main/resources/assets/aquanaut/lang/en_us.json`
- Modify: `src/main/resources/assets/aquanaut/lang/zh_cn.json`
- Create: `scripts/GenerateAquanautSeaweedStem.py`
- Create: `src/main/resources/assets/aquanaut/textures/block/seaweed_stem.png`

- [ ] **Step 1: Add the new block/item registrations**

- [ ] **Step 2: Create the blockstate/model/item model/loot JSON**

- [ ] **Step 3: Add the texture generator and output the stem texture**

- [ ] **Step 4: Add the language strings and creative tab entry**

- [ ] **Step 5: Run the image generator and verify the texture is binary alpha only**

Run: `python3 scripts/GenerateAquanautSeaweedStem.py`

- [ ] **Step 6: Run a full compile**

Run: `./gradlew compileJava`
Expected: exit code `0`.

### Task 3: Sanity check the final wiring

**Files:**
- Review: `src/main/java/com/dexer/aquanaut/common/block/SeaweedStemBlock.java`
- Review: `src/main/resources/assets/aquanaut/models/block/seaweed_stem_center.json`
- Review: `src/main/resources/assets/aquanaut/models/block/seaweed_stem_arm.json`
- Review: `src/main/resources/assets/aquanaut/blockstates/seaweed_stem.json`

- [ ] **Step 1: Re-run the stem behavior test**

Run: `./gradlew test --tests com.dexer.aquanaut.common.block.SeaweedStemBlockTest`
Expected: PASS.

- [ ] **Step 2: Inspect the generated stem texture**

Open `src/main/resources/assets/aquanaut/textures/block/seaweed_stem.png` and confirm it reads as a sleek 6x6 organic stem.

- [ ] **Step 3: Confirm no unrelated files changed**

Run: `git status --short`
Expected: only the intended seaweed stem files and the existing seaweed-friendliness edits are modified.
