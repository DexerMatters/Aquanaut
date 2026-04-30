package com.dexer.aquanaut.client;

/**
 * Client-side cache of the local player's extra-air capacity and current
 * extra air, synced from the server.
 */
public final class ClientAirData {

    private static int maxExtraAir = 0;
    private static int currentExtraAir = 0;

    private ClientAirData() {
    }

    public static void setMaxExtraAir(int max) {
        maxExtraAir = Math.max(0, max);
    }

    public static int getMaxExtraAir() {
        return maxExtraAir;
    }

    public static void setCurrentExtraAir(int extra) {
        currentExtraAir = Math.max(0, extra);
    }

    public static int getCurrentExtraAir() {
        return currentExtraAir;
    }
}
