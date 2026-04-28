package com.dexer.aquanaut.client;

/**
 * Client-side cache of the local player's effective extra-air capacity, synced
 * from the server.
 */
public final class ClientAirData {

    private static int maxExtraAir = 0;

    private ClientAirData() {
    }

    public static void setMaxExtraAir(int max) {
        maxExtraAir = Math.max(0, max);
    }

    public static int getMaxExtraAir() {
        return maxExtraAir;
    }
}
