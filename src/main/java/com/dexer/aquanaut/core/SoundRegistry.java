package com.dexer.aquanaut.core;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.sonar.SonarSignal;
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
    public static final DeferredHolder<SoundEvent, SoundEvent> CAMERA_SHUTTER = register("camera_shutter");

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

    // ── portable sonar ────────────────────────────────────────────────────────
    /** The transducer firing. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SONAR_PING = register("sonar_ping");
    /** A body answering: a soft, rounded reflection. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SONAR_ECHO_BIOLOGICAL = register(
            "sonar_echo_biological");
    /** Crystal and ore answering: a hard, bright ring. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SONAR_ECHO_MINERAL = register("sonar_echo_mineral");
    /** A void behind a face answering: a low, hollow note. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SONAR_ECHO_CAVITY = register("sonar_echo_cavity");
    /** The long echo. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SONAR_ECHO_ABYSS = register("sonar_echo_abyss");

    /**
     * The voice a contact answers in. The mapping lives here rather than on {@link SonarSignal} so
     * that the shared table stays free of game types and can be checked without a registry.
     */
    public static SoundEvent echo(SonarSignal signal) {
        return switch (signal) {
            case BIOLOGICAL -> SONAR_ECHO_BIOLOGICAL.get();
            case MINERAL -> SONAR_ECHO_MINERAL.get();
            case CAVITY -> SONAR_ECHO_CAVITY.get();
            case ABYSS -> SONAR_ECHO_ABYSS.get();
        };
    }

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
