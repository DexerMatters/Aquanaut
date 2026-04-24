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
    /**
     * How much the darkness fades each rendered frame when surfacing (~1 s to clear
     * at 60 fps).
     */
    private static final float CLEAR_SPEED = 1.0F / 60.0F;
    /** Drown damage per hit (matches the @ModifyArg double: 2.0 * 2 = 4.0). */
    private static final float DROWN_DAMAGE_PER_HIT = 4.0F;
    /** Vanilla ticks between drown damage hits. */
    private static final float DROWN_TICKS_PER_HIT = 20.0F;
    /** Assumed client frame rate for timer calculations. */
    private static final float ASSUMED_FPS = 60.0F;

    /** Smooth client-side darkness level [0, 1] driven by the drowning timer. */
    private static float drowningDarkness = 0.0F;
    /** Darken increment per frame, computed from health at drowning start. */
    private static float darkenSpeed = 0.0F;

    private ClientFogEvents() {
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Entity entity = event.getCamera().getEntity();
        if (entity == null) {
            return;
        }

        // Tick the smooth darkness every frame so it also fades when out of water.
        tickDrowningDarkness(entity);

        if (event.getCamera().getFluidInCamera() != FogType.WATER) {
            return;
        }

        float factor = getWaterFogFactor(entity);

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

        float factor = getWaterFogFactor(entity);

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

    private static float getWaterFogFactor(Entity entity) {
        float factor = PressureHelper.getPressure(entity);
        if (entity instanceof LivingEntity living && living.hasEffect(MobEffects.NIGHT_VISION)) {
            factor = Math.min(factor, NIGHT_VISION_CAP);
        }

        return Math.max(factor, drowningDarkness);
    }

    private static void tickDrowningDarkness(Entity entity) {
        boolean isDrowning = entity instanceof LivingEntity living
                && living.isInWater()
                && living.getAirSupply() <= 0;

        if (isDrowning) {
            // On the first frame of drowning, calculate how many frames until death
            // based on *current* health so the darkness fills up exactly by the time
            // the player would die.
            if (darkenSpeed == 0.0F && entity instanceof LivingEntity living) {
                float hitsUntilDeath = living.getHealth() / DROWN_DAMAGE_PER_HIT;
                float framesUntilDeath = hitsUntilDeath * DROWN_TICKS_PER_HIT * (ASSUMED_FPS / 20.0F);
                darkenSpeed = framesUntilDeath > 0 ? 1.0F / framesUntilDeath : 1.0F;
            }
            drowningDarkness = Math.min(1.0F, drowningDarkness + darkenSpeed);
        } else {
            darkenSpeed = 0.0F;
            drowningDarkness = Math.max(0.0F, drowningDarkness - CLEAR_SPEED);
        }
    }
}