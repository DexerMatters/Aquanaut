package com.dexer.aquanaut.common.light;

/** Small renderer/server-independent light calculations kept easy to unit-test. */
public final class DynamicLightMath {
    private DynamicLightMath() {
    }

    public static int outgoingLevel(int level, int progress, int total) {
        if (total <= 0 || progress >= total) {
            return 0;
        }
        return Math.round(level * (1.0F - (float) progress / total));
    }

    public static int incomingLevel(int level, int progress, int total) {
        if (total <= 0) {
            return level;
        }
        return Math.round(level * ((float) progress / total));
    }

    public static int falloffLevel(int level, double distance, double radius) {
        if (distance >= radius || radius <= 0.0D) {
            return 0;
        }
        return Math.round(level * (float) Math.min(1.0D, 1.0D - distance / radius));
    }

    public static int applyPackedBlockLevel(int vanillaPackedLight, int dynamicLevel) {
        int vanillaLevel = (vanillaPackedLight >> 4) & 0xF;
        if (dynamicLevel <= vanillaLevel) {
            return vanillaPackedLight;
        }
        return (vanillaPackedLight & ~0xF0) | (Math.min(15, dynamicLevel) << 4);
    }
}
