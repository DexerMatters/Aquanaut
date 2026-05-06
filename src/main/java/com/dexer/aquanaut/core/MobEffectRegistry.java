package com.dexer.aquanaut.core;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.effect.NarcosisMobEffect;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MobEffectRegistry {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister
            .create(BuiltInRegistries.MOB_EFFECT, Aquanaut.MODID);

    public static final DeferredHolder<MobEffect, NarcosisMobEffect> NARCOSIS = MOB_EFFECTS.register("narcosis",
            NarcosisMobEffect::new);

    private MobEffectRegistry() {
    }

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}