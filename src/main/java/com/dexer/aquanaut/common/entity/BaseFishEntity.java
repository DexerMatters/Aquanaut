package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.ai.FishAttackMode;
import com.dexer.aquanaut.common.ai.FishMovementController;
import com.dexer.aquanaut.common.ai.FishResponseMode;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public abstract class BaseFishEntity extends WaterAnimal {
    private static final EntityDataAccessor<Boolean> SPRINTING_AWAY = SynchedEntityData.defineId(BaseFishEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CHARGING_PLAYER = SynchedEntityData.defineId(BaseFishEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ESCAPE_LAUNCHING = SynchedEntityData.defineId(BaseFishEntity.class,
            EntityDataSerializers.BOOLEAN);

    private final FishMovementController movementController = new FishMovementController();

    protected BaseFishEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SPRINTING_AWAY, false);
        builder.define(CHARGING_PLAYER, false);
        builder.define(ESCAPE_LAUNCHING, false);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.isEffectiveAi()) {
            return;
        }

        this.movementController.tick(this);
        this.entityData.set(SPRINTING_AWAY, this.movementController.isSprintingAway());
        this.entityData.set(CHARGING_PLAYER, this.movementController.isChargingPlayer());
        this.entityData.set(ESCAPE_LAUNCHING, this.movementController.isEscapeLaunching());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && this.isEffectiveAi()) {
            this.movementController.onHurt(this, source);
        }

        return hurt;
    }

    public boolean isSprintingAway() {
        if (this.level().isClientSide) {
            return this.entityData.get(SPRINTING_AWAY);
        }

        return this.movementController.isSprintingAway();
    }

    public boolean isChargingPlayer() {
        if (this.level().isClientSide) {
            return this.entityData.get(CHARGING_PLAYER);
        }

        return this.movementController.isChargingPlayer();
    }

    public boolean isEscapeLaunching() {
        if (this.level().isClientSide) {
            return this.entityData.get(ESCAPE_LAUNCHING);
        }

        return this.movementController.isEscapeLaunching();
    }

    @Override
    public float getPickRadius() {
        return super.getPickRadius() + this.hitboxPickInflation();
    }

    public final double hitboxVisualYOffset() {
        return this.getHitboxVisualYOffset();
    }

    protected double getHitboxVisualYOffset() {
        return 0.0D;
    }

    public final double hitboxPitchPivotOffsetY() {
        return this.getHitboxPitchPivotOffsetY();
    }

    protected double getHitboxPitchPivotOffsetY() {
        return 0.0D;
    }

    public final FishResponseMode responseMode() {
        return this.getResponseMode();
    }

    protected FishResponseMode getResponseMode() {
        return FishResponseMode.AVOIDANCE;
    }

    public final FishAttackMode attackMode() {
        return this.getAttackMode();
    }

    protected FishAttackMode getAttackMode() {
        return FishAttackMode.NONE;
    }

    public final double cruiseAcceleration() {
        return this.getCruiseAcceleration();
    }

    protected double getCruiseAcceleration() {
        return 0.012D;
    }

    public final double cruiseMaxSpeed() {
        return this.getCruiseMaxSpeed();
    }

    protected double getCruiseMaxSpeed() {
        return 0.15D;
    }

    public final boolean schoolingEnabled() {
        return this.getSchoolingEnabled();
    }

    protected boolean getSchoolingEnabled() {
        return false;
    }

    public final boolean curvedCruiseMovement() {
        return this.getCurvedCruiseMovement();
    }

    protected boolean getCurvedCruiseMovement() {
        return false;
    }

    public final float cruiseCurveTorqueDegrees() {
        return this.getCruiseCurveTorqueDegrees();
    }

    protected float getCruiseCurveTorqueDegrees() {
        return 0.0F;
    }

    public final double escapeAcceleration() {
        return this.getEscapeAcceleration();
    }

    protected double getEscapeAcceleration() {
        return 0.032D;
    }

    public final double escapeMaxSpeed() {
        return this.getEscapeMaxSpeed();
    }

    protected double getEscapeMaxSpeed() {
        return 0.36D;
    }

    public final double chargeAcceleration() {
        return this.getChargeAcceleration();
    }

    protected double getChargeAcceleration() {
        return this.escapeAcceleration();
    }

    public final double chargeMaxSpeed() {
        return this.getChargeMaxSpeed();
    }

    protected double getChargeMaxSpeed() {
        return this.escapeMaxSpeed();
    }

    public final int cruisePropulsionIntervalTicks() {
        return this.getCruisePropulsionIntervalTicks();
    }

    protected int getCruisePropulsionIntervalTicks() {
        return 1;
    }

    public final int cruisePropulsionBurstTicks() {
        return this.getCruisePropulsionBurstTicks();
    }

    protected int getCruisePropulsionBurstTicks() {
        return 1;
    }

    public final double cruisePropulsionGlideAccelerationFactor() {
        return this.getCruisePropulsionGlideAccelerationFactor();
    }

    protected double getCruisePropulsionGlideAccelerationFactor() {
        return 1.0D;
    }

    public final double cruisePropulsionBurstAccelerationFactor() {
        return this.getCruisePropulsionBurstAccelerationFactor();
    }

    protected double getCruisePropulsionBurstAccelerationFactor() {
        return 1.0D;
    }

    public final int sprintPropulsionIntervalTicks() {
        return this.getSprintPropulsionIntervalTicks();
    }

    protected int getSprintPropulsionIntervalTicks() {
        return 1;
    }

    public final int sprintPropulsionBurstTicks() {
        return this.getSprintPropulsionBurstTicks();
    }

    protected int getSprintPropulsionBurstTicks() {
        return 1;
    }

    public final double sprintPropulsionGlideAccelerationFactor() {
        return this.getSprintPropulsionGlideAccelerationFactor();
    }

    protected double getSprintPropulsionGlideAccelerationFactor() {
        return 1.0D;
    }

    public final double sprintPropulsionBurstAccelerationFactor() {
        return this.getSprintPropulsionBurstAccelerationFactor();
    }

    protected double getSprintPropulsionBurstAccelerationFactor() {
        return 1.0D;
    }

    public final double waterDrag() {
        return this.getWaterDrag();
    }

    protected double getWaterDrag() {
        return 0.92D;
    }

    public final double playerDetectionRange() {
        return this.getPlayerDetectionRange();
    }

    protected double getPlayerDetectionRange() {
        return 10.0D;
    }

    public final float escapeTurnRateDegrees() {
        return this.getEscapeTurnRateDegrees();
    }

    protected float getEscapeTurnRateDegrees() {
        return 14.0F;
    }

    public final float chargeTurnRateDegrees() {
        return this.getChargeTurnRateDegrees();
    }

    protected float getChargeTurnRateDegrees() {
        return Math.max(15.0F, this.escapeTurnRateDegrees());
    }

    public final float chargePitchTurnRateDegrees() {
        return this.getChargePitchTurnRateDegrees();
    }

    protected float getChargePitchTurnRateDegrees() {
        return this.chargeTurnRateDegrees() * 0.82F;
    }

    public final float bodyTurnRateDegrees() {
        return this.getBodyTurnRateDegrees();
    }

    protected float getBodyTurnRateDegrees() {
        return 8.0F;
    }

    public final int reactiveMemoryTicks() {
        return this.getReactiveMemoryTicks();
    }

    protected int getReactiveMemoryTicks() {
        return 110;
    }

    public final int chargeTargetLostResetTicks() {
        return this.getChargeTargetLostResetTicks();
    }

    protected int getChargeTargetLostResetTicks() {
        return 24;
    }

    public final double behaviorPersistenceRangeMultiplier() {
        return this.getBehaviorPersistenceRangeMultiplier();
    }

    protected double getBehaviorPersistenceRangeMultiplier() {
        return 1.9D;
    }

    public final int biteCooldownTicks() {
        return this.getBiteCooldownTicks();
    }

    protected int getBiteCooldownTicks() {
        return 8;
    }

    public final double biteReachBonus() {
        return this.getBiteReachBonus();
    }

    protected double getBiteReachBonus() {
        return 0.85D;
    }

    public final double baseBiteDamage() {
        return this.getBaseBiteDamage();
    }

    protected double getBaseBiteDamage() {
        return 1.0D;
    }

    public final int passByRetreatTicks() {
        return this.getPassByRetreatTicks();
    }

    protected int getPassByRetreatTicks() {
        return 24;
    }

    public final int passByReengageCooldownTicks() {
        return this.getPassByReengageCooldownTicks();
    }

    protected int getPassByReengageCooldownTicks() {
        return 18;
    }

    public final double passByInertiaBoost() {
        return this.getPassByInertiaBoost();
    }

    protected double getPassByInertiaBoost() {
        return 0.22D;
    }

    public final double passByInertiaMaxSpeedMultiplier() {
        return this.getPassByInertiaMaxSpeedMultiplier();
    }

    protected double getPassByInertiaMaxSpeedMultiplier() {
        return 1.12D;
    }

    public final double chargeNoTurnDistance() {
        return this.getChargeNoTurnDistance();
    }

    protected double getChargeNoTurnDistance() {
        return 0.0D;
    }

    public final float hitboxPickInflation() {
        return this.getHitboxPickInflation();
    }

    protected float getHitboxPickInflation() {
        return 0.14F;
    }

    public final double passByStrikeForwardDistance() {
        return this.getPassByStrikeForwardDistance();
    }

    protected double getPassByStrikeForwardDistance() {
        return this.biteReachBonus() + this.getBbWidth() * 1.2D;
    }

    public final double passByStrikeRearAllowance() {
        return this.getPassByStrikeRearAllowance();
    }

    protected double getPassByStrikeRearAllowance() {
        return this.getBbWidth() * 0.7D;
    }

    public final double passByStrikeRadius() {
        return this.getPassByStrikeRadius();
    }

    protected double getPassByStrikeRadius() {
        return this.getBbWidth() * 0.85D + 0.65D;
    }

    public void onSuccessfulBite(Player player) {
    }

    public final float cruiseTurnChance() {
        return this.getCruiseTurnChance();
    }

    protected float getCruiseTurnChance() {
        return 0.08F;
    }

    public final float cruiseTurnRangeDegrees() {
        return this.getCruiseTurnRangeDegrees();
    }

    protected float getCruiseTurnRangeDegrees() {
        return 36.0F;
    }

    public final float cruisePitchRangeDegrees() {
        return this.getCruisePitchRangeDegrees();
    }

    protected float getCruisePitchRangeDegrees() {
        return 24.0F;
    }

    public final float cruisePitchTurnRateDegrees() {
        return this.getCruisePitchTurnRateDegrees();
    }

    protected float getCruisePitchTurnRateDegrees() {
        return 1.8F;
    }

    public final int cruisePitchDecisionMinTicks() {
        return this.getCruisePitchDecisionMinTicks();
    }

    protected int getCruisePitchDecisionMinTicks() {
        return 45;
    }

    public final int cruisePitchDecisionRandomTicks() {
        return this.getCruisePitchDecisionRandomTicks();
    }

    protected int getCruisePitchDecisionRandomTicks() {
        return 55;
    }

    public final double cruiseDepthRange() {
        return this.getCruiseDepthRange();
    }

    protected double getCruiseDepthRange() {
        return 4.5D;
    }

    public final double cruiseDepthPitchDistance() {
        return this.getCruiseDepthPitchDistance();
    }

    protected double getCruiseDepthPitchDistance() {
        return 2.2D;
    }

    public final double cruiseDepthEmergencyOffset() {
        return this.getCruiseDepthEmergencyOffset();
    }

    protected double getCruiseDepthEmergencyOffset() {
        return 2.5D;
    }

    public final double cruiseVerticalAssist() {
        return this.getCruiseVerticalAssist();
    }

    protected double getCruiseVerticalAssist() {
        return 0.024D;
    }

    public final float cruiseYawTurnRateDegrees() {
        return this.getCruiseYawTurnRateDegrees();
    }

    protected float getCruiseYawTurnRateDegrees() {
        return 2.4F;
    }

    public final int cruiseYawDecisionMinTicks() {
        return this.getCruiseYawDecisionMinTicks();
    }

    protected int getCruiseYawDecisionMinTicks() {
        return 28;
    }

    public final int cruiseYawDecisionRandomTicks() {
        return this.getCruiseYawDecisionRandomTicks();
    }

    protected int getCruiseYawDecisionRandomTicks() {
        return 32;
    }

    public final float maxTiltDegrees() {
        return this.getMaxTiltDegrees();
    }

    protected float getMaxTiltDegrees() {
        return 35.0F;
    }

    public final int collisionTurnCooldownTicks() {
        return this.getCollisionTurnCooldownTicks();
    }

    protected int getCollisionTurnCooldownTicks() {
        return 8;
    }

    public final boolean escapeLaunchBehaviorEnabled() {
        return this.getEscapeLaunchBehaviorEnabled();
    }

    protected boolean getEscapeLaunchBehaviorEnabled() {
        return false;
    }

    public final int escapeLaunchAnimationTicks() {
        return this.getEscapeLaunchAnimationTicks();
    }

    protected int getEscapeLaunchAnimationTicks() {
        return 0;
    }

    public final int escapeLaunchBurstLeadTicks() {
        return this.getEscapeLaunchBurstLeadTicks();
    }

    protected int getEscapeLaunchBurstLeadTicks() {
        return 0;
    }

    public final double escapeLaunchPrepDrag() {
        return this.getEscapeLaunchPrepDrag();
    }

    protected double getEscapeLaunchPrepDrag() {
        return 0.84D;
    }

    public final double escapeLaunchBurstSpeed() {
        return this.getEscapeLaunchBurstSpeed();
    }

    protected double getEscapeLaunchBurstSpeed() {
        return this.escapeMaxSpeed();
    }

    public final double escapeLaunchPostBurstDrag() {
        return this.getEscapeLaunchPostBurstDrag();
    }

    protected double getEscapeLaunchPostBurstDrag() {
        return this.waterDrag();
    }

    public final double escapeLaunchSustainAcceleration() {
        return this.getEscapeLaunchSustainAcceleration();
    }

    protected double getEscapeLaunchSustainAcceleration() {
        return this.escapeAcceleration();
    }

    public final double escapeLaunchMaxSpeed() {
        return this.getEscapeLaunchMaxSpeed();
    }

    protected double getEscapeLaunchMaxSpeed() {
        return Math.max(this.escapeMaxSpeed(), this.escapeLaunchBurstSpeed());
    }

    public final int escapeLaunchSteeringLockTicks() {
        return this.getEscapeLaunchSteeringLockTicks();
    }

    protected int getEscapeLaunchSteeringLockTicks() {
        return 0;
    }
}
