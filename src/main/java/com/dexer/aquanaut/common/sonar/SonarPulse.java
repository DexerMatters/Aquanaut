package com.dexer.aquanaut.common.sonar;

import net.minecraft.util.Mth;

/**
 * The portable sonar's timing, in one place.
 *
 * <p>
 * A ping is a single event with a shape: a wavefront leaves the transducer and runs out to the edge
 * of its range at the speed of sound in water, each contact it passes throws a copy of it back, and
 * the copies run home again. Everything the instrument does — the ring on the scope, the shell in
 * the water, the moment an echo sounds, the light on the display, the cooldown — is a function of
 * how old the ping is, so all of it is derived here rather than kept as state on either side. The
 * server sends one ping and forgets it; the client re-derives the whole animation from the age.
 *
 * <p>
 * Deliberately free of Minecraft and of any renderer, so the shape of a ping can be checked by a
 * plain unit test and can never disagree between the picture in the water and the picture on the
 * scope.
 */
public final class SonarPulse {

    /**
     * How far the pulse is listened for, in blocks. Twenty is a diver's own visibility, near enough
     * that the ring on the water and the ring on the scope are the same picture, and short enough
     * that the answer arrives while the question is still interesting.
     */
    public static final double RANGE = 20.0D;

    /**
     * How long the instrument takes to recharge, in ticks. Five seconds is long enough that a ping
     * is a decision rather than a habit, and it is deliberately a little longer than the readout
     * below, so the picture is always finished with before the next one can be fired.
     */
    public static final int COOLDOWN_TICKS = 100;

    /** How long the wavefront takes to cross {@link #RANGE}. */
    public static final int TRAVEL_TICKS = 26;

    /** How long an echo takes to run from a contact back to the transducer. */
    public static final int RETURN_TICKS = 8;

    /** How long the finished picture is held on the scope before it starts to clear. */
    public static final int HOLD_TICKS = 40;

    /** How long the picture takes to fade once the hold is up. */
    public static final int FADE_TICKS = 16;

    /**
     * How long the whole ping lasts, from the flash to the last ghost of the readout. Shorter than
     * {@link #COOLDOWN_TICKS}: the instrument is dark for a moment before it can speak again.
     */
    public static final int DISPLAY_TICKS = TRAVEL_TICKS + RETURN_TICKS + HOLD_TICKS + FADE_TICKS;

    /** How fast the wavefront travels, in blocks per tick. */
    public static final double SPEED = RANGE / TRAVEL_TICKS;

    private SonarPulse() {
    }

    /** How far through its run the wavefront is, {@code 0..1}. */
    public static float sweep(double ageTicks) {
        return Mth.clamp((float) (ageTicks / TRAVEL_TICKS), 0.0F, 1.0F);
    }

    /** The radius of the wavefront, in blocks. */
    public static double wavefrontRadius(double ageTicks) {
        return RANGE * sweep(ageTicks);
    }

    /** When the wavefront passes a contact, in ticks after the ping. */
    public static double arrival(double distance) {
        return Mth.clamp(distance, 0.0D, RANGE) / SPEED;
    }

    /** Whether the wavefront has reached a contact yet. */
    public static boolean hasReached(double ageTicks, double distance) {
        return ageTicks >= arrival(distance);
    }

    /**
     * How far an echo has run back towards the transducer, {@code 0..1}, or {@code 0} while the
     * wavefront is still on its way out. An echo that is nearly home reads brighter and shorter on
     * the scope and louder in the water, so this is the value that drives all of that.
     */
    public static float echoProgress(double ageTicks, double distance) {
        double home = ageTicks - arrival(distance);
        return Mth.clamp((float) (home / RETURN_TICKS), 0.0F, 1.0F);
    }

    /**
     * How visible the readout is, {@code 0..1}: up as the ping fires, held while it runs, and then
     * a slow fade. The scope never snaps on and never snaps off.
     */
    public static float readout(double ageTicks) {
        if (ageTicks < 0.0D || ageTicks > DISPLAY_TICKS) {
            return 0.0F;
        }
        float rise = Mth.clamp((float) (ageTicks / 3.0D), 0.0F, 1.0F);
        float fall = ageTicks <= TRAVEL_TICKS + RETURN_TICKS + HOLD_TICKS
                ? 1.0F
                : Mth.clamp((float) ((DISPLAY_TICKS - ageTicks) / FADE_TICKS), 0.0F, 1.0F);
        return rise * fall;
    }

    /** Whether a ping of this age is still worth drawing anywhere at all. */
    public static boolean isAlive(double ageTicks) {
        return ageTicks >= 0.0D && ageTicks <= DISPLAY_TICKS;
    }

    /**
     * The wavefront's own brightness, {@code 0..1}: it flashes into being at the transducer,
     * brightens slightly as it clears the instrument, and then spends itself as it goes — the
     * inverse-square thinning every sound in water pays.
     */
    public static float waveGlow(double ageTicks) {
        if (!isAlive(ageTicks)) {
            return 0.0F;
        }
        float sweep = sweep(ageTicks);
        // Half a tick of head start, so the shell exists on the frame the pulse is fired rather
        // than fading up out of nothing a tick later.
        float birth = Mth.clamp((float) ((ageTicks + 0.5D) / 2.5D), 0.0F, 1.0F);
        float spend = 1.0F - 0.72F * sweep * sweep;
        return birth * spend;
    }

    /**
     * How much of a contact's strength is left, {@code 0..1}, as the echo runs home. Used for the
     * size and the brightness of the return on the scope, and for the volume of the echo: a
     * reflection loses everything it has to the distance it came from.
     */
    public static float echoGlow(double ageTicks, double distance, float strength) {
        if (!hasReached(ageTicks, distance)) {
            return 0.0F;
        }
        float range = 1.0F - (float) (distance / RANGE) * 0.55F;
        float decay = 1.0F - 0.35F * echoProgress(ageTicks, distance);
        return Mth.clamp(strength * range * decay, 0.0F, 1.0F);
    }
}
