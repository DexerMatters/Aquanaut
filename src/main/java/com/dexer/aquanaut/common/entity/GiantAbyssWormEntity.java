package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.entity.serpentine.AbstractSerpentineEntity;
import com.dexer.aquanaut.common.entity.serpentine.SegmentChainAlgorithm;
import com.dexer.aquanaut.common.entity.serpentine.SegmentDefinition;
import com.dexer.aquanaut.common.entity.serpentine.SerpentineSegment;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class GiantAbyssWormEntity extends AbstractSerpentineEntity implements GeoEntity {

    // --- body layout ---
    // entity = head visual (head model, 48-unit z = 3.0 blocks, half = 1.5)
    // segments[0..28] = 30 body sections (section model, 40-unit z = 2.5 blocks,
    // half = 1.25)
    // segments[29] = tail (tail model, 64-unit z = 4.0 blocks, half = 2.0)
    private static final int BODY_SECTION_COUNT = 50;
    private static final int TOTAL_SEGMENTS = 51; // body sections + tail
    private static final float SEG_WIDTH = 4.0F;
    private static final float SEG_HEIGHT = 4.0F;
    // spacing = centre-to-centre link distance from PRECEDING part
    private static final float LINK_HEAD_TO_FIRST = 1.5F + 1.25F; // 2.75
    private static final float LINK_SECTION = 1.25F + 1.25F; // 2.50
    private static final float LINK_SECTION_TO_TAIL = 1.25F + 2.0F; // 3.25

    // --- motion ---
    private static final float MAX_SPEED = 1.56F;
    private static final float IDLE_SPEED = 1.14F;
    private static final float MAX_TURN_RAD = 0.035F;
    private static final float IDLE_PHASE_STEP = 0.10F;
    private static final double SWAY_AMP = 1.2D;
    private static final double HEAVE_AMP = 0.9D;

    // --- detection / attack ---
    private static final double HIT_RANGE = 3.5D;
    private static final float ATTACK_REPULSION = 4.2F;
    private static final float ATTACK_DAMAGE = 18.0F;
    private static final int ATTACK_COOLDOWN_TICKS = 20;
    private static final float EXPLOSION_POWER_DIG = 5.0F;
    private static final float EXPLOSION_POWER_ATTACK = 6.0F;
    private static final int DIG_EXPLOSION_COOLDOWN_TICKS = 20;

    // --- stalk/charge/retreat ---
    private static final float CHARGE_TURN_RAD = 0.10F;
    private static final int STALK_DURATION_MIN = 15;
    private static final int STALK_DURATION_MAX = 50;
    private static final int CHARGE_DURATION = 60;
    private static final int RETREAT_DURATION = 30;
    private static final double STALK_VERTICAL_OFFSET = 14.0D;

    private enum AttackState {
        HUNT, CHARGE, RETREAT
    }

    // --- coil ---
    private static final int COIL_INTERVAL_MIN = 70;
    private static final int COIL_INTERVAL_MAX = 180;
    private static final int COIL_DURATION_MIN = 40;
    private static final int COIL_DURATION_MAX = 100;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // movement state
    private Vec3 steerDir = new Vec3(1.0D, 0.0D, 0.0D);
    private float currentSpeed = 0.3F;

    // idle oscillation
    private double idlePhase = 0.0D;
    private double idlePhaseV = Math.PI * 0.4D;
    // stable sway frame axes — recomputed when currentDir changes significantly
    private Vec3 swayRight = new Vec3(1.0D, 0.0D, 0.0D);
    private Vec3 swayUp = new Vec3(0.0D, 1.0D, 0.0D);

    // coil
    private int nextCoilIn = COIL_INTERVAL_MIN + (int) (Math.random() * (COIL_INTERVAL_MAX - COIL_INTERVAL_MIN));
    private int coilTicksLeft = 0;
    private Vec3 coilNormal = new Vec3(0.0D, 1.0D, 0.0D);

    // attack
    private final ServerBossEvent bossEvent;
    private AttackState attackState = AttackState.HUNT;
    private int stateTimer = 0;
    private int attackCooldown = 0;
    private boolean stalkAbove = true;
    private boolean wasHeadInBlock = false;
    private int digExplosionCooldown = 0;
    private float headRoll = 0.0F;
    private float headRollPrev = 0.0F;

    public GiantAbyssWormEntity(EntityType<? extends GiantAbyssWormEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.bossEvent = new ServerBossEvent(
                this.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
    }

    public static AttributeSupplier createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 500.0D)
                .add(Attributes.ARMOR, 20.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 80.0D)
                .add(Attributes.ATTACK_DAMAGE, 18.0D)
                .add(Attributes.SCALE, 1.0D)
                .add(Attributes.WATER_MOVEMENT_EFFICIENCY, 0.0D)
                .add(NeoForgeMod.SWIM_SPEED, 0.0D)
                .build();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected List<SegmentDefinition> createSegmentDefinitions() {
        List<SegmentDefinition> defs = new ArrayList<>(TOTAL_SEGMENTS);
        defs.add(SegmentDefinition.of(SEG_WIDTH, SEG_HEIGHT, LINK_HEAD_TO_FIRST));
        for (int i = 1; i < BODY_SECTION_COUNT; i++) {
            defs.add(SegmentDefinition.of(SEG_WIDTH, SEG_HEIGHT, LINK_SECTION));
        }
        defs.add(SegmentDefinition.of(SEG_WIDTH, SEG_HEIGHT, LINK_SECTION_TO_TAIL));
        return defs;
    }

    @Override
    protected SegmentChainAlgorithm createChainAlgorithm() {
        return new Chain();
    }

    @Override
    protected void driveHead(ServerLevel level) {
        if (attackCooldown > 0)
            attackCooldown--;
        if (digExplosionCooldown > 0)
            digExplosionCooldown--;
        stateTimer++;

        bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        @Nullable
        Player target = findNearestPlayer();

        Vec3 goalDir = steerDir;
        float goalSpeed = IDLE_SPEED;
        float turnRad = MAX_TURN_RAD;

        if (target != null) {
            coilTicksLeft = 0;

            switch (attackState) {
                case HUNT -> {
                    Vec3 targetCenter = target.getBoundingBox().getCenter();
                    Vec3 headC = headCenter();
                    double verticalOffset = stalkAbove ? STALK_VERTICAL_OFFSET : -STALK_VERTICAL_OFFSET;
                    Vec3 huntTarget = targetCenter.add(0.0D, verticalOffset, 0.0D);
                    double seaLevel = level().getSeaLevel();
                    if (huntTarget.y > seaLevel - 5.0D)
                        huntTarget = new Vec3(huntTarget.x, seaLevel - 5.0D, huntTarget.z);

                    goalDir = huntTarget.subtract(headC).normalize();
                    goalSpeed = IDLE_SPEED;
                    if (stateTimer > STALK_DURATION_MIN + random.nextInt(STALK_DURATION_MAX - STALK_DURATION_MIN)) {
                        attackState = AttackState.CHARGE;
                        stateTimer = 0;
                    }
                }
                case CHARGE -> {
                    goalDir = target.getBoundingBox().getCenter().subtract(headCenter()).normalize();
                    goalSpeed = MAX_SPEED;
                    turnRad = CHARGE_TURN_RAD;
                    if (headCenter().distanceTo(target.getBoundingBox().getCenter()) < 3.0D
                            || stateTimer > CHARGE_DURATION) {
                        attackState = AttackState.RETREAT;
                        stateTimer = 0;
                        stalkAbove = !stalkAbove;
                    }
                }
                case RETREAT -> {
                    Vec3 headC = headCenter();
                    Vec3 targetCenter = target.getBoundingBox().getCenter();
                    goalDir = headC.subtract(targetCenter).normalize();
                    goalDir = goalDir.add(0.0D, 0.3D, 0.0D).normalize();
                    goalSpeed = MAX_SPEED;
                    if (stateTimer > RETREAT_DURATION) {
                        attackState = AttackState.HUNT;
                        stateTimer = 0;
                    }
                }
            }
        } else {
            attackState = AttackState.HUNT;
            stateTimer = 0;
            nextCoilIn--;
            if (coilTicksLeft > 0) {
                goalDir = computeCoilDir();
                goalSpeed = IDLE_SPEED * 0.85F;
                coilTicksLeft--;
            } else {
                if (nextCoilIn <= 0) {
                    coilTicksLeft = COIL_DURATION_MIN + this.random.nextInt(COIL_DURATION_MAX - COIL_DURATION_MIN);
                    Vec3 randomAxis = new Vec3(random.nextDouble() - 0.5D, random.nextDouble() - 0.5D,
                            random.nextDouble() - 0.5D).normalize();
                    coilNormal = randomAxis.cross(steerDir).normalize();
                    if (coilNormal.lengthSqr() < 0.01D)
                        coilNormal = steerDir.cross(swayUp).normalize();
                    nextCoilIn = COIL_INTERVAL_MIN + this.random.nextInt(COIL_INTERVAL_MAX - COIL_INTERVAL_MIN);
                }
                goalDir = computeIdleDir(level);
                goalSpeed = IDLE_SPEED;
            }
        }

        // Safety clamps (applied after all state logic)
        double seaLevel = level.getSeaLevel();
        double y = this.getY();
        boolean inWater = this.isInWater();
        boolean inSolid = isHeadInSolid(level);

        if (y < level.getMinBuildHeight() + 25.0D) {
            double urgency = Math.min((level.getMinBuildHeight() + 25.0D - y) / 25.0D, 1.0D);
            goalDir = goalDir.add(0.0D, urgency * 5.0D, 0.0D).normalize();
        } else if (inSolid) {
            goalDir = goalDir.add(0.0D, 3.0D, 0.0D).normalize();
        } else if (!inWater) {
            double towardWater = seaLevel - y;
            goalDir = goalDir.add(0.0D, Math.signum(towardWater) * 2.0D, 0.0D).normalize();
        }

        steerDir = steerToward(steerDir, goalDir, turnRad);
        currentSpeed = currentSpeed + (goalSpeed - currentSpeed) * 0.12F;

        Vec3 vel = steerDir.scale(currentSpeed);
        this.setDeltaMovement(vel);
        this.setPos(this.getX() + vel.x, this.getY() + vel.y, this.getZ() + vel.z);

        Vec3 move = vel;
        double horizLen = Math.sqrt(move.x * move.x + move.z * move.z);
        float targetYaw = (float) Math.toDegrees(Math.atan2(-move.x, move.z));
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(move.y, horizLen)));
        this.setYRot(targetYaw);
        this.setXRot(targetPitch);

        boolean headInBlock = isHeadInSolid(level);
        if (headInBlock != wasHeadInBlock && digExplosionCooldown <= 0) {
            Vec3 headPos = headCenter();
            level.explode(this, headPos.x, headPos.y, headPos.z,
                    EXPLOSION_POWER_DIG, false, Level.ExplosionInteraction.TNT);
            digExplosionCooldown = DIG_EXPLOSION_COOLDOWN_TICKS;
        }
        wasHeadInBlock = headInBlock;

        updateSwayAxes();

        idlePhase += IDLE_PHASE_STEP;
        idlePhaseV += IDLE_PHASE_STEP * 0.73D;

        headRollPrev = headRoll;
    }

    @Override
    protected void afterSegmentsUpdated(ServerLevel level) {
        if (attackCooldown > 0)
            return;

        List<Player> players = level.getEntitiesOfClass(Player.class,
                this.getBoundingBoxForCulling().inflate(HIT_RANGE + 2.0D));
        for (Player player : players) {
            if (player.isCreative() || player.isSpectator())
                continue;

            // Check head
            Vec3 headC = headCenter();
            double headDist = headC.distanceTo(player.getBoundingBox().getCenter());
            if (headDist < HIT_RANGE) {
                doAttackExplosion(level, player, headC);
                return;
            }

            // Check body segments
            for (SerpentineSegment seg : getSegments()) {
                Vec3 segC = new Vec3(seg.getX(), seg.getY() + seg.getBbHeight() * 0.5D, seg.getZ());
                double segDist = segC.distanceTo(player.getBoundingBox().getCenter());
                if (segDist < HIT_RANGE) {
                    doAttackExplosion(level, player, segC);
                    return;
                }
            }
        }
    }

    private void doAttackExplosion(ServerLevel level, Player player, Vec3 pos) {
        level.explode(null, pos.x, pos.y, pos.z,
                EXPLOSION_POWER_ATTACK, false, Level.ExplosionInteraction.MOB);
        Vec3 repDir = player.getBoundingBox().getCenter().subtract(pos).normalize();
        repDir = new Vec3(repDir.x, repDir.y + 0.4D, repDir.z).normalize();
        player.setDeltaMovement(repDir.scale(ATTACK_REPULSION));
        player.hurt(this.damageSources().mobAttack(this), ATTACK_DAMAGE);
        attackCooldown = ATTACK_COOLDOWN_TICKS;
    }

    @Nullable
    private Player findNearestPlayer() {
        if (this.level().players().isEmpty())
            return null;
        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Player player : this.level().players()) {
            if (player.isCreative() || player.isSpectator())
                continue;
            double dist = player.distanceToSqr(this);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = player;
            }
        }
        return nearest;
    }

    private Vec3 headCenter() {
        return new Vec3(this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ());
    }

    private boolean isHeadInSolid(ServerLevel level) {
        BlockPos pos = BlockPos.containing(headCenter());
        BlockState state = level.getBlockState(pos);
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    private Vec3 computeIdleDir(ServerLevel level) {
        double sway = Math.sin(idlePhase) * SWAY_AMP;
        double heave = Math.sin(idlePhaseV) * HEAVE_AMP;
        Vec3 raw = steerDir.add(swayRight.scale(sway)).add(swayUp.scale(heave));
        double seaLevel = level().getSeaLevel();
        double y = this.getY();
        if (!this.isInWater()) {
            raw = raw.add(0.0D, -2.0D, 0.0D);
        } else if (y > seaLevel - 4.0D) {
            raw = raw.add(0.0D, -0.5D, 0.0D);
        } else if (y < seaLevel - 50.0D) {
            raw = raw.add(0.0D, 2.0D, 0.0D);
        } else if (y < seaLevel - 30.0D) {
            raw = raw.add(0.0D, 0.5D, 0.0D);
        }
        if (isHeadInSolid(level)) {
            raw = raw.add(0.0D, 2.5D, 0.0D);
        }
        if (y < level().getMinBuildHeight() + 10.0D) {
            raw = raw.add(0.0D, 2.5D, 0.0D);
        }
        double len = raw.length();
        if (len < 1.0E-5D)
            return steerDir;
        return raw.scale(1.0D / len);
    }

    private Vec3 computeCoilDir() {
        Vec3 tangent = coilNormal.cross(steerDir).normalize();
        Vec3 raw = steerDir.scale(0.1D).add(tangent.scale(0.9D));
        double len = raw.length();
        if (len < 1.0E-5D)
            return steerDir;
        return raw.scale(1.0D / len);
    }

    private static Vec3 steerToward(Vec3 current, Vec3 goal, float maxRadians) {
        double cosA = current.dot(goal);
        cosA = Mth.clamp(cosA, -1.0D, 1.0D);
        double angle = Math.acos(cosA);
        if (angle < 1.0E-6D)
            return goal;
        double t = Math.min(maxRadians / angle, 1.0D);
        Vec3 cross = current.cross(goal);
        double crossLen = cross.length();
        if (crossLen < 1.0E-8D) {
            Vec3 perp = Math.abs(current.x) < 0.9D
                    ? current.cross(new Vec3(1, 0, 0)).normalize()
                    : current.cross(new Vec3(0, 1, 0)).normalize();
            cross = perp;
            crossLen = 1.0D;
        }
        cross = cross.scale(1.0D / crossLen);
        double a = angle * t;
        double sinA = Math.sin(a);
        double cosB = Math.cos(a);
        Vec3 result = current.scale(cosB)
                .add(cross.cross(current).scale(sinA))
                .add(cross.scale(cross.dot(current) * (1.0D - cosB)));
        double len = result.length();
        if (len < 1.0E-8D)
            return goal;
        return result.scale(1.0D / len);
    }

    private void updateSwayAxes() {
        Vec3 targetUp = new Vec3(0.0D, 1.0D, 0.0D);
        swayUp = new Vec3(
                Mth.lerp(0.97D, swayUp.x, targetUp.x),
                Mth.lerp(0.97D, swayUp.y, targetUp.y),
                Mth.lerp(0.97D, swayUp.z, targetUp.z)).normalize();
        Vec3 right = steerDir.cross(swayUp);
        double rLen = right.length();
        if (rLen < 0.01D) {
            right = steerDir.cross(new Vec3(1.0D, 0.0D, 0.0D));
            rLen = right.length();
        }
        if (rLen > 1.0E-6D) {
            swayRight = right.scale(1.0D / rLen);
            swayUp = swayRight.cross(steerDir).normalize();
        }
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypeTags.IS_EXPLOSION))
            return true;
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source == damageSources().inWall())
            return false;
        return super.hurt(source, amount);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public void travel(Vec3 travelVector) {
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    public float getHeadRoll(float partialTick) {
        return Mth.lerp(partialTick, headRollPrev, headRoll);
    }

    private class Chain implements SegmentChainAlgorithm {
        private Vec3[] smoothedDirs = new Vec3[0];
        private float prevHeadYaw;

        @Override
        public void updateSegments(AbstractSerpentineEntity head, SerpentineSegment[] segments) {
            int n = segments.length;
            if (n == 0)
                return;

            if (smoothedDirs.length != n) {
                smoothedDirs = new Vec3[n];
                Vec3 headDir = Vec3.directionFromRotation(head.getXRot(), head.getYRot());
                for (int i = 0; i < n; i++)
                    smoothedDirs[i] = headDir;
            }

            Vec3 prevCenter = new Vec3(head.getX(), head.getY() + head.getBbHeight() * 0.5D, head.getZ());
            Vec3 prevDir = Vec3.directionFromRotation(head.getXRot(), head.getYRot());

            float currentYaw = head.getYRot();
            float yawDelta = Mth.wrapDegrees(currentYaw - prevHeadYaw);
            headRoll -= yawDelta * 0.12F;
            headRoll = Mth.approachDegrees(headRoll, 0.0F, 1.2F);
            prevHeadYaw = currentYaw;

            for (int i = 0; i < n; i++) {
                SerpentineSegment seg = segments[i];
                float linkLen = seg.getDefinition().spacing;

                Vec3 idealCenter = prevCenter.subtract(prevDir.scale(linkLen));
                double posAlpha = 0.96D;
                Vec3 segCenter = new Vec3(
                        Mth.lerp(posAlpha, seg.getCenterX(), idealCenter.x),
                        Mth.lerp(posAlpha, seg.getCenterY(), idealCenter.y),
                        Mth.lerp(posAlpha, seg.getCenterZ(), idealCenter.z));
                seg.setCenterPos(segCenter.x, segCenter.y, segCenter.z);

                Vec3 toPrev = prevCenter.subtract(segCenter);
                Vec3 idealDir = toPrev.lengthSqr() > 1.0E-8D ? toPrev.normalize() : prevDir;

                double dirAlpha = 0.65D;
                Vec3 sd = smoothedDirs[i];
                Vec3 blended = new Vec3(
                        Mth.lerp(dirAlpha, sd.x, idealDir.x),
                        Mth.lerp(dirAlpha, sd.y, idealDir.y),
                        Mth.lerp(dirAlpha, sd.z, idealDir.z));
                if (blended.lengthSqr() > 1.0E-6D)
                    blended = blended.normalize();
                else
                    blended = idealDir;
                smoothedDirs[i] = blended;

                double horiz = Math.sqrt(blended.x * blended.x + blended.z * blended.z);
                float targetYaw = (float) Math.toDegrees(Math.atan2(-blended.x, blended.z));
                float targetPitch = (float) (-Math.toDegrees(Math.atan2(blended.y, horiz)));
                seg.setYRot(targetYaw);
                seg.setXRot(targetPitch);

                float rollTarget = (i == 0) ? headRoll : segments[i - 1].getRoll() * 0.90F;
                seg.setRoll(Mth.approachDegrees(seg.getRoll(), rollTarget, 2.5F));

                prevCenter = segCenter;
                prevDir = blended;
            }
        }
    }
}
