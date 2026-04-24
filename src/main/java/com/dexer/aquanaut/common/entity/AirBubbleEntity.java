package com.dexer.aquanaut.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class AirBubbleEntity extends Entity {

    private static final EntityDataAccessor<Integer> BURST_FRAME = SynchedEntityData.defineId(AirBubbleEntity.class,
            EntityDataSerializers.INT);

    /** Ticks between each burst frame advance. 8 frames × 2 ticks ≈ 0.8 s total. */
    private static final int BURST_FRAME_INTERVAL = 2;
    private int burstTickCounter = 0;

    public AirBubbleEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(BURST_FRAME, 0);
    }

    /** 0 = intact, 1–7 = burst animation frame index in bubble_burst.png */
    public int getBurstFrame() {
        return this.entityData.get(BURST_FRAME);
    }

    public boolean isBursting() {
        return getBurstFrame() > 0;
    }

    private void startBurst() {
        this.entityData.set(BURST_FRAME, 1);
        this.burstTickCounter = 0;
        this.noPhysics = true; // freeze movement during burst
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide)
            return;

        int frame = getBurstFrame();

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

        // Block collision: burst if our AABB intersects any solid block shape
        if (!this.level().noCollision(this, this.getBoundingBox())) {
            startBurst();
        }
    }

    /** Called when another entity physically overlaps and pushes this entity. */
    @Override
    public void push(Entity entity) {
        if (!this.level().isClientSide && getVehicle() == null && !isBursting()) {
            startBurst();
        }
        // Suppress normal push physics so the bubble doesn't fly away
    }

    @Override
    public void playerTouch(Player player) {
        if (!this.level().isClientSide && getVehicle() == null && !isBursting()) {
            startBurst();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && getVehicle() == null && !isBursting()) {
            startBurst();
        }
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
