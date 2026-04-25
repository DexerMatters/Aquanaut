package com.dexer.aquanaut.client;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.client.renderer.AirBubbleRenderer;
import com.dexer.aquanaut.client.renderer.AnglerfishRenderer;
import com.dexer.aquanaut.client.renderer.DonutfishRenderer;
import com.dexer.aquanaut.client.renderer.ElectrofishRenderer;
import com.dexer.aquanaut.client.renderer.HelicoprionRenderer;
import com.dexer.aquanaut.client.renderer.IcerailRenderer;
import com.dexer.aquanaut.client.renderer.OctopusRenderer;
import com.dexer.aquanaut.client.renderer.SardineRenderer;
import com.dexer.aquanaut.client.renderer.SpringfishRenderer;
import com.dexer.aquanaut.core.EntityRegistry;
import com.dexer.aquanaut.core.ItemRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = Aquanaut.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
                ItemRegistry.HELICOPRION_SPAWN_EGG.get());
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
        event.registerEntityRenderer(EntityRegistry.AIR_BUBBLE.get(), AirBubbleRenderer::new);
    }
}
