package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.AirSupplyHelper;
import com.dexer.aquanaut.core.EntityRegistry;
import com.dexer.aquanaut.core.SoundRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

public class AirBubbleEntity extends Entity {

    private static final EntityDataAccessor<Integer> BURST_FRAME = SynchedEntityData.defineId(AirBubbleEntity.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SIZE = SynchedEntityData.defineId(AirBubbleEntity.class,
            EntityDataSerializers.INT);

    /** Ticks between each burst frame advance. 8 frames × 2 ticks ≈ 0.8 s total. */
    private static final int BURST_FRAME_INTERVAL = 2;

    /** Base dimensions matching EntityType.Builder.sized(0.9375F, 0.9375F). */
    private static final EntityDimensions BASE_DIMENSIONS = EntityDimensions.fixed(0.9375F, 0.9375F);

    /** Drag factor applied each tick to slow the bubble down. */
    private static final double DRAG = 0.98;

    /** 4 seconds lifetime for a freshly spawned bubble — 80 ticks at 20 tps. */
    private static final int MAX_LIFETIME = 20 * 4;

    /** Interval between ambient bubble sounds (ticks). */
    private static final int AMBIENT_SOUND_INTERVAL = 20; // every 1 second

    private int burstTickCounter = 0;
    private int ambientSoundTimer = 0;

    /**
     * Transient flag that prevents double-merge when two bubbles push each other
     * simultaneously.
     */
    private boolean merging = false;

    /**
     * Remaining ticks during which this bubble is "preserved" — it will not merge
     * with or burst from other bubbles. Decrements each tick.
     */
    private int preservingTime = 0;

    /** Ticks remaining before the bubble bursts from old age. */
    private int lifetime = MAX_LIFETIME;

    public AirBubbleEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(BURST_FRAME, 0);
        builder.define(SIZE, 1);
    }

    /** 0 = intact, 1–7 = burst animation frame index in bubble_burst.png */
    public int getBurstFrame() {
        return this.entityData.get(BURST_FRAME);
    }

    public boolean isBursting() {
        return getBurstFrame() > 0;
    }

    /** Current bubble size (volume multiplier). 1 = default. */
    public int getSize() {
        return this.entityData.get(SIZE);
    }

    public void setSize(int size) {
        size = Math.max(1, size);
        this.entityData.set(SIZE, size);
        // Immediately apply new bounding box on whichever side this runs
        this.refreshDimensions();
        // Burst when bubble grows too large (SIZE=4 means 4× base width, very large)
        if (size >= 4 && !isBursting()) {
            startBurst();
        }
    }

    /** Remaining preserving time in ticks. */
    public int getPreservingTime() {
        return preservingTime;
    }

    /**
     * Set preserving time — during this many ticks the bubble won't merge/burst
     * from others.
     */
    public void setPreservingTime(int ticks) {
        this.preservingTime = Math.max(0, ticks);
    }

    private void startBurst() {
        this.entityData.set(BURST_FRAME, 1);
        this.burstTickCounter = 0;
        this.noPhysics = true;

        // Play burst sound locally on both sides
        this.level().playLocalSound(
                this.getX(), this.getY(), this.getZ(),
                SoundRegistry.BUBBLE_BURST.get(),
                SoundSource.AMBIENT,
                0.5f + this.random.nextFloat() * 0.2f,
                1.2f + this.random.nextFloat() * 0.6f,
                false);

        // Grant air to nearby living entities when server-side
        if (!this.level().isClientSide) {
            grantAirOnBurst();
        }
    }

    private void grantAirOnBurst() {
        // Rebalanced for unified-air system: 1 base bubble (120 ticks) per SIZE.
        int bubblesPerSize = 1;
        int baseTicksPerBubble = AirSupplyHelper.BASE_AIR_SUPPLY_TICKS / 10;
        float radius = getDimensions(getPose()).width() * 1.5f;
        AABB searchBox = this.getBoundingBox().inflate(radius);

        List<LivingEntity> creatures = this.level().getEntitiesOfClass(LivingEntity.class, searchBox);
        for (LivingEntity entity : creatures) {
            int airGrant = bubblesPerSize * getSize() * baseTicksPerBubble;
            AirSupplyHelper.addAir(entity, airGrant);
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        // SIZE directly controls scale, just like vanilla Slime
        return BASE_DIMENSIONS.scale((float) getSize());
    }

    @Override
    protected AABB makeBoundingBox() {
        EntityDimensions dims = getDimensions(getPose());
        float w = dims.width() / 2.0F;
        float h = dims.height() / 2.0F;
        Vec3 pos = position();
        return new AABB(pos.x - w, pos.y - h, pos.z - w, pos.x + w, pos.y + h, pos.z + w);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (SIZE.equals(key)) {
            this.refreshDimensions();
        }
        super.onSyncedDataUpdated(key);
    }

    @Override
    public void tick() {
        super.tick();

        // Decrement preserving time (server + client)
        if (preservingTime > 0) {
            preservingTime--;
        }

        int frame = getBurstFrame();

        // Movement runs on both sides for smooth client motion
        if (frame == 0) {
            // Server: decrement lifetime and auto-burst when expired.
            // Riding a donutfish pauses aging completely.
            if (!this.level().isClientSide && tickCount > 0 && !(getVehicle() instanceof DonutfishEntity)) {
                lifetime--;
                if (lifetime <= 0) {
                    startBurst();
                    return;
                }
            }

            // Ambient bubble sound (on both sides for responsive local playback,
            // but only random.random is deterministic enough to avoid spam)
            ambientSoundTimer--;
            if (ambientSoundTimer <= 0) {
                ambientSoundTimer = AMBIENT_SOUND_INTERVAL + this.random.nextInt(10);
                this.level().playLocalSound(
                        this.getX(), this.getY(), this.getZ(),
                        SoundRegistry.BUBBLE_AMBIENT.get(),
                        SoundSource.AMBIENT,
                        0.15f + this.random.nextFloat() * 0.1f,
                        0.8f + this.random.nextFloat() * 0.4f,
                        false);
            }

            Vec3 motion = getDeltaMovement();
            if (motion.lengthSqr() > 1.0E-7) {
                // Save position before moving to detect collision
                double x0 = this.getX();
                double y0 = this.getY();
                double z0 = this.getZ();

                this.move(MoverType.SELF, motion);

                // Actual movement that happened after collision
                double dx = this.getX() - x0;
                double dy = this.getY() - y0;
                double dz = this.getZ() - z0;
                double moved = Math.sqrt(dx * dx + dy * dy + dz * dz);
                double wanted = motion.length();

                setDeltaMovement(motion.scale(DRAG));

                // If movement was blocked by a block and preserving time expired → burst
                if (!this.level().isClientSide && moved < wanted - 0.01 && preservingTime == 0) {
                    startBurst();
                    return;
                }
            } else {
                setDeltaMovement(Vec3.ZERO);
            }
        }

        // Server-only logic: burst animation, merging scan
        if (this.level().isClientSide)
            return;

        if (frame > 0) {
            // Advance burst animation
            burstTickCounter++;
            if (burstTickCounter >= BURST_FRAME_INTERVAL) {
                burstTickCounter = 0;
                int next = frame + 1;
                if (next > 7) {
                    this.discard();
                } else {
                    this.entityData.set(BURST_FRAME, next);
                }
            }
            return;
        }

        // Intact — stay intact while mounted on a vehicle
        if (getVehicle() != null)
            return;

        // Burst on contact with any non-bubble entity when preserving time has expired
        if (preservingTime == 0) {
            AABB scan = this.getBoundingBox().inflate(0.1);
            Predicate<Entity> filter = e -> e != this
                    && !(e instanceof AirBubbleEntity)
                    && e.isAlive()
                    && !e.isSpectator()
                    && e.getBoundingBox().intersects(this.getBoundingBox());
            for (Entity e : this.level().getEntitiesOfClass(Entity.class, scan)) {
                if (filter.test(e)) {
                    startBurst();
                    return;
                }
            }
        }

        // Merge with nearby bubbles — inflate by own width so larger bubbles attract
        // from farther
        if (!merging && preservingTime == 0) {
            double mergeInflate = getDimensions(getPose()).width() * 0.6;
            List<AirBubbleEntity> nearby = this.level().getEntitiesOfClass(
                    AirBubbleEntity.class, this.getBoundingBox().inflate(mergeInflate));
            for (AirBubbleEntity other : nearby) {
                if (other == this || other.merging || other.isBursting()
                        || other.getVehicle() != null || other.preservingTime > 0)
                    continue;

                int totalSize = this.getSize() + other.getSize();
                this.merging = true;
                other.merging = true;

                AirBubbleEntity merged = new AirBubbleEntity(EntityRegistry.AIR_BUBBLE.get(), this.level());
                merged.setPos(
                        (this.getX() + other.getX()) / 2.0,
                        (this.getY() + other.getY()) / 2.0,
                        (this.getZ() + other.getZ()) / 2.0);
                merged.setSize(totalSize);

                // Weighted-average velocity
                Vec3 mergedVel = this.getDeltaMovement().scale(this.getSize())
                        .add(other.getDeltaMovement().scale(other.getSize()))
                        .scale(1.0 / totalSize);
                merged.setDeltaMovement(mergedVel);
                // Give the merged bubble a brief protected window
                merged.setPreservingTime(4);

                // Play merge sound
                this.level().playLocalSound(
                        merged.getX(), merged.getY(), merged.getZ(),
                        SoundRegistry.BUBBLE_MERGE.get(),
                        SoundSource.AMBIENT,
                        0.3f + this.random.nextFloat() * 0.15f,
                        1.0f + this.random.nextFloat() * 0.4f,
                        false);

                this.level().addFreshEntity(merged);
                this.discard();
                other.discard();
                return;
            }
        }
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    /** Called when another entity physically overlaps and pushes this entity. */
    @Override
    public void push(Entity entity) {
        // Merging is handled in tick(); this only handles bursting from non-bubble
        // contacts.
        if (this.level().isClientSide || merging || preservingTime > 0)
            return;
        if (entity instanceof AirBubbleEntity)
            return; // handled in tick()
        if (getVehicle() == null && !isBursting()) {
            startBurst();
        }
    }

    @Override
    public void playerTouch(Player player) {
        if (!this.level().isClientSide && getVehicle() == null && !isBursting() && preservingTime == 0) {
            startBurst();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && getVehicle() == null && !isBursting() && preservingTime == 0) {
            startBurst();
        }
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("BubbleSize")) {
            setSize(tag.getInt("BubbleSize"));
        }
        if (tag.contains("PreservingTime")) {
            preservingTime = tag.getInt("PreservingTime");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("BubbleSize", getSize());
        tag.putInt("PreservingTime", preservingTime);
    }
}
