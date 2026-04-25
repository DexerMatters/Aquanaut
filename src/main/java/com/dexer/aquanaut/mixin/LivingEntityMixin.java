package com.dexer.aquanaut.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dexer.aquanaut.common.AirRegenTracker;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    private static final int PLAYER_AIR_REGEN_PER_TICK = 24;

    /** Boosts the main air regen rate for players and records the call. */
    @Inject(method = "increaseAirSupply(I)I", at = @At("HEAD"), cancellable = true)
    private void aquanaut$increaseAirSupply(int airSupply, CallbackInfoReturnable<Integer> cir) {
        if (!((Object) this instanceof Player player))
            return;
        AirRegenTracker.AIR_INCREASE_CALLED.add(player.getUUID());
        cir.setReturnValue(Math.min(airSupply + PLAYER_AIR_REGEN_PER_TICK, ((Entity) (Object) this).getMaxAirSupply()));
    }
}