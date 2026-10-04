package com.dexer.aquanaut.common.mud;

/**
 * Central tuning values for the mud zone content group.
 *
 * <p>All gameplay numbers for mud terrain and mud creatures live here so balance
 * changes and tests do not have to chase constants across block, entity, and
 * worldgen classes.</p>
 */
public final class MudZoneConfig {

    private MudZoneConfig() {
    }

    // --- Mud block: sinking and suffocation ---

    /** Downward drift applied per tick while an entity is inside mud. */
    public static final double SINK_SPEED = 0.012D;

    /** Horizontal velocity multiplier applied while inside mud. */
    public static final double SINK_HORIZONTAL_SLOWDOWN = 0.55D;

    /** Ticks an entity may stay submerged in mud before suffocation begins. */
    public static final int SUFFOCATION_DELAY_TICKS = 100;

    /** Damage dealt every {@link #SUFFOCATION_DAMAGE_INTERVAL_TICKS} once drowning. */
    public static final float SUFFOCATION_DAMAGE = 1.0F;

    /** Interval in ticks between suffocation damage hits. */
    public static final int SUFFOCATION_DAMAGE_INTERVAL_TICKS = 20;

    // --- Nutrient-rich mud ---

    /** Nausea duration applied when an entity steps on nutrient-rich mud. */
    public static final int NAUSEA_DURATION_TICKS = 60;

    /** Nausea effect amplifier (0 = level I). */
    public static final int NAUSEA_AMPLIFIER = 0;

    /** Chance per random tick to emit a green bubble particle. */
    public static final float NUTRIENT_BUBBLE_CHANCE = 0.35F;

    // --- Parasitic mud ---

    /** Minimum silverfish spawned when parasitic mud is mined. */
    public static final int PARASITIC_MIN_SPAWN = 1;

    /** Maximum silverfish spawned when parasitic mud is mined. */
    public static final int PARASITIC_MAX_SPAWN = 2;

    /** Bubble particle chance for parasitic mud; lower than plain mud to hint at danger. */
    public static final float PARASITIC_BUBBLE_CHANCE = 0.08F;

    // --- Hermit crab ---

    /** Ticks the hermit crab stays shelled after being hurt. */
    public static final int SHELL_DURATION_TICKS = 60;

    /** Damage multiplier while shelled (halves incoming damage). */
    public static final float SHELL_DAMAGE_MULTIPLIER = 0.5F;

    /** Chance to upgrade to a larger shell when near shell blocks. */
    public static final float SHELL_UPGRADE_CHANCE = 0.10F;

    /** Largest shell tier a hermit crab can grow into; the model scales with the tier. */
    public static final int SHELL_MAX_SIZE = 3;

    /** Extra max health granted per shell tier. */
    public static final double SHELL_HEALTH_PER_TIER = 2.0D;

    /** Radius in blocks scanned for shell blocks when rolling an upgrade. */
    public static final double SHELL_SEARCH_RADIUS = 4.0D;

    // --- Mud silverfish ---

    /** Light level at or above which the mud silverfish flees. */
    public static final int BRIGHT_LIGHT_THRESHOLD = 12;

    // --- Ambush fish ---

    /** Range at which the ambush fish bursts out of hiding. */
    public static final double AMBUSH_TRIGGER_RANGE = 1.0D;

    // --- Garden eel ---

    /** Cruise speed of the stationary garden eel. */
    public static final double GARDEN_EEL_CRUISE_SPEED = 0.01D;
}
