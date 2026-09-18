package com.dexer.aquanaut.common.inventory.aquarium;

/**
 * Harness check that every geometry file produced by the models.zip pipeline parses with the game's
 * own aquarium footprint reader.
 *
 * <p>
 * The catalog sizes aquarium slots from these footprints, so a geometry file that the reader cannot
 * parse silently falls back to the entity hitbox. This catches conversion slips (unknown parents,
 * malformed cubes) before they reach a world.
 */
public final class NewSpeciesFootprintTest {

    private static final String[] SPECIES = {
            "vamprey", "oresucker", "flagellonautilus", "skeleton_carp", "golden_carp", "silver_carp",
            "gentlefish", "slimmy", "ionfin", "opticichthus", "gemini_jellyfish", "ecofish",
            "pale_abyss_hydra", "three_headed_shark"
    };

    public static void main(String[] args) {
        new NewSpeciesFootprintTest().allConvertedModelsProduceFootprints();
        System.out.println("NewSpeciesFootprintTest PASSED (" + SPECIES.length + " models)");
    }

    private void allConvertedModelsProduceFootprints() {
        for (String slug : SPECIES) {
            AquariumModelFootprint.Footprint footprint = AquariumModelFootprint
                    .resolve("aquanaut", "geo/" + slug + ".geo.json")
                    .orElseThrow(() -> new AssertionError("missing footprint for " + slug));

            if (footprint.length() <= 0.0F || footprint.width() <= 0.0F || footprint.height() <= 0.0F) {
                throw new AssertionError(slug + " has a degenerate footprint: " + footprint);
            }
            if (footprint.gridWidth() < 1 || footprint.gridHeight() < 1) {
                throw new AssertionError(slug + " has an invalid grid: "
                        + footprint.gridWidth() + "x" + footprint.gridHeight());
            }
            System.out.printf("  %-20s %.2fL x %.2fW x %.2fH  grid=%dx%d%n", slug, footprint.length(),
                    footprint.width(), footprint.height(), footprint.gridWidth(), footprint.gridHeight());
        }
    }
}
