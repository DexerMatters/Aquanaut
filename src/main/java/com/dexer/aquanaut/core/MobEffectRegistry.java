package com.dexer.aquanaut.core;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.effect.ChargedMobEffect;
import com.dexer.aquanaut.common.effect.NarcosisMobEffect;
import com.dexer.aquanaut.common.effect.PellucidMobEffect;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MobEffectRegistry {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister
            .create(BuiltInRegistries.MOB_EFFECT, Aquanaut.MODID);

    public static final DeferredHolder<MobEffect, ChargedMobEffect> CHARGED = MOB_EFFECTS.register("charged",
            ChargedMobEffect::new);

    public static final DeferredHolder<MobEffect, NarcosisMobEffect> NARCOSIS = MOB_EFFECTS.register("narcosis",
            NarcosisMobEffect::new);

    public static final DeferredHolder<MobEffect, PellucidMobEffect> PELLUCID = MOB_EFFECTS.register("pellucid",
            PellucidMobEffect::new);

    private MobEffectRegistry() {
    }

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}