package com.dexer.aquanaut.common.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class SwirlEntity extends Entity {

    private static final EntityDataAccessor<Byte> SWIRL_TYPE = SynchedEntityData.defineId(
            SwirlEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> SWIRL_YAW = SynchedEntityData.defineId(
            SwirlEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SWIRL_PITCH = SynchedEntityData.defineId(
            SwirlEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SWIRL_STRENGTH = SynchedEntityData.defineId(
            SwirlEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> SWIRL_LIFETIME = SynchedEntityData.defineId(
            SwirlEntity.class, EntityDataSerializers.INT);

    public static final byte TYPE_INHALE = 0;
    public static final byte TYPE_EXHALE = 1;

    public static final double BASE_LENGTH = 8.0D;
    private static final double BASE_RADIUS = 2.5D;
    private static final double TIP_RADIUS = 0.4D;
    private static final double FORCE_PADDING = 1.5D;

    private static final int RINGS = 8;
    private static final int PARTICLES_PER_RING = 8;
    private static final float SPIN = 0.28F;

    private int age;
    private float spinAngle;
    private Entity owner;

    public SwirlEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(SWIRL_TYPE, TYPE_INHALE);
        b.define(SWIRL_YAW, 0.0F);
        b.define(SWIRL_PITCH, 0.0F);
        b.define(SWIRL_STRENGTH, 1.0F);
        b.define(SWIRL_LIFETIME, 100);
    }

    public byte getSwirlType() { return entityData.get(SWIRL_TYPE); }
    public void setSwirlType(byte t) { entityData.set(SWIRL_TYPE, t); }
    public boolean isInhale() { return getSwirlType() == TYPE_INHALE; }

    public float getSwirlYaw() { return entityData.get(SWIRL_YAW); }
    public void setSwirlYaw(float v) { entityData.set(SWIRL_YAW, v); }
    public float getSwirlPitch() { return entityData.get(SWIRL_PITCH); }
    public void setSwirlPitch(float v) { entityData.set(SWIRL_PITCH, Mth.clamp(v, -90, 90)); }

    public float getStrength() { return entityData.get(SWIRL_STRENGTH); }
    public void setStrength(float v) { entityData.set(SWIRL_STRENGTH, Mth.clamp(v, 0.1F, 5)); }

    public int getSwirlLifetime() { return entityData.get(SWIRL_LIFETIME); }
    public void setSwirlLifetime(int v) { entityData.set(SWIRL_LIFETIME, Math.max(1, v)); }

    public void setOwner(Entity e) { this.owner = e; }

    public void setDirection(float yaw, float pitch) { setSwirlYaw(yaw); setSwirlPitch(pitch); }
    public void setDirection(Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from).normalize();
        setDirection((float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90,
                     (float) (-Mth.atan2(d.y, d.horizontalDistance()) * Mth.RAD_TO_DEG));
    }
    public void configure(byte type, float yaw, float pitch, float strength, int life) {
        setSwirlType(type); setDirection(yaw, pitch); setStrength(strength); setSwirlLifetime(life);
    }

    // --- Geometry ---
    private Vec3 axis() {
        float y = getSwirlYaw() * Mth.DEG_TO_RAD;
        float p = -getSwirlPitch() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(y) * Mth.cos(p), Mth.sin(p), Mth.cos(y) * Mth.cos(p));
    }
    private Vec3 perpA() {
        Vec3 a = axis(), u = new Vec3(0, 1, 0), c = a.cross(u);
        if (c.lengthSqr() < 0.001) c = a.cross(new Vec3(1, 0, 0));
        return c.normalize();
    }
    private Vec3 perpB() { return axis().cross(perpA()).normalize(); }

    private double fullLen() { return BASE_LENGTH * getStrength(); }
    private double baseR() { return BASE_RADIUS * getStrength(); }

    private Vec3 point(double t) {
        double len = fullLen();
        return isInhale()
            ? position().add(axis().scale(t * len))
            : position().add(axis().scale((1 - t) * len));
    }
    private double radius(double t) { return TIP_RADIUS * getStrength() + (baseR() - TIP_RADIUS * getStrength()) * t; }

    // --- No collision ---
    @Override public boolean isPushable() { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override protected boolean canRide(Entity e) { return false; }
    @Override public void push(Entity e) {}

    // --- Tick ---
    @Override
    public void tick() {
        super.tick();
        age++;
        spinAngle += SPIN;
        if (!level().isClientSide) {
            if (age >= getSwirlLifetime()) { discard(); return; }
            applyForces();
        }
        if (level() instanceof ServerLevel sl) spawnParticles(sl);
    }

    private void applyForces() {
        Vec3 ax = axis();
        double len = fullLen();
        float str = getStrength();
        double srch = baseR() + FORCE_PADDING;
        Vec3 tip = point(0), base = point(1);
        AABB box = new AABB(
            Math.min(tip.x, base.x) - srch, Math.min(tip.y, base.y) - srch, Math.min(tip.z, base.z) - srch,
            Math.max(tip.x, base.x) + srch, Math.max(tip.y, base.y) + srch, Math.max(tip.z, base.z) + srch);

        List<Entity> entities = level().getEntitiesOfClass(Entity.class, box,
            e -> e != this && e.isAlive() && !(e instanceof SwirlEntity) && e != owner);

        for (Entity e : entities) {
            Vec3 ep = e.position().add(0, e.getBbHeight() * 0.5, 0);
            double d = ep.subtract(tip).dot(ax);
            double cl = Mth.clamp(d, 0, len);
            Vec3 near = tip.add(ax.scale(cl));
            double rd = ep.subtract(near).length();
            double er = radius(cl / len) + FORCE_PADDING;
            if (rd > er) continue;

            // Strong quadratic falloff
            double f = str * (1.0 - Mth.clamp(rd / er, 0, 1));
            f = f * f * 0.12;

            // Axial: pull toward tip (inhale) or push away (exhale)
            double axF = isInhale() ? -f * (1.0 - cl / len) * 1.8 : f * (cl / len) * 1.8;

            // Tangential spin
            Vec3 rad = ep.subtract(near);
            Vec3 tan = ax.cross(rad).normalize();
            if (tan.lengthSqr() < 0.001) tan = perpA();

            // Radial: pull toward axis center
            Vec3 radDir = rd > 0.001 ? rad.normalize().scale(-1) : Vec3.ZERO;

            Vec3 delta = ax.scale(axF).add(tan.scale(f * 0.7)).add(radDir.scale(f * 0.5));

            if (e instanceof Player player) {
                double yawRad = player.getYRot() * Mth.DEG_TO_RAD;
                double fwd = -delta.x * Mth.sin((float) yawRad) + delta.z * Mth.cos((float) yawRad);
                double strafe = delta.x * Mth.cos((float) yawRad) + delta.z * Mth.sin((float) yawRad);
                player.xxa += (float) Mth.clamp(strafe, -0.3F, 0.3F);
                player.zza += (float) Mth.clamp(fwd, -0.3F, 0.3F);
            } else {
                e.setDeltaMovement(e.getDeltaMovement().add(delta));
            }
            e.hasImpulse = true;

            // Extra tug when very close to axis
            if (rd < 0.6 && isInhale()) {
                if (e instanceof Player player) {
                    double yawRad = player.getYRot() * Mth.DEG_TO_RAD;
                    Vec3 tug = ax.scale(-f * 0.35);
                    double fwd = -tug.x * Mth.sin((float) yawRad) + tug.z * Mth.cos((float) yawRad);
                    player.zza += (float) Mth.clamp(fwd, -0.2F, 0.2F);
                } else {
                    e.setDeltaMovement(e.getDeltaMovement().add(ax.scale(-f * 0.35)));
                }
                e.hasImpulse = true;
            }
        }
    }

    private void spawnParticles(ServerLevel level) {
        Vec3 ax = axis(), pA = perpA(), pB = perpB();

        for (int ring = 0; ring < RINGS; ring++) {
            double t = (double) ring / (RINGS - 1);
            Vec3 c = point(t);
            double r = radius(t);

            for (int i = 0; i < PARTICLES_PER_RING; i++) {
                double a = spinAngle * (2.0 - t * 0.8) + (double) i / PARTICLES_PER_RING * Math.PI * 2.0;
                double rr = r * (0.55 + random.nextDouble() * 0.45);

                Vec3 pos = c.add(pA.scale(Math.cos(a) * rr))
                            .add(pB.scale(Math.sin(a) * rr))
                            .add(pA.scale(random.nextGaussian() * 0.06))
                            .add(pB.scale(random.nextGaussian() * 0.06));

                level.sendParticles(ParticleTypes.BUBBLE, pos.x, pos.y, pos.z,
                    1, 0.02, 0.02, 0.02, 0.01);
            }

            // Extra splash particles on outer rings
            if (ring == 0 || ring == RINGS - 1) {
                for (int i = 0; i < 3; i++) {
                    double a = spinAngle + random.nextDouble() * Math.PI * 2.0;
                    double rr = r * (0.5 + random.nextDouble() * 0.5);
                    Vec3 pos = c.add(pA.scale(Math.cos(a) * rr)).add(pB.scale(Math.sin(a) * rr));
                    level.sendParticles(ParticleTypes.SPLASH, pos.x, pos.y, pos.z,
                        1, 0.05, 0.03, 0.05, 0.02);
                }
            }
        }

        // Tip accent
        if (random.nextFloat() < 0.5f) {
            Vec3 tip = point(0);
            double tr = radius(0) * 0.6;
            level.sendParticles(ParticleTypes.BUBBLE_POP, tip.x, tip.y, tip.z, 1, tr, tr, tr, 0.03);
        }
    }

    // --- NBT ---
    @Override protected void readAdditionalSaveData(CompoundTag t) {
        if (t.contains("SwirlType")) setSwirlType(t.getByte("SwirlType"));
        if (t.contains("SwirlYaw")) setSwirlYaw(t.getFloat("SwirlYaw"));
        if (t.contains("SwirlPitch")) setSwirlPitch(t.getFloat("SwirlPitch"));
        if (t.contains("SwirlStrength")) setStrength(t.getFloat("SwirlStrength"));
        if (t.contains("SwirlLifetime")) setSwirlLifetime(t.getInt("SwirlLifetime"));
        age = t.getInt("Age");
    }
    @Override protected void addAdditionalSaveData(CompoundTag t) {
        t.putByte("SwirlType", getSwirlType());
        t.putFloat("SwirlYaw", getSwirlYaw());
        t.putFloat("SwirlPitch", getSwirlPitch());
        t.putFloat("SwirlStrength", getStrength());
        t.putInt("SwirlLifetime", getSwirlLifetime());
        t.putInt("Age", age);
    }
}
