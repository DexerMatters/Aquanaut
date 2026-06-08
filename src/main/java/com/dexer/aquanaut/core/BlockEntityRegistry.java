package com.dexer.aquanaut.core;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.block.entity.GasPipeBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BlockEntityRegistry {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(
            Registries.BLOCK_ENTITY_TYPE, Aquanaut.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GasPipeBlockEntity>> GAS_PIPE = BLOCK_ENTITY_TYPES
            .register("gas_pipe",
                    () -> BlockEntityType.Builder.of(GasPipeBlockEntity::new, BlockRegistry.GAS_PIPE.get())
                            .build(null));

    private BlockEntityRegistry() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
