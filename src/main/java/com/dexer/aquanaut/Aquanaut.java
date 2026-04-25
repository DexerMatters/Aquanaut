package com.dexer.aquanaut;

import com.dexer.aquanaut.core.AttachmentRegistry;
import com.dexer.aquanaut.core.EntityRegistry;
import com.dexer.aquanaut.core.ItemRegistry;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Aquanaut.MODID)
public class Aquanaut {
    public static final String MODID = "aquanaut";

    public static final Logger LOGGER = LogUtils.getLogger();

    public Aquanaut(IEventBus modEventBus, ModContainer modContainer) {

        modEventBus.addListener(this::commonSetup);

        // Registers
        AttachmentRegistry.register(modEventBus);
        EntityRegistry.register(modEventBus);
        ItemRegistry.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {

    }
}
