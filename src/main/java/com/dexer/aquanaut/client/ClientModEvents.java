package com.dexer.aquanaut.client;

import java.util.function.Supplier;

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
import com.dexer.aquanaut.client.renderer.DivingEquipmentRenderLayer;
import com.dexer.aquanaut.client.renderer.GasPipeBlockEntityRenderer;
import com.dexer.aquanaut.client.renderer.InvestigationBoardBlockEntityRenderer;
import com.dexer.aquanaut.client.renderer.CatfishRenderer;
import com.dexer.aquanaut.client.renderer.CursorRenderer;
import com.dexer.aquanaut.client.renderer.BiologicalDetectorRenderer;
import com.dexer.aquanaut.client.renderer.SubmarineDroneRenderer;
import com.dexer.aquanaut.client.drone.ClientDroneEvents;
import com.dexer.aquanaut.client.drone.DroneHeadlightClientProvider;
import com.dexer.aquanaut.client.renderer.item.SubmarineCompassItemPropertyFunction;
import com.dexer.aquanaut.client.screen.TagScreen;
import com.dexer.aquanaut.common.fog.FogProfiles;
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
import com.dexer.aquanaut.client.renderer.SaltCrustRenderer;
import com.dexer.aquanaut.client.renderer.SpringfishRenderer;
import com.dexer.aquanaut.client.renderer.SwirlMakerRenderer;
import com.dexer.aquanaut.client.renderer.SwirlRenderer;
import com.dexer.aquanaut.client.renderer.TripodRenderer;
import com.dexer.aquanaut.client.renderer.item.GasFlowMeterItemRenderer;
import com.dexer.aquanaut.client.renderer.item.HandheldAirBladderItemRenderer;
import com.dexer.aquanaut.client.renderer.item.HandheldSearchlightItemRenderer;
import com.dexer.aquanaut.client.renderer.item.PortableSonarItemRenderer;
import com.dexer.aquanaut.client.renderer.item.ShellCameraItemRenderer;
import com.dexer.aquanaut.client.light.ClientDynamicLightManager;
import com.dexer.aquanaut.client.particle.SoftWispParticle;
import com.dexer.aquanaut.client.particle.SonarMoteParticle;
import com.dexer.aquanaut.client.searchlight.SearchlightClientProvider;
import com.dexer.aquanaut.client.screen.AquariumScreen;
import com.dexer.aquanaut.client.screen.PhotoRinsingScreen;
import com.dexer.aquanaut.common.item.GasFlowMeterItem;
import com.dexer.aquanaut.common.item.ShellCameraItem;
import com.dexer.aquanaut.common.item.SubmarineDroneControllerItem;
import com.dexer.aquanaut.core.EntityRegistry;
import com.dexer.aquanaut.core.BlockEntityRegistry;
import com.dexer.aquanaut.core.ItemRegistry;
import com.dexer.aquanaut.core.MenuRegistry;
import com.dexer.aquanaut.core.ParticleRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

@EventBusSubscriber(modid = Aquanaut.MODID, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    /** Registers the tag editor, which a taggable entity opens through a common-side hook. */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        TagScreen.install();        ClientDynamicLightManager.registerProvider(SearchlightClientProvider.ID, new SearchlightClientProvider());
        ClientDynamicLightManager.registerProvider(DroneHeadlightClientProvider.ID,
                new DroneHeadlightClientProvider());

        // The compass needle is an item-model property, exactly like vanilla's compass: the
        // generated submarine_compass.json picks one of its 32 frames from this value.
        ItemProperties.register(
                ItemRegistry.SUBMARINE_COMPASS.get(),
                ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "angle"),
                new SubmarineCompassItemPropertyFunction());

        // The controller has two sprites, not two items: the powered one is chosen from the link in
        // the stack's NBT, so a controller that points at a drone looks live in the hand.
        ItemProperties.register(
                ItemRegistry.SUBMARINE_DRONE_CONTROLLER.get(),
                ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "linked"),
                (stack, level, entity, seed) -> SubmarineDroneControllerItem.isLinked(stack) ? 1.0F : 0.0F);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ClientDroneEvents.HEADLIGHT_KEY);
    }

    /** Loads the fog table (visibility per ocean, and the look of acid). */
    @SubscribeEvent
    public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(FogProfiles.reloadListener());
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
        event.registerEntityRenderer(EntityRegistry.SALT_CRUST.get(), SaltCrustRenderer::new);
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
        event.registerEntityRenderer(EntityRegistry.CURSOR.get(), CursorRenderer::new);
        event.registerEntityRenderer(EntityRegistry.SUBMARINE_DRONE.get(), SubmarineDroneRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BIOLOGICAL_DETECTOR.get(), BiologicalDetectorRenderer::new);
        event.registerEntityRenderer(EntityRegistry.HARPOON.get(), HarpoonRenderer::new);
        event.registerEntityRenderer(EntityRegistry.LIGHTNING.get(), LightningRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.GAS_PIPE.get(), GasPipeBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.DISSECTION_TABLE.get(),
                DissectionTableBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.INVESTIGATION_BOARD.get(),
                InvestigationBoardBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void addDivingEquipmentPlayerLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new DivingEquipmentRenderLayer(renderer));
            }
        }
    }

    @SubscribeEvent
    public static void clearDivingEquipmentOnPlayerLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide
                && event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            ClientDivingEquipmentData.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void clearDivingEquipmentOnLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientDivingEquipmentData.clear();
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        GasFlowMeterItemRenderer.registerAdditionalModels(event);
        HandheldAirBladderItemRenderer.registerAdditionalModels(event);
        HandheldSearchlightItemRenderer.registerAdditionalModels(event);
        PortableSonarItemRenderer.registerAdditionalModels(event);
        ShellCameraItemRenderer.registerAdditionalModels(event);
    }

    /**
     * The bladder swaps between the inventory sprite and the element model, and
     * the searchlight additionally swaps between its dark and burning states, so
     * both need the custom renderer. The flow meter brings its own extension,
     * which carries its targeting hand transform alongside the renderer.
     * Registered here rather than through the removed {@code Item#initializeClient}
     * hook, and resolved lazily so the renderers are built after the client exists.
     */
    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(customRenderer(HandheldAirBladderItemRenderer::small),
                ItemRegistry.HANDHELD_AIR_BLADDER.get());
        event.registerItem(customRenderer(HandheldAirBladderItemRenderer::large),
                ItemRegistry.LARGE_HANDHELD_AIR_BLADDER.get());
        event.registerItem(customRenderer(HandheldSearchlightItemRenderer::getInstance),
                ItemRegistry.HANDHELD_SEARCHLIGHT.get());
        event.registerItem(customRenderer(PortableSonarItemRenderer::getInstance),
                ItemRegistry.PORTABLE_SONAR.get());
        event.registerItem(customRenderer(ShellCameraItemRenderer::getInstance), ItemRegistry.SHELL_CAMERA.get());
        event.registerItem(GasFlowMeterItem.CLIENT_EXTENSIONS, ItemRegistry.GAS_FLOW_METER.get());
    }

    /** Vapor, steam and drifting ash of the Brimstone Caldera. */
    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticleRegistry.VENT_STEAM.get(),
                sprites -> new SoftWispParticle.Provider(sprites, 0.93F, 0.94F, 0.95F,
                        0.36F, 24, 0.003F, 0.50F));
        event.registerSpriteSet(ParticleRegistry.SULFUR_GAS.get(),
                sprites -> new SoftWispParticle.Provider(sprites, 0.88F, 0.88F, 0.45F,
                        0.28F, 20, 0.002F, 0.45F));
        event.registerSpriteSet(ParticleRegistry.ASH_MOTE.get(),
                sprites -> new SoftWispParticle.Provider(sprites, 0.30F, 0.30F, 0.33F,
                        0.16F, 45, 0.004F, 0.75F));
        event.registerSpriteSet(ParticleRegistry.SONAR_MOTE.get(), SonarMoteParticle.Provider::new);
    }

    private static IClientItemExtensions customRenderer(Supplier<BlockEntityWithoutLevelRenderer> renderer) {
        return new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer.get();
            }
        };
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(MenuRegistry.AQUARIUM.get(), AquariumScreen::new);
        event.register(MenuRegistry.PHOTO_RINSING_BASIN.get(), PhotoRinsingScreen::new);
    }

    /**
     * Vanilla's cooldown sweep asks {@code ItemCooldowns}, which is keyed by item, so it would
     * darken every shell camera in the inventory at once. The wind-on is tracked per stack, so the
     * sweep is drawn here for the one camera that actually fired, in the vanilla shape and colour.
     */
    @SubscribeEvent
    public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
        event.register(ItemRegistry.SHELL_CAMERA.get(), (graphics, font, stack, x, y) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return false;
            }
            float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
            float fraction = ShellCameraItem.cooldownFraction(stack, minecraft.level.getGameTime(), partialTick);
            if (fraction <= 0.0F) {
                return false;
            }
            int top = y + Mth.floor(16.0F * (1.0F - fraction));
            int bottom = top + Mth.ceil(16.0F * fraction);
            graphics.fill(x, top, x + 16, bottom, Integer.MAX_VALUE);
            return false;
        });
    }
}
