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
import com.dexer.aquanaut.core.ParticleRegistry;
import com.dexer.aquanaut.core.SoundRegistry;
import com.dexer.aquanaut.core.WorldgenPackRegistry;
import com.dexer.aquanaut.common.light.ServerDynamicLightManager;
import com.dexer.aquanaut.common.searchlight.SearchlightServerProvider;
import com.dexer.aquanaut.common.drone.DroneHeadlightServerProvider;
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
        ParticleRegistry.register(modEventBus);
        SoundRegistry.register(modEventBus);
        FeatureRegistry.register(modEventBus);
        // The abyssal overworld height travels as an optional, discoverable data pack so players
        // can see and disable an override this invasive; see WorldgenPackRegistry.
        modEventBus.addListener(WorldgenPackRegistry::onAddPackFinders);
        ServerDynamicLightManager.registerProvider(SearchlightServerProvider.ID, new SearchlightServerProvider());
        ServerDynamicLightManager.registerProvider(DroneHeadlightServerProvider.ID,
                new DroneHeadlightServerProvider());
        modEventBus.addListener(this::onCommonSetup);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        // Client-side fog controls live in a client config: whether a shader pack's atmosphere
        // should be supplemented by the mod's own veil is a property of the player's install,
        // not of the world. Touched only behind the dist check, so a dedicated server never
        // loads the class.
        if (net.neoforged.fml.loading.FMLEnvironment.dist
                == net.neoforged.api.distmarker.Dist.CLIENT) {
            modContainer.registerConfig(ModConfig.Type.CLIENT,
                    com.dexer.aquanaut.client.fog.FogClientConfig.SPEC);
        }
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(BiomeRegistry::register);
    }

}
