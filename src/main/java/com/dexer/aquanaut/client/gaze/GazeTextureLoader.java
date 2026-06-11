package com.dexer.aquanaut.client.gaze;

import com.dexer.aquanaut.Aquanaut;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

import java.io.IOException;
import java.io.InputStream;

@EventBusSubscriber(modid = Aquanaut.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
@SuppressWarnings("removal")
public final class GazeTextureLoader {

    public static final ResourceLocation GAZE_GLINT_LOCATION = ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID,
            "textures/misc/gaze_glint");

    private GazeTextureLoader() {
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(
                (net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier stage,
                        ResourceManager manager,
                        net.minecraft.util.profiling.ProfilerFiller prepProfiler,
                        net.minecraft.util.profiling.ProfilerFiller reloadProfiler,
                        java.util.concurrent.Executor bgExecutor,
                        java.util.concurrent.Executor gameExecutor) -> stage.wait(null).thenRunAsync(() -> {
                            try (InputStream is = Aquanaut.class.getResourceAsStream(
                                    "/assets/aquanaut/textures/misc/gaze_glint.png")) {
                                if (is == null) {
                                    Aquanaut.LOGGER.error("Gaze glint texture not found in classpath");
                                    return;
                                }
                                NativeImage image = NativeImage.read(is);
                                Minecraft.getInstance().getTextureManager().register(GAZE_GLINT_LOCATION,
                                        new AbstractTexture() {
                                            @Override
                                            public void load(ResourceManager mgr) {
                                                if (this.getId() == -1) {
                                                    this.id = TextureUtil.generateTextureId();
                                                }
                                                TextureUtil.prepareImage(getId(), image.getWidth(), image.getHeight());
                                                image.upload(0, 0, 0, false);
                                            }

                                            @Override
                                            public void close() {
                                                image.close();
                                                this.releaseId();
                                            }
                                        });
                                Aquanaut.LOGGER.info("Registered gaze glint texture from classpath");
                            } catch (IOException e) {
                                Aquanaut.LOGGER.error("Failed to load gaze glint texture", e);
                            }
                        }, gameExecutor));
    }
}
