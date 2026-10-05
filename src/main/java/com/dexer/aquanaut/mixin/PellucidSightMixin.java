package com.dexer.aquanaut.mixin;

import com.dexer.aquanaut.common.effect.EffectLevelScaling;
import com.dexer.aquanaut.core.MobEffectRegistry;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lends pellucid the sight half of its bargain without handing the player a night vision effect to
 * carry around.
 *
 * <p>Every entry of the lightmap is already lifted towards its fully lit colour when the player has
 * night vision, by the scale {@code GameRenderer#getNightVisionScale} returns. Pellucid borrows that
 * branch instead of adding an effect instance: the gate is opened for it, and the scale it feeds in
 * is steady, where vanilla's own scale oscillates whenever a night vision effect is within ten
 * seconds of running out. That oscillation is exactly what a player sees if the effect is granted
 * and refreshed from a tick handler, which is why this effect does not do that.</p>
 *
 * <p>A player who really is carrying night vision keeps vanilla's behaviour, shimmer and all, and
 * the conduit power branch is passed through untouched -- the first redirect covers every
 * {@code hasEffect} lookup in the lightmap update, not only the night vision one. Both redirects
 * name the instruction's own owner and signature: the lightmap reads these off its local player,
 * so the bytecode says {@code LocalPlayer.hasEffect}, not the {@code LivingEntity} declaration it
 * inherits.</p>
 */
@Mixin(value = LightTexture.class, remap = false)
public abstract class PellucidSightMixin {

    @Redirect(method = "updateLightTexture", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/core/Holder;)Z"),
            remap = false)
    private boolean aquanaut$pellucidSeesInTheDark(LocalPlayer player, Holder<MobEffect> effect) {
        if (player.hasEffect(effect)) {
            return true;
        }
        return effect == MobEffects.NIGHT_VISION && player.hasEffect(MobEffectRegistry.PELLUCID);
    }

    @Redirect(method = "updateLightTexture", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;getNightVisionScale(Lnet/minecraft/world/entity/LivingEntity;F)F"),
            remap = false)
    private static float aquanaut$steadyPellucidSight(LivingEntity entity, float partialTicks) {
        if (entity.hasEffect(MobEffects.NIGHT_VISION)) {
            return GameRenderer.getNightVisionScale(entity, partialTicks);
        }
        MobEffectInstance pellucid = entity.getEffect(MobEffectRegistry.PELLUCID);
        return pellucid == null ? 0.0F : EffectLevelScaling.sightStrength(pellucid.getAmplifier());
    }
}
