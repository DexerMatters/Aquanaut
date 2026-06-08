package com.dexer.aquanaut.core;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumContainerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MenuRegistry {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister
            .create(Registries.MENU, Aquanaut.MODID);

    @SuppressWarnings("unchecked")
    public static final net.neoforged.neoforge.registries.DeferredHolder<MenuType<?>, MenuType<AquariumContainerMenu>> AQUARIUM =
            (net.neoforged.neoforge.registries.DeferredHolder<MenuType<?>, MenuType<AquariumContainerMenu>>)
            (Object) MENUS.register("aquarium",
                    () -> IMenuTypeExtension.create(
                            (containerId, inv, buf) -> new AquariumContainerMenu(containerId, inv)));

    private MenuRegistry() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
