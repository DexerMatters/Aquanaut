package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.AirSupplyHelper;
import com.dexer.aquanaut.common.ai.FishAttackMode;
import com.dexer.aquanaut.common.ai.FishResponseMode;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Opticichthus — the fish that is mostly lens.
 *
 * <p>
 * It does not chase and it does not bite. It lines up from a very long way off, holds a targeting
 * beam on the diver while its aperture floods with light, and then fires a single coherent lance.
 * Breaking line of sight is the counter; the shot itself is hitscan, so ducking behind rock for the
 * length of the charge is the only reliable answer.
 */
public class OpticichthusEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM_ANIMATION = RawAnimation.begin().thenLoop("swim");
    private static final RawAnimation TARGET_ANIMATION = RawAnimation.begin().thenLoop("target");

    /** Charges up, aperture closing on the target. */
    public static final int PHASE_IDLE = 0;
    public static final int PHASE_CHARGING = 1;
    /** Beam is live; this is the phase the renderer draws the lance for. */
    public static final int PHASE_FIRING = 2;
    public static final int PHASE_COOLDOWN = 3;

    /** Acquisition range. Deliberately very long: this is the one fish that out-ranges a diver. */
    public static final double BEAM_RANGE = 48.0D;
    public static final int CHARGE_TICKS = 36;
    public static final int FIRING_TICKS = 16;
    private static final int COOLDOWN_TICKS = 90;
    /** Grace period before a charge is abandoned after line of sight breaks. */
    private static final int SIGHT_LOST_GRACE = 20;
    private static final double MIN_SHOT_DISTANCE = 3.5D;
    private static final int CHARGE_CHIME_INTERVAL = 6;
    /** Ticks between line-of-sight sweeps while idle, so a tank full of lenses stays cheap. */
    private static final int ACQUIRE_INTERVAL = 4;
    private static final int CHARGE_PARTICLE_INTERVAL = 3;

    private static final float BEAM_DAMAGE = 8.0F;
    private static final double BEAM_KNOCKBACK = 1.15D;
    private static final double BEAM_RECOIL = 0.30D;
    private static final int IMPACT_BLINDNESS_TICKS = 40;
    private static final int IMPACT_AIR_DRAIN_TICKS = 120;
    /** Ticks of target movement the aim leads by: strafing at the right moment dodges the lance. */
    private static final int AIM_LEAD_TICKS = 5;

    /** The model is lifted this far by the renderer; the lens rides on top of that. */
    public static final double LENS_HEIGHT = 0.5D;
    /** The lens sits just behind the nose tip, one block-ish ahead of the entity origin. */
    public static final double LENS_FORWARD = 0.75D;

    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(
            OpticichthusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CHARGE = SynchedEntityData.defineId(
            OpticichthusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BEAM_AGE = SynchedEntityData.defineId(
            OpticichthusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> AIM_X = SynchedEntityData.defineId(
            OpticichthusEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AIM_Y = SynchedEntityData.defineId(
            OpticichthusEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AIM_Z = SynchedEntityData.defineId(
            OpticichthusEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BEAM_LENGTH = SynchedEntityData.defineId(
            OpticichthusEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> BEAM_CONTACT = SynchedEntityData.defineId(
            OpticichthusEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** Server-side only: the diver currently lined up in the lens. */
    private Player beamTarget;
    private int lostSightTicks;
    private int cooldownTicks;

    public OpticichthusEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, PHASE_IDLE);
        builder.define(CHARGE, 0);
        builder.define(BEAM_AGE, 0);
        builder.define(AIM_X, 0.0F);
        builder.define(AIM_Y, 0.0F);
        builder.define(AIM_Z, 1.0F);
        builder.define(BEAM_LENGTH, 0.0F);
        builder.define(BEAM_CONTACT, false);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 3, state -> {
            int phase = this.getPhase();
            if (phase == PHASE_CHARGING || phase == PHASE_FIRING) {
                state.getController().setAnimationSpeed(1.0D);
                return state.setAndContinue(TARGET_ANIMATION);
            }
            state.getController().setAnimationSpeed(animSpeed(0.5, 1.1, 1.2, 1.9));
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
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .build();
    }

    // ---------------------------------------------------------------------------------------------
    // Synced state
    // ---------------------------------------------------------------------------------------------

    public int getPhase() {
        return this.entityData.get(PHASE);
    }

    private void setPhase(int phase) {
        this.entityData.set(PHASE, phase);
    }

    public int getChargeTicks() {
        return this.entityData.get(CHARGE);
    }

    /** 0..1 charge progress; drives the shrinking aperture and rising whine on the client. */
    public float getChargeProgress() {
        return Mth.clamp(this.getChargeTicks() / (float) CHARGE_TICKS, 0.0F, 1.0F);
    }

    private void setChargeTicks(int ticks) {
        this.entityData.set(CHARGE, Mth.clamp(ticks, 0, CHARGE_TICKS));
    }

    public int getBeamAge() {
        return this.entityData.get(BEAM_AGE);
    }

    /** 0..1 progress through the firing flash. */
    public float getBeamProgress() {
        return Mth.clamp(this.getBeamAge() / (float) FIRING_TICKS, 0.0F, 1.0F);
    }

    private void setBeamAge(int age) {
        this.entityData.set(BEAM_AGE, Mth.clamp(age, 0, FIRING_TICKS));
    }

    /** Unit vector the lens is currently pointing along. */
    public Vec3 getAimDirection() {
        return new Vec3(this.entityData.get(AIM_X), this.entityData.get(AIM_Y), this.entityData.get(AIM_Z));
    }

    private void setAimDirection(Vec3 direction) {
        this.entityData.set(AIM_X, (float) direction.x);
        this.entityData.set(AIM_Y, (float) direction.y);
        this.entityData.set(AIM_Z, (float) direction.z);
    }

    public float getBeamLength() {
        return this.entityData.get(BEAM_LENGTH);
    }

    private void setBeamLength(float length) {
        this.entityData.set(BEAM_LENGTH, Math.max(0.0F, length));
    }

    /** Whether the lance found a body rather than rock (the renderer flares harder for a body). */
    public boolean isBeamContact() {
        return this.entityData.get(BEAM_CONTACT);
    }

    private void setBeamContact(boolean contact) {
        this.entityData.set(BEAM_CONTACT, contact);
    }

    /** World-space muzzle, offset along the current aim so client and server agree on the origin. */
    public Vec3 getLensOrigin(float partialTick) {
        return this.getPosition(partialTick)
                .add(0.0D, LENS_HEIGHT, 0.0D)
                .add(this.getAimDirection().scale(LENS_FORWARD));
    }

    // ---------------------------------------------------------------------------------------------
    // Behaviour
    // ---------------------------------------------------------------------------------------------

    @Override
    protected FishResponseMode getResponseMode() {
        return FishResponseMode.AVOIDANCE;
    }

    @Override
    protected FishAttackMode getAttackMode() {
        return FishAttackMode.NONE;
    }

    @Override
    protected double getCruiseAcceleration() {
        return 0.012D;
    }

    @Override
    protected double getCruiseMaxSpeed() {
        return 0.16D;
    }

    @Override
    protected double getEscapeAcceleration() {
        return 0.035D;
    }

    @Override
    protected double getEscapeMaxSpeed() {
        return 0.36D;
    }

    @Override
    protected double getWaterDrag() {
        return 0.89D;
    }

    @Override
    protected double getPlayerDetectionRange() {
        return 16.0D;
    }

    @Override
    protected float getCruiseYawTurnRateDegrees() {
        return 1.6F;
    }

    @Override
    protected float getCruisePitchTurnRateDegrees() {
        return 1.2F;
    }

    @Override
    protected float getBodyTurnRateDegrees() {
        return 4.0F;
    }

    @Override
    protected float getMaxTiltDegrees() {
        return 26.0F;
    }

    @Override
    protected float getHitboxPickInflation() {
        return 0.22F;
    }

    @Override
    protected double getHitboxVisualYOffset() {
        return LENS_HEIGHT;
    }

    @Override
    protected double getHitboxPitchPivotOffsetY() {
        return 0.25D;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.isEffectiveAi() || this.level().isClientSide) {
            return;
        }

        switch (this.getPhase()) {
            case PHASE_CHARGING -> this.tickCharging();
            case PHASE_FIRING -> this.tickFiring();
            case PHASE_COOLDOWN -> this.tickCooldown();
            default -> this.tickIdle();
        }
    }

    private void tickIdle() {
        this.setChargeTicks(0);
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        if (!this.isInWater() || this.tickCount % ACQUIRE_INTERVAL != 0) {
            return;
        }
        Player target = this.findBeamTarget();
        if (target != null) {
            this.beginCharge(target);
        }
    }

    private void tickCooldown() {
        this.setChargeTicks(0);
        this.beamTarget = null;
        if (--this.cooldownTicks <= 0) {
            this.cooldownTicks = 0;
            this.setPhase(PHASE_IDLE);
        }
    }

    private void beginCharge(Player target) {
        this.beamTarget = target;
        this.lostSightTicks = 0;
        this.setBeamAge(0);
        this.setBeamLength(0.0F);
        this.setBeamContact(false);
        this.setChargeTicks(0);
        this.updateAim(target);
        this.setPhase(PHASE_CHARGING);
        this.playAtFish(SoundEvents.CONDUIT_ACTIVATE, 1.0F, 0.85F);
        // The diver gets a low resonance of their own: something has found them, and it is far away.
        this.playAt(SoundEvents.AMETHYST_BLOCK_RESONATE, target, 0.4F, 0.7F);
    }

    private void tickCharging() {
        Player target = this.beamTarget;
        if (this.isValidBeamTarget(target)) {
            this.lostSightTicks = 0;
            this.updateAim(target);
        } else if (++this.lostSightTicks > SIGHT_LOST_GRACE) {
            this.abortCharge();
            return;
        }

        this.setChargeTicks(this.getChargeTicks() + 1);
        this.holdStation();

        int charge = this.getChargeTicks();
        if (charge % CHARGE_PARTICLE_INTERVAL == 0) {
            this.spawnChargeParticles();
        }
        if (charge % CHARGE_CHIME_INTERVAL == 0) {
            float pitch = 0.85F + 1.15F * (charge / (float) CHARGE_TICKS);
            this.playAtFish(SoundEvents.AMETHYST_BLOCK_CHIME, 0.55F, pitch);
            // Faint echo on the target, so a diver being ranged hears the whine climb even when the
            // fish is forty blocks away and its own sound has attenuated to nothing.
            this.playAt(SoundEvents.AMETHYST_BLOCK_CHIME, target, 0.35F, pitch + 0.15F);
        }

        if (charge >= CHARGE_TICKS) {
            this.fireBeam(target);
        }
    }

    private void tickFiring() {
        this.holdStation();
        if (this.getBeamAge() + 1 >= FIRING_TICKS) {
            this.enterCooldown();
            return;
        }
        this.setBeamAge(this.getBeamAge() + 1);
    }

    private void abortCharge() {
        this.beamTarget = null;
        this.setChargeTicks(0);
        this.setBeamLength(0.0F);
        this.setPhase(PHASE_COOLDOWN);
        this.cooldownTicks = COOLDOWN_TICKS / 2;
        this.playAtFish(SoundEvents.BEACON_DEACTIVATE, 0.9F, 1.3F);
    }

    private void enterCooldown() {
        this.beamTarget = null;
        this.setBeamAge(0);
        this.setBeamLength(0.0F);
        this.setChargeTicks(0);
        this.setPhase(PHASE_COOLDOWN);
        this.cooldownTicks = COOLDOWN_TICKS;
        this.playAtFish(SoundEvents.BEACON_DEACTIVATE, 1.0F, 1.1F);
    }

    /** Locks the lens onto the lead position and holds the rifle steady. */
    private void holdStation() {
        this.setDeltaMovement(this.getDeltaMovement().scale(0.28D));
        this.hasImpulse = true;

        Vec3 aim = this.getAimDirection();
        float yaw = (float) (Mth.atan2(aim.z, aim.x) * (180.0D / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        this.setXRot(Mth.clamp((float) (-Mth.atan2(aim.y, aim.horizontalDistance())
                * (180.0D / Math.PI)), -this.getMaxTiltDegrees(), this.getMaxTiltDegrees()));
    }

    private Player findBeamTarget() {
        Player best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Player candidate : this.level().players()) {
            if (!this.isValidBeamTarget(candidate)) {
                continue;
            }
            double distance = this.distanceToSqr(candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    private boolean isValidBeamTarget(Player candidate) {
        if (candidate == null || !candidate.isAlive() || candidate.isCreative() || candidate.isSpectator()) {
            return false;
        }
        double distance = this.distanceTo(candidate);
        // Both ends matter: it will not waste a charge on a diver hugging its flank, and it must
        // stay inside the lance's own reach so the shot cannot fall short of a target it locked on to.
        if (distance < MIN_SHOT_DISTANCE || distance > BEAM_RANGE) {
            return false;
        }
        return this.hasLineOfSight(candidate);
    }

    private void updateAim(Player target) {
        Vec3 base = new Vec3(this.getX(), this.getY() + LENS_HEIGHT, this.getZ());
        Vec3 predicted = target.getEyePosition().add(target.getDeltaMovement().scale(AIM_LEAD_TICKS));
        Vec3 delta = predicted.subtract(base);
        if (delta.lengthSqr() < 1.0E-8D) {
            return;
        }
        this.setAimDirection(delta.normalize());
    }

    private void fireBeam(Player target) {
        if (target != null) {
            this.updateAim(target);
        }

        Vec3 eye = new Vec3(this.getX(), this.getY() + LENS_HEIGHT, this.getZ());
        Vec3 direction = this.getAimDirection();
        Vec3 origin = eye.add(direction.scale(LENS_FORWARD));
        Vec3 reach = origin.add(direction.scale(BEAM_RANGE));

        double length = BEAM_RANGE;
        BlockHitResult blockHit = this.level().clip(new ClipContext(origin, reach,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (blockHit.getType() != HitResult.Type.MISS) {
            length = origin.distanceTo(blockHit.getLocation());
        }

        LivingEntity victim = null;
        AABB search = this.getBoundingBox().expandTowards(direction.scale(BEAM_RANGE)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(this.level(), this, origin,
                origin.add(direction.scale(length)), search, this::canBeamHit);
        if (entityHit != null) {
            double hitDistance = origin.distanceTo(entityHit.getLocation());
            if (hitDistance <= length) {
                length = hitDistance;
                if (entityHit.getEntity() instanceof LivingEntity living) {
                    victim = living;
                }
            }
        }

        this.setBeamLength((float) length);
        this.setBeamContact(victim != null);
        this.setBeamAge(0);
        this.setPhase(PHASE_FIRING);

        this.playFireSound(origin.add(direction.scale(length)));
        this.spawnFireParticles(origin, direction, length, victim);
        if (victim != null) {
            this.applyBeamImpact(victim, direction);
        }
        // Recoil: a lance that heavy shoves the fish backwards off its station.
        this.push(-direction.x * BEAM_RECOIL, 0.04D, -direction.z * BEAM_RECOIL);
        this.hurtMarked = true;
    }

    /**
     * The lance hits divers and anything that is not native to the water; it never cooks its own
     * kind, and creative/spectator players are ignored so builders are not sniped in their own tanks.
     */
    private boolean canBeamHit(Entity candidate) {
        if (candidate == this || !candidate.isAlive() || candidate instanceof OpticichthusEntity) {
            return false;
        }
        if (candidate instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }
        return candidate instanceof Mob && !(candidate instanceof WaterAnimal);
    }

    private void applyBeamImpact(LivingEntity victim, Vec3 direction) {
        if (!victim.hurt(this.damageSources().indirectMagic(this, this), BEAM_DAMAGE)) {
            return;
        }
        victim.push(direction.x * BEAM_KNOCKBACK,
                Math.max(0.28D, direction.y * BEAM_KNOCKBACK),
                direction.z * BEAM_KNOCKBACK);
        victim.hurtMarked = true;
        victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, IMPACT_BLINDNESS_TICKS, 0), this);
        AirSupplyHelper.removeAir(victim, IMPACT_AIR_DRAIN_TICKS);
        if (victim instanceof Player player) {
            this.onSuccessfulBite(player);
        }
    }

    private void playFireSound(Vec3 impact) {
        this.playAtFish(SoundEvents.WARDEN_SONIC_BOOM, 2.0F, 1.45F);
        this.playAtFish(SoundEvents.BEACON_ACTIVATE, 1.5F, 1.8F);
        // The crack of the lance arriving, heard where it actually landed.
        this.level().playSound(null, impact.x, impact.y, impact.z,
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.6F, 1.15F);
    }

    private void playAtFish(SoundEvent sound, float volume, float pitch) {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), sound, SoundSource.HOSTILE,
                volume, pitch);
    }

    private void playAt(SoundEvent sound, Player listener, float volume, float pitch) {
        if (listener == null) {
            return;
        }
        this.level().playSound(null, listener.getX(), listener.getY(), listener.getZ(), sound,
                SoundSource.HOSTILE, volume, pitch);
    }

    /** A shrinking halo of sparks funnelling into the aperture while the lens floods. */
    private void spawnChargeParticles() {
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        float progress = this.getChargeProgress();
        double radius = 2.4D - 1.9D * progress;
        Vec3 origin = this.getLensOrigin(1.0F);
        Vec3 direction = this.getAimDirection();
        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-6D) {
            side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        side = side.normalize();
        Vec3 up = direction.cross(side).normalize();

        int sparks = 3 + (int) (progress * 7.0F);
        for (int i = 0; i < sparks; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            Vec3 offset = side.scale(Math.cos(angle) * radius).add(up.scale(Math.sin(angle) * radius));
            Vec3 at = origin.add(offset);
            server.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            if (i % 2 == 0) {
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 1,
                        0.05D, 0.05D, 0.05D, 0.02D);
            }
        }
        server.sendParticles(ParticleTypes.ENCHANT, origin.x, origin.y, origin.z,
                2, 0.35D, 0.35D, 0.35D, 0.0D);
    }

    private void spawnFireParticles(Vec3 origin, Vec3 direction, double length, LivingEntity victim) {
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }

        // Muzzle flash.
        server.sendParticles(ParticleTypes.FLASH, origin.x, origin.y, origin.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, origin.x, origin.y, origin.z,
                14, 0.18D, 0.18D, 0.18D, 0.35D);

        // The lance itself: sparks strung all the way down the beam.
        int steps = Mth.clamp((int) (length / 2.0D), 4, 24);
        for (int i = 1; i <= steps; i++) {
            Vec3 at = origin.add(direction.scale(length * i / steps));
            ParticleOptions options = i % 2 == 0 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.END_ROD;
            server.sendParticles(options, at.x, at.y, at.z, 1, 0.04D, 0.04D, 0.04D, 0.0D);
        }

        Vec3 end = origin.add(direction.scale(length));
        if (victim != null) {
            server.sendParticles(ParticleTypes.FLASH, end.x, end.y, end.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            server.sendParticles(ParticleTypes.SONIC_BOOM, end.x, end.y, end.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z,
                    22, 0.3D, 0.3D, 0.3D, 0.6D);
            // The hit boils the water around the wound.
            server.sendParticles(ParticleTypes.BUBBLE, end.x, end.y, end.z, 12, 0.3D, 0.4D, 0.3D, 0.12D);
        } else {
            server.sendParticles(ParticleTypes.FLASH, end.x, end.y, end.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z,
                    16, 0.25D, 0.25D, 0.25D, 0.45D);
            server.sendParticles(ParticleTypes.BUBBLE_POP, end.x, end.y, end.z, 8, 0.25D, 0.25D, 0.25D, 0.1D);
        }
    }
}
