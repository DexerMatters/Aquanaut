# Lightning Entity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a short-lived abstract lightning entity with a damage hitbox, directional rotation, branching bolt rendering, and a vanilla-like white texture.

**Architecture:** Introduce an abstract lightning base that owns lifetime, orientation, branch parameters, synced state, and server-side damage application. Add a concrete `LightningEntity` with defaults for immediate use, then render it client-side as layered white bolt segments with deterministic branches so the shape stays stable between client and server.

**Tech Stack:** Java, NeoForge entity registration, Minecraft client renderer API, resource PNG assets, Gradle compile verification

---

### Task 1: Add lightning entities and registry entries

**Files:**
- Create: `src/main/java/com/dexer/aquanaut/common/entity/AbstractLightningEntity.java`
- Create: `src/main/java/com/dexer/aquanaut/common/entity/LightningEntity.java`
- Modify: `src/main/java/com/dexer/aquanaut/core/EntityRegistry.java`

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/dexer/aquanaut/common/entity/LightningEntityTest.java`:

```java
package com.dexer.aquanaut.common.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LightningEntityTest {
    @Test
    void lightningEntityClassExists() {
        assertTrue(LightningEntity.class.getSimpleName().contains("Lightning"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.dexer.aquanaut.common.entity.LightningEntityTest`

Expected: FAIL because `LightningEntity` does not exist yet.

- [ ] **Step 3: Write minimal implementation**

Implement an abstract entity that:

```java
public abstract class AbstractLightningEntity extends Entity {
    // Defines duration, direction, bolt length, branch count, branch decay, damage, and owner.
    // Server-side tick:
    // - age out after a short lifetime
    // - build a swept AABB around the bolt path
    // - damage living entities intersecting the bolt hitbox
    // - do not collide with blocks or push entities
}
```

Implement a concrete default:

```java
public final class LightningEntity extends AbstractLightningEntity {
    public LightningEntity(EntityType<? extends LightningEntity> type, Level level) {
        super(type, level);
    }
}
```

Add a registry entry:

```java
public static final DeferredHolder<EntityType<?>, EntityType<LightningEntity>> LIGHTNING = ENTITIES.register(
        "lightning",
        () -> EntityType.Builder
                .<LightningEntity>of(LightningEntity::new, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .clientTrackingRange(8)
                .updateInterval(1)
                .build("lightning"));
```

Register any attributes only if the entity type requires them.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.dexer.aquanaut.common.entity.LightningEntityTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/dexer/aquanaut/common/entity/LightningEntityTest.java src/main/java/com/dexer/aquanaut/common/entity/AbstractLightningEntity.java src/main/java/com/dexer/aquanaut/common/entity/LightningEntity.java src/main/java/com/dexer/aquanaut/core/EntityRegistry.java
git commit -m "feat: add lightning entity base"
```

### Task 2: Add client renderer and registration

**Files:**
- Create: `src/main/java/com/dexer/aquanaut/client/renderer/LightningRenderer.java`
- Modify: `src/main/java/com/dexer/aquanaut/client/ClientModEvents.java`

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/dexer/aquanaut/client/renderer/LightningRendererTest.java`:

```java
package com.dexer.aquanaut.client.renderer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LightningRendererTest {
    @Test
    void lightningRendererClassExists() {
        assertTrue(LightningRenderer.class.getSimpleName().contains("Lightning"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.dexer.aquanaut.client.renderer.LightningRendererTest`

Expected: FAIL because the renderer does not exist yet.

- [ ] **Step 3: Write minimal implementation**

Implement a renderer that:

```java
public final class LightningRenderer extends EntityRenderer<LightningEntity> {
    // Render a layered bolt using a white texture.
    // Rotate by entity pitch/yaw so the bolt can point in any direction.
    // Draw the main trunk plus branches that get shorter and thinner as they get farther out.
}
```

Register it in `ClientModEvents`:

```java
event.registerEntityRenderer(EntityRegistry.LIGHTNING.get(), LightningRenderer::new);
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.dexer.aquanaut.client.renderer.LightningRendererTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/dexer/aquanaut/client/renderer/LightningRendererTest.java src/main/java/com/dexer/aquanaut/client/renderer/LightningRenderer.java src/main/java/com/dexer/aquanaut/client/ClientModEvents.java
git commit -m "feat: render lightning entity"
```

### Task 3: Add the lightning texture asset and verify the project compiles

**Files:**
- Create: `src/main/resources/assets/aquanaut/textures/entity/lightning.png`

- [ ] **Step 1: Write the failing test**

Add a resource existence check in `src/test/java/com/dexer/aquanaut/resources/LightningTextureTest.java`:

```java
package com.dexer.aquanaut.resources;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class LightningTextureTest {
    @Test
    void lightningTextureExistsOnClasspath() {
        assertNotNull(LightningTextureTest.class.getResource(
                "/assets/aquanaut/textures/entity/lightning.png"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.dexer.aquanaut.resources.LightningTextureTest`

Expected: FAIL because the texture is not present yet.

- [ ] **Step 3: Write minimal implementation**

Add a white lightning sprite at `src/main/resources/assets/aquanaut/textures/entity/lightning.png`.
The image should be a narrow white bolt with subtle branching and alpha falloff, designed to support additive/bright rendering.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.dexer.aquanaut.resources.LightningTextureTest`

Expected: PASS.

- [ ] **Step 5: Verify the whole project still compiles**

Run: `./gradlew compileJava`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/test/java/com/dexer/aquanaut/resources/LightningTextureTest.java src/main/resources/assets/aquanaut/textures/entity/lightning.png
git commit -m "feat: add lightning texture"
```
