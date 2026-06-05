package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.gaze.GazeCatalog;
import com.dexer.aquanaut.common.gaze.GazeHelper;
import com.dexer.aquanaut.common.item.HarpoonItem;
import com.dexer.aquanaut.core.EntityRegistry;
import com.dexer.aquanaut.core.ItemRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import org.joml.Vector3f;

public class HarpoonEntity extends AbstractArrow {
    private static final double AUTO_RECALL_DISTANCE = 48.0D;

    private static final EntityDataAccessor<ItemStack> DATA_HARPOON_STACK = SynchedEntityData
            .defineId(HarpoonEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> DATA_SHADOW = SynchedEntityData
            .defineId(HarpoonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_RECALLING = SynchedEntityData
            .defineId(HarpoonEntity.class, EntityDataSerializers.BOOLEAN);

    private boolean dealtDamage;
    private boolean thrownFromOffhand;
    private int shadowLevel;
    private final Set<Integer> penetratedEntityIds = new HashSet<>();

    public HarpoonEntity(EntityType<? extends HarpoonEntity> type, Level level) {
        super(type, level);
    }

    public HarpoonEntity(Level level, Player shooter, ItemStack stack) {
        super(EntityRegistry.HARPOON.get(), shooter, level, stack.copy(), null);
        this.entityData.set(DATA_HARPOON_STACK, stack.copy());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HARPOON_STACK, ItemStack.EMPTY);
        builder.define(DATA_SHADOW, false);
        builder.define(DATA_RECALLING, false);
    }

    /** Returns the synced stack — correct item type even on client. */
    public ItemStack getSyncedHarpoonStack() {
        return this.entityData.get(DATA_HARPOON_STACK);
    }

    public boolean isShadow() {
        return this.entityData.get(DATA_SHADOW);
    }

    public boolean isRecalling() {
        return this.entityData.get(DATA_RECALLING);
    }

    public void setShadow(int level) {
        this.shadowLevel = Math.max(1, Math.min(3, level));
        this.entityData.set(DATA_SHADOW, true);
        this.pickup = Pickup.DISALLOWED;
    }

    public void beginRecall() {
        if (!isShadow() && isRecallable()) {
            this.entityData.set(DATA_RECALLING, true);
            this.setNoGravity(true);
            this.noPhysics = true;
            this.inGround = false;
            this.shakeTime = 0;
        }
    }

    public void setThrownHand(InteractionHand hand) {
        this.thrownFromOffhand = hand == InteractionHand.OFF_HAND;
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ItemRegistry.IRON_HARPOON.get());
    }

    @Nullable
    @Override
    protected EntityHitResult findHitEntity(Vec3 startVec, Vec3 endVec) {
        if (this.dealtDamage && !canPenetrate() && !isRecalling()) {
            return null;
        }
        EntityHitResult result = super.findHitEntity(startVec, endVec);
        if (result == null) {
            return null;
        }
        Entity entity = result.getEntity();
        if (entity == getOwner() || penetratedEntityIds.contains(entity.getId())) {
            return null;
        }
        return result;
    }

    @Override
    public void tick() {
        if (isRecalling()) {
            tickRecall();
        } else {
            tickAutoRecall();
            updateFloatingGravity();
        }
        super.tick();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        float damage = 5.0F;
        ItemStack weapon = this.getPickupItemStackOrigin();
        if (weapon.getItem() instanceof HarpoonItem harpoon) {
            damage = harpoon.getThrownDamage();
        }
        if (isShadow()) {
            damage *= shadowDamageMultiplier();
        }

        Entity owner = this.getOwner();
        DamageSource source = this.damageSources().trident(this, owner != null ? owner : this);

        if (this.level() instanceof ServerLevel serverLevel) {
            damage = EnchantmentHelper.modifyDamage(serverLevel, weapon, target, source, damage);
        }

        boolean penetrates = isRecalling() || target instanceof LivingEntity livingTarget
                && shouldPenetrate(damage, livingTarget.getMaxHealth());
        if (penetrates) {
            penetratedEntityIds.add(target.getId());
        }
        this.dealtDamage = !penetrates;
        SoundEvent soundEvent = SoundEvents.TRIDENT_HIT;

        if (target.hurt(source, damage)) {
            if (target instanceof LivingEntity livingTarget) {
                livingTarget.invulnerableTime = 0;
                if (this.level() instanceof ServerLevel serverLevel1) {
                    EnchantmentHelper.doPostAttackEffectsWithItemSource(serverLevel1, target, source, weapon);
                }
                this.doPostHurtEffects(livingTarget);
            }
        }

        if (isShadow()) {
            discardWithAquaParticles();
            return;
        }

        if (isRecallable() && !isRecalling() && !penetrates) {
            beginRecall();
            this.playSound(soundEvent, 1.0F, 1.0F);
            return;
        }

        if (!penetrates) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(-0.01, -0.1, -0.01));
        }
        this.playSound(soundEvent, 1.0F, 1.0F);

        // Damage the harpoon on hit (skip in creative)
        if (this.pickup != Pickup.CREATIVE_ONLY) {
            ItemStack pickupStack = this.getPickupItemStackOrigin();
            if (!pickupStack.isEmpty() && pickupStack.isDamageableItem()
                    && pickupStack.getDamageValue() < pickupStack.getMaxDamage()) {
                pickupStack.setDamageValue(pickupStack.getDamageValue() + 1);
                this.entityData.set(DATA_HARPOON_STACK, pickupStack.copy());
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (isShadow()) {
            discardWithAquaParticles();
            return;
        }
        if (isRecalling()) {
            detachFromBlock(result);
            return;
        }
        if (GazeHelper.getLevel(getPickupItemStackOrigin(), GazeCatalog.RECALLING) > 0) {
            beginRecall();
            detachFromBlock(result);
            return;
        }
        super.onHitBlock(result);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.dealtDamage = tag.getBoolean("DealtDamage");
        this.thrownFromOffhand = tag.getBoolean("ThrownFromOffhand");
        this.shadowLevel = tag.getInt("ShadowLevel");
        this.entityData.set(DATA_SHADOW, tag.getBoolean("Shadow"));
        this.entityData.set(DATA_RECALLING, tag.getBoolean("Recalling"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("DealtDamage", this.dealtDamage);
        tag.putBoolean("ThrownFromOffhand", this.thrownFromOffhand);
        tag.putBoolean("Shadow", isShadow());
        tag.putInt("ShadowLevel", this.shadowLevel);
        tag.putBoolean("Recalling", isRecalling());
    }

    @Override
    public boolean isNoGravity() {
        return isRecalling() || floatingLevel() >= 3 && isInWater() || super.isNoGravity();
    }

    @Override
    protected float getWaterInertia() {
        return 1.0F;
    }

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return true;
    }

    private void tickRecall() {
        Entity owner = getOwner();
        if (!(owner instanceof Player player) || !player.isAlive()) {
            this.entityData.set(DATA_RECALLING, false);
            this.setNoGravity(false);
            this.noPhysics = false;
            this.inGround = false;
            return;
        }

        Vec3 target = player.getEyePosition().add(0.0D, -0.35D, 0.0D);
        Vec3 toPlayer = target.subtract(position());
        this.noPhysics = true;
        this.inGround = false;
        if (toPlayer.lengthSqr() < 1.0D) {
            if (!level().isClientSide && this.pickup != Pickup.CREATIVE_ONLY && !getPickupItemStackOrigin().isEmpty()) {
                returnToThrowingHand(player, getPickupItemStackOrigin().copy());
            }
            discard();
            return;
        }

        setDeltaMovement(toPlayer.normalize().scale(1.45D));
        damageEntitiesDuringRecall();
    }

    private void returnToThrowingHand(Player player, ItemStack stack) {
        InteractionHand hand = thrownFromOffhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (player.getItemInHand(hand).isEmpty()) {
            player.setItemInHand(hand, stack);
        } else {
            player.getInventory().add(stack);
        }
    }

    private void damageEntitiesDuringRecall() {
        Vec3 movement = getDeltaMovement();
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().expandTowards(movement).inflate(0.35D),
                target -> target != getOwner() && target.isAlive() && !penetratedEntityIds.contains(target.getId()))) {
            onHitEntity(new EntityHitResult(target));
        }
    }

    private void tickAutoRecall() {
        if (!isRecallable()) {
            return;
        }
        Entity owner = getOwner();
        if (owner != null && distanceToSqr(owner) > AUTO_RECALL_DISTANCE * AUTO_RECALL_DISTANCE) {
            beginRecall();
        }
    }

    private void detachFromBlock(BlockHitResult result) {
        Vec3 normal = Vec3.atLowerCornerOf(result.getDirection().getNormal());
        Vec3 location = result.getLocation().add(normal.scale(0.18D));
        Vec3 velocity = getDeltaMovement();
        Vec3 slide = velocity.subtract(normal.scale(velocity.dot(normal)));

        this.inGround = false;
        this.shakeTime = 0;
        setPos(location.x, location.y, location.z);
        setDeltaMovement(slide.add(normal.scale(0.25D)));
    }

    private void updateFloatingGravity() {
        int level = floatingLevel();
        if (!isInWater()) {
            if (!isRecalling() && super.isNoGravity()) {
                setNoGravity(false);
            }
            return;
        }
        if (level >= 3) {
            setNoGravity(true);
        } else if (level > 0 && !isNoGravity()) {
            double gravityCompensation = level == 1 ? 0.025D : 0.04D;
            setDeltaMovement(getDeltaMovement().add(0.0D, gravityCompensation, 0.0D));
        }
    }

    private boolean canPenetrate() {
        return GazeHelper.getLevel(getPickupItemStackOrigin(), GazeCatalog.PENETRATION) > 0;
    }

    private boolean shouldPenetrate(float damage, float health) {
        int level = GazeHelper.getLevel(getPickupItemStackOrigin(), GazeCatalog.PENETRATION);
        if (level <= 0 || health <= 0.0F) {
            return false;
        }
        float ratio = damage / health;
        return ratio >= switch (level) {
            case 1 -> 1.0F;
            case 2 -> 0.6F;
            default -> 0.3F;
        };
    }

    private int floatingLevel() {
        return GazeHelper.getLevel(getPickupItemStackOrigin(), GazeCatalog.FLOATING);
    }

    private boolean isRecallable() {
        return GazeHelper.getLevel(getPickupItemStackOrigin(), GazeCatalog.RECALLING) > 0;
    }

    private float shadowDamageMultiplier() {
        return switch (shadowLevel) {
            case 1 -> 0.5F;
            case 2 -> 0.6F;
            default -> 0.75F;
        };
    }

    private void discardWithAquaParticles() {
        if (level() instanceof ServerLevel serverLevel) {
            Vec3 back = getDeltaMovement().lengthSqr() > 1.0E-4D
                    ? getDeltaMovement().normalize().reverse()
                    : new Vec3(0.0D, 0.05D, 0.0D);
            DustParticleOptions aquaDust = new DustParticleOptions(new Vector3f(0.15F, 0.85F, 0.9F), 0.7F);
            for (int i = 0; i < 10; i++) {
                Vec3 offset = back.scale(i * 0.045D);
                serverLevel.sendParticles(aquaDust, getX() + offset.x, getY() + offset.y, getZ() + offset.z,
                        1, 0.025D, 0.025D, 0.025D, 0.0D);
            }
            serverLevel.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(),
                    3, 0.04D, 0.04D, 0.04D, 0.01D);
        }
        discard();
    }
}
