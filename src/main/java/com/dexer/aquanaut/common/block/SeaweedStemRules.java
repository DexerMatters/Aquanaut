package com.dexer.aquanaut.common.block;

import java.util.Set;

final class SeaweedStemRules {
    private static final String STEM_CLASS = "SeaweedStemBlock";
    private static final Set<String> SEAWEED_FAMILY = Set.of(
            "SeaweedBlock",
            "DroopingSeaweedBlock");

    private SeaweedStemRules() {
    }

    static boolean connectsToSameStem(Class<?> otherClass) {
        return otherClass != null && STEM_CLASS.equals(otherClass.getSimpleName());
    }

    static boolean connectsToSeaweedFamily(Class<?> neighborClass) {
        return neighborClass != null && SEAWEED_FAMILY.contains(neighborClass.getSimpleName());
    }
}
