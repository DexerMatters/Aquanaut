package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.ai.FishAttackMode;
import com.dexer.aquanaut.common.ai.FishResponseMode;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class CreeporpedoEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM_ANIMATION = RawAnimation.begin().thenLoop("swim");
    private static final RawAnimation IGNITE_ANIMATION = RawAnimation.begin().thenLoop("ignite");

    private static final EntityDataAccessor<Boolean> IS_IGNITED = SynchedEntityData.defineId(
            CreeporpedoEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> IGNITE_TIMER_SYNC = SynchedEntityData.defineId(
            CreeporpedoEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final double IGNITE_DISTANCE = 3.0D;
    private static final int EXPLODE_DELAY_TICKS = 30;

    private int igniteTimer = 0;

    public CreeporpedoEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_IGNITED, false);
        builder.define(IGNITE_TIMER_SYNC, 0);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (this.isIgnited()) {
                state.getController().setAnimationSpeed(1.0D);
                return state.setAndContinue(IGNITE_ANIMATION);
            }

            if (this.isChargingPlayer()) {
                state.getController().setAnimationSpeed(this.getChargeAnimationSpeed());
            } else {
                state.getController().setAnimationSpeed(this.getSwimAnimationSpeed());
            }
            return state.setAndContinue(SWIM_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.12D)
                .build();
    }

    protected double getSwimAnimationSpeed() {
        return 0.5D;
    }

    protected double getChargeAnimationSpeed() {
        return 1.4D;
    }

    @Override
    protected FishResponseMode getResponseMode() {
        return FishResponseMode.CHARGE;
    }

    @Override
    protected FishAttackMode getAttackMode() {
        return FishAttackMode.NONE;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.isEffectiveAi() || this.isVehicle()) {
            return;
        }

        if (this.isIgnited()) {
            this.setDeltaMovement(0, 0, 0);
            this.hasImpulse = true;

            if (!this.level().isClientSide) {
                igniteTimer--;
                this.entityData.set(IGNITE_TIMER_SYNC, igniteTimer);
                if (igniteTimer <= 0) {
                    this.explode();
                }
            }
            return;
        }

        Player nearestPlayer = this.level().getNearestPlayer(this, this.getPlayerDetectionRange());
        if (nearestPlayer != null && this.isChargingPlayer()
                && this.distanceToSqr(nearestPlayer) < IGNITE_DISTANCE * IGNITE_DISTANCE) {
            this.ignite();
        }
    }

    private void ignite() {
        this.entityData.set(IS_IGNITED, true);
        igniteTimer = EXPLODE_DELAY_TICKS;
        this.entityData.set(IGNITE_TIMER_SYNC, igniteTimer);
        this.setDeltaMovement(0, 0, 0);
        this.hasImpulse = true;

        if (!this.level().isClientSide) {
            this.playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 1.0F);
        }
    }

    private void explode() {
        if (!this.level().isClientSide) {
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), 3.0F, Level.ExplosionInteraction.MOB);
            this.discard();
        }
    }

    public boolean isIgnited() {
        return this.entityData.get(IS_IGNITED);
    }

    public float getIgniteProgress() {
        if (!isIgnited()) return 0.0F;
        int remaining = this.entityData.get(IGNITE_TIMER_SYNC);
        return 1.0F - (float) remaining / EXPLODE_DELAY_TICKS;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.getItemInHand(hand).is(Items.FLINT_AND_STEEL) && !this.isIgnited()) {
            this.ignite();
            if (!this.level().isClientSide) {
                player.getItemInHand(hand).hurtAndBreak(1, player,
                        hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected double getPlayerDetectionRange() {
        return 14.0D;
    }

    @Override
    protected double getCruiseAcceleration() {
        return 0.005D;
    }

    @Override
    protected double getCruiseMaxSpeed() {
        return 0.06D;
    }

    @Override
    protected double getChargeAcceleration() {
        return 0.045D;
    }

    @Override
    protected double getChargeMaxSpeed() {
        return 0.5D;
    }

    @Override
    protected double getWaterDrag() {
        return 0.88D;
    }

    @Override
    protected float getCruiseTurnChance() {
        return 0.12F;
    }

    @Override
    protected float getCruiseTurnRangeDegrees() {
        return 32.0F;
    }

    @Override
    protected float getCruiseYawTurnRateDegrees() {
        return 1.2F;
    }

    @Override
    protected int getCruiseYawDecisionMinTicks() {
        return 60;
    }

    @Override
    protected int getCruiseYawDecisionRandomTicks() {
        return 50;
    }

    @Override
    protected float getCruisePitchTurnRateDegrees() {
        return 1.0F;
    }

    @Override
    protected int getCruisePitchDecisionMinTicks() {
        return 50;
    }

    @Override
    protected int getCruisePitchDecisionRandomTicks() {
        return 50;
    }

    @Override
    protected double getCruiseDepthRange() {
        return 4.5D;
    }

    @Override
    protected double getCruiseDepthPitchDistance() {
        return 2.5D;
    }

    @Override
    protected double getCruiseDepthEmergencyOffset() {
        return 2.6D;
    }

    @Override
    protected double getCruiseVerticalAssist() {
        return 0.015D;
    }

    @Override
    protected float getBodyTurnRateDegrees() {
        return 6.0F;
    }

    @Override
    protected float getMaxTiltDegrees() {
        return 30.0F;
    }

    @Override
    protected float getHitboxPickInflation() {
        return 0.4F;
    }

    @Override
    protected double getHitboxVisualYOffset() {
        return -0.7D;
    }

    @Override
    protected double getHitboxPitchPivotOffsetY() {
        return 0.5D;
    }

    @Override
    protected float getChargeTurnRateDegrees() {
        return 18.0F;
    }

    @Override
    protected int getChargeTargetLostResetTicks() {
        return 30;
    }
}
