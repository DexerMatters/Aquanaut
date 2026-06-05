package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.entity.serpentine.AbstractSerpentineEntity;
import com.dexer.aquanaut.common.entity.serpentine.SegmentChainAlgorithm;
import com.dexer.aquanaut.common.entity.serpentine.SegmentDefinition;
import com.dexer.aquanaut.common.entity.serpentine.SerpentineSegment;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class GiantOctopusTentacleEntity extends AbstractSerpentineEntity implements GeoEntity {

    private static final int SEGMENT_COUNT = 15;
    private static final float BASE_SECTION_SIZE = 4.0F;
    private static final float END_SECTION_SIZE = 1.6F;
    private static final float TIP_SIZE = 1.1F;
    private static final int WHIP_DURATION_TICKS = 36;
    private static final int WHIP_COOLDOWN_TICKS = 34;
    private static final int WHIP_RECOVERY_TICKS = 44;
    private static final int WHIP_HIT_COOLDOWN_TICKS = 10;
    private static final double WHIP_WINDUP_PORTION = 0.72D;
    private static final double PLAYER_DETECTION_RANGE = 25.0D;
    private static final double WHIP_DAMAGE = 16.0D;
    private static final double WHIP_REACH = 12.0D;
    private static final double WHIP_AIM_VERTICAL_SCALE = 0.18D;
    private static final double WHIP_AIM_VERTICAL_OFFSET_LIMIT = 0.8D;
    private static final double WHIP_REPULSION_STRENGTH = 2.35D;
    private static final double WHIP_REPULSION_VERTICAL_BOOST = 0.46D;
    private static final double WHIP_REPULSION_VERTICAL_SCALE = 0.28D;
    private static final int CLOSE_STRIKE_SEGMENT_COUNT = 6;
    private static final double CLOSE_STRIKE_HEAD_INFLATION = 0.9D;
    private static final double CLOSE_STRIKE_PROGRESS = WHIP_WINDUP_PORTION * 0.45D;
    private static final double WHIP_STRIKE_PULSE_WIDTH = 0.34D;
    private static final double WHIP_STRIKE_PULSE_THRESHOLD = 0.12D;
    private static final double WHIP_STRIKE_BOX_INFLATION = 0.4D;
    private static final double WHIP_STRIKE_SWEEP_INFLATION = 0.22D;
    private static final double BASE_SWAY_X = 3.6D;
    private static final double BASE_SWAY_Z = 2.8D;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int whipTicksRemaining;
    private int whipCooldownTicks;
    private int whipRecoveryTicksRemaining;
    private int whipHitCooldownTicks;
    private Vec3 whipDirection = new Vec3(0.0D, 1.0D, 0.0D);
    private Vec3 whipPlaneForward = new Vec3(0.0D, 0.0D, 1.0D);
    private Vec3[] segmentStrikeMotions = new Vec3[0];
    private double[] segmentStrikeWeights = new double[0];
    private Vec3 lastStrikeMotion = new Vec3(0.0D, 0.0D, 1.0D);

    public GiantOctopusTentacleEntity(EntityType<? extends GiantOctopusTentacleEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    @Override
    protected List<SegmentDefinition> createSegmentDefinitions() {
        List<SegmentDefinition> defs = new ArrayList<>(SEGMENT_COUNT);

        for (int i = 0; i < SEGMENT_COUNT - 1; i++) {
            float t = i / (float) (SEGMENT_COUNT - 2);
            float eased = (float) Math.pow(t, 0.82D);
            float size = Mth.lerp(eased, BASE_SECTION_SIZE, END_SECTION_SIZE);
            defs.add(SegmentDefinition.of(size, size, size));
        }

        defs.add(SegmentDefinition.of(TIP_SIZE, TIP_SIZE, TIP_SIZE));
        return defs;
    }

    @Override
    protected SegmentChainAlgorithm createChainAlgorithm() {
        return new Chain();
    }

    @Override
    protected void driveHead(ServerLevel level) {
        this.setDeltaMovement(Vec3.ZERO);
        this.hasImpulse = false;
        this.setXRot(0.0F);

        if (this.whipHitCooldownTicks > 0) {
            this.whipHitCooldownTicks--;
        }

        Player target = findThreatPlayer();
        Vec3 anchor = getAnchorCenter();
        boolean wasWhipping = this.whipTicksRemaining > 0;

        if (wasWhipping) {
            this.whipTicksRemaining--;

            if (target != null) {
                Vec3 desired = getWhipAimPoint(target, anchor).subtract(anchor);
                if (desired.lengthSqr() > 1.0E-5D) {
                    Vec3 desiredDirection = biasTowardHorizontal(desired, WHIP_AIM_VERTICAL_SCALE, this.whipDirection);
                    double smashEase = easeOutCubic(getWhipSmashProgress());
                    if (smashEase > 0.0D) {
                        this.whipDirection = blendDirection(this.whipDirection, desiredDirection,
                                0.02D + smashEase * 0.18D);
                        this.whipPlaneForward = blendDirection(this.whipPlaneForward,
                                horizontalDirectionOrFallback(desiredDirection, this.whipPlaneForward),
                                0.01D + smashEase * 0.08D);
                    }
                }
            }

            if (this.whipTicksRemaining == 0) {
                this.whipRecoveryTicksRemaining = WHIP_RECOVERY_TICKS;
                this.whipCooldownTicks = Math.max(this.whipCooldownTicks, WHIP_COOLDOWN_TICKS);
            }
        } else {
            if (this.whipCooldownTicks > 0) {
                this.whipCooldownTicks--;
            }

            if (this.whipRecoveryTicksRemaining > 0) {
                this.whipRecoveryTicksRemaining--;
            }
        }

        if (!wasWhipping && this.whipCooldownTicks <= 0 && this.whipRecoveryTicksRemaining <= 0 && target != null) {
            Vec3 desired = getWhipAimPoint(target, anchor).subtract(anchor);
            if (desired.lengthSqr() > 1.0E-5D) {
                this.whipDirection = biasTowardHorizontal(desired, WHIP_AIM_VERTICAL_SCALE, this.whipDirection);
                this.whipPlaneForward = horizontalDirectionOrFallback(this.whipDirection, this.whipPlaneForward);
                this.whipTicksRemaining = WHIP_DURATION_TICKS;
            }
        }

        double recoveryBlend = easeInOutSine(getWhipRecoveryProgress());
        Vec3 idleHorizontal = horizontalDirectionOrFallback(this.whipDirection, this.whipPlaneForward);
        Vec3 horizontal = isWhipping()
                ? this.whipPlaneForward
                : recoveryBlend > 0.0D
                        ? blendDirection(idleHorizontal, this.whipPlaneForward, recoveryBlend)
                        : idleHorizontal;
        if (horizontal.lengthSqr() > 1.0E-5D) {
            float targetYaw = (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(-horizontal.x, horizontal.z)));
            this.setYRot(Mth.approachDegrees(this.getYRot(), targetYaw,
                    isWhipping() ? 4.0F : recoveryBlend > 0.0D ? 2.0F + (float) (recoveryBlend * 2.0D) : 10.0F));
        }
    }

    @Override
    protected void afterSegmentsUpdated(ServerLevel level) {
        if (this.whipTicksRemaining <= 0 || this.whipHitCooldownTicks > 0) {
            return;
        }

        double whipProgress = getWhipProgress();
        double whipSmash = getWhipSmashProgress();
        boolean closeStrikeReady = whipProgress >= CLOSE_STRIKE_PROGRESS;
        if (whipSmash < 0.18D && !closeStrikeReady) {
            return;
        }

        double whipPulseCenter = 0.08D + easeOutCubic(whipSmash) * 0.98D;
        AABB headStrikeBox = this.getBoundingBox().inflate(CLOSE_STRIKE_HEAD_INFLATION);

        SerpentineSegment[] segments = this.getSegments();
        AABB strikeBounds = closeStrikeReady ? headStrikeBox : null;

        for (int i = 0; i < segments.length; i++) {
            if (!canStrikeSegment(i, whipPulseCenter, closeStrikeReady)) {
                continue;
            }

            AABB box = getStrikeBox(segments[i], i);
            strikeBounds = strikeBounds == null ? box : strikeBounds.minmax(box);
        }

        if (strikeBounds == null) {
            return;
        }

        List<Player> players = level.getEntitiesOfClass(Player.class, strikeBounds, this::isValidTarget);
        for (Player player : players) {
            if (closeStrikeReady && headStrikeBox.intersects(player.getBoundingBox())) {
                whipPlayer(player, getAnchorCenter(), 0);
                this.whipHitCooldownTicks = WHIP_HIT_COOLDOWN_TICKS;
                return;
            }

            for (int i = 0; i < segments.length; i++) {
                if (!canStrikeSegment(i, whipPulseCenter, closeStrikeReady)) {
                    continue;
                }

                SerpentineSegment segment = segments[i];
                if (!getStrikeBox(segment, i).intersects(player.getBoundingBox())) {
                    continue;
                }

                whipPlayer(player, segment.getCenterPos(), i);
                this.whipHitCooldownTicks = WHIP_HIT_COOLDOWN_TICKS;
                return;
            }
        }
    }

    private boolean canStrikeSegment(int segmentIndex, double whipPulseCenter, boolean closeStrikeReady) {
        return isStrikeSegment(segmentIndex, whipPulseCenter)
                || closeStrikeReady && segmentIndex < CLOSE_STRIKE_SEGMENT_COUNT;
    }

    private boolean isStrikeSegment(int segmentIndex, double whipPulseCenter) {
        if (segmentIndex < 0 || segmentIndex >= this.segmentStrikeWeights.length) {
            return false;
        }

        double pulse = pulseWave(this.segmentStrikeWeights[segmentIndex], whipPulseCenter, WHIP_STRIKE_PULSE_WIDTH);
        return pulse >= WHIP_STRIKE_PULSE_THRESHOLD;
    }

    @Override
    protected int decreaseAirSupply(int currentAir) {
        return currentAir;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public void travel(Vec3 travelVector) {
        this.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static AttributeSupplier createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.ATTACK_DAMAGE, WHIP_DAMAGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .build();
    }

    public Vec3 getAnchorCenter() {
        return new Vec3(this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ());
    }

    public boolean isWhipping() {
        return this.whipTicksRemaining > 0;
    }

    public boolean isRecovering() {
        return !isWhipping() && this.whipRecoveryTicksRemaining > 0;
    }

    public double getWhipProgress() {
        if (this.whipTicksRemaining <= 0) {
            return 0.0D;
        }

        return 1.0D - this.whipTicksRemaining / (double) WHIP_DURATION_TICKS;
    }

    public double getWhipWindupProgress() {
        if (this.whipTicksRemaining <= 0) {
            return 0.0D;
        }

        return Mth.clamp(getWhipProgress() / WHIP_WINDUP_PORTION, 0.0D, 1.0D);
    }

    public double getWhipSmashProgress() {
        if (this.whipTicksRemaining <= 0) {
            return 0.0D;
        }

        return Mth.clamp((getWhipProgress() - WHIP_WINDUP_PORTION) / (1.0D - WHIP_WINDUP_PORTION), 0.0D, 1.0D);
    }

    public double getWhipRecoveryProgress() {
        if (!isRecovering()) {
            return 0.0D;
        }

        return Mth.clamp(this.whipRecoveryTicksRemaining / (double) WHIP_RECOVERY_TICKS, 0.0D, 1.0D);
    }

    public double getWhipEnvelope() {
        double phase = getWhipProgress();
        if (phase <= 0.0D) {
            return 0.0D;
        }

        if (phase < WHIP_WINDUP_PORTION) {
            return easeInOutSine(phase / WHIP_WINDUP_PORTION);
        }

        double smashEase = easeOutCubic((phase - WHIP_WINDUP_PORTION) / (1.0D - WHIP_WINDUP_PORTION));
        return 1.0D - smashEase * 0.18D;
    }

    public Vec3 getWhipDirection() {
        return this.whipDirection;
    }

    private void whipPlayer(Player player, Vec3 impactPoint, int hitSegmentIndex) {
        DamageSource source = this.damageSources().mobAttack(this);
        float damage = (float) Math.max(WHIP_DAMAGE, this.getAttributeValue(Attributes.ATTACK_DAMAGE));
        player.hurt(source, damage);

        Vec3 repulsion = getWhipRepulsion(hitSegmentIndex, impactPoint, player);
        player.push(repulsion.x * WHIP_REPULSION_STRENGTH,
                Math.max(WHIP_REPULSION_VERTICAL_BOOST,
                        WHIP_REPULSION_VERTICAL_BOOST
                                + repulsion.y * WHIP_REPULSION_STRENGTH * WHIP_REPULSION_VERTICAL_SCALE),
                repulsion.z * WHIP_REPULSION_STRENGTH);
        player.hurtMarked = true;
    }

    private AABB getStrikeBox(SerpentineSegment segment, int segmentIndex) {
        AABB strikeBox = segment.getBoundingBox().inflate(WHIP_STRIKE_BOX_INFLATION);
        if (segmentIndex < 0 || segmentIndex >= this.segmentStrikeMotions.length) {
            return strikeBox;
        }

        Vec3 motion = this.segmentStrikeMotions[segmentIndex];
        if (motion.lengthSqr() < 1.0E-6D) {
            return strikeBox;
        }

        return strikeBox.expandTowards(motion.scale(-1.35D)).inflate(WHIP_STRIKE_SWEEP_INFLATION);
    }

    private Vec3 getWhipRepulsion(int hitSegmentIndex, Vec3 impactPoint, Player player) {
        Vec3 motion = hitSegmentIndex >= 0 && hitSegmentIndex < this.segmentStrikeMotions.length
                ? this.segmentStrikeMotions[hitSegmentIndex]
                : Vec3.ZERO;

        if (motion.lengthSqr() > 1.0E-6D && this.lastStrikeMotion.lengthSqr() > 1.0E-6D) {
            motion = blendDirection(this.lastStrikeMotion, motion.normalize(), 0.72D);
        } else if (motion.lengthSqr() < 1.0E-6D) {
            motion = this.lastStrikeMotion;
        }

        if (motion.lengthSqr() < 1.0E-6D) {
            motion = getPlayerAimPoint(player).subtract(impactPoint);
        }

        if (motion.lengthSqr() < 1.0E-6D) {
            motion = this.whipDirection;
        }

        return biasTowardHorizontal(motion, 0.35D, this.whipDirection);
    }

    private void ensureStrikeMotionState(int count) {
        if (this.segmentStrikeMotions.length == count && this.segmentStrikeWeights.length == count) {
            return;
        }

        this.segmentStrikeMotions = new Vec3[count];
        this.segmentStrikeWeights = new double[count];
        for (int i = 0; i < count; i++) {
            this.segmentStrikeMotions[i] = Vec3.ZERO;
            this.segmentStrikeWeights[i] = 0.0D;
        }
    }

    @Nullable
    private Player findThreatPlayer() {
        return this.level().getNearestPlayer(
                this.getX(),
                this.getY() + this.getBbHeight() * 0.25D,
                this.getZ(),
                PLAYER_DETECTION_RANGE,
                entity -> entity instanceof Player candidate && isValidTarget(candidate));
    }

    private boolean isValidTarget(@Nullable Player player) {
        return player != null && player.isAlive() && !player.isCreative() && !player.isSpectator();
    }

    private Vec3 getPlayerAimPoint(Player player) {
        return player.position().add(0.0D, player.getBbHeight() * 0.4D, 0.0D);
    }

    private Vec3 getWhipAimPoint(Player player, Vec3 anchor) {
        Vec3 aimPoint = getPlayerAimPoint(player);
        double verticalOffset = Mth.clamp(aimPoint.y - anchor.y,
                -WHIP_AIM_VERTICAL_OFFSET_LIMIT,
                WHIP_AIM_VERTICAL_OFFSET_LIMIT * 0.45D);
        return new Vec3(aimPoint.x, anchor.y + verticalOffset, aimPoint.z);
    }

    private static Vec3 blendDirection(Vec3 current, Vec3 target, double factor) {
        Vec3 blended = current.scale(1.0D - factor).add(target.scale(factor));
        return blended.lengthSqr() < 1.0E-6D ? target : blended.normalize();
    }

    private static Vec3 blendPosition(Vec3 current, Vec3 target, double factor) {
        return current.scale(1.0D - factor).add(target.scale(factor));
    }

    private static Vec3 biasTowardHorizontal(Vec3 direction, double verticalScale, Vec3 fallback) {
        Vec3 adjusted = new Vec3(direction.x, direction.y * verticalScale, direction.z);
        if (adjusted.lengthSqr() > 1.0E-6D) {
            return adjusted.normalize();
        }

        return fallback.lengthSqr() > 1.0E-6D ? fallback.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
    }

    private static Vec3 horizontalDirectionOrFallback(Vec3 direction, Vec3 fallback) {
        Vec3 horizontal = direction.multiply(1.0D, 0.0D, 1.0D);
        if (horizontal.lengthSqr() > 1.0E-6D) {
            return horizontal.normalize();
        }

        Vec3 horizontalFallback = fallback.multiply(1.0D, 0.0D, 1.0D);
        if (horizontalFallback.lengthSqr() > 1.0E-6D) {
            return horizontalFallback.normalize();
        }

        return new Vec3(0.0D, 0.0D, 1.0D);
    }

    private static double pulseWave(double position, double center, double width) {
        double distance = Math.abs(position - center);
        if (distance >= width) {
            return 0.0D;
        }

        double normalized = 1.0D - distance / width;
        return normalized * normalized * (3.0D - 2.0D * normalized);
    }

    private static double easeInOutSine(double value) {
        double clamped = Mth.clamp(value, 0.0D, 1.0D);
        return 0.5D - 0.5D * Math.cos(Math.PI * clamped);
    }

    private static double easeOutCubic(double value) {
        double clamped = Mth.clamp(value, 0.0D, 1.0D);
        double inverse = 1.0D - clamped;
        return 1.0D - inverse * inverse * inverse;
    }

    private final class Chain implements SegmentChainAlgorithm {
        // Per-link direction (unit vector from parent joint to child joint).
        private Vec3[] linkDirs = new Vec3[0];
        // Per-link angular velocity (as a direction-delta applied each tick).
        private Vec3[] linkAngVel = new Vec3[0];
        // Smoothed tip goal during whip.
        private Vec3 smoothedTipGoal = Vec3.ZERO;
        private boolean hasSmoothedTipGoal;

        @Override
        public void updateSegments(AbstractSerpentineEntity head, SerpentineSegment[] segments) {
            Vec3 anchor = getAnchorCenter();
            final Vec3 upward = new Vec3(0.0D, 1.0D, 0.0D);
            int n = segments.length;

            // --- link lengths and chain fractions ---
            double[] linkLengths = new double[n];
            double[] chainFractions = new double[n];
            double totalLength = 0.0D;
            for (int i = 0; i < n; i++) {
                linkLengths[i] = segments[i].getDefinition().spacing;
                totalLength += linkLengths[i];
            }
            double accumulated = 0.0D;
            for (int i = 0; i < n; i++) {
                accumulated += linkLengths[i];
                chainFractions[i] = accumulated / totalLength;
            }

            // --- ensure state arrays ---
            ensureState(n, anchor, upward, linkLengths);
            ensureStrikeMotionState(n);

            boolean whipping = isWhipping();
            boolean recovering = isRecovering();
            double whipWindup = easeInOutSine(getWhipWindupProgress());
            double whipSmash = easeOutCubic(getWhipSmashProgress());
            Vec3 swayForward = Vec3.directionFromRotation(0.0F, getYRot()).multiply(1.0D, 0.0D, 1.0D);
            if (swayForward.lengthSqr() < 1.0E-6D)
                swayForward = new Vec3(0.0D, 0.0D, 1.0D);
            else
                swayForward = swayForward.normalize();
            Vec3 whipForward = horizontalDirectionOrFallback(GiantOctopusTentacleEntity.this.whipPlaneForward, swayForward);
            Vec3 whipRight = new Vec3(-whipForward.z, 0.0D, whipForward.x);

            // --- tip target ---
            Player target = findThreatPlayer();
            Vec3 targetPoint = target != null ? getWhipAimPoint(target, anchor) : null;

            Vec3 tipGoal = null;
            if (whipping && targetPoint != null) {
                // Windup: raise tip high above anchor, slightly backward — like lifting an arm
                // overhead before a chop.
                Vec3 strikeDir = biasTowardHorizontal(targetPoint.subtract(anchor), WHIP_AIM_VERTICAL_SCALE,
                        whipForward);
                Vec3 windupDir = strikeDir.scale(-0.30D).add(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
                double windupDist = Math.min(totalLength * 0.68D, WHIP_REACH * 0.62D);
                Vec3 windupGoal = anchor.add(windupDir.scale(windupDist));
                // Smash: chop down diagonally — tip arcs forward and low, landing at feet
                // level.
                double chopY = target != null ? target.getY() + 0.3D : targetPoint.y - 1.0D;
                Vec3 chopBase = new Vec3(targetPoint.x, chopY, targetPoint.z);
                double sweepPhase = Math.sin((whipSmash - 0.5D) * Math.PI);
                double sweepAmt = Math.min(2.0D, totalLength * 0.10D) * whipSmash;
                Vec3 strikeGoal = clampToReach(anchor,
                        chopBase.add(strikeDir.scale(1.0D + whipSmash * 0.8D))
                                .add(whipRight.scale(sweepPhase * sweepAmt)),
                        totalLength * 0.97D);
                Vec3 rawGoal = blendPosition(windupGoal, strikeGoal, whipSmash);
                // Smooth the goal to avoid sudden jumps.
                double goalFollow = 0.06D + whipWindup * 0.06D + whipSmash * 0.22D;
                if (!this.hasSmoothedTipGoal) {
                    this.smoothedTipGoal = rawGoal;
                    this.hasSmoothedTipGoal = true;
                } else {
                    this.smoothedTipGoal = blendPosition(this.smoothedTipGoal, rawGoal, goalFollow);
                }
                tipGoal = this.smoothedTipGoal;
            } else if (whipping) {
                // No target: just extend in whip direction.
                tipGoal = anchor.add(whipForward.scale(totalLength * 0.92D));
                this.hasSmoothedTipGoal = false;
            } else {
                this.hasSmoothedTipGoal = false;
            }

            // --- FABRIK solve when whipping: drive tip to goal, keep anchor fixed ---
            if (tipGoal != null) {
                // Only drive the tip strongly during smash; during windup drive it weakly so
                // the
                // chain still shows inertia rather than snapping immediately.
                double driveStrength = whipWindup * 0.28D + whipSmash * 0.72D;
                Vec3 clampedGoal = clampToReach(anchor, tipGoal, totalLength * 0.97D);
                solveFABRIK(anchor, linkLengths, driveStrength, clampedGoal);
            }

            // --- idle sway: angular target per link ---
            double time = tickCount * 0.045D + getId() * 0.217D;
            double swayPrimary = Math.max(0.24D, BASE_SWAY_X / totalLength);
            double swaySecondary = Math.max(0.17D, BASE_SWAY_Z / totalLength);

            for (int i = 0; i < n; i++) {
                double t = chainFractions[i];
                double bendWeight = Math.pow(t, 1.55D);

                // Rest direction: upward with gentle sway at the tip.
                double latPrimary = Math.sin(time * 0.42D - t * 4.8D + 0.25D) * swayPrimary
                        * (0.9D + 1.3D * bendWeight);
                double latSecondary = Math.cos(time * 0.31D - t * 2.7D + 1.1D) * swaySecondary * (0.75D + bendWeight);
                double vertWave = Math.sin(time * 0.21D - t * 1.6D + 0.35D);
                double upBias = 1.0D - 0.55D * bendWeight + vertWave * 0.15D * bendWeight;

                Vec3 frameRight = new Vec3(-swayForward.z, 0.0D, swayForward.x);
                Vec3 restDir = upward.scale(upBias).add(frameRight.scale(latPrimary))
                        .add(swayForward.scale(latSecondary));
                if (restDir.lengthSqr() < 1.0E-6D)
                    restDir = upward;
                else
                    restDir = restDir.normalize();

                // During whip, suppress sway on proximal links and let FABRIK-solved direction
                // dominate; damping is applied in the angular velocity step below.
                if (whipping) {
                    double suppressSway = Math.min(1.0D, (whipWindup + whipSmash) * (0.5D + 0.5D * (1.0D - t)));
                    restDir = blendDirection(restDir, this.linkDirs[i], suppressSway);
                }

                // Angular spring: pull link direction toward rest, scaled by distance from
                // anchor
                // (tip bends more freely than base).
                double stiffness = whipping
                        ? 0.006D + (1.0D - bendWeight) * 0.024D // base links stiffer during whip
                        : recovering
                                ? 0.022D + bendWeight * 0.018D // tip snaps back faster in recovery
                                : 0.018D + bendWeight * 0.022D;

                Vec3 angAccel = restDir.subtract(this.linkDirs[i]).scale(stiffness);

                // Damping: higher during recovery so chain settles cleanly without oscillation.
                double damping = whipping ? 0.88D : recovering ? 0.70D : 0.82D;
                this.linkAngVel[i] = this.linkAngVel[i].scale(damping).add(angAccel);

                // Apply angular velocity to link direction and renormalize.
                Vec3 newDir = this.linkDirs[i].add(this.linkAngVel[i]);
                if (newDir.lengthSqr() < 1.0E-6D)
                    newDir = upward;
                else
                    newDir = newDir.normalize();
                this.linkDirs[i] = newDir;
            }

            // --- propagate forward: compute joint positions with collision avoidance ---
            Vec3[] centers = new Vec3[n];
            Vec3 joint = anchor;
            for (int i = 0; i < n; i++) {
                Vec3 desired = joint.add(this.linkDirs[i].scale(linkLengths[i]));
                AABB box = boxAt(segments[i], desired);
                if (isBlocked(segments[i], box)) {
                    // Candidate is inside a block/entity. Try the previous center first;
                    // if that too is blocked (e.g. anchor shifted into a wall) keep desired
                    // but zero the angular velocity so the link stops fighting the obstacle.
                    Vec3 prev = segments[i].getCenterPos();
                    if (!isBlocked(segments[i], boxAt(segments[i], prev))) {
                        desired = prev;
                    }
                    // Kill angular velocity on this link so it doesn't keep pushing into the
                    // obstacle.
                    this.linkAngVel[i] = Vec3.ZERO;
                }
                centers[i] = desired;
                joint = desired;
            }

            // --- update segments ---
            Vec3 previous = anchor;
            Vec3 previousDir = upward;
            Vec3 strikeMotionSum = Vec3.ZERO;
            double strikeMotionWeight = 0.0D;
            for (int i = 0; i < n; i++) {
                SerpentineSegment segment = segments[i];
                Vec3 oldCenter = segment.getCenterPos();
                Vec3 newCenter = centers[i];

                segment.setCenterPos(newCenter.x, newCenter.y, newCenter.z);
                Vec3 motion = newCenter.subtract(oldCenter);
                GiantOctopusTentacleEntity.this.segmentStrikeMotions[i] = motion;
                GiantOctopusTentacleEntity.this.segmentStrikeWeights[i] = chainFractions[i];

                if (chainFractions[i] >= 0.18D && motion.lengthSqr() > 1.0E-6D) {
                    double w = 0.45D + chainFractions[i] * 1.35D;
                    strikeMotionSum = strikeMotionSum.add(motion.scale(w));
                    strikeMotionWeight += w;
                }

                Vec3 facing = newCenter.subtract(previous);
                if (facing.lengthSqr() > 1.0E-6D) {
                    boolean tailSeg = i >= n - 2;
                    float rotSpeed = tailSeg ? 10.0F : 22.0F;
                    if (recovering)
                        rotSpeed = tailSeg ? 3.0F : 6.0F;
                    Vec3 orientDir = blendDirection(previousDir, facing.normalize(), tailSeg ? 0.34D : 0.82D);
                    double horiz = Math.sqrt(orientDir.x * orientDir.x + orientDir.z * orientDir.z);
                    float targetYaw = (float) Math.toDegrees(Math.atan2(-orientDir.x, orientDir.z));
                    float targetPitch = (float) (-Math.toDegrees(Math.atan2(orientDir.y, horiz)));
                    segment.setYRot(Mth.approachDegrees(segment.getYRot(), targetYaw, rotSpeed));
                    segment.setXRot(Mth.approachDegrees(segment.getXRot(), targetPitch, rotSpeed * 0.75F));
                    previousDir = orientDir;
                }

                previous = newCenter;
            }

            if (strikeMotionWeight > 0.0D && strikeMotionSum.lengthSqr() > 1.0E-6D) {
                GiantOctopusTentacleEntity.this.lastStrikeMotion = strikeMotionSum.scale(1.0D / strikeMotionWeight);
            } else if (whipping) {
                GiantOctopusTentacleEntity.this.lastStrikeMotion = GiantOctopusTentacleEntity.this.whipDirection;
            }
        }

        /** Initialize link direction arrays if they are the wrong size. */
        private void ensureState(int n, Vec3 anchor, Vec3 upward, double[] linkLengths) {
            if (this.linkDirs.length == n)
                return;
            this.linkDirs = new Vec3[n];
            this.linkAngVel = new Vec3[n];
            Vec3 joint = anchor;
            for (int i = 0; i < n; i++) {
                joint = joint.add(upward.scale(linkLengths[i]));
                this.linkDirs[i] = upward;
                this.linkAngVel[i] = Vec3.ZERO;
            }
        }

        /**
         * FABRIK solve: move all link directions so that the tip approaches
         * {@code goal},
         * blended by {@code strength} to preserve inertia.
         */
        private void solveFABRIK(Vec3 anchor, double[] linkLengths, double strength, Vec3 goal) {
            int n = this.linkDirs.length;
            // Reconstruct joint positions.
            Vec3[] joints = new Vec3[n + 1];
            joints[0] = anchor;
            for (int i = 0; i < n; i++) {
                joints[i + 1] = joints[i].add(this.linkDirs[i].scale(linkLengths[i]));
            }

            // FABRIK: 2 iterations.
            for (int iter = 0; iter < 2; iter++) {
                // Backward pass: pull tip to goal.
                joints[n] = goal;
                for (int i = n - 1; i >= 0; i--) {
                    Vec3 dir = joints[i].subtract(joints[i + 1]);
                    if (dir.lengthSqr() < 1.0E-6D)
                        dir = this.linkDirs[i].scale(-1.0D);
                    joints[i] = joints[i + 1].add(dir.normalize().scale(linkLengths[i]));
                }
                // Forward pass: fix anchor.
                joints[0] = anchor;
                for (int i = 0; i < n; i++) {
                    Vec3 dir = joints[i + 1].subtract(joints[i]);
                    if (dir.lengthSqr() < 1.0E-6D)
                        dir = this.linkDirs[i];
                    joints[i + 1] = joints[i].add(dir.normalize().scale(linkLengths[i]));
                }
            }

            // Blend solved directions back into linkDirs by strength.
            for (int i = 0; i < n; i++) {
                Vec3 solvedDir = joints[i + 1].subtract(joints[i]);
                if (solvedDir.lengthSqr() < 1.0E-6D)
                    continue;
                solvedDir = solvedDir.normalize();
                Vec3 blended = blendDirection(this.linkDirs[i], solvedDir, strength);
                // Drive angular velocity toward the solved direction.
                this.linkAngVel[i] = this.linkAngVel[i].add(blended.subtract(this.linkDirs[i]).scale(0.35D));
                this.linkDirs[i] = blended;
            }
        }

        private Vec3 clampToReach(Vec3 anchor, Vec3 goal, double maxReach) {
            Vec3 delta = goal.subtract(anchor);
            if (delta.lengthSqr() <= maxReach * maxReach)
                return goal;
            return anchor.add(delta.normalize().scale(maxReach));
        }

        private AABB boxAt(SerpentineSegment segment, Vec3 center) {
            return segment.getBoundingBox().move(center.subtract(segment.getCenterPos()));
        }

        private boolean isBlocked(SerpentineSegment segment, AABB box) {
            for (VoxelShape shape : GiantOctopusTentacleEntity.this.level().getBlockCollisions(segment, box)) {
                if (!shape.isEmpty())
                    return true;
            }
            return !GiantOctopusTentacleEntity.this.level().getEntities(segment, box,
                    e -> e.isPickable()
                            && e != GiantOctopusTentacleEntity.this
                            && (!(e instanceof SerpentineSegment s) || s.getParent() != GiantOctopusTentacleEntity.this))
                    .isEmpty();
        }
    }
}
