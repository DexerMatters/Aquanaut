package com.dexer.aquanaut.core;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.fluid.SulfuricAcidFluid;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Custom liquids of the caldera. Registration order matters only in that fluids resolve
 * before blocks and items pick them up (BuiltInRegistries registers FLUID first).
 */
public final class FluidRegistry {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Aquanaut.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Aquanaut.MODID);

    public static final DeferredHolder<FluidType, FluidType> SULFURIC_ACID_TYPE =
            FLUID_TYPES.register("sulfuric_acid", () -> SulfuricAcidFluid.TYPE);
    public static final DeferredHolder<Fluid, SulfuricAcidFluid> SULFURIC_ACID =
            FLUIDS.register("sulfuric_acid", SulfuricAcidFluid.Source::new);
    public static final DeferredHolder<Fluid, SulfuricAcidFluid> FLOWING_SULFURIC_ACID =
            FLUIDS.register("flowing_sulfuric_acid", SulfuricAcidFluid.Flowing::new);

    private FluidRegistry() {
    }

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }
}
