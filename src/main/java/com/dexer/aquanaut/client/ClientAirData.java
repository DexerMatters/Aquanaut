package com.dexer.aquanaut.client;

/**
 * Client-side cache of the local player's extra air supply, synced from the
 * server.
 */
public final class ClientAirData {

    private static int extraAir = 0;
    private static int maxExtraAir = 0;

    private ClientAirData() {
    }

    public static void set(int extra, int max) {
        extraAir = extra;
        maxExtraAir = max;
    }

    public static int getExtraAir() {
        return extraAir;
    }

    public static int getMaxExtraAir() {
        return maxExtraAir;
    }
}
