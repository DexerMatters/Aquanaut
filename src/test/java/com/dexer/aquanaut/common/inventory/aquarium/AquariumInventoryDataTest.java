package com.dexer.aquanaut.common.inventory.aquarium;

public final class AquariumInventoryDataTest {

    public static void main(String[] args) {
        AquariumInventoryDataTest test = new AquariumInventoryDataTest();
        test.roundTripsEntityEntriesThroughSyncSerialization();
        test.emptySerializationFallsBackToEmptyAquarium();
    }

    private void roundTripsEntityEntriesThroughSyncSerialization() {
        AquariumInventoryData data = new AquariumInventoryData(java.util.List.of(
                new AquariumFishEntry("aquanaut:sardine", 5.0F, "{Health:5.0f}"),
                AquariumFishEntry.EMPTY));

        AquariumInventoryData decoded = AquariumInventoryData.deserialize(data.serialize());

        AquariumFishEntry first = decoded.entryAt(0)
                .orElseThrow(() -> new AssertionError("expected serialized aquarium entry at slot 0"));
        if (!"aquanaut:sardine".equals(first.entityId())) {
            throw new AssertionError("expected sardine id after round trip but was " + first.entityId());
        }
        if (!"{Health:5.0f}".equals(first.entityData())) {
            throw new AssertionError("expected entity snapshot to round trip unchanged but was " + first.entityData());
        }
        if (Math.abs(first.health() - 5.0F) > 0.01F) {
            throw new AssertionError("expected health 5.0 but was " + first.health());
        }
    }

    private void emptySerializationFallsBackToEmptyAquarium() {
        AquariumInventoryData decoded = AquariumInventoryData.deserialize("");
        if (decoded.entryAt(0).isPresent()) {
            throw new AssertionError("empty serialized aquarium should decode to empty data");
        }
    }
}
