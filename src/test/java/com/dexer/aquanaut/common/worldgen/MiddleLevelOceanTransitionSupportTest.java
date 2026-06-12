package com.dexer.aquanaut.common.worldgen;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class MiddleLevelOceanTransitionSupportTest {
    @Test
    void currentChunkSupportStillRequiresARealFullySubmergedDeepOceanCell() {
        assertTrue(MiddleLevelOceanTransitionSupport.supportsCurrentChunkCell(minecraft("deep_ocean"), 16));
        assertFalse(MiddleLevelOceanTransitionSupport.supportsCurrentChunkCell(minecraft("deep_ocean"), 15));
        assertFalse(MiddleLevelOceanTransitionSupport.supportsCurrentChunkCell(minecraft("ocean"), 16));
    }

    @Test
    void haloSupportUsesOnlyDeepOceanBiomeMembership() {
        assertTrue(MiddleLevelOceanTransitionSupport.supportsHaloCell(minecraft("deep_ocean")));
        assertTrue(MiddleLevelOceanTransitionSupport.supportsHaloCell(minecraft("deep_cold_ocean")));
        assertFalse(MiddleLevelOceanTransitionSupport.supportsHaloCell(minecraft("ocean")));
        assertFalse(MiddleLevelOceanTransitionSupport.supportsHaloCell(minecraft("plains")));
        assertFalse(MiddleLevelOceanTransitionSupport.supportsHaloCell(null));
    }

    private static ResourceLocation minecraft(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }
}
