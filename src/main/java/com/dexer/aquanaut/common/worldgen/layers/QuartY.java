package com.dexer.aquanaut.common.worldgen.layers;

public final class QuartY {
    private QuartY() {
    }

    public static int fromBlock(int blockY) {
        return blockY >> 2;
    }

    public static int toBlock(int quartY) {
        return quartY << 2;
    }

    public static int toBlockCenter(int quartY) {
        return (quartY << 2) + 2;
    }
}
