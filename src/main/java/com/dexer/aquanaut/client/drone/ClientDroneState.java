package com.dexer.aquanaut.client.drone;

import javax.annotation.Nullable;

import com.dexer.aquanaut.common.entity.SubmarineDroneEntity;

/**
 * The client's view of the flying session.
 *
 * <p>
 * Everything the HUD draws, and every decision the input handler and the feed renderer make, is read
 * from here, so there is exactly one place that decides whether the player is flying a drone and
 * which one. It is written only by {@link ClientDroneEvents}.
 *
 * <p>
 * "Flying" means the controller is in hand and the drone it points at is live — not that the player
 * has lost their own camera. The player keeps their own view throughout and watches the drone
 * through the feed panel, or with their own eyes if it is close enough to see.
 */
public final class ClientDroneState {

    @Nullable
    private static SubmarineDroneEntity drone;
    private static boolean flying;

    /** The command the player is holding this tick. */
    private static int input;

    private ClientDroneState() {
    }

    // ------------------------------------------------------------------
    // read side
    // ------------------------------------------------------------------

    /** Whether the player is currently flying a drone. */
    public static boolean isFlying() {
        return flying;
    }

    /** The drone being flown, or {@code null}. */
    @Nullable
    public static SubmarineDroneEntity drone() {
        return drone;
    }

    /** The control bits the player is holding. */
    public static int input() {
        return input;
    }

    // ------------------------------------------------------------------
    // write side
    // ------------------------------------------------------------------

    static void setSession(@Nullable SubmarineDroneEntity flownDrone, int mask) {
        drone = flownDrone;
        flying = flownDrone != null;
        input = mask;
    }

    static void stop() {
        drone = null;
        flying = false;
        input = 0;
    }
}
