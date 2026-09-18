package com.dexer.aquanaut.client;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.client.renderer.AirBubbleRenderer;
import com.dexer.aquanaut.client.renderer.AnglerfishRenderer;
import com.dexer.aquanaut.client.renderer.VampreyRenderer;
import com.dexer.aquanaut.client.renderer.OresuckerRenderer;
import com.dexer.aquanaut.client.renderer.FlagellonautilusRenderer;
import com.dexer.aquanaut.client.renderer.SkeletonCarpRenderer;
import com.dexer.aquanaut.client.renderer.GoldenCarpRenderer;
import com.dexer.aquanaut.client.renderer.SilverCarpRenderer;
import com.dexer.aquanaut.client.renderer.GentlefishRenderer;
import com.dexer.aquanaut.client.renderer.SlimyRenderer;
import com.dexer.aquanaut.client.renderer.IonfinRenderer;
import com.dexer.aquanaut.client.renderer.OpticichthusRenderer;
import com.dexer.aquanaut.client.renderer.GeminiJellyfishRenderer;
import com.dexer.aquanaut.client.renderer.EcofishRenderer;
import com.dexer.aquanaut.client.renderer.PaleAbyssHydraRenderer;
import com.dexer.aquanaut.client.renderer.ThreeHeadedSharkRenderer;
import com.dexer.aquanaut.client.renderer.BlueJellyfishRenderer;
import com.dexer.aquanaut.client.renderer.BlueRingedWormfishRenderer;
import com.dexer.aquanaut.client.renderer.DissectionTableBlockEntityRenderer;
import com.dexer.aquanaut.client.renderer.GasPipeBlockEntityRenderer;
import com.dexer.aquanaut.client.renderer.CatfishRenderer;
import com.dexer.aquanaut.client.renderer.CreeporpedoRenderer;
import com.dexer.aquanaut.client.renderer.DonutfishRenderer;
import com.dexer.aquanaut.client.renderer.ElectrofishRenderer;
import com.dexer.aquanaut.client.renderer.FlatfishRenderer;
import com.dexer.aquanaut.client.renderer.GloomgazerRenderer;
import com.dexer.aquanaut.client.renderer.HarpoonRenderer;
import com.dexer.aquanaut.client.renderer.LightingWormRenderer;
import com.dexer.aquanaut.client.renderer.LightningRenderer;
import com.dexer.aquanaut.client.renderer.HelicoprionRenderer;
import com.dexer.aquanaut.client.renderer.IcerailRenderer;
import com.dexer.aquanaut.client.renderer.MantaRayRenderer;
import com.dexer.aquanaut.client.renderer.OctopusRenderer;
import com.dexer.aquanaut.client.renderer.OxygenBreederRenderer;
import com.dexer.aquanaut.client.renderer.RadioanemoneRenderer;
import com.dexer.aquanaut.client.renderer.RedJellyfishRenderer;
import com.dexer.aquanaut.client.renderer.RingfishRenderer;
import com.dexer.aquanaut.client.renderer.GiantAbyssWormRenderer;
import com.dexer.aquanaut.client.renderer.GiantOctopusTentacleRenderer;
import com.dexer.aquanaut.client.renderer.SardineRenderer;
import com.dexer.aquanaut.client.renderer.SpringfishRenderer;
import com.dexer.aquanaut.client.renderer.SwirlMakerRenderer;
import com.dexer.aquanaut.client.renderer.SwirlRenderer;
import com.dexer.aquanaut.client.renderer.TripodRenderer;
import com.dexer.aquanaut.client.renderer.item.GasFlowMeterItemRenderer;
import com.dexer.aquanaut.client.screen.AquariumScreen;
import com.dexer.aquanaut.core.EntityRegistry;
import com.dexer.aquanaut.core.BlockEntityRegistry;
import com.dexer.aquanaut.core.ItemRegistry;
import com.dexer.aquanaut.core.MenuRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Aquanaut.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
@SuppressWarnings("removal")
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> -1,
                ItemRegistry.OCTOPUS_SPAWN_EGG.get(),
                ItemRegistry.SARDINE_SPAWN_EGG.get(),
                ItemRegistry.ANGLERFISH_SPAWN_EGG.get(),
                ItemRegistry.ELECTROFISH_SPAWN_EGG.get(),
                ItemRegistry.DONUTFISH_SPAWN_EGG.get(),
                ItemRegistry.SPRINGFISH_SPAWN_EGG.get(),
                ItemRegistry.ICERAIL_SPAWN_EGG.get(),
                ItemRegistry.HELICOPRION_SPAWN_EGG.get(),
                ItemRegistry.CATFISH_SPAWN_EGG.get(),
                ItemRegistry.MANTA_RAY_SPAWN_EGG.get(),
                ItemRegistry.GIANT_OCTOPUS_TENTACLE_SPAWN_EGG.get(),
                ItemRegistry.GIANT_ABYSS_WORM_SPAWN_EGG.get(),
                ItemRegistry.LIGHTING_WORM_SPAWN_EGG.get(),
                ItemRegistry.CREEPORPEDO_SPAWN_EGG.get(),
                ItemRegistry.SWIRL_MAKER_SPAWN_EGG.get(),
                ItemRegistry.GLOOMGAZER_SPAWN_EGG.get(),
                ItemRegistry.RADIOANEMONE_SPAWN_EGG.get(),
                ItemRegistry.OXYGEN_BREEDER_SPAWN_EGG.get(),
                ItemRegistry.RED_JELLYFISH_SPAWN_EGG.get(),
                ItemRegistry.RINGFISH_SPAWN_EGG.get(),
                ItemRegistry.TRIPOD_SPAWN_EGG.get(),
                ItemRegistry.BLUE_RINGED_WORMFISH_SPAWN_EGG.get(),
                ItemRegistry.BLUE_JELLYFISH_SPAWN_EGG.get(),
                ItemRegistry.FLATFISH_SPAWN_EGG.get(),
                ItemRegistry.VAMPREY_SPAWN_EGG.get(),
                ItemRegistry.ORESUCKER_SPAWN_EGG.get(),
                ItemRegistry.FLAGELLONAUTILUS_SPAWN_EGG.get(),
                ItemRegistry.SKELETON_CARP_SPAWN_EGG.get(),
                ItemRegistry.GOLDEN_CARP_SPAWN_EGG.get(),
                ItemRegistry.SILVER_CARP_SPAWN_EGG.get(),
                ItemRegistry.GENTLEFISH_SPAWN_EGG.get(),
                ItemRegistry.SLIMMY_SPAWN_EGG.get(),
                ItemRegistry.IONFIN_SPAWN_EGG.get(),
                ItemRegistry.OPTICICHTHUS_SPAWN_EGG.get(),
                ItemRegistry.GEMINI_JELLYFISH_SPAWN_EGG.get(),
                ItemRegistry.ECOFISH_SPAWN_EGG.get(),
                ItemRegistry.PALE_ABYSS_HYDRA_SPAWN_EGG.get(),
                ItemRegistry.THREE_HEADED_SHARK_SPAWN_EGG.get());
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.OCTOPUS.get(), OctopusRenderer::new);
        event.registerEntityRenderer(EntityRegistry.SARDINE.get(), SardineRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ANGLERFISH.get(), AnglerfishRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ELECTROFISH.get(), ElectrofishRenderer::new);
        event.registerEntityRenderer(EntityRegistry.DONUTFISH.get(), DonutfishRenderer::new);
        event.registerEntityRenderer(EntityRegistry.SPRINGFISH.get(), SpringfishRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ICERAIL.get(), IcerailRenderer::new);
        event.registerEntityRenderer(EntityRegistry.HELICOPRION.get(), HelicoprionRenderer::new);
        event.registerEntityRenderer(EntityRegistry.CATFISH.get(), CatfishRenderer::new);
        event.registerEntityRenderer(EntityRegistry.MANTA_RAY.get(), MantaRayRenderer::new);
        event.registerEntityRenderer(EntityRegistry.GIANT_OCTOPUS_TENTACLE.get(), GiantOctopusTentacleRenderer::new);
        event.registerEntityRenderer(EntityRegistry.GIANT_ABYSS_WORM.get(), GiantAbyssWormRenderer::new);
        event.registerEntityRenderer(EntityRegistry.LIGHTING_WORM.get(), LightingWormRenderer::new);
        event.registerEntityRenderer(EntityRegistry.CREEPORPEDO.get(), CreeporpedoRenderer::new);
        event.registerEntityRenderer(EntityRegistry.SWIRL_MAKER.get(), SwirlMakerRenderer::new);
        event.registerEntityRenderer(EntityRegistry.SWIRL.get(), SwirlRenderer::new);
        event.registerEntityRenderer(EntityRegistry.GLOOMGAZER.get(), GloomgazerRenderer::new);
        event.registerEntityRenderer(EntityRegistry.RADIOANEMONE.get(), RadioanemoneRenderer::new);
        event.registerEntityRenderer(EntityRegistry.OXYGEN_BREEDER.get(), OxygenBreederRenderer::new);
        event.registerEntityRenderer(EntityRegistry.RED_JELLYFISH.get(), RedJellyfishRenderer::new);
        event.registerEntityRenderer(EntityRegistry.RINGFISH.get(), RingfishRenderer::new);
        event.registerEntityRenderer(EntityRegistry.TRIPOD.get(), TripodRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BLUE_RINGED_WORMFISH.get(), BlueRingedWormfishRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BLUE_JELLYFISH.get(), BlueJellyfishRenderer::new);
        event.registerEntityRenderer(EntityRegistry.FLATFISH.get(), FlatfishRenderer::new);
        event.registerEntityRenderer(EntityRegistry.VAMPREY.get(), VampreyRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ORESUCKER.get(), OresuckerRenderer::new);
        event.registerEntityRenderer(EntityRegistry.FLAGELLONAUTILUS.get(), FlagellonautilusRenderer::new);
        event.registerEntityRenderer(EntityRegistry.SKELETON_CARP.get(), SkeletonCarpRenderer::new);
        event.registerEntityRenderer(EntityRegistry.GOLDEN_CARP.get(), GoldenCarpRenderer::new);
        event.registerEntityRenderer(EntityRegistry.SILVER_CARP.get(), SilverCarpRenderer::new);
        event.registerEntityRenderer(EntityRegistry.GENTLEFISH.get(), GentlefishRenderer::new);
        event.registerEntityRenderer(EntityRegistry.SLIMMY.get(), SlimyRenderer::new);
        event.registerEntityRenderer(EntityRegistry.IONFIN.get(), IonfinRenderer::new);
        event.registerEntityRenderer(EntityRegistry.OPTICICHTHUS.get(), OpticichthusRenderer::new);
        event.registerEntityRenderer(EntityRegistry.GEMINI_JELLYFISH.get(), GeminiJellyfishRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ECOFISH.get(), EcofishRenderer::new);
        event.registerEntityRenderer(EntityRegistry.PALE_ABYSS_HYDRA.get(), PaleAbyssHydraRenderer::new);
        event.registerEntityRenderer(EntityRegistry.THREE_HEADED_SHARK.get(), ThreeHeadedSharkRenderer::new);
        event.registerEntityRenderer(EntityRegistry.AIR_BUBBLE.get(), AirBubbleRenderer::new);
        event.registerEntityRenderer(EntityRegistry.HARPOON.get(), HarpoonRenderer::new);
        event.registerEntityRenderer(EntityRegistry.LIGHTNING.get(), LightningRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.GAS_PIPE.get(), GasPipeBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.DISSECTION_TABLE.get(),
                DissectionTableBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        GasFlowMeterItemRenderer.registerAdditionalModels(event);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(MenuRegistry.AQUARIUM.get(), AquariumScreen::new);
    }
}
