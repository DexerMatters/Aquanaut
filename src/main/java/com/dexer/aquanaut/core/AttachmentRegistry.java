package com.dexer.aquanaut.core;

import com.dexer.aquanaut.Aquanaut;
import com.mojang.serialization.Codec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class AttachmentRegistry {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister
            .create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Aquanaut.MODID);

    /**
     * Maximum extra air capacity added on top of the base 1200 ticks. Default: 0.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> MAX_EXTRA_AIR_SUPPLY = ATTACHMENT_TYPES
            .register("max_extra_air_supply",
                    () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build());

    private AttachmentRegistry() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
