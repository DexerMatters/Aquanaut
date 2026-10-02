package com.dexer.aquanaut.common.sonar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import com.dexer.aquanaut.core.ParticleRegistry;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * One mote of disturbed water, carrying the colour it was stirred up by.
 *
 * <p>
 * A sonar pulse is sound, and sound is invisible: what a diver actually sees when one goes past is
 * the water itself sulking — the silt lifted off the bottom and the micro-bubbles shaken out of
 * solution, briefly, in a shell. That is a small, soft, almost motionless speck, and there is no
 * vanilla particle that is one: the bubbles rise and pop, the sparks are made of magic, and the
 * smoke is black and belongs in air. So the instrument has its own.
 *
 * <p>
 * The colour rides on the particle rather than on the type because it is the whole point of the
 * instrument: the same speck has to be able to come back cyan off a vein, green off something
 * alive, amber out of a hollow and violet out of the deep, and four separate particle types to say
 * so would be four registrations, four definitions and four textures for one idea.
 */
public record SonarMoteOptions(int colour) implements ParticleOptions {

    public static final MapCodec<SonarMoteOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(Codec.INT.fieldOf("colour").forGetter(SonarMoteOptions::colour))
            .apply(instance, SonarMoteOptions::new));

    public static final StreamCodec<ByteBuf, SonarMoteOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SonarMoteOptions::colour,
            SonarMoteOptions::new);

    /** A mote in the pulse's own colour, for the wavefront itself. */
    public static final SonarMoteOptions PULSE = new SonarMoteOptions(0x8FE6FF);

    /** A mote carrying one voice's colour. */
    public static SonarMoteOptions of(SonarSignal signal) {
        return new SonarMoteOptions(signal.colour);
    }

    public float red() {
        return ((this.colour >> 16) & 0xFF) / 255.0F;
    }

    public float green() {
        return ((this.colour >> 8) & 0xFF) / 255.0F;
    }

    public float blue() {
        return (this.colour & 0xFF) / 255.0F;
    }

    @Override
    public ParticleType<?> getType() {
        return ParticleRegistry.SONAR_MOTE.get();
    }
}
