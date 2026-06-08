package com.dexer.aquanaut.client;

import com.dexer.aquanaut.common.inventory.aquarium.AquariumInventoryData;

public final class ClientAquariumData {

    private static AquariumInventoryData aquarium = AquariumInventoryData.EMPTY;

    private ClientAquariumData() {
    }

    public static void setFromData(AquariumInventoryData data) {
        aquarium = data;
    }

    public static void setFromSerialized(String serializedData) {
        aquarium = AquariumInventoryData.deserialize(serializedData);
    }

    public static AquariumInventoryData getAquarium() {
        return aquarium;
    }
}
