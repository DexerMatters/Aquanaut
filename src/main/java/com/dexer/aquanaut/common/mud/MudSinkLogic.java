package com.dexer.aquanaut.common.mud;

import net.minecraft.world.phys.Vec3;

/** Pure calculations for entities moving through mud. */
public final class MudSinkLogic {
    private MudSinkLogic() {
    }

    public static int nextContactTicks(int previousTicks, boolean wasContinuousContact) {
        if (!wasContinuousContact) {
            return 1;
        }
        return Math.min(Math.max(0, previousTicks), MudZoneConfig.SUFFOCATION_DELAY_TICKS) + 1;
    }

    public static int submergedTicks(int previousTicks, long lastTick, long currentTick, boolean submerged) {
        if (!submerged) {
            return 0;
        }
        if (lastTick == currentTick) {
            return previousTicks;
        }
        return nextContactTicks(previousTicks, lastTick == currentTick - 1);
    }

    public static boolean shouldSuffocate(int contactTicks) {
        return contactTicks >= MudZoneConfig.SUFFOCATION_DELAY_TICKS;
    }

    public static boolean shouldDamage(int contactTicks, long gameTime, long lastDamageGameTime) {
        return shouldSuffocate(contactTicks)
                && (lastDamageGameTime < 0
                        || gameTime - lastDamageGameTime >= MudZoneConfig.SUFFOCATION_DAMAGE_INTERVAL_TICKS);
    }

    public static Vec3 applySinking(Vec3 velocity) {
        return new Vec3(slowHorizontal(velocity.x), sinkingY(velocity.y), slowHorizontal(velocity.z));
    }

    public static double slowHorizontal(double velocity) {
        return velocity * MudZoneConfig.SINK_HORIZONTAL_SLOWDOWN;
    }

    public static double sinkingY(double velocity) {
        // Limit falling rather than accelerating it; allow swimming/jumping out.
        return velocity > 0 ? velocity : -MudZoneConfig.SINK_SPEED;
    }
}
