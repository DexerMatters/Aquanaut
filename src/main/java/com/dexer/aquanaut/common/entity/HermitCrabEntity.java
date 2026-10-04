package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.mud.MudZoneConfig;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class HermitCrabEntity extends WaterAnimal implements GeoEntity {
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation HIDE = RawAnimation.begin().thenPlay("hide");
    private static final RawAnimation EMERGE = RawAnimation.begin().thenPlay("emerge");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");
    private static final EntityDataAccessor<Boolean> SHELLED = SynchedEntityData.defineId(HermitCrabEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SHELL_SIZE = SynchedEntityData.defineId(HermitCrabEntity.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> EMERGE_TIMER = SynchedEntityData.defineId(HermitCrabEntity.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ATTACK_TIMER = SynchedEntityData.defineId(HermitCrabEntity.class,
            EntityDataSerializers.INT);
    /** Ticks the emerge clip plays for: 0.4 s at 20 tps, matching the exported clip. */
    private static final int EMERGE_TICKS = 8;
    /** Ticks the claw-snap clip plays for after a landed hit. */
    private static final int ATTACK_ANIM_TICKS = 10;
    /** Horizontal speed above which the crab reads as walking rather than idle. */
    private static final double CRAWL_SPEED_SQR = 4.0E-4D;
    /** The current shell tier's health bonus, keyed so a new tier replaces it instead of stacking. */
    private static final ResourceLocation SHELL_HEALTH_ID =
            ResourceLocation.fromNamespaceAndPath("aquanaut", "shell_tier_health");
    private int shellTicks;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public HermitCrabEntity(EntityType<? extends WaterAnimal> type, Level level) { super(type, level); }
    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.12D)
                .add(Attributes.ATTACK_DAMAGE, 1.5D).build();
    }
    @Override protected void registerGoals() {
        super.registerGoals();
        // Neutral: it only fights back after being hurt.
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SHELLED, false);
        builder.define(SHELL_SIZE, 0);
        builder.define(EMERGE_TIMER, 0);
        builder.define(ATTACK_TIMER, 0);
    }
    @Override public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) {
            setShelled(true);
        }
        return super.hurt(source, isShelled() ? amount * MudZoneConfig.SHELL_DAMAGE_MULTIPLIER : amount);
    }
    @Override public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && !level().isClientSide) {
            entityData.set(ATTACK_TIMER, ATTACK_ANIM_TICKS);
        }
        return hit;
    }
    @Override public void aiStep() {
        if (!level().isClientSide && attackTimer() > 0) {
            entityData.set(ATTACK_TIMER, attackTimer() - 1);
        }
        if (isShelled()) {
            if (!level().isClientSide && --shellTicks <= 0) {
                setShelled(false);
            }
            return;
        }
        if (!level().isClientSide && emergeTimer() > 0) {
            entityData.set(EMERGE_TIMER, emergeTimer() - 1);
        }
        if (!level().isClientSide && tickCount % 40 == 0 && shellSize() < MudZoneConfig.SHELL_MAX_SIZE
                && level().getBlockStates(new net.minecraft.world.phys.AABB(blockPosition()).inflate(
                        MudZoneConfig.SHELL_SEARCH_RADIUS))
                        .anyMatch(state -> state.is(BlockRegistry.SHELL_BLOCK.get())
                                || state.is(BlockRegistry.HARD_SHELL_BLOCK.get())
                                || state.is(BlockRegistry.SHELL_PILE.get()))
                && random.nextFloat() < MudZoneConfig.SHELL_UPGRADE_CHANCE) {
            growShell();
        }
        super.aiStep();
    }
    public boolean isShelled() { return entityData.get(SHELLED); }
    public int shellSize() { return entityData.get(SHELL_SIZE); }
    private int emergeTimer() { return entityData.get(EMERGE_TIMER); }
    private int attackTimer() { return entityData.get(ATTACK_TIMER); }
    private boolean isCrawling() {
        return getDeltaMovement().horizontalDistanceSqr() > CRAWL_SPEED_SQR;
    }
    /**
     * Grow into the next shell tier: a bigger shell (the renderer scales with the tier), a
     * tougher body, and a fresh emerge so the swap reads as an event.
     */
    private void growShell() {
        int tier = Math.min(shellSize() + 1, MudZoneConfig.SHELL_MAX_SIZE);
        entityData.set(SHELL_SIZE, tier);
        AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.removeModifier(SHELL_HEALTH_ID);
            health.addPermanentModifier(new AttributeModifier(SHELL_HEALTH_ID,
                    tier * MudZoneConfig.SHELL_HEALTH_PER_TIER, AttributeModifier.Operation.ADD_VALUE));
            setHealth(getMaxHealth());
        }
        entityData.set(EMERGE_TIMER, EMERGE_TICKS);
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (attackTimer() > 0) {
                return state.setAndContinue(ATTACK);
            }
            if (isShelled()) {
                return state.setAndContinue(HIDE);
            }
            if (emergeTimer() > 0) {
                return state.setAndContinue(EMERGE);
            }
            if (isCrawling()) {
                return state.setAndContinue(WALK);
            }
            return state.setAndContinue(IDLE);
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    private void setShelled(boolean value) {
        entityData.set(SHELLED, value);
        if (value) {
            shellTicks = MudZoneConfig.SHELL_DURATION_TICKS;
        } else {
            entityData.set(EMERGE_TIMER, EMERGE_TICKS);
        }
    }
}
