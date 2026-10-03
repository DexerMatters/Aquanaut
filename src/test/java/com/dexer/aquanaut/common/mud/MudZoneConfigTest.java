package com.dexer.aquanaut.common.mud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class MudZoneConfigTest {

    @Test
    void suffocationDelayIsFiveSeconds() {
        assertEquals(100, MudZoneConfig.SUFFOCATION_DELAY_TICKS, "5 seconds = 100 ticks");
    }

    @Test
    void nauseaLastsThreeSeconds() {
        assertEquals(60, MudZoneConfig.NAUSEA_DURATION_TICKS, "3 seconds = 60 ticks");
    }

    @Test
    void shellDurationIsThreeSeconds() {
        assertEquals(60, MudZoneConfig.SHELL_DURATION_TICKS, "3 seconds = 60 ticks");
    }

    @Test
    void shellHalvesIncomingDamage() {
        assertEquals(0.5F, MudZoneConfig.SHELL_DAMAGE_MULTIPLIER, "shelled damage is halved");
    }

    @Test
    void shellUpgradeChanceIsTenPercent() {
        assertEquals(0.10F, MudZoneConfig.SHELL_UPGRADE_CHANCE, "10% shell upgrade chance");
    }

    @Test
    void parasiticSpawnRangeIsOneToTwo() {
        assertEquals(1, MudZoneConfig.PARASITIC_MIN_SPAWN);
        assertEquals(2, MudZoneConfig.PARASITIC_MAX_SPAWN);
        assertTrue(MudZoneConfig.PARASITIC_MIN_SPAWN <= MudZoneConfig.PARASITIC_MAX_SPAWN,
                "min spawn must not exceed max spawn");
    }

    @Test
    void parasiticMudIsLessBubblyThanPlainMud() {
        assertTrue(MudZoneConfig.PARASITIC_BUBBLE_CHANCE < MudZoneConfig.NUTRIENT_BUBBLE_CHANCE,
                "parasitic mud should emit fewer particles");
    }
}
