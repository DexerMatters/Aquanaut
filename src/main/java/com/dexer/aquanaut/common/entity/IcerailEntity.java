package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.ai.FishAttackMode;
import com.dexer.aquanaut.common.ai.FishResponseMode;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class IcerailEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM_ANIMATION = RawAnimation.begin().thenLoop("swim");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Map<BlockPos, Integer> frozenWaterTimers = new HashMap<>();

    private int barrierCooldownTicks;

    public IcerailEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            double speed = this.getDeltaMovement().length();
            double referenceSpeed = this.isSprintingAway() ? this.escapeMaxSpeed() : this.cruiseMaxSpeed();
            double normalizedSpeed = referenceSpeed <= 1.0E-4D ? 0.0D : Mth.clamp(speed / referenceSpeed, 0.0D, 1.0D);
            double minAnimSpeed = this.isSprintingAway() ? 1.0D : 0.52D;
            double maxAnimSpeed = this.isSprintingAway() ? 2.35D : 1.02D;
            double animationSpeed = Mth.lerp(normalizedSpeed, minAnimSpeed, maxAnimSpeed);
            state.getController().setAnimationSpeed(animationSpeed);
            return state.setAndContinue(SWIM_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.level().isClientSide) {
            return;
        }

        this.tickFrozenWater();
        if (this.barrierCooldownTicks > 0) {
            this.barrierCooldownTicks--;
        }

        if (!this.isInWater() || !this.isSprintingAway() || this.barrierCooldownTicks > 0) {
            return;
        }

        Player threat = this.findThreatPlayer();
        if (threat == null) {
            return;
        }

        this.createFrozenBarrier(threat);
        this.barrierCooldownTicks = this.getBarrierCooldownTicks();
    }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 2.5D)
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

    protected int getBarrierCooldownTicks() {
        return 34;
    }

    protected int getBarrierSegmentCount() {
        return 2;
    }

    protected double getBarrierStartDistanceFromTail() {
        return 0.28D;
    }

    protected double getBarrierMinDistanceFromBody() {
        return 0.52D;
    }

    protected double getBarrierSegmentSpacing() {
        return 0.58D;
    }

    protected int getFrozenWaterLifetimeTicks() {
        return 72;
    }

    @Override
    protected double getCruiseAcceleration() {
        return 0.008D;
    }

    @Override
    protected double getCruiseMaxSpeed() {
        return 0.11D;
    }

    @Override
    protected double getEscapeAcceleration() {
        return 0.08D;
    }

    @Override
    protected double getEscapeMaxSpeed() {
        return 0.8D;
    }

    @Override
    protected double getChargeAcceleration() {
        return 0.082D;
    }

    @Override
    protected double getChargeMaxSpeed() {
        return 0.82D;
    }

    @Override
    protected double getWaterDrag() {
        return 0.91D;
    }

    @Override
    protected double getPlayerDetectionRange() {
        return 14.0D;
    }

    @Override
    protected float getEscapeTurnRateDegrees() {
        return 17.5F;
    }

    @Override
    protected float getChargeTurnRateDegrees() {
        return 18.5F;
    }

    @Override
    protected float getBodyTurnRateDegrees() {
        return 10.8F;
    }

    @Override
    protected float getMaxTiltDegrees() {
        return 30.0F;
    }

    @Override
    protected int getSprintPropulsionIntervalTicks() {
        return 10;
    }

    @Override
    protected int getSprintPropulsionBurstTicks() {
        return 4;
    }

    @Override
    protected double getSprintPropulsionGlideAccelerationFactor() {
        return 0.45D;
    }

    @Override
    protected double getSprintPropulsionBurstAccelerationFactor() {
        return 2.0D;
    }

    @Override
    protected int getReactiveMemoryTicks() {
        return 130;
    }

    @Override
    protected int getBiteCooldownTicks() {
        return 14;
    }

    @Override
    protected int getPassByRetreatTicks() {
        return 22;
    }

    @Override
    protected int getPassByReengageCooldownTicks() {
        return 20;
    }

    @Override
    protected float getHitboxPickInflation() {
        return 0.32F;
    }

    @Override
    protected double getHitboxVisualYOffset() {
        return -0.36D;
    }

    private Player findThreatPlayer() {
        Player player = this.level().getNearestPlayer(this, this.playerDetectionRange());
        if (player == null || !player.isAlive() || player.isCreative() || player.isSpectator()) {
            return null;
        }
        return this.hasLineOfSight(player) ? player : null;
    }

    private void createFrozenBarrier(Player player) {
        Vec3 forwardDirection = Vec3.directionFromRotation(this.getXRot(), this.getYRot()).normalize();
        Vec3 fishCenter = this.position().add(0.0D, this.getBbHeight() * 0.42D, 0.0D);
        Vec3 tailOrigin = fishCenter.subtract(forwardDirection.scale(Math.max(0.6D, this.getBbWidth() * 0.95D)));
        Vec3 playerCenter = player.position().add(0.0D, player.getBbHeight() * 0.38D, 0.0D);

        Vec3 toPlayer = playerCenter.subtract(tailOrigin);
        if (toPlayer.lengthSqr() < 1.0E-6D) {
            return;
        }

        Vec3 shotDirection = toPlayer.normalize();
        double rayLength = toPlayer.length();
        int segments = this.getRandom().nextBoolean() ? 1 : Math.max(1, this.getBarrierSegmentCount());
        double spacing = Math.max(0.3D, this.getBarrierSegmentSpacing());
        double startDistance = Math.max(0.0D, this.getBarrierStartDistanceFromTail());
        double minBodyDistanceSqr = this.getBarrierMinDistanceFromBody() * this.getBarrierMinDistanceFromBody();

        if (rayLength <= startDistance + 0.1D) {
            return;
        }

        for (int i = 0; i < segments; i++) {
            double segmentDistance = startDistance + i * spacing;
            if (segmentDistance > rayLength - 0.1D) {
                break;
            }

            Vec3 samplePoint = tailOrigin.add(shotDirection.scale(segmentDistance));
            if (samplePoint.distanceToSqr(fishCenter) < minBodyDistanceSqr) {
                continue;
            }
            this.freezeWaterNear(samplePoint);
        }
    }

    private void freezeWaterNear(Vec3 samplePoint) {
        BlockPos basePos = BlockPos.containing(samplePoint);
        if (this.freezeWaterAt(basePos)) {
            return;
        }
        if (this.freezeWaterAt(basePos.below())) {
            return;
        }
        this.freezeWaterAt(basePos.above());
    }

    private boolean freezeWaterAt(BlockPos pos) {
        if (!this.level().isLoaded(pos)) {
            return false;
        }

        if (!this.level().getFluidState(pos).is(FluidTags.WATER)) {
            return false;
        }

        BlockState state = this.level().getBlockState(pos);
        if (!state.getFluidState().is(FluidTags.WATER)) {
            return false;
        }

        this.level().setBlock(pos, Blocks.FROSTED_ICE.defaultBlockState(), 3);
        this.frozenWaterTimers.put(pos.immutable(), this.getFrozenWaterLifetimeTicks());
        return true;
    }

    private void tickFrozenWater() {
        Iterator<Map.Entry<BlockPos, Integer>> iterator = this.frozenWaterTimers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = iterator.next();
            BlockPos pos = entry.getKey();
            BlockState state = this.level().getBlockState(pos);

            if (!state.is(Blocks.FROSTED_ICE)) {
                iterator.remove();
                continue;
            }

            int remainingTicks = entry.getValue() - 1;
            if (remainingTicks > 0) {
                entry.setValue(remainingTicks);
                continue;
            }

            this.level().setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
            iterator.remove();
        }
    }
}
