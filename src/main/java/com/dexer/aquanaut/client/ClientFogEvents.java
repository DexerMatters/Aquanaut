package com.dexer.aquanaut.client;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.client.drone.ClientDroneState;
import com.dexer.aquanaut.client.fog.ClientFogState;
import com.dexer.aquanaut.client.fog.FogClientConfig;
import com.dexer.aquanaut.client.fog.IrisCompat;
import com.dexer.aquanaut.common.PressureHelper;
import com.dexer.aquanaut.common.fog.FogMath;

import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.shaders.FogShape;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * The abyss, applied as fog — the half of the fog authority that the world can see.
 *
 * <p>Everything this event writes lands in the vanilla fog state, which is exactly what a
 * shader pack reads: Iris fills its {@code fogColor}, {@code fogStart}, {@code fogEnd},
 * {@code fogShape} and {@code fogDensity} uniforms from that state (and injects into
 * {@code FogRenderer} without cancelling it), so a pack that honours vanilla fog follows the
 * biome's colour, the acid's murk and this abyss without knowing the mod exists. The packs
 * that compute their own atmosphere are covered by the veil in
 * {@link com.dexer.aquanaut.client.fog.FogVeilRenderer}, drawn from the same decision.</p>
 *
 * <p>The darkness scales with {@link PressureHelper pressure}, deepening towards black and
 * closing in to a few blocks as the player descends, and a drowning episode darkens it
 * further still.</p>
 *
 * <h3>The fog belongs to the observer, not to the water</h3>
 * Both of these are properties of <em>a camera</em>: the fog event carries the one being set
 * up, and the same frame sets up more than one. The drone's feed renders the level a second
 * time through a camera of its own, so it arrives here too — and it must not be treated as a
 * second player. A pod neither drinks the abyss nor owns the operator's drowning clock; it
 * keeps the water's own colour and visibility, which is what makes its picture worth watching
 * and what a camera sent into the deep is for. Recognising it is {@link #isSensorPod(Camera)}.
 */
@EventBusSubscriber(modid = Aquanaut.MODID, value = Dist.CLIENT)
public final class ClientFogEvents {
    /**
     * How much the darkness fades each rendered frame when surfacing (~1 s to clear
     * at 60 fps).
     */
    private static final float CLEAR_SPEED = 1.0F / 60.0F;
    /** Drown damage per hit (matches the @ModifyArg double: 2.0 * 2 = 4.0). */
    private static final float DROWN_DAMAGE_PER_HIT = 4.0F;
    /** Real-time milliseconds between drown damage hits (20 ticks × 50 ms/tick). */
    private static final float DROWN_MS_PER_HIT = 1000.0F;

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

        // The pod is a camera, not an observer. See the class documentation: it neither drinks the
        // abyss nor owns the drowning clock, so the feed shows the water's own colour and its picture
        // does not swing with the state of whoever is watching it. It is excluded before the
        // authority is consulted at all, so its pass can never drive the operator's easing.
        if (isSensorPod(event.getCamera())) {
            return;
        }

        // Tick the smooth darkness every frame so it also fades when out of water. It is one clock,
        // shared by every view, and any other camera driving it hands its own lungs to the player: a
        // drone that never surfaces would keep an episode open forever, while a drone that never runs
        // out of air would clear it every frame.
        if (Minecraft.getInstance().player == entity) {
            tickDrowningDarkness(entity);
        }

        // The colour comes from the authority, which blends the oceans around the camera and eases
        // every change; it is handed to the world's fog state as-is, so a shader pack reading that
        // state and the veil drawn over the frame cannot disagree.
        ClientFogState.Snapshot snapshot = ClientFogState.resolve(event.getCamera(),
                event.getRed(), event.getGreen(), event.getBlue());
        if (snapshot.submersion() <= 0.001F) {
            return;
        }

        // Entering and leaving a medium are crossfades: at full submersion the authority's blended
        // colour (darkened by the depth ramp) replaces the world's own, and while surfacing it hands
        // the world's colour back over a fraction of a second instead of cutting to it.
        int world = packRgb(event.getRed(), event.getGreen(), event.getBlue());
        int rgb = FogMath.mixRgb(world, snapshot.fogRgb(), snapshot.submersion());
        event.setRed(((rgb >> 16) & 0xFF) / 255.0F);
        event.setGreen(((rgb >> 8) & 0xFF) / 255.0F);
        event.setBlue((rgb & 0xFF) / 255.0F);
    }

    private static int packRgb(float red, float green, float blue) {
        int r = Math.round(Mth.clamp(red, 0.0F, 1.0F) * 255.0F);
        int g = Math.round(Mth.clamp(green, 0.0F, 1.0F) * 255.0F);
        int b = Math.round(Mth.clamp(blue, 0.0F, 1.0F) * 255.0F);
        return (r << 16) | (g << 8) | b;
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Entity entity = event.getCamera().getEntity();
        if (entity == null) {
            return;
        }

        // The pod keeps the water's own visibility. Compressing the feed's fog to the operator's
        // pressure is what turns the picture into a flat wash a few blocks wide — the darkness belongs
        // to the eyes standing at the controls, and a camera sent into the deep is exactly the tool
        // for seeing past it.
        if (isSensorPod(event.getCamera())) {
            return;
        }

        ClientFogState.Snapshot snapshot = ClientFogState.get();
        float submersion = snapshot.submersion();
        if (submersion <= 0.001F) {
            return;
        }

        // The eye's reach is crossfaded too, against whatever the world's fog would have been, so
        // surfacing opens the view up gradually rather than in one frame.
        event.setNearPlaneDistance(FogMath.lerp(submersion, event.getNearPlaneDistance(),
                snapshot.nearPlane()));
        event.setFarPlaneDistance(FogMath.lerp(submersion, event.getFarPlaneDistance(),
                snapshot.farPlane()));
        event.setFogShape(FogShape.CYLINDER);
        event.setCanceled(true);
    }

    /**
     * Whether this camera belongs to the drone whose feed is being rendered.
     *
     * <p>The feed's pass runs the level render a second time, on a camera of its own, in the same frame
     * and through the same fog events as the operator's. That pass has to be recognised here, or the
     * drone's depth and air supply would silently drive the operator's fog — and the drone's own
     * picture would inherit the operator's darkness, which is the one thing its camera exists to see
     * through.
     */
    private static boolean isSensorPod(Camera camera) {
        return camera.getEntity() != null && camera.getEntity() == ClientDroneState.drone();
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

        // The fog authority's own state, so a player with a shader pack can see which half of it is
        // doing the work instead of guessing from the picture.
        ClientFogState.Snapshot snapshot = ClientFogState.get();
        left.add(String.format(Locale.ROOT, "Fog: %s depth=%.2f in=%.0f%% near=%.1f far=%.1f shaders=%s veil=%s",
                snapshot.medium().name().toLowerCase(Locale.ROOT), snapshot.ramp(),
                snapshot.submersion() * 100.0F,
                snapshot.nearPlane(), snapshot.farPlane(),
                IrisCompat.isPackInUse() ? "yes" : "no",
                IrisCompat.isPackInUse() ? FogClientConfig.describe() : "n/a"));
    }

    private static void tickDrowningDarkness(Entity entity) {
        if (!(entity instanceof LivingEntity living)
                || !living.isInWater()
                || living.getAirSupply() > 0) {
            drowningStartMs = -1L;
            hadNegativeAir = false;
            ClientFogState.setDrowningDarkness(
                    Math.max(0.0F, ClientFogState.drowningDarkness() - CLEAR_SPEED));
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
        ClientFogState.setDrowningDarkness(Mth.clamp(elapsed / drowningDurationMs, 0.0F, 1.0F));
    }
}