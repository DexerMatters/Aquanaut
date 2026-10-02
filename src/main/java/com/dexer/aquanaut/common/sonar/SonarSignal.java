package com.dexer.aquanaut.common.sonar;

import java.util.Locale;

/**
 * The four kinds of return the portable sonar can hear, and everything the code needs to know about
 * how each one is shown.
 *
 * <p>
 * A sonar does not photograph anything: every return is the same dimensionless echo, and what makes
 * one worth reading is the <em>timbre</em> of it — how hard the surface rang, how long it kept
 * ringing, and how much of the pulse it threw back. The instrument therefore sorts every contact
 * into one of these four voices, and the colour, the sound, the shape of the blip and the weight of
 * the text all follow from the voice rather than from the block or the creature that produced it.
 * That is what lets a new mineral, a new fish or a new kind of cavern be added by pointing a tag at
 * it, with nothing in the client needing to learn about it.
 *
 * <p>
 * The order is the order of the legend on the scope, from the softest and most common voice to the
 * rarest, and it is written into the wire format: a return carries an ordinal, so this enum must
 * only ever grow at the end.
 *
 * <p>
 * Deliberately free of Minecraft types: the colours, the ranges and the ranks are plain numbers, so
 * the whole table can be checked by a unit test with nothing on the classpath.
 */
public enum SonarSignal {

    /**
     * Something alive. A body is mostly water, so it answers with a soft, rounded reflection that
     * spreads rather than rings — the quietest and the shortest of the four.
     */
    BIOLOGICAL(0x5FE0A0, 1.0F, 1.5F),

    /**
     * Crystal and ore. A lattice rings: the reflection comes back bright, hard-edged and almost
     * immediate, with the harmonics of a struck glass.
     */
    MINERAL(0x82E4FF, 1.35F, 4.5F),

    /**
     * A void behind a face — a cave, a crevice, a cavity in the rock. Most of the pulse goes in and
     * never comes back, so the return is a low, hollow, drawn-out note.
     */
    CAVITY(0xE3A75C, 0.8F, 5.5F),

    /**
     * A shadow too large to be anything ordinary: the long echo. Nothing in the shipped world
     * produces it yet beyond very large bodies — see {@link SonarScan} for the hook — but the voice,
     * the colour and the range exist so that the content which will can simply declare itself.
     */
    ABYSS(0xC77DFF, 1.6F, 3.0F);

    /** The signal's colour, packed {@code 0xRRGGBB}, as the scope and the wavefront draw it. */
    public final int colour;

    /**
     * How hard the voice rings, as a multiplier on the strength the scan measured. A crystal reads
     * louder than the muscle next to it and a cavity reads quieter, which is what stops a cave and a
     * fish of the same size plotting as the same contact.
     */
    public final float gain;

    /**
     * How far apart two returns of this voice may be and still be read as one contact, in blocks.
     * A vein, a shoal and a cave are each one thing on the scope however many rays found them.
     */
    public final float cluster;

    SonarSignal(int colour, float gain, float cluster) {
        this.colour = colour;
        this.gain = gain;
        this.cluster = cluster;
    }

    /** Red, green and blue as the renderer wants them, each {@code 0..1}. */
    public float red() {
        return ((this.colour >> 16) & 0xFF) / 255.0F;
    }

    public float green() {
        return ((this.colour >> 8) & 0xFF) / 255.0F;
    }

    public float blue() {
        return (this.colour & 0xFF) / 255.0F;
    }

    /**
     * The sound the return plays as it comes back, in the instrument's own namespace. This and the
     * colour are the whole of what a voice is: the instrument has no name for any of them, because
     * naming one would be telling the diver what they are looking at, which is the one thing a
     * sounder must never do — it reports water that answered, not what answered.
     */
    public String soundName() {
        return "sonar_echo_" + name().toLowerCase(Locale.ROOT);
    }

    /**
     * The voice for a wire ordinal, or {@link #BIOLOGICAL} for one this build does not know. A
     * server running a newer version of the mod must never be able to crash a client that has only
     * heard of four voices.
     */
    public static SonarSignal byOrdinal(int ordinal) {
        SonarSignal[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : BIOLOGICAL;
    }
}
