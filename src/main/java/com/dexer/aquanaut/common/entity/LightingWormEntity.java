package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.ai.FishAttackMode;
import com.dexer.aquanaut.common.ai.FishResponseMode;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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

public class LightingWormEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM_ANIMATION = RawAnimation.begin().thenLoop("swim");

    private static final EntityDataAccessor<Boolean> IS_GLOWING = SynchedEntityData.defineId(
            LightingWormEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int glowTimer = 0;
    private int glowOffTimer = 0;

    public LightingWormEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_GLOWING, false);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            double animationSpeed = LightingWormAnimationSpeed.resolve(
                    this.getDeltaMovement().length(),
                    this.isSprintingAway() ? this.escapeMaxSpeed() : this.cruiseMaxSpeed(),
                    this.isSprintingAway());
            state.getController().setAnimationSpeed(animationSpeed);
            return state.setAndContinue(SWIM_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.06D)
                .build();
    }

    @Override
    protected FishResponseMode getResponseMode() {
        return FishResponseMode.AVOIDANCE;
    }

    @Override
    protected FishAttackMode getAttackMode() {
        return FishAttackMode.NONE;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            if (this.entityData.get(IS_GLOWING)) {
                glowTimer--;
                if (glowTimer <= 0) {
                    this.entityData.set(IS_GLOWING, false);
                    glowOffTimer = this.random.nextInt(60, 120);
                }
            } else {
                glowOffTimer--;
                if (glowOffTimer <= 0) {
                    this.entityData.set(IS_GLOWING, true);
                    glowTimer = this.random.nextInt(30, 80);
                }
            }
        }
    }

    public boolean isGlowing() {
        return this.entityData.get(IS_GLOWING);
    }

    @Override
    protected double getPlayerDetectionRange() {
        return 8.0D;
    }

    @Override
    protected double getCruiseAcceleration() {
        return 0.0025D;
    }

    @Override
    protected double getCruiseMaxSpeed() {
        return 0.038D;
    }

    @Override
    protected double getEscapeAcceleration() {
        return 0.022D;
    }

    @Override
    protected double getEscapeMaxSpeed() {
        return 0.28D;
    }

    @Override
    protected double getWaterDrag() {
        return 0.88D;
    }

    @Override
    protected float getCruiseTurnChance() {
        return 0.6F;
    }

    @Override
    protected float getCruiseTurnRangeDegrees() {
        return 48.0F;
    }

    @Override
    protected float getCruiseYawTurnRateDegrees() {
        return 0.42F;
    }

    @Override
    protected int getCruiseYawDecisionMinTicks() {
        return 100;
    }

    @Override
    protected int getCruiseYawDecisionRandomTicks() {
        return 80;
    }

    @Override
    protected float getCruisePitchTurnRateDegrees() {
        return 0.42F;
    }

    @Override
    protected int getCruisePitchDecisionMinTicks() {
        return 80;
    }

    @Override
    protected int getCruisePitchDecisionRandomTicks() {
        return 70;
    }

    @Override
    protected double getCruiseDepthRange() {
        return 4.0D;
    }

    @Override
    protected double getCruiseDepthPitchDistance() {
        return 3.0D;
    }

    @Override
    protected double getCruiseDepthEmergencyOffset() {
        return 2.2D;
    }

    @Override
    protected double getCruiseVerticalAssist() {
        return 0.006D;
    }

    @Override
    protected boolean getIsFlexibleBody() {
        return true;
    }

    @Override
    protected float getMaxTiltDegrees() {
        return 50.0F;
    }

    @Override
    protected float getBodyTurnRateDegrees() {
        return 7.0F;
    }

    @Override
    protected float getHitboxPickInflation() {
        return 0.3F;
    }

    @Override
    protected double getHitboxVisualYOffset() {
        return -0.1D;
    }

    @Override
    protected double getHitboxPitchPivotOffsetY() {
        return 0.2D;
    }

    @Override
    protected float getEscapeTurnRateDegrees() {
        return 12.0F;
    }

    @Override
    protected int getReactiveMemoryTicks() {
        return 130;
    }

    @Override
    protected double getBehaviorPersistenceRangeMultiplier() {
        return 1.5D;
    }
}
