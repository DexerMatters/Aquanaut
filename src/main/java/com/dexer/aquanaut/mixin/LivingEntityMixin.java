package com.dexer.aquanaut.mixin;

import com.dexer.aquanaut.common.AirSupplyHelper;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /**
     * Boosted air regen for entities with diving equipment.
     * <p>
     * Fills base air up to {@link AirSupplyHelper#BASE_AIR_SUPPLY_TICKS}.
     * Extra air regen is handled separately in {@code PlayerAirEvents} when base
     * is full.
     */
    @Inject(method = "increaseAirSupply(I)I", at = @At("HEAD"), cancellable = true)
    private void aquanaut$increaseAirSupply(int air, CallbackInfoReturnable<Integer> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        int regenPerTick = AirSupplyHelper.getRegenPerTick(self);
        if (regenPerTick <= 0) {
            return;
        }

        int baseMax = AirSupplyHelper.BASE_AIR_SUPPLY_TICKS;
        int newAir = Math.min(air + regenPerTick, baseMax);

        cir.setReturnValue(newAir);
    }
}
