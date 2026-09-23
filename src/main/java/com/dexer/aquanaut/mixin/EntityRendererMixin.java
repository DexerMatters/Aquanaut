package com.dexer.aquanaut.mixin;

import com.dexer.aquanaut.client.light.ClientDynamicLightManager;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Raises vanilla entity packed light when an Aquanaut source is near the entity. */
@Mixin(value = EntityRenderer.class, remap = false)
public abstract class EntityRendererMixin {
    @Inject(method = "getPackedLightCoords", at = @At("RETURN"), cancellable = true, remap = false)
    private void aquanaut$applyDynamicLight(Entity entity, float partialTick,
            CallbackInfoReturnable<Integer> cir) {
        if (entity.level() instanceof net.minecraft.client.multiplayer.ClientLevel level) {
            cir.setReturnValue(ClientDynamicLightManager.applyPackedLight(level,
                    entity.blockPosition(), cir.getReturnValueI()));
        }
    }
}
