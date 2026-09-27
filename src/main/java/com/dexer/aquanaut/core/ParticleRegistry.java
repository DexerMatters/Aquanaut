package com.dexer.aquanaut.core;

import com.dexer.aquanaut.Aquanaut;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Ambient and vent particles of Brimstone Caldera: steam and sulfur gas over the hot
 * springs (热泉), and the ash motes drifting through the water.
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

    private ParticleRegistry() {
    }

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}
