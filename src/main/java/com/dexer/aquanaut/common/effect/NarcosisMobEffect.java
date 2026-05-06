package com.dexer.aquanaut.common.effect;

import com.dexer.aquanaut.common.AirSupplyHelper;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public final class NarcosisMobEffect extends MobEffect {

    public NarcosisMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x5A7896);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) {
            return true;
        }
        if (AirSupplyHelper.getTotalAir(entity) <= 0) {
            return false;
        }
        int airLoss = amplifier + 1;
        AirSupplyHelper.removeAir(entity, airLoss);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}