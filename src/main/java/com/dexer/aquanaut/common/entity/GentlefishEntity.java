package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.ai.FishAttackMode;
import com.dexer.aquanaut.common.ai.FishResponseMode;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * Gentlefish — the diver's polite companion.
 *
 * <p>
 * It never bites. Instead it patrols the water around a diver, picks up dropped items it finds,
 * carries them back and hands them over. The top hat is not decorative: it bobs when it makes a
 * delivery and the fish moves faster while it is on an errand.
 */
public class GentlefishEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM_ANIMATION = RawAnimation.begin().thenLoop("swim");

    private static final EntityDataAccessor<Boolean> CARRYING = SynchedEntityData.defineId(
            GentlefishEntity.class, EntityDataSerializers.BOOLEAN);

    private static final double OWNER_RANGE = 12.0D;
    private static final double ITEM_RANGE = 8.0D;
    private static final double PICKUP_DISTANCE = 1.4D;
    private static final double DELIVERY_DISTANCE = 1.8D;
    private static final int RESCAN_TICKS = 20;
    private static final double STEER_SPEED = 0.085D;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int carriedItemId = -1;
    private int ownerId = -1;
    private int rescanCooldown;
    private ItemStack pendingDelivery = ItemStack.EMPTY;

    public GentlefishEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CARRYING, false);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 2, state -> {
            state.getController().setAnimationSpeed(this.isCarrying()
                    ? animSpeed(0.9, 1.5, 1.1, 1.8)
                    : animSpeed(0.4, 0.9, 0.9, 1.4));
            return state.setAndContinue(SWIM_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .build();
    }

    public boolean isCarrying() {
        return this.entityData.get(CARRYING);
    }

    private void setCarrying(boolean carrying) {
        this.entityData.set(CARRYING, carrying);
    }

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
        return 0.018D;
    }

    @Override
    protected double getCruiseMaxSpeed() {
        return 0.20D;
    }

    @Override
    protected double getEscapeAcceleration() {
        return 0.045D;
    }

    @Override
    protected double getEscapeMaxSpeed() {
        return 0.46D;
    }

    @Override
    protected double getPlayerDetectionRange() {
        return 8.0D;
    }

    @Override
    protected double getWaterDrag() {
        return 0.90D;
    }

    @Override
    protected float getBodyTurnRateDegrees() {
        return 10.0F;
    }

    @Override
    protected float getMaxTiltDegrees() {
        return 20.0F;
    }

    @Override
    protected float getHitboxPickInflation() {
        return 0.18F;
    }

    @Override
    protected double getHitboxVisualYOffset() {
        return 0.14D;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.isEffectiveAi() || this.level().isClientSide) {
            return;
        }

        if (this.rescanCooldown > 0) {
            this.rescanCooldown--;
        } else {
            this.rescanCooldown = RESCAN_TICKS;
            this.rescan();
        }

        if (this.isCarrying()) {
            this.runDelivery();
        } else {
            this.runPickup();
        }
    }

    private void rescan() {
        if (this.ownerId < 0) {
            Player owner = this.level().getNearestPlayer(this, OWNER_RANGE);
            this.ownerId = owner == null ? -1 : owner.getId();
        }

        if (this.carriedItemId >= 0) {
            return;
        }

        ItemEntity item = this.findItem();
        this.carriedItemId = item == null ? -1 : item.getId();
    }

    private ItemEntity findItem() {
        AABB area = this.getBoundingBox().inflate(ITEM_RANGE);
        List<ItemEntity> items = this.level().getEntitiesOfClass(ItemEntity.class, area,
                entity -> entity.isAlive() && !entity.getItem().isEmpty());
        ItemEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (ItemEntity item : items) {
            double distance = this.distanceToSqr(item);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = item;
            }
        }
        return closest;
    }

    private void runPickup() {
        if (this.carriedItemId < 0) {
            return;
        }

        Entity target = this.level().getEntity(this.carriedItemId);
        if (!(target instanceof ItemEntity item) || !item.isAlive()) {
            this.carriedItemId = -1;
            return;
        }

        this.steerTowards(item.position().add(0.0D, 0.15D, 0.0D));
        if (this.distanceToSqr(item) <= PICKUP_DISTANCE * PICKUP_DISTANCE) {
            this.pendingDelivery = item.getItem().copy();
            item.discard();
            this.carriedItemId = -1;
            this.setCarrying(true);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(),
                        this.getY() + this.getBbHeight() * 0.6D, this.getZ(), 6, 0.2D, 0.15D, 0.2D, 0.01D);
            }
        }
    }

    private void runDelivery() {
        Player owner = this.ownerId >= 0 && this.level().getEntity(this.ownerId) instanceof Player player
                ? player
                : this.level().getNearestPlayer(this, OWNER_RANGE);

        if (owner == null) {
            // Nobody to hand it to: put it back in the water and resume patrolling.
            if (!this.pendingDelivery.isEmpty()) {
                this.spawnAtLocation(this.pendingDelivery);
                this.pendingDelivery = ItemStack.EMPTY;
            }
            this.setCarrying(false);
            return;
        }

        if (this.pendingDelivery.isEmpty()) {
            this.setCarrying(false);
            return;
        }

        this.ownerId = owner.getId();
        this.steerTowards(owner.position().add(0.0D, owner.getBbHeight() * 0.5D, 0.0D));

        if (this.distanceToSqr(owner) > DELIVERY_DISTANCE * DELIVERY_DISTANCE) {
            return;
        }

        if (!owner.getInventory().add(this.pendingDelivery)) {
            this.spawnAtLocation(this.pendingDelivery);
        }
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.6F, 1.4F);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART, this.getX(),
                    this.getY() + this.getBbHeight() * 0.7D, this.getZ(), 3, 0.15D, 0.1D, 0.15D, 0.01D);
        }
        this.pendingDelivery = ItemStack.EMPTY;
        this.ownerId = -1;
        this.setCarrying(false);
        this.rescanCooldown = RESCAN_TICKS * 2;
    }

    private void steerTowards(Vec3 target) {
        Vec3 direction = target.subtract(this.position());
        if (direction.lengthSqr() < 1.0E-4D) {
            return;
        }

        this.setDeltaMovement(direction.normalize().scale(STEER_SPEED));
        this.hasImpulse = true;

        float yaw = (float) (Mth.atan2(direction.z, direction.x) * (180.0D / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Carrying", this.isCarrying());
        if (!this.pendingDelivery.isEmpty()) {
            tag.put("Delivery", this.pendingDelivery.save(this.registryAccess()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setCarrying(tag.getBoolean("Carrying"));
        this.pendingDelivery = tag.contains("Delivery")
                ? ItemStack.parseOptional(this.registryAccess(), tag.getCompound("Delivery"))
                : ItemStack.EMPTY;
    }
}
