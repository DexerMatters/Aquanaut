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
    public static final DeferredHolder<SoundEvent, SoundEvent> CREEPORPEDO_IGNITE = register("creeporpedo_ignite");

    // ── submarine drone ───────────────────────────────────────────────────────
    /** Controller handshake: the drone answers the link request. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SUBMARINE_DRONE_ACTIVATE = register(
            "submarine_drone_activate");
    /** The link is dropped and the drone powers down. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SUBMARINE_DRONE_DEACTIVATE = register(
            "submarine_drone_deactivate");
    /** The water jet, played under power. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SUBMARINE_DRONE_THRUSTER = register(
            "submarine_drone_thruster");
    /** The drone is picked back up and its hull comes out of the water. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SUBMARINE_DRONE_RETRIEVE = register(
            "submarine_drone_retrieve");
    /** A link was refused: the drone is already under another pilot. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SUBMARINE_DRONE_REJECT = register("submarine_drone_reject");

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
