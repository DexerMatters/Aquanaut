package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

/**
 * A halite-crusted marine gastropod that grazes the vertical faces of salt pillars.
 *
 * <p>
 * Grazing is a three-beat behaviour. The creature swims to an open-water point just off
 * a pillar face, stops, then slowly swings its body onto the surface before it starts
 * rasping. The body turn is deliberately kept out of the travel phase: a near-vertical
 * shell sweeping around the yaw axis while the mob is still steering looks like a spin,
 * so the tilt only ramps once the heading is locked onto the pillar. The salt is never
 * broken -- the only trace of the meal is the puff it chips away -- and after a while the
 * creature releases and drifts to a different pillar.
 */
public class SaltCrustEntity extends WaterAnimal implements GeoEntity {
    private static final EntityDataAccessor<Boolean> CONSUMING = SynchedEntityData.defineId(SaltCrustEntity.class,
            EntityDataSerializers.BOOLEAN);
    /** Degrees the body is asked to turn onto the surface; 88 while latched, 0 in open water. */
    private static final EntityDataAccessor<Float> ATTACH_TILT = SynchedEntityData.defineId(SaltCrustEntity.class,
            EntityDataSerializers.FLOAT);

    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("swim");
    private static final RawAnimation CONSUME = RawAnimation.begin().thenLoop("consume");

    /** How far the belly swings onto the surface once latched. */
    private static final float LATCHED_TILT = 88.0F;
    /** Fraction of the remaining tilt closed per tick: ~1.3 s for a slow, readable turn. */
    private static final float TURN_RATE = 0.045F;

    /** Salt pillars: the vertical faces of these are the food. */
    private static final Block[] PILLAR_BLOCKS = new Block[] {
            BlockRegistry.HALITE_PIPE.get(),
            BlockRegistry.GYPSUM_BLADE.get(),
    };

    /** Built on first use: passing {@code this} at field-init time would leak a
     * half-constructed entity to GeckoLib before the subclass constructor has run. */
    private AnimatableInstanceCache cache;

    private float attachPitch;
    private float attachPitchO;

    public SaltCrustEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CONSUMING, false);
        builder.define(ATTACH_TILT, 0.0F);
    }

    public boolean isConsuming() {
        return this.entityData.get(CONSUMING);
    }

    public void setConsuming(boolean consuming) {
        if (this.level().isClientSide) {
            return;
        }
        this.entityData.set(CONSUMING, consuming);
    }

    /** Degrees the body is asked to turn onto the surface; 0 in open water. */
    public float getAttachTilt() {
        return this.entityData.get(ATTACH_TILT);
    }

    public void setAttachTilt(float tilt) {
        if (this.level().isClientSide) {
            return;
        }
        this.entityData.set(ATTACH_TILT, tilt);
    }

    /**
     * The eased body turn used for rendering. Positive values pitch the nose up, which
     * lays the ventral side against whatever the creature is facing.
     */
    public float getAttachPitch(float partialTick) {
        return Mth.lerp(partialTick, this.attachPitchO, this.attachPitch);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.attachPitchO = this.attachPitch;
        this.attachPitch += (this.getAttachTilt() - this.attachPitch) * TURN_RATE;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new SaltGrazeGoal(this));
        this.goalSelector.addGoal(6, new RandomSwimmingGoal(this, 1.0D, 40));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, state -> {
            if (this.isConsuming()) {
                return state.setAndContinue(CONSUME);
            }
            double speed = this.getDeltaMovement().horizontalDistance();
            state.getController().setAnimationSpeed(0.8D + speed * 6.0D);
            return state.setAndContinue(SWIM);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        AnimatableInstanceCache c = this.cache;
        if (c == null) {
            c = this.cache = GeckoLibUtil.createInstanceCache(this);
        }
        return c;
    }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .build();
    }

    private static boolean isPillar(BlockState state) {
        Block block = state.getBlock();
        for (Block pillar : PILLAR_BLOCKS) {
            if (block == pillar) {
                return true;
            }
        }
        return false;
    }

    /**
     * A clingable vertical face of a pillar: the face normal, plus a point in open water
     * just off that face that the creature can actually navigate to.
     */
    private record GrazeSpot(BlockPos block, Vec3 normal, Vec3 attachPoint) {
    }

    /**
     * Picks the nearest pillar face turned toward the creature. Only exposed side faces
     * count, so the creature always ends up against a vertical surface rather than on top.
     */
    private static GrazeSpot findGrazeSpot(SaltCrustEntity mob, int range, BlockPos exclude) {
        Level level = mob.level();
        BlockPos base = mob.blockPosition();
        Vec3 mobPosition = mob.position();
        GrazeSpot best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos candidate : BlockPos.betweenClosed(base.offset(-range, -range, -range),
                base.offset(range, range, range))) {
            BlockPos pos = candidate.immutable();
            if (exclude != null && pos.equals(exclude)) {
                continue;
            }
            if (!isPillar(level.getBlockState(pos))) {
                continue;
            }
            Vec3 normal = bestFaceNormal(level, pos, mobPosition);
            if (normal == null) {
                continue;
            }
            GrazeSpot spot = new GrazeSpot(pos, normal, Vec3.atCenterOf(pos).add(normal.scale(0.9D)));
            double distance = mob.distanceToSqr(spot.attachPoint());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = spot;
            }
        }
        return best;
    }

    /** Unit normal of the nearest open side face; null when every side is buried. */
    private static Vec3 bestFaceNormal(Level level, BlockPos pos, Vec3 from) {
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 best = null;
        double bestAlignment = -Double.MAX_VALUE;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(direction);
            if (!level.getBlockState(side).getCollisionShape(level, side).isEmpty()) {
                continue;
            }
            Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
            double alignment = from.subtract(center).normalize().dot(normal);
            if (alignment > bestAlignment) {
                bestAlignment = alignment;
                best = normal;
            }
        }
        return best;
    }

    /**
     * Swings onto a pillar face, rasps for a while, then wanders to a different pillar.
     */
    private static final class SaltGrazeGoal extends Goal {
        /** Distance squared at which the slow turn toward the surface begins. */
        private static final double SETTLE_DISTANCE_SQ = 2.25D;
        /** Distance squared at which the belly is on the salt. */
        private static final double LATCH_DISTANCE_SQ = 0.25D;
        /** Body pitch that counts as fully turned onto the surface. */
        private static final float LATCHED_PITCH = 70.0F;

        private final SaltCrustEntity mob;
        private GrazeSpot spot;
        private BlockPos lastGrazed;
        private int scanCooldown;
        private int repathCooldown;
        private int raspTicks;
        private int settleTicks;
        private int moveOnTimer;
        private int wanderTicks;

        SaltGrazeGoal(SaltCrustEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.wanderTicks > 0) {
                // Free-swimming interval between grazes: leave the mob to wander.
                this.wanderTicks--;
                return false;
            }
            if (this.spot != null && isPillar(this.mob.level().getBlockState(this.spot.block()))) {
                return true;
            }
            if (this.scanCooldown-- > 0) {
                return false;
            }
            this.scanCooldown = 40;
            this.spot = seekSpot(6);
            return this.spot != null;
        }

        /** Prefers a pillar it has not just left, so it really moves between them. */
        private GrazeSpot seekSpot(int range) {
            GrazeSpot next = findGrazeSpot(this.mob, range, this.lastGrazed);
            return next != null ? next : findGrazeSpot(this.mob, range, null);
        }

        @Override
        public boolean canContinueToUse() {
            return this.spot != null
                    && isPillar(this.mob.level().getBlockState(this.spot.block()))
                    && this.mob.distanceToSqr(this.spot.attachPoint()) < 144.0D;
        }

        @Override
        public void start() {
            this.raspTicks = 0;
            this.settleTicks = 0;
            this.repathCooldown = 0;
            this.moveOnTimer = nextMoveOnDelay();
        }

        @Override
        public void stop() {
            this.spot = null;
            this.raspTicks = 0;
            this.settleTicks = 0;
            this.mob.setConsuming(false);
            this.mob.setAttachTilt(0.0F);
            this.mob.setNoGravity(false);
        }

        @Override
        public void tick() {
            if (this.spot == null) {
                return;
            }
            Vec3 attach = this.spot.attachPoint();
            double distanceSq = this.mob.distanceToSqr(attach);

            if (distanceSq > SETTLE_DISTANCE_SQ) {
                // Travel: level body, plain swimming toward the open water just off the face.
                this.mob.setConsuming(false);
                this.mob.setAttachTilt(0.0F);
                this.mob.setNoGravity(false);
                if (--this.repathCooldown <= 0) {
                    this.repathCooldown = 10;
                    this.mob.getNavigation().moveTo(attach.x, attach.y, attach.z, 1.0D);
                }
                return;
            }

            // Settle: stop against the pillar with the heading locked, then slowly swing on.
            this.mob.getNavigation().stop();
            this.faceSurface();
            this.mob.setNoGravity(true);

            double distance = Math.sqrt(distanceSq);
            // Spring the body onto the face and keep pulling. The attach point sits just
            // inside the creature's collision reach, so the pull never lets go and the
            // belly stays pressed flat against the salt instead of hovering off it.
            Vec3 drift = this.mob.getDeltaMovement().scale(0.6D)
                    .add(attach.subtract(this.mob.position()).scale(0.02D));
            this.mob.setDeltaMovement(drift);
            this.settleTicks++;

            // The turn tracks how close the belly is to the salt, so it eases on slowly.
            float proximity = (float) Mth.clamp((Math.sqrt(SETTLE_DISTANCE_SQ) - distance) / 1.1D, 0.0D, 1.0D);
            this.mob.setAttachTilt(LATCHED_TILT * proximity);

            boolean latched = (distanceSq <= LATCH_DISTANCE_SQ || this.settleTicks > 40)
                    && this.mob.getAttachPitch(1.0F) > LATCHED_PITCH;
            this.mob.setConsuming(latched);
            if (!latched) {
                return;
            }

            if (++this.raspTicks >= 6) {
                this.raspTicks = 0;
                spawnMunch();
            }

            if (this.settleTicks >= this.moveOnTimer) {
                // Let go and swim: a real interval of open water before the next attach.
                this.lastGrazed = this.spot.block();
                this.spot = null;
                this.settleTicks = 0;
                this.wanderTicks = nextWanderDelay();
                this.mob.setConsuming(false);
                this.mob.setAttachTilt(0.0F);
                this.mob.setNoGravity(false);
            }
        }

        /**
         * Locks the heading onto the pillar face. Holding the yaw still matters: a tilted
         * body that keeps changing heading sweeps around like a propeller.
         */
        private void faceSurface() {
            Vec3 normal = this.spot.normal();
            float yaw = (float) (Mth.atan2(-normal.z, -normal.x) * (180.0D / Math.PI)) - 90.0F;
            this.mob.setYRot(yaw);
            this.mob.yBodyRot = yaw;
            this.mob.yBodyRotO = yaw;
            this.mob.yHeadRot = yaw;
            this.mob.yHeadRotO = yaw;
            this.mob.setXRot(0.0F);
            this.mob.xRotO = 0.0F;
        }

        private int nextMoveOnDelay() {
            return 160 + this.mob.getRandom().nextInt(280);
        }

        /** How long it swims freely before the next attach: 10-29 s. */
        private int nextWanderDelay() {
            return 200 + this.mob.getRandom().nextInt(380);
        }

        /** Small salt puffs off the contact point: the whole visible trace of the meal. */
        private void spawnMunch() {
            if (!(this.mob.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            Vec3 mouth = this.mob.getEyePosition(1.0F);
            Vec3 at = this.spot.attachPoint();
            double x = (mouth.x + at.x) * 0.5D;
            double y = (mouth.y + at.y) * 0.5D;
            double z = (mouth.z + at.z) * 0.5D;
            serverLevel.sendParticles(ParticleTypes.POOF, x, y, z, 1, 0.1D, 0.1D, 0.1D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.CRIT, x, y, z, 2, 0.14D, 0.14D, 0.14D, 0.02D);
        }
    }
}
