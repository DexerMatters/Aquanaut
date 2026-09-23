package com.dexer.aquanaut.mixin;

import com.dexer.aquanaut.common.AirSupplyHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The mod's air-supply policy on the entity level.
 *
 * <p>
 * {@code Entity#getMaxAirSupply} decides how much air any entity may carry — overridden per entity
 * type by the diving rules in {@link AirSupplyHelper} — and {@code Entity#baseTick} is rewound to
 * let those rules drain and refill air on ordinary entities too.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "getMaxAirSupply", at = @At("HEAD"), cancellable = true)
    private void aquanaut$getMaxAirSupply(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof LivingEntity living && AirSupplyHelper.usesExtraAirSupply(living)) {
            cir.setReturnValue(AirSupplyHelper.BASE_AIR_SUPPLY_TICKS);
        }
    }

    /**
     * Double drowning damage for the player.
     * The vanilla drown hurt call is the first hurt() call inside baseTick().
     */
    @ModifyArg(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", ordinal = 0), index = 1)
    private float aquanaut$doubledrownDamage(float amount) {
        return (Object) this instanceof Player ? amount * 4.0F : amount;
    }

}
