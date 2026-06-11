package com.dexer.aquanaut.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractLightningEntity extends Entity {
    private static final EntityDataAccessor<Long> DATA_SEED = SynchedEntityData.defineId(AbstractLightningEntity.class,
            EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DATA_ACTIVE_TICKS = SynchedEntityData.defineId(
            AbstractLightningEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FADE_TICKS = SynchedEntityData.defineId(
            AbstractLightningEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_LENGTH = SynchedEntityData.defineId(
            AbstractLightningEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DAMAGE = SynchedEntityData.defineId(
            AbstractLightningEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_THICKNESS = SynchedEntityData.defineId(
            AbstractLightningEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_BRANCH_COUNT = SynchedEntityData.defineId(
            AbstractLightningEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BRANCH_DEPTH = SynchedEntityData.defineId(
            AbstractLightningEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_BRANCH_SCALE = SynchedEntityData.defineId(
            AbstractLightningEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_YAW = SynchedEntityData.defineId(
            AbstractLightningEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PITCH = SynchedEntityData.defineId(
            AbstractLightningEntity.class, EntityDataSerializers.FLOAT);

    private final Set<Integer> damagedEntityIds = new HashSet<>();

    protected AbstractLightningEntity(EntityType<? extends AbstractLightningEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        applyDefaultProfile();
        if (getLightningSeed() == 0L) {
            setLightningSeed(this.random.nextLong());
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SEED, 0L);
        builder.define(DATA_ACTIVE_TICKS, defaultActiveTicks());
        builder.define(DATA_FADE_TICKS, defaultFadeTicks());
        builder.define(DATA_LENGTH, defaultLength());
        builder.define(DATA_DAMAGE, defaultDamage());
        builder.define(DATA_THICKNESS, defaultThickness());
        builder.define(DATA_BRANCH_COUNT, defaultBranchCount());
        builder.define(DATA_BRANCH_DEPTH, defaultBranchDepth());
        builder.define(DATA_BRANCH_SCALE, defaultBranchScale());
        builder.define(DATA_YAW, 0.0F);
        builder.define(DATA_PITCH, 0.0F);
    }

    protected void applyDefaultProfile() {
        setLightningActiveTicks(defaultActiveTicks());
        setLightningFadeTicks(defaultFadeTicks());
        setLightningLength(defaultLength());
        setLightningDamage(defaultDamage());
        setLightningThickness(defaultThickness());
        setLightningBranchCount(defaultBranchCount());
        setLightningBranchDepth(defaultBranchDepth());
        setLightningBranchScale(defaultBranchScale());
        setLightningYaw(defaultYaw());
        setLightningPitch(defaultPitch());
    }

    protected int defaultActiveTicks() {
        return 30;
    }

    protected int defaultFadeTicks() {
        return 12;
    }

    protected float defaultLength() {
        return 5.5F;
    }

    protected float defaultDamage() {
        return 8.0F;
    }

    protected float defaultThickness() {
        return 0.12F;
    }

    protected int defaultBranchCount() {
        return 2;
    }

    protected int defaultBranchDepth() {
        return 2;
    }

    protected float defaultBranchScale() {
        return 0.58F;
    }

    protected float defaultYaw() {
        return 0.0F;
    }

    protected float defaultPitch() {
        return 0.0F;
    }

    public long getLightningSeed() {
        return this.entityData.get(DATA_SEED);
    }

    public void setLightningSeed(long seed) {
        this.entityData.set(DATA_SEED, seed);
    }

    public int getLightningActiveTicks() {
        return this.entityData.get(DATA_ACTIVE_TICKS);
    }

    public void setLightningActiveTicks(int activeTicks) {
        this.entityData.set(DATA_ACTIVE_TICKS, Math.max(1, activeTicks));
    }

    public int getLightningFadeTicks() {
        return this.entityData.get(DATA_FADE_TICKS);
    }

    public void setLightningFadeTicks(int fadeTicks) {
        this.entityData.set(DATA_FADE_TICKS, Math.max(0, fadeTicks));
    }

    public int getLightningTotalTicks() {
        return getLightningActiveTicks() + getLightningFadeTicks();
    }

    public float getLightningLength() {
        return this.entityData.get(DATA_LENGTH);
    }

    public void setLightningLength(float length) {
        this.entityData.set(DATA_LENGTH, Math.max(0.25F, length));
    }

    public float getLightningDamage() {
        return this.entityData.get(DATA_DAMAGE);
    }

    public void setLightningDamage(float damage) {
        this.entityData.set(DATA_DAMAGE, Math.max(0.0F, damage));
    }

    public float getLightningThickness() {
        return this.entityData.get(DATA_THICKNESS);
    }

    public void setLightningThickness(float thickness) {
        this.entityData.set(DATA_THICKNESS, Mth.clamp(thickness, 0.02F, 1.0F));
    }

    public int getLightningBranchCount() {
        return this.entityData.get(DATA_BRANCH_COUNT);
    }

    public void setLightningBranchCount(int branchCount) {
        this.entityData.set(DATA_BRANCH_COUNT, Math.max(0, branchCount));
    }

    public int getLightningBranchDepth() {
        return this.entityData.get(DATA_BRANCH_DEPTH);
    }

    public void setLightningBranchDepth(int branchDepth) {
        this.entityData.set(DATA_BRANCH_DEPTH, Math.max(0, branchDepth));
    }

    public float getLightningBranchScale() {
        return this.entityData.get(DATA_BRANCH_SCALE);
    }

    public void setLightningBranchScale(float branchScale) {
        this.entityData.set(DATA_BRANCH_SCALE, Mth.clamp(branchScale, 0.1F, 0.95F));
    }

    public float getLightningYaw() {
        return this.entityData.get(DATA_YAW);
    }

    public void setLightningYaw(float yaw) {
        this.entityData.set(DATA_YAW, yaw);
    }

    public float getLightningPitch() {
        return this.entityData.get(DATA_PITCH);
    }

    public void setLightningPitch(float pitch) {
        this.entityData.set(DATA_PITCH, pitch);
    }

    public void excludeEntity(int entityId) {
        this.damagedEntityIds.add(entityId);
    }

    public int getLightningAge() {
        return this.tickCount;
    }

    public boolean isExpired() {
        return this.getLightningAge() >= getLightningTotalTicks();
    }

    public List<LightningBoltGeometry.Path> getLightningPaths() {
        return LightningBoltGeometry.buildPaths(getLightningSeed(), getLightningLength(), getLightningBranchDepth(),
                getLightningBranchCount(), getLightningThickness(), getLightningBranchScale());
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            return;
        }

        if (this.isExpired()) {
            this.discard();
            return;
        }

        if (this.getLightningAge() < getLightningActiveTicks()) {
            damageEntities();
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide) {
            this.discard();
        }
        return false;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return this.makeBoundingBox();
    }

    @Override
    protected AABB makeBoundingBox() {
        Vec3 origin = this.position();
        AABB bounds = LightningBoltGeometry.boundsForPaths(getLightningPaths(), origin, getLightningYaw(),
                getLightningPitch());
        return bounds.inflate(0.5D);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(1.0F, 10.0F);
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        if (getLightningSeed() == 0L) {
            setLightningSeed(this.random.nextLong());
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setLightningSeed(tag.getLong("LightningSeed"));
        setLightningActiveTicks(tag.getInt("LightningActiveTicks"));
        setLightningFadeTicks(tag.getInt("LightningFadeTicks"));
        setLightningLength(tag.getFloat("LightningLength"));
        setLightningDamage(tag.getFloat("LightningDamage"));
        setLightningThickness(tag.getFloat("LightningThickness"));
        setLightningBranchCount(tag.getInt("LightningBranchCount"));
        setLightningBranchDepth(tag.getInt("LightningBranchDepth"));
        setLightningBranchScale(tag.getFloat("LightningBranchScale"));
        setLightningYaw(tag.getFloat("LightningYaw"));
        setLightningPitch(tag.getFloat("LightningPitch"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putLong("LightningSeed", getLightningSeed());
        tag.putInt("LightningActiveTicks", getLightningActiveTicks());
        tag.putInt("LightningFadeTicks", getLightningFadeTicks());
        tag.putFloat("LightningLength", getLightningLength());
        tag.putFloat("LightningDamage", getLightningDamage());
        tag.putFloat("LightningThickness", getLightningThickness());
        tag.putInt("LightningBranchCount", getLightningBranchCount());
        tag.putInt("LightningBranchDepth", getLightningBranchDepth());
        tag.putFloat("LightningBranchScale", getLightningBranchScale());
        tag.putFloat("LightningYaw", getLightningYaw());
        tag.putFloat("LightningPitch", getLightningPitch());
    }

    private void damageEntities() {
        float damage = getLightningDamage();
        if (damage <= 0.0F) {
            return;
        }

        Vec3 origin = this.position();
        float yaw = getLightningYaw();
        float pitch = getLightningPitch();
        List<LightningBoltGeometry.Path> paths = getLightningPaths();
        AABB searchBox = LightningBoltGeometry.boundsForPaths(paths, origin, yaw, pitch).inflate(getLightningThickness());
        List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                target -> target.isAlive() && target != this.getControllingPassenger());

        for (LivingEntity target : entities) {
            if (this.damagedEntityIds.contains(target.getId())) {
                continue;
            }

            if (target.hurt(this.damageSources().lightningBolt(), damage)) {
                this.damagedEntityIds.add(target.getId());
            } else {
                this.damagedEntityIds.add(target.getId());
            }
        }
    }
}
