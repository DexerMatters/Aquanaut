package com.dexer.aquanaut.mixin;

import com.dexer.aquanaut.common.gaze.GazeHelper;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemStack.class, remap = false)
public abstract class ItemStackMixin {

    @Inject(method = "hasFoil", at = @At("HEAD"), cancellable = true, remap = false)
    private void aquanaut$suppressFoilForGaze(CallbackInfoReturnable<Boolean> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (GazeHelper.hasGaze(self)) {
            cir.setReturnValue(false);
        }
    }
}
