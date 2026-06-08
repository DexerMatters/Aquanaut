package com.dexer.aquanaut.common.inventory.aquarium;

public final class AquariumModelFootprintTest {

    public static void main(String[] args) {
        AquariumModelFootprintTest test = new AquariumModelFootprintTest();
        test.usesModelLengthAndHeightToBuildSlotFootprints();
        test.usesLateralLengthInsteadOfBodyThickness();
        test.appliesBoneRotationWhenBuildingFootprints();
        test.missingModelsDoNotProduceCatalogEntries();
    }

    private void usesModelLengthAndHeightToBuildSlotFootprints() {
        assertFootprint("octopus", 2, 3);
        assertFootprint("anglerfish", 3, 2);
        assertFootprint("helicoprion", 5, 3);
        assertFootprint("manta_ray", 3, 1);
    }

    private void usesLateralLengthInsteadOfBodyThickness() {
        AquariumModelFootprint.Footprint footprint = AquariumModelFootprint.resolve("aquanaut",
                        "geo/creeporpedo.geo.json")
                .orElseThrow(() -> new AssertionError("missing aquarium model footprint for creeporpedo"));
        if (footprint.length() <= footprint.width()) {
            throw new AssertionError("creeporpedo length should exceed body thickness in lateral footprint math");
        }
        if (footprint.gridWidth() != 2 || footprint.gridHeight() != 1) {
            throw new AssertionError("unexpected creeporpedo footprint: expected 2x1 but was "
                    + footprint.gridWidth() + "x" + footprint.gridHeight());
        }
    }

    private void appliesBoneRotationWhenBuildingFootprints() {
        AquariumModelFootprint.Footprint footprint = AquariumModelFootprint.resolve("aquanaut",
                        "geo/donutfish.geo.json")
                .orElseThrow(() -> new AssertionError("missing aquarium model footprint for donutfish"));
        if (footprint.gridWidth() != 3 || footprint.gridHeight() != 2) {
            throw new AssertionError("unexpected donutfish footprint: expected 3x2 but was "
                    + footprint.gridWidth() + "x" + footprint.gridHeight());
        }
        if (footprint.length() <= footprint.width()) {
            throw new AssertionError("donutfish footprint should use the rotated lateral span as length");
        }
    }

    private void missingModelsDoNotProduceCatalogEntries() {
        boolean present = AquariumModelFootprint.resolve("aquanaut", "geo/does_not_exist.geo.json").isPresent();
        if (present) {
            throw new AssertionError("missing geo resources should not create aquarium footprints");
        }
    }

    private void assertFootprint(String path, int expectedWidth, int expectedHeight) {
        AquariumModelFootprint.Footprint footprint = AquariumModelFootprint.resolve("aquanaut",
                        "geo/" + path + ".geo.json")
                .orElseThrow(() -> new AssertionError("missing aquarium model footprint for " + path));
        if (footprint.gridWidth() != expectedWidth || footprint.gridHeight() != expectedHeight) {
            throw new AssertionError("unexpected footprint for " + path + ": expected "
                    + expectedWidth + "x" + expectedHeight + " but was "
                    + footprint.gridWidth() + "x" + footprint.gridHeight());
        }
    }
}
