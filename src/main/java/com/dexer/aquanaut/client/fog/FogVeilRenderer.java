package com.dexer.aquanaut.client.fog;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.fog.FogMath;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * The pack-proof half of the fog authority: a veil of the medium's own colour drawn over the
 * finished frame.
 *
 * <h3>Why a veil, and why here</h3>
 * A shader pack that reads vanilla fog already follows the mod through Iris's uniforms. One
 * that computes its own atmosphere replaces fog and light entirely, and no mod can reach
 * inside another pack's shaders — but nothing stops the mod drawing over the result. This
 * layer is therefore the guarantee that a deep ocean still looks deep whatever the pack does.
 *
 * <p>It is drawn from {@link RenderGuiEvent.Pre}: that is after the world render, and so
 * after Iris has composited its frame (which happens at the end of
 * {@code LevelRenderer#renderLevel}), and before the HUD — so the veil sits under the
 * crosshair, the hotbar and the drone's feed picture, which is why a camera sent into the
 * abyss still returns a watchable image. It is also drawn when the HUD is hidden, exactly as
 * vanilla's own water overlay is.</p>
 *
 * <p>Nothing is eased here. The colour, the opacity and the crossfade in and out of a medium
 * are all resolved and eased once, in {@link ClientFogState}, and this layer simply draws what
 * that decision says — which is what keeps the veil and the world's fog state in step instead
 * of each fading at its own rate.</p>
 */
@EventBusSubscriber(modid = Aquanaut.MODID, value = Dist.CLIENT)
public final class FogVeilRenderer {
    /** The top of the screen takes this fraction of the veil's opacity: the abyss presses down. */
    private static final float TOP_SHARE = 0.75F;

    private FogVeilRenderer() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientFogState.Snapshot snapshot = ClientFogState.get();
        if (minecraft.level == null || minecraft.player == null
                || snapshot.submersion() <= 0.001F
                || !snapshot.veiled()
                || !IrisCompat.isPackInUse()
                || !FogClientConfig.fallbackEnabled()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        float alpha = snapshot.veilAlpha() * snapshot.submersion();
        int rgb = snapshot.fogRgb();
        int bottom = argb(alpha, rgb);
        int top = argb(alpha * TOP_SHARE, rgb);
        graphics.fillGradient(0, 0, graphics.guiWidth(), graphics.guiHeight(), top, bottom);
    }

    private static int argb(float alpha, int rgb) {
        int a = Math.round(FogMath.clamp01(alpha) * 255.0F);
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}