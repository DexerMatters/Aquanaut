package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.AirSupplyHelper;
import com.dexer.aquanaut.common.ai.FishAttackMode;
import com.dexer.aquanaut.common.ai.FishResponseMode;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * Ecofish — not a fish, a drifting reef.
 *
 * <p>
 * Coral grows from its back and small life shelters under its bell, so it is a mobile oasis: it
 * breathes usable oxygen into the water around it, heals the creatures that swim with it and seeds
 * seaweed on suitable seabed as it passes. It has no attack at all.
 */
public class EcofishEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM_ANIMATION = RawAnimation.begin().thenLoop("swim");

    private static final double OASIS_RANGE = 8.0D;
    private static final int AIR_GIFT_INTERVAL = 40;
    private static final int AIR_GIFT_TICKS = 40;
    private static final int HEAL_INTERVAL = 60;
    private static final int REGENERATION_TICKS = 120;
    private static final int PLANT_INTERVAL = 400;
    private static final int PLANT_RADIUS = 4;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public EcofishEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 4, state -> {
            state.getController().setAnimationSpeed(animSpeed(0.2, 0.6, 0.5, 0.9));
            return state.setAndContinue(SWIM_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.20D)
                .build();
    }

    @Override
    protected FishResponseMode getResponseMode() {
        return FishResponseMode.PASSIVE;
    }

    @Override
    protected FishAttackMode getAttackMode() {
        return FishAttackMode.NONE;
    }

    @Override
    protected double getCruiseAcceleration() {
        return 0.0018D;
    }

    @Override
    protected double getCruiseMaxSpeed() {
        return 0.030D;
    }

    @Override
    protected double getEscapeAcceleration() {
        return 0.004D;
    }

    @Override
    protected double getEscapeMaxSpeed() {
        return 0.05D;
    }

    @Override
    protected double getWaterDrag() {
        return 0.93D;
    }

    @Override
    protected double getPlayerDetectionRange() {
        return 4.0D;
    }

    @Override
    protected float getCruiseYawTurnRateDegrees() {
        return 0.25F;
    }

    @Override
    protected float getCruisePitchTurnRateDegrees() {
        return 0.2F;
    }

    @Override
    protected int getCruiseYawDecisionMinTicks() {
        return 200;
    }

    @Override
    protected int getCruiseYawDecisionRandomTicks() {
        return 120;
    }

    @Override
    protected int getCruisePitchDecisionMinTicks() {
        return 200;
    }

    @Override
    protected int getCruisePitchDecisionRandomTicks() {
        return 120;
    }

    @Override
    protected double getCruiseDepthRange() {
        return 6.0D;
    }

    @Override
    protected float getBodyTurnRateDegrees() {
        return 0.8F;
    }

    @Override
    protected float getMaxTiltDegrees() {
        return 10.0F;
    }

    @Override
    protected float getHitboxPickInflation() {
        return 0.35F;
    }

    @Override
    protected double getHitboxVisualYOffset() {
        return 7.58D;
    }

    @Override
    protected double getHitboxPitchPivotOffsetY() {
        return 0.5D;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.isEffectiveAi() || this.level().isClientSide) {
            return;
        }

        if (this.tickCount % AIR_GIFT_INTERVAL == 0) {
            this.breatheIntoWater();
        }
        if (this.tickCount % HEAL_INTERVAL == 0) {
            this.shelterNearbyLife();
        }
        if (this.tickCount % PLANT_INTERVAL == 0) {
            this.seedSeabed();
        }
    }

    private void breatheIntoWater() {
        List<Player> nearby = this.level().getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(OASIS_RANGE), player -> !player.isCreative() && !player.isSpectator());
        for (Player player : nearby) {
            AirSupplyHelper.addAir(player, AIR_GIFT_TICKS);
        }
    }

    private void shelterNearbyLife() {
        List<BaseFishEntity> shoal = this.level().getEntitiesOfClass(BaseFishEntity.class,
                this.getBoundingBox().inflate(OASIS_RANGE), entity -> entity != this && entity.isAlive());
        for (BaseFishEntity fish : shoal) {
            fish.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGENERATION_TICKS, 0, true, false));
        }
    }

    private void seedSeabed() {
        BlockPos origin = this.blockPosition();
        for (int attempt = 0; attempt < 6; attempt++) {
            BlockPos candidate = origin.offset(
                    this.random.nextInt(PLANT_RADIUS * 2 + 1) - PLANT_RADIUS,
                    -2,
                    this.random.nextInt(PLANT_RADIUS * 2 + 1) - PLANT_RADIUS);

            BlockState ground = this.level().getBlockState(candidate);
            if (!ground.is(BlockRegistry.CORAL_SAND.get()) && !ground.is(BlockRegistry.NUTRIENT_RICH_MUD.get())) {
                continue;
            }

            BlockPos above = candidate.above();
            BlockState space = this.level().getBlockState(above);
            if (!space.is(Blocks.WATER)) {
                continue;
            }

            this.level().setBlockAndUpdate(above, BlockRegistry.SEAWEED.get().defaultBlockState());
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, above.getX() + 0.5D, above.getY() + 0.5D,
                        above.getZ() + 0.5D, 8, 0.3D, 0.3D, 0.3D, 0.01D);
            }
            return;
        }
    }
}
