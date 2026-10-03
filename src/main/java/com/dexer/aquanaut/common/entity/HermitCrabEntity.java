package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.mud.MudZoneConfig;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class HermitCrabEntity extends WaterAnimal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation HIDE = RawAnimation.begin().thenPlay("hide");
    private static final EntityDataAccessor<Boolean> SHELLED = SynchedEntityData.defineId(HermitCrabEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SHELL_SIZE = SynchedEntityData.defineId(HermitCrabEntity.class,
            EntityDataSerializers.INT);
    private int shellTicks;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public HermitCrabEntity(EntityType<? extends WaterAnimal> type, Level level) { super(type, level); }
    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.12D).build();
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SHELLED, false);
        builder.define(SHELL_SIZE, 0);
    }
    @Override public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) {
            setShelled(true);
        }
        return super.hurt(source, isShelled() ? amount * MudZoneConfig.SHELL_DAMAGE_MULTIPLIER : amount);
    }
    @Override public void aiStep() {
        if (isShelled()) {
            if (!level().isClientSide && --shellTicks <= 0) {
                setShelled(false);
            }
            return;
        }
        if (!level().isClientSide && tickCount % 40 == 0 && shellSize() == 0
                && level().getBlockStates(new net.minecraft.world.phys.AABB(blockPosition()).inflate(
                        MudZoneConfig.SHELL_SEARCH_RADIUS))
                        .anyMatch(state -> state.is(BlockRegistry.SHELL_BLOCK.get())
                                || state.is(BlockRegistry.HARD_SHELL_BLOCK.get())
                                || state.is(BlockRegistry.SHELL_PILE.get()))
                && random.nextFloat() < MudZoneConfig.SHELL_UPGRADE_CHANCE) {
            entityData.set(SHELL_SIZE, 1);
        }
        super.aiStep();
    }
    public boolean isShelled() { return entityData.get(SHELLED); }
    public int shellSize() { return entityData.get(SHELL_SIZE); }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (isShelled()) {
                return state.setAndContinue(HIDE);
            }
            return state.setAndContinue(IDLE);
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    private void setShelled(boolean value) {
        entityData.set(SHELLED, value);
        if (value) shellTicks = MudZoneConfig.SHELL_DURATION_TICKS;
    }
}
