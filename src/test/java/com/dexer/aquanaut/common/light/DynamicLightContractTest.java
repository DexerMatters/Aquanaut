package com.dexer.aquanaut.common.light;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

final class DynamicLightContractTest {
    private static final ResourceLocation PROVIDER = ResourceLocation.fromNamespaceAndPath("aquanaut", "test");
    private static final DynamicLightOwner OWNER = new DynamicLightOwner.Entity(new UUID(1L, 2L));

    @Test
    void identitiesIncludeProviderOwnerAndChannel() {
        DynamicLightId first = new DynamicLightId(PROVIDER, OWNER, 0);
        DynamicLightId same = new DynamicLightId(PROVIDER, OWNER, 0);
        DynamicLightId otherChannel = new DynamicLightId(PROVIDER, OWNER, 1);

        assertEquals(first, same);
        assertEquals(false, first.equals(otherChannel));
    }

    @Test
    void transitionsRejectNegativeDurationsAndChannels() {
        assertThrows(IllegalArgumentException.class, () -> new LightTransition(-1));
        assertThrows(IllegalArgumentException.class, () -> new DynamicLightId(PROVIDER, OWNER, -1));
    }

    @Test
    void searchlightHandoffUsesMakeBeforeBreak() {
        assertEquals(4, LightTransition.FOUR_TICK_HANDOFF.ticks());
        assertEquals(LightTransition.Strategy.MAKE_BEFORE_BREAK,
                LightTransition.FOUR_TICK_HANDOFF.strategy());
        for (int progress = 1; progress < 4; progress++) {
            assertEquals(15, LightTransition.FOUR_TICK_HANDOFF.outgoingLevel(15, progress));
            assertEquals(15, LightTransition.FOUR_TICK_HANDOFF.incomingLevel(15, progress));
        }
        assertEquals(0, LightTransition.FOUR_TICK_HANDOFF.outgoingLevel(15, 4));
        assertEquals(15, LightTransition.FOUR_TICK_HANDOFF.incomingLevel(15, 4));
    }
}
