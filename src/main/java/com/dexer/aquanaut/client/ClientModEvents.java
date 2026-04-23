package com.dexer.aquanaut.client;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.client.renderer.AnglerfishRenderer;
import com.dexer.aquanaut.client.renderer.ElectrofishRenderer;
import com.dexer.aquanaut.client.renderer.HelicoprionRenderer;
import com.dexer.aquanaut.client.renderer.IcerailRenderer;
import com.dexer.aquanaut.client.renderer.OctopusRenderer;
import com.dexer.aquanaut.client.renderer.SardineRenderer;
import com.dexer.aquanaut.core.EntityRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Aquanaut.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.OCTOPUS.get(), OctopusRenderer::new);
        event.registerEntityRenderer(EntityRegistry.SARDINE.get(), SardineRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ANGLERFISH.get(), AnglerfishRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ELECTROFISH.get(), ElectrofishRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ICERAIL.get(), IcerailRenderer::new);
        event.registerEntityRenderer(EntityRegistry.HELICOPRION.get(), HelicoprionRenderer::new);
    }
}
