package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.ai.FishAttackMode;
import com.dexer.aquanaut.common.ai.FishResponseMode;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Rare passive nautilus associated with fossil beds. */
public final class AncientNautilusEntity extends BaseFishEntity implements GeoEntity {
    private static final EntityDataAccessor<Boolean> SHELLED = SynchedEntityData.defineId(
            AncientNautilusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("swim");
    private static final RawAnimation HIDE = RawAnimation.begin().thenPlay("hide");
    private AnimatableInstanceCache cache;
    private int shellTicks;

    public AncientNautilusEntity(EntityType<? extends WaterAnimal> type, Level level) { super(type, level); }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.025D).build();
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SHELLED, false);
    }

    @Override public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, isShelled() ? amount * 0.35F : amount);
        if (hurt && !level().isClientSide) {
            entityData.set(SHELLED, true);
            shellTicks = 80;
        }
        return hurt;
    }

    @Override public void tick() {
        super.tick();
        if (!level().isClientSide && isShelled() && --shellTicks <= 0) entityData.set(SHELLED, false);
    }

    public boolean isShelled() { return entityData.get(SHELLED); }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0,
                state -> state.setAndContinue(isShelled() ? HIDE : SWIM)));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() {
        if (cache == null) cache = GeckoLibUtil.createInstanceCache(this);
        return cache;
    }
    @Override protected FishResponseMode getResponseMode() { return FishResponseMode.PASSIVE; }
    @Override protected FishAttackMode getAttackMode() { return FishAttackMode.NONE; }
    @Override protected double getCruiseMaxSpeed() { return 0.02D; }
    @Override protected double getCruiseAcceleration() { return 0.0008D; }
    @Override protected double getCruiseFloorBias() { return 0.85D; }
    @Override protected double getCruiseDepthRange() { return 1.8D; }
}
