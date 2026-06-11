package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.entity.serpentine.SegmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractBranchingLightningEntity extends Entity {
    private static final EntityDataAccessor<Long> DATA_SEED = SynchedEntityData.defineId(
            AbstractBranchingLightningEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DATA_ACTIVE_TICKS = SynchedEntityData.defineId(
            AbstractBranchingLightningEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FADE_TICKS = SynchedEntityData.defineId(
            AbstractBranchingLightningEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_LENGTH = SynchedEntityData.defineId(
            AbstractBranchingLightningEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DAMAGE = SynchedEntityData.defineId(
            AbstractBranchingLightningEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_THICKNESS = SynchedEntityData.defineId(
            AbstractBranchingLightningEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_BRANCH_COUNT = SynchedEntityData.defineId(
            AbstractBranchingLightningEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BRANCH_DEPTH = SynchedEntityData.defineId(
            AbstractBranchingLightningEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_BRANCH_SCALE = SynchedEntityData.defineId(
            AbstractBranchingLightningEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_YAW = SynchedEntityData.defineId(
            AbstractBranchingLightningEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PITCH = SynchedEntityData.defineId(
            AbstractBranchingLightningEntity.class, EntityDataSerializers.FLOAT);

    private final Set<Integer> damagedEntityIds = new HashSet<>();
    private boolean causesFire;
    private static final PartEntity<?>[] NO_PARTS = new PartEntity<?>[0];

    private LightningBranchSegment[] segments = new LightningBranchSegment[0];
    private List<LightningBoltGeometry.Segment> localRenderSegments = List.of();
    private List<LightningBoltGeometry.Segment> localSegments = List.of();
    private long lastBuiltSeed;
    private int lastBuiltLengthBits;
    private int lastBuiltBranchDepth;
    private int lastBuiltBranchCount;
    private int lastBuiltThicknessBits;
    private int lastBuiltBranchScaleBits;

    protected AbstractBranchingLightningEntity(EntityType<? extends AbstractBranchingLightningEntity> type,
            Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        applyDefaultProfile();
        if (getLightningSeed() == 0L) {
            setLightningSeed(this.random.nextLong());
        }
        ensureSegmentsBuilt();
        updateSegmentTransforms();
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
        return 40;
    }

    protected int defaultFadeTicks() {
        return 0;
    }

    protected float defaultLength() {
        return 10.0F;
    }

    protected float defaultDamage() {
        return 9.0F;
    }

    protected float defaultThickness() {
        return 0.08F;
    }

    protected int defaultBranchCount() {
        return 5;
    }

    protected int defaultBranchDepth() {
        return 4;
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
        markGeometryDirty();
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
        markGeometryDirty();
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
        markGeometryDirty();
    }

    public int getLightningBranchCount() {
        return this.entityData.get(DATA_BRANCH_COUNT);
    }

    public void setLightningBranchCount(int branchCount) {
        this.entityData.set(DATA_BRANCH_COUNT, Math.max(1, branchCount));
        markGeometryDirty();
    }

    public int getLightningBranchDepth() {
        return this.entityData.get(DATA_BRANCH_DEPTH);
    }

    public void setLightningBranchDepth(int branchDepth) {
        this.entityData.set(DATA_BRANCH_DEPTH, Math.max(1, branchDepth));
        markGeometryDirty();
    }

    public float getLightningBranchScale() {
        return this.entityData.get(DATA_BRANCH_SCALE);
    }

    public void setLightningBranchScale(float branchScale) {
        this.entityData.set(DATA_BRANCH_SCALE, Mth.clamp(branchScale, 0.1F, 0.95F));
        markGeometryDirty();
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

    public int getLightningAge() {
        return this.tickCount;
    }

    public boolean isExpired() {
        return this.getLightningAge() >= getLightningTotalTicks();
    }

    public LightningBranchSegment[] getLightningSegments() {
        ensureSegmentsBuilt();
        return segments;
    }

    public List<LightningBoltGeometry.Segment> getLightningRenderSegments() {
        ensureSegmentsBuilt();
        return localRenderSegments;
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        if (segments == null) {
            return NO_PARTS;
        }
        ensureSegmentsBuilt();
        return segments;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return this.makeBoundingBox();
    }

    public void excludeEntity(int entityId) {
        this.damagedEntityIds.add(entityId);
    }

    public void setCausesFire(boolean causesFire) {
        this.causesFire = causesFire;
    }

    @Override
    protected AABB makeBoundingBox() {
        if (segments == null) {
            return super.makeBoundingBox();
        }
        ensureSegmentsBuilt();
        Vec3 origin = this.position();
        return LightningBoltGeometry.boundsForSegments(localSegments, origin, getLightningYaw(), getLightningPitch())
                .inflate(0.5D);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(1.0F, 10.0F);
    }

    @Override
    public void tick() {
        super.tick();

        ensureSegmentsBuilt();
        updateSegmentTransforms();

        if (this.tickCount == 1 && !this.level().isClientSide) {
            playThunderSound();
        }

        if (this.level().isClientSide) {
            return;
        }

        if (this.isExpired()) {
            this.discard();
            return;
        }

        if (this.getLightningAge() < getLightningActiveTicks()) {
            damageEntities();
            spawnParticles();
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
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        if (getLightningSeed() == 0L) {
            setLightningSeed(this.random.nextLong());
        }
        ensureSegmentsBuilt();
        updateSegmentTransforms();
        for (int i = 0; i < segments.length; i++) {
            segments[i].setId(packet.getId() + i + 1);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("LightningSeed")) {
            setLightningSeed(tag.getLong("LightningSeed"));
        }
        if (tag.contains("LightningActiveTicks")) {
            setLightningActiveTicks(tag.getInt("LightningActiveTicks"));
        }
        if (tag.contains("LightningFadeTicks")) {
            setLightningFadeTicks(tag.getInt("LightningFadeTicks"));
        }
        if (tag.contains("LightningLength") && tag.getFloat("LightningLength") > 1.0F) {
            setLightningLength(tag.getFloat("LightningLength"));
        }
        if (tag.contains("LightningDamage")) {
            setLightningDamage(tag.getFloat("LightningDamage"));
        }
        if (tag.contains("LightningThickness") && tag.getFloat("LightningThickness") >= 0.05F) {
            setLightningThickness(tag.getFloat("LightningThickness"));
        }
        if (tag.contains("LightningBranchCount") && tag.getInt("LightningBranchCount") > 0) {
            setLightningBranchCount(tag.getInt("LightningBranchCount"));
        }
        if (tag.contains("LightningBranchDepth") && tag.getInt("LightningBranchDepth") > 0) {
            setLightningBranchDepth(tag.getInt("LightningBranchDepth"));
        }
        if (tag.contains("LightningBranchScale") && tag.getFloat("LightningBranchScale") >= 0.2F) {
            setLightningBranchScale(tag.getFloat("LightningBranchScale"));
        }
        if (tag.contains("LightningYaw")) {
            setLightningYaw(tag.getFloat("LightningYaw"));
        }
        if (tag.contains("LightningPitch")) {
            setLightningPitch(tag.getFloat("LightningPitch"));
        }
        ensureSegmentsBuilt();
        updateSegmentTransforms();
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

    private void markGeometryDirty() {
        lastBuiltSeed = Long.MIN_VALUE;
    }

    private void ensureSegmentsBuilt() {
        if (segments == null) {
            return;
        }
        if (needsGeometryRebuild()) {
            rebuildSegments();
        }
    }

    private boolean needsGeometryRebuild() {
        return lastBuiltSeed != getLightningSeed()
                || lastBuiltLengthBits != Float.floatToIntBits(getLightningLength())
                || lastBuiltBranchDepth != getLightningBranchDepth()
                || lastBuiltBranchCount != getLightningBranchCount()
                || lastBuiltThicknessBits != Float.floatToIntBits(getLightningThickness())
                || lastBuiltBranchScaleBits != Float.floatToIntBits(getLightningBranchScale());
    }

    private void rebuildSegments() {
        List<LightningBoltGeometry.Path> paths = getLightningPaths();
        List<LightningBoltGeometry.Segment> renderSegments = LightningBoltGeometry.flattenRenderSegments(paths);
        List<LightningBoltGeometry.Segment> flatSegments = LightningBoltGeometry.flattenHitboxSegments(paths);

        LightningBranchSegment[] newSegments = new LightningBranchSegment[flatSegments.size()];
        LightningBranchSegment[] previousSegments = segments == null ? new LightningBranchSegment[0] : segments;
        for (int i = 0; i < flatSegments.size(); i++) {
            LightningBoltGeometry.Segment segment = flatSegments.get(i);
            float hitboxSize = Math.max(visualPlateWidth(segment), Math.max(0.12F, segment.length() * 0.25F));
            SegmentDefinition definition = SegmentDefinition.of(hitboxSize, hitboxSize, Math.max(hitboxSize, segment.length()));
            LightningBranchSegment part = i < previousSegments.length ? previousSegments[i] : null;
            if (part == null || part.getSegmentIndex() != i || part.getDefinition().width != definition.width
                    || part.getDefinition().height != definition.height
                    || part.getDefinition().spacing != definition.spacing) {
                part = new LightningBranchSegment(this, definition, i);
            }
            part.setLocalSegment(segment);
            part.setId(this.getId() + i + 1);
            newSegments[i] = part;
        }

        this.segments = newSegments;
        this.localRenderSegments = renderSegments;
        this.localSegments = flatSegments;
        this.lastBuiltSeed = getLightningSeed();
        this.lastBuiltLengthBits = Float.floatToIntBits(getLightningLength());
        this.lastBuiltBranchDepth = getLightningBranchDepth();
        this.lastBuiltBranchCount = getLightningBranchCount();
        this.lastBuiltThicknessBits = Float.floatToIntBits(getLightningThickness());
        this.lastBuiltBranchScaleBits = Float.floatToIntBits(getLightningBranchScale());
    }

    private void updateSegmentTransforms() {
        ensureSegmentsBuilt();
        Vec3 origin = this.position();
        float yaw = getLightningYaw();
        float pitch = getLightningPitch();

        for (LightningBranchSegment segment : segments) {
            LightningBoltGeometry.Segment local = segment.getLocalSegment();
            if (local == null) {
                continue;
            }

            Vec3 start = LightningBoltGeometry.transformPoint(local.start(), origin, yaw, pitch);
            Vec3 end = LightningBoltGeometry.transformPoint(local.end(), origin, yaw, pitch);
            Vec3 center = new Vec3(
                    (start.x + end.x) * 0.5D,
                    (start.y + end.y) * 0.5D,
                    (start.z + end.z) * 0.5D);
            segment.setCenterPos(center.x, center.y, center.z);
            segment.setOldPosAndRot();
        }
    }

    private List<LightningBoltGeometry.Path> getLightningPaths() {
        return LightningBoltGeometry.buildPaths(getLightningSeed(), getLightningLength(), getLightningBranchDepth(),
                getLightningBranchCount(), getLightningThickness(), getLightningBranchScale());
    }

    private float visualPlateWidth(LightningBoltGeometry.Segment segment) {
        return Math.max(0.9F, segment.width() * 2.4F);
    }

    private void damageEntities() {
        float damage = getLightningDamage();
        if (damage <= 0.0F) {
            return;
        }

        Vec3 origin = this.position();
        float yaw = getLightningYaw();
        float pitch = getLightningPitch();
        AABB searchBox = LightningBoltGeometry.boundsForSegments(localSegments, origin, yaw, pitch)
                .inflate(getLightningThickness());
        List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                target -> target.isAlive() && target != this.getControllingPassenger());

        for (LivingEntity target : entities) {
            if (this.damagedEntityIds.contains(target.getId())) {
                continue;
            }

            if (target.hurt(this.damageSources().lightningBolt(), damage)) {
                if (this.causesFire) {
                    target.setRemainingFireTicks(target.getRemainingFireTicks() + 100);
                }
                this.damagedEntityIds.add(target.getId());
            } else {
                this.damagedEntityIds.add(target.getId());
            }
        }
    }

    private void playThunderSound() {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 10000.0F,
                0.8F + this.random.nextFloat() * 0.2F);
    }

    private void spawnParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 origin = this.position();
        float yaw = getLightningYaw();
        float pitch = getLightningPitch();
        List<LightningBoltGeometry.Segment> segments = this.localRenderSegments;
        if (segments.isEmpty()) {
            return;
        }

        int totalSegments = segments.size();
        int particlesPerTick = Math.min(6 + totalSegments / 3, 24);

        for (int i = 0; i < particlesPerTick; i++) {
            int segIndex = this.random.nextInt(totalSegments);
            LightningBoltGeometry.Segment seg = segments.get(segIndex);
            float t = this.random.nextFloat();
            LightningBoltGeometry.Point sampled = LightningBoltGeometry.Point.lerp(seg.start(), seg.end(), t);
            Vec3 worldPos = LightningBoltGeometry.transformPoint(sampled, origin, yaw, pitch);

            double offsetX = (this.random.nextDouble() - 0.5D) * 0.15D;
            double offsetY = (this.random.nextDouble() - 0.5D) * 0.15D;
            double offsetZ = (this.random.nextDouble() - 0.5D) * 0.15D;

            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    worldPos.x + offsetX, worldPos.y + offsetY, worldPos.z + offsetZ,
                    1, 0.0D, 0.0D, 0.0D, 0.02D);
        }
    }
}
