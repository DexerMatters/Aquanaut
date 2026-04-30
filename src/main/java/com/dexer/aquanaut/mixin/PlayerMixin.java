package com.dexer.aquanaut.mixin;

import com.dexer.aquanaut.common.diving.DivingEquipmentHelper;
import com.dexer.aquanaut.common.diving.DivingEquipmentSlotType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Player.class, remap = false)
public abstract class PlayerMixin {

    @Inject(method = "hurtArmor", at = @At("TAIL"), remap = false)
    private void aquanaut$hurtDivingEquipment(DamageSource damageSource, float damage, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        DivingEquipmentHelper.hurtEquippedItem(player, DivingEquipmentSlotType.MASK, damageSource, damage,
                EquipmentSlot.HEAD);
        DivingEquipmentHelper.hurtEquippedItem(player, DivingEquipmentSlotType.TANK, damageSource, damage,
                EquipmentSlot.CHEST);
        DivingEquipmentHelper.hurtEquippedItem(player, DivingEquipmentSlotType.FLIPPERS, damageSource, damage,
                EquipmentSlot.FEET);
    }

    @Inject(method = "hurtHelmet", at = @At("TAIL"), remap = false)
    private void aquanaut$hurtDivingMask(DamageSource damageSource, float damage, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        DivingEquipmentHelper.hurtEquippedItem(player, DivingEquipmentSlotType.MASK, damageSource, damage,
                EquipmentSlot.HEAD);
    }
}