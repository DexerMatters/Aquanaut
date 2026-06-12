package com.dexer.aquanaut;

import com.dexer.aquanaut.core.BlockRegistry;
import com.dexer.aquanaut.core.BlockEntityRegistry;
import com.dexer.aquanaut.core.AttachmentRegistry;
import com.dexer.aquanaut.core.EntityRegistry;
import com.dexer.aquanaut.core.GameRuleRegistry;
import com.dexer.aquanaut.core.GazeRegistry;
import com.dexer.aquanaut.core.BiomeRegistry;
import com.dexer.aquanaut.core.FeatureRegistry;
import com.dexer.aquanaut.core.ItemRegistry;
import com.dexer.aquanaut.core.MenuRegistry;
import com.dexer.aquanaut.core.MobEffectRegistry;
import com.dexer.aquanaut.core.SoundRegistry;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(Aquanaut.MODID)
public class Aquanaut {
    public static final String MODID = "aquanaut";

    public static final Logger LOGGER = LogUtils.getLogger();

    public Aquanaut(IEventBus modEventBus, ModContainer modContainer) {

        BlockRegistry.register(modEventBus);
        BlockEntityRegistry.register(modEventBus);
        AttachmentRegistry.register(modEventBus);
        EntityRegistry.register(modEventBus);
        GameRuleRegistry.register(modEventBus);
        GazeRegistry.register(modEventBus);
        ItemRegistry.register(modEventBus);
        MenuRegistry.register(modEventBus);
        MobEffectRegistry.register(modEventBus);
        SoundRegistry.register(modEventBus);
        FeatureRegistry.register(modEventBus);
        modEventBus.addListener(this::onCommonSetup);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(BiomeRegistry::register);
    }

}
