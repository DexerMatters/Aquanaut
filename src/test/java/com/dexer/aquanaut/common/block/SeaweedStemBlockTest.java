package com.dexer.aquanaut.common.block;

import org.junit.jupiter.api.Test;

public final class SeaweedStemBlockTest {
    @Test
    void connectsOnlyToStemClass() {
        assertTrue(SeaweedStemRules.connectsToSameStem(SeaweedStemBlock.class),
                "stem should connect to itself");
        assertFalse(SeaweedStemRules.connectsToSameStem(GasPipeBlock.class),
                "stem should not connect to gas pipes");
    }

    @Test
    void connectsToSeaweedFamilyClasses() {
        assertTrue(SeaweedStemRules.connectsToSeaweedFamily(SeaweedBlock.class),
                "stem should connect to seaweed");
        assertTrue(SeaweedStemRules.connectsToSeaweedFamily(DroopingSeaweedBlock.class),
                "stem should connect to drooping seaweed");
    }

    @Test
    void rejectsUnrelatedClasses() {
        assertFalse(SeaweedStemRules.connectsToSeaweedFamily(StoneBlock.class),
                "stem should not connect to unrelated blocks");
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean value, String message) {
        if (value) {
            throw new AssertionError(message);
        }
    }

    private static final class SeaweedStemBlock {
    }

    private static final class SeaweedBlock {
    }

    private static final class DroopingSeaweedBlock {
    }

    private static final class GasPipeBlock {
    }

    private static final class StoneBlock {
    }
}
