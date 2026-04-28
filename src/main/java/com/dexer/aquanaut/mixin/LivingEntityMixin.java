package com.dexer.aquanaut.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dexer.aquanaut.common.AirRegenTracker;
import com.dexer.aquanaut.core.AttachmentRegistry;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    /**
     * Extra regen per tick added on top of vanilla respiration / bubble-column
     * rates. Only applies when the player already has extra-air capacity.
     */
    private static final int EXTRA_AIR_REGEN_PER_TICK = 24;

    /** Boosts the main air regen rate for players who have extra-air capacity. */
    @Inject(method = "increaseAirSupply(I)I", at = @At("HEAD"), cancellable = true)
    private void aquanaut$increaseAirSupply(int airSupply, CallbackInfoReturnable<Integer> cir) {
        if (!((Object) this instanceof Player player))
            return;
        // Only boost regen if the player has any extra-air capacity
        if (!player.hasData(AttachmentRegistry.MAX_EXTRA_AIR_SUPPLY))
            return;
        int maxExtra = player.getData(AttachmentRegistry.MAX_EXTRA_AIR_SUPPLY);
        if (maxExtra <= 0)
            return;
        AirRegenTracker.AIR_INCREASE_CALLED.add(player.getUUID());
        cir.setReturnValue(Math.min(airSupply + EXTRA_AIR_REGEN_PER_TICK,
                ((Entity) (Object) this).getMaxAirSupply()));
    }
}