package com.dexer.aquanaut.mixin;

import com.dexer.aquanaut.common.AirSupplyHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    /** Boosts air regen for living entities that have extra-air capacity. */
    @Inject(method = "increaseAirSupply(I)I", at = @At("HEAD"), cancellable = true)
    private void aquanaut$increaseAirSupply(int airSupply, CallbackInfoReturnable<Integer> cir) {
        if (!((Object) this instanceof LivingEntity livingEntity)) {
            return;
        }
        int regenPerTick = AirSupplyHelper.getRegenPerTick(livingEntity);
        if (regenPerTick <= 0) {
            return;
        }

        cir.setReturnValue(Math.min(airSupply + regenPerTick,
                ((Entity) (Object) this).getMaxAirSupply()));
    }
}