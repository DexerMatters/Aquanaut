package com.dexer.aquanaut.core;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.gaze.GazeInstance;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class GazeRegistry {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister
            .create(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE, Aquanaut.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<GazeInstance>>> GAZE = DATA_COMPONENTS
            .register("gaze",
                    () -> DataComponentType.<List<GazeInstance>>builder()
                            .persistent(GazeInstance.CODEC.listOf())
                            .build());

    private GazeRegistry() {
    }

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }
}
