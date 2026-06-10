package com.dexer.aquanaut.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.common.NeoForgeMod;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class RadioanemoneEntity extends Mob implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("radioanemone.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public RadioanemoneEntity(EntityType<? extends Mob> type, Level level) { super(type, level); }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "ctrl", 0, s -> s.setAndContinue(IDLE)));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    public static AttributeSupplier createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 18.0D).add(Attributes.KNOCKBACK_RESISTANCE, 1.0D).build();
    }

    @Override public void aiStep() { super.aiStep(); }
    @Override public boolean isPushable() { return false; }
    @Override protected boolean isImmobile() { return true; }
    @Override public boolean canDrownInFluidType(FluidType type) { return false; }

    @Override
    public void baseTick() {
        super.baseTick();
        if (this.isEyeInFluidType(NeoForgeMod.WATER_TYPE.value())) {
            this.setAirSupply(this.getMaxAirSupply());
        }
    }
}
