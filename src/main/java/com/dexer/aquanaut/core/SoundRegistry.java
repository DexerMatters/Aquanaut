package com.dexer.aquanaut.core;

import com.dexer.aquanaut.Aquanaut;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SoundRegistry {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister
            .create(BuiltInRegistries.SOUND_EVENT, Aquanaut.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> BUBBLE_BURST = register("bubble_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUBBLE_AMBIENT = register("bubble_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUBBLE_MERGE = register("bubble_merge");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    private SoundRegistry() {
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
