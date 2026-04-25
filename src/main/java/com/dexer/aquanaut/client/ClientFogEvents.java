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
    /** Real-time milliseconds between drown damage hits (20 ticks × 50 ms/tick). */
    private static final float DROWN_MS_PER_HIT = 1000.0F;

    /** Smooth client-side darkness level [0, 1]. */
    private static float drowningDarkness = 0.0F;
    /**
     * System.currentTimeMillis() when the current drowning episode started. -1 = no
     * episode.
     */
    private static long drowningStartMs = -1L;
    /** Estimated milliseconds from episode start until death. */
    private static float drowningDurationMs = 0.0F;
    /**
     * True once we have seen airSupply < 0, meaning the damage counter is active.
     * Used to distinguish the initial air=0 (just ran out) from the post-damage
     * air=0 reset.
     */
    private static boolean hadNegativeAir = false;

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
        if (!(entity instanceof LivingEntity living)
                || !living.isInWater()
                || living.getAirSupply() > 0) {
            drowningStartMs = -1L;
            hadNegativeAir = false;
            drowningDarkness = Math.max(0.0F, drowningDarkness - CLEAR_SPEED);
            return;
        }

        int air = living.getAirSupply();

        // Track when the damage countdown goes negative (i.e., < 0).
        if (air < 0) {
            hadNegativeAir = true;
        }

        if (drowningStartMs < 0) {
            // Start the episode only on the FIRST DAMAGE HIT.
            if (hadNegativeAir && air == 0) {
                drowningStartMs = System.currentTimeMillis();
                float hitsToLive = Math.max(1.0F, living.getHealth() / DROWN_DAMAGE_PER_HIT);
                drowningDurationMs = hitsToLive * DROWN_MS_PER_HIT * 2.0F;
            }
            return;
        }

        // Episode active: smoothly map real elapsed time to [0, 1].
        long elapsed = System.currentTimeMillis() - drowningStartMs;
        drowningDarkness = Mth.clamp(elapsed / drowningDurationMs, 0.0F, 1.0F);

    }
}