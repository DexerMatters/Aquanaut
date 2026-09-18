package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.AirSupplyHelper;
import com.dexer.aquanaut.common.ai.FishAttackMode;
import com.dexer.aquanaut.common.ai.FishResponseMode;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Vamprey — a ribbon of pale muscle that feeds on air as much as on flesh.
 *
 * <p>
 * It latches on with a sucker ring of recurved fangs. A landed bite does little direct harm, but it
 * drains the victim's air supply and heals the lamprey, so a diver low on air has to break contact
 * rather than trade blows.
 */
public class VampreyEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM_ANIMATION = RawAnimation.begin().thenLoop("swim");
    private static final RawAnimation CHARGE_ANIMATION = RawAnimation.begin().thenLoop("charge");
    private static final RawAnimation OPEN_ANIMATION = RawAnimation.begin().thenPlay("open");
    private static final RawAnimation CLOSE_ANIMATION = RawAnimation.begin().thenPlay("close");

    private static final EntityDataAccessor<Integer> BITE_TIMER = SynchedEntityData.defineId(
            VampreyEntity.class, EntityDataSerializers.INT);

    private static final int BITE_OPEN_TICKS = 8;
    private static final int BITE_TOTAL_TICKS = 18;
    private static final int AIR_DRAIN_TICKS = 60;
    private static final float BITE_HEAL = 1.0F;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public VampreyEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BITE_TIMER, 0);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 1, state -> {
            int biteTimer = this.getBiteTimer();
            if (biteTimer > 0) {
                state.getController().setAnimationSpeed(1.0D);
                return state.setAndContinue(biteTimer > BITE_OPEN_TICKS ? OPEN_ANIMATION : CLOSE_ANIMATION);
            }
            if (this.isChargingPlayer()) {
                state.getController().setAnimationSpeed(1.0D);
                return state.setAndContinue(CHARGE_ANIMATION);
            }
            state.getController().setAnimationSpeed(animSpeed(0.6, 1.25, 1.3, 2.1));
            return state.setAndContinue(SWIM_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 1.5D)
                .build();
    }

    public int getBiteTimer() {
        return this.entityData.get(BITE_TIMER);
    }

    private void setBiteTimer(int ticks) {
        this.entityData.set(BITE_TIMER, ticks);
    }

    @Override
    protected FishResponseMode getResponseMode() {
        // A lamprey hunts by smell: it closes on a diver the moment it notices one instead of only
        // retaliating after being hit.
        return FishResponseMode.CHARGE;
    }

    @Override
    protected FishAttackMode getAttackMode() {
        return FishAttackMode.TRACKING_BITE;
    }

    @Override
    protected double getBaseBiteDamage() {
        return 1.5D;
    }

    @Override
    protected int getBiteCooldownTicks() {
        return 14;
    }

    @Override
    protected double getBiteReachBonus() {
        return 0.55D;
    }

    @Override
    protected boolean getEscapeLaunchBehaviorEnabled() {
        return true;
    }

    @Override
    protected int getEscapeLaunchAnimationTicks() {
        return 10;
    }

    @Override
    protected double getEscapeLaunchBurstSpeed() {
        return 0.72D;
    }

    @Override
    protected double getPlayerDetectionRange() {
        return 9.0D;
    }

    @Override
    protected double getCruiseAcceleration() {
        return 0.018D;
    }

    @Override
    protected double getCruiseMaxSpeed() {
        return 0.24D;
    }

    @Override
    protected double getEscapeAcceleration() {
        return 0.055D;
    }

    @Override
    protected double getEscapeMaxSpeed() {
        return 0.62D;
    }

    @Override
    protected double getChargeAcceleration() {
        return 0.042D;
    }

    @Override
    protected double getChargeMaxSpeed() {
        return 0.44D;
    }

    @Override
    protected double getWaterDrag() {
        return 0.90D;
    }

    @Override
    protected float getEscapeTurnRateDegrees() {
        return 20.0F;
    }

    @Override
    protected float getChargeTurnRateDegrees() {
        return 18.0F;
    }

    @Override
    protected float getBodyTurnRateDegrees() {
        return 9.0F;
    }

    @Override
    protected float getMaxTiltDegrees() {
        return 30.0F;
    }

    @Override
    protected boolean getIsFlexibleBody() {
        return true;
    }

    @Override
    protected float getHitboxPickInflation() {
        return 0.18F;
    }

    @Override
    protected double getHitboxVisualYOffset() {
        return 0.10D;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.isEffectiveAi() || this.level().isClientSide) {
            return;
        }

        int biteTimer = this.getBiteTimer();
        if (biteTimer > 0) {
            this.setBiteTimer(biteTimer - 1);
        }
    }

    @Override
    public void onSuccessfulBite(Player player) {
        // Drain the diver's air rather than their health, then feed on it.
        AirSupplyHelper.removeAir(player, AIR_DRAIN_TICKS);
        this.heal(BITE_HEAL);
        this.setBiteTimer(BITE_TOTAL_TICKS);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, player.getX(),
                    player.getY() + player.getBbHeight() * 0.7D, player.getZ(), 4, 0.15D, 0.15D, 0.15D, 0.0D);
        }
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.GENERIC_DRINK, SoundSource.HOSTILE, 0.6F, 1.6F);
    }
}
