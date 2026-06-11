# Contributing to Aquanaut

Aquanaut is a NeoForge mod for Minecraft 1.21.1. This guide covers the project-specific setup and conventions contributors should know before making changes.

## Baseline

- Use Java 21.
- Use the Gradle wrapper from the repo root.
- The current dependency set is NeoForge 21.1.233, GeckoLib 4.8.4, and TerraBlender 4.1.0.8.
- The mod id is `aquanaut` and the main package is `com.dexer.aquanaut`.

If Gradle cannot find Java 21 on your machine, check the `org.gradle.java.home` entry in `gradle.properties` and override it locally if needed.

## Project layout

- `src/main/java/com/dexer/aquanaut/core` contains registries and startup wiring for blocks, items, entities, menus, sounds, and other game objects.
- `src/main/java/com/dexer/aquanaut/common` contains gameplay logic shared by client and server.
- `src/main/java/com/dexer/aquanaut/client` contains client-only renderers, HUD code, screens, and models.
- `src/main/java/com/dexer/aquanaut/network` contains custom packet payloads and sync logic.
- `src/main/java/com/dexer/aquanaut/mixin` contains vanilla injections used to change game behavior.
- `src/main/resources/assets/aquanaut` contains models, textures, blockstates, language files, and other client assets.
- `src/main/resources/data/aquanaut` contains data-driven content such as recipes, notebook entries, loot tables, and tags.
- `src/main/templates` contains templated metadata files expanded by the Gradle build.
- `src/generated/resources` is included in the main resources set and is where data generation output is expected to land.
- `src/test/java` contains both JUnit tests and small standalone harnesses with `main` methods.
- `scripts` contains asset-generation helpers and other one-off tooling used by the project.
- `net/minecraft` and `net/neoforged` at the repo root are local source shims for API compatibility. Treat them as support files, not ordinary mod code.

## Build and run

- `./gradlew compileJava` checks that the main mod sources compile.
- `./gradlew compileTestJava` checks that the test sources compile.
- `./gradlew test` runs the JUnit test suite.
- `./gradlew test --tests com.dexer.aquanaut.SomeTest` runs one JUnit test class.
- For harness-style tests with `main`, run the compiled class directly from `build/classes/java/main` and `build/classes/java/test`.
- Use the NeoForge client, server, game test server, and data-generation run configurations for in-game verification.

The build generates mixin refmap and mod metadata outputs. Edit the sources that feed those generated files, not the generated files themselves.

## Testing style

Aquanaut uses two test styles:

- JUnit 5 for data-heavy or assertion-friendly tests.
- Small `main`-based harnesses for math, layout, and other dependency-light logic.

Keep tests deterministic. Favor temporary directories, in-memory data, and explicit assertions over game runtime dependence where possible.

When you fix a bug, add or update a regression test before considering the change done.

## Content conventions

- Register new game objects through the relevant registry class in `src/main/java/com/dexer/aquanaut/core`.
- Keep shared gameplay logic in `common`; do not pull client-only classes into shared code.
- Keep renderers, screens, HUD code, and other visual behavior in `client`.
- Keep packet encoding/decoding and handler logic together in `network`.
- Keep mixins narrow and document why the injection point is needed.
- If you add or rename a mixin, update `src/main/resources/aquanaut.mixins.json`.
- Edit `src/main/templates/META-INF/neoforge.mods.toml` and the Gradle properties that feed it, not the generated metadata under `build/`.

## Assets and data

- The authoritative art license is `LICENSE-ART`: CC BY-NC-SA 4.0.
- Code is licensed under `LICENSE`: LGPL-3.0-only.
- Keep user-facing strings in both `en_us.json` and `zh_cn.json` in sync unless a translation is intentionally incomplete.
- If you change generated textures or model workflows, check `scripts/` before inventing a new manual process.
- If you touch notebook, recipe, loot table, or tag content, make sure the corresponding JSON data stays consistent with the gameplay code and tests.

## Practical workflow

1. Make the smallest change that solves the problem.
2. Add or update a test if the change affects logic.
3. Run `./gradlew compileJava` and `./gradlew test`.
4. Verify the relevant client or data run configuration if the change affects rendering, UI, assets, or generated data.
5. Keep commits focused so review stays manageable.

## Notes for contributors

- Do not commit build outputs.
- Do not hand-edit generated refmaps or generated metadata.
- If you regenerate resources, include the command or script used in your pull request notes.
- If you are unsure whether a file is source or generated, check the build scripts first.

