package com.dexer.aquanaut.client;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.PressureHelper;
import com.mojang.blaze3d.shaders.FogShape;

import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = Aquanaut.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientFogEvents {
    private static final float NIGHT_VISION_CAP = 0.9F;
    private static final float ABYSS_RED = 0.01F;
    private static final float ABYSS_GREEN = 0.02F;
    private static final float ABYSS_BLUE = 0.05F;
    private static final float TARGET_FAR_PLANE = 8.0F;
    private static final float TARGET_NEAR_PLANE = -4.0F;

    private ClientFogEvents() {
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Entity entity = event.getCamera().getEntity();
        if (entity == null || event.getCamera().getFluidInCamera() != FogType.WATER) {
            return;
        }

        float factor = PressureHelper.getPressure(entity);
        if (entity instanceof LivingEntity living && living.hasEffect(MobEffects.NIGHT_VISION)) {
            factor = Math.min(factor, NIGHT_VISION_CAP);
        }

        event.setRed(Mth.lerp(factor, event.getRed(), ABYSS_RED));
        event.setGreen(Mth.lerp(factor, event.getGreen(), ABYSS_GREEN));
        event.setBlue(Mth.lerp(factor, event.getBlue(), ABYSS_BLUE));
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (event.getType() != FogType.WATER) {
            return;
        }

        Entity entity = event.getCamera().getEntity();
        if (entity == null) {
            return;
        }

        float factor = PressureHelper.getPressure(entity);
        if (entity instanceof LivingEntity living && living.hasEffect(MobEffects.NIGHT_VISION)) {
            factor = Math.min(factor, NIGHT_VISION_CAP);
        }

        event.setNearPlaneDistance(Mth.lerp(factor, event.getNearPlaneDistance(), TARGET_NEAR_PLANE));
        event.setFarPlaneDistance(Mth.lerp(factor, event.getFarPlaneDistance(), TARGET_FAR_PLANE));
        event.setFogShape(FogShape.CYLINDER);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        float pressure = PressureHelper.getPressure(minecraft.player);
        List<String> left = event.getLeft();
        left.add(String.format(Locale.ROOT, "Water Pressure: %.2f (%.0f%%)", pressure, pressure * 100.0F));
    }
}