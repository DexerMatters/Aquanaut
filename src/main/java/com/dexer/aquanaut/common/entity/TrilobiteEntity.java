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

/**
 * An ancient trilobite: a flat, segmented arthropod that scuttles across the mud zone floor.
 * When hurt it curls into a ball (a rolled trilobite) and shrugs off most of the blow for a
 * moment, then unrolls and goes on its way.
 */
public final class TrilobiteEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation CURL = RawAnimation.begin().thenPlay("curl");
    private static final EntityDataAccessor<Boolean> CURLED = SynchedEntityData.defineId(
            TrilobiteEntity.class, EntityDataSerializers.BOOLEAN);
    /** Ticks the trilobite stays rolled up after a hit. */
    private static final int CURL_TICKS = 60;
    /** Horizontal speed above which the trilobite reads as crawling rather than resting. */
    private static final double CRAWL_SPEED_SQR = 4.0E-4D;
    private int curlTicks;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TrilobiteEntity(EntityType<? extends WaterAnimal> type, Level level) { super(type, level); }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.10D).build();
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CURLED, false);
    }

    @Override public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) {
            entityData.set(CURLED, true);
            curlTicks = CURL_TICKS;
        }
        return super.hurt(source, isCurled() ? amount * 0.4F : amount);
    }

    @Override public void tick() {
        super.tick();
        if (!level().isClientSide && isCurled() && --curlTicks <= 0) {
            entityData.set(CURLED, false);
        }
    }

    public boolean isCurled() { return entityData.get(CURLED); }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (isCurled()) {
                return state.setAndContinue(CURL);
            }
            return state.setAndContinue(
                    getDeltaMovement().horizontalDistanceSqr() > CRAWL_SPEED_SQR ? WALK : IDLE);
        }));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override protected FishResponseMode getResponseMode() { return FishResponseMode.PASSIVE; }
    @Override protected FishAttackMode getAttackMode() { return FishAttackMode.NONE; }
    @Override protected double getCruiseMaxSpeed() { return 0.06D; }
    @Override protected double getCruiseAcceleration() { return 0.004D; }
    @Override protected double getCruiseFloorBias() { return 1.0D; }
    @Override protected double getCruiseDepthRange() { return 0.6D; }
}
