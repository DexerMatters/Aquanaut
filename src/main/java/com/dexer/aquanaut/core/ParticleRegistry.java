package com.dexer.aquanaut.core;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.sonar.SonarMoteOptions;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's own particle types.
 *
 * <p>
 * The caldera's ambient three — steam, sulfur gas and drifting ash — are plain {@code SimpleParticleType}s
 * because each one is a single look fixed at registration. The sonar's mote is not: the same speck has
 * to come back in whichever colour stirred it up, so its colour travels on the particle. See
 * {@link SonarMoteOptions}.
 */
public final class ParticleRegistry {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, Aquanaut.MODID);

    /** Hot spring steam hissing from fumaroles and sinter vents. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VENT_STEAM =
            PARTICLE_TYPES.register("vent_steam", () -> new SimpleParticleType(true));
    /** Sour sulfur gas puffed between the steam. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SULFUR_GAS =
            PARTICLE_TYPES.register("sulfur_gas", () -> new SimpleParticleType(true));
    /** Falling ash: the ever-present dust of the caldera. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ASH_MOTE =
            PARTICLE_TYPES.register("ash_mote", () -> new SimpleParticleType(true));

    /** A speck of water disturbed by a sonar pulse, in the colour of whatever stirred it. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<SonarMoteOptions>> SONAR_MOTE =
            PARTICLE_TYPES.register("sonar_mote", () -> new ParticleType<SonarMoteOptions>(false) {
                @Override
                public MapCodec<SonarMoteOptions> codec() {
                    return SonarMoteOptions.CODEC;
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, SonarMoteOptions> streamCodec() {
                    return SonarMoteOptions.STREAM_CODEC;
                }
            });

    private ParticleRegistry() {
    }

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}
