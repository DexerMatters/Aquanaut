package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.ai.FishAttackMode;
import com.dexer.aquanaut.common.ai.FishResponseMode;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;

/**
 * Skeleton carp — the reanimated variant of the carp rig.
 *
 * <p>
 * Bare ribs, a working tail and no interest in fleeing: it scavenges and pursues. The first blow
 * that would end it instead knocks the frame apart, and the bones knit back together at a
 * fraction of their strength before the structure gives out for good.
 */
public class SkeletonCarpEntity extends AbstractCarpEntity {
    private static final float REASSEMBLY_HEALTH_FRACTION = 0.40F;
    private static final int REASSEMBLY_INVULNERABILITY_TICKS = 20;
    private static final int REASSEMBLY_COOLDOWN_TICKS = 1200;

    private boolean reassembled;
    private int reassemblyCooldown;

    public SkeletonCarpEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .build();
    }

    @Override
    protected FishResponseMode getResponseMode() {
        return FishResponseMode.CHARGE;
    }

    @Override
    protected FishAttackMode getAttackMode() {
        return FishAttackMode.TRACKING_BITE;
    }

    @Override
    protected boolean getSchoolingEnabled() {
        return false;
    }

    @Override
    protected double getPlayerDetectionRange() {
        return 12.0D;
    }

    @Override
    protected double getChargeAcceleration() {
        return 0.045D;
    }

    @Override
    protected double getChargeMaxSpeed() {
        return 0.48D;
    }

    @Override
    protected double getBaseBiteDamage() {
        return 2.0D;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.reassemblyCooldown > 0) {
            this.reassemblyCooldown--;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide
                && !this.reassembled
                && this.reassemblyCooldown <= 0
                && this.getHealth() > 0.0F
                && amount >= this.getHealth()) {
            this.reassembled = true;
            this.reassemblyCooldown = REASSEMBLY_COOLDOWN_TICKS;
            this.setHealth(Math.max(1.0F, this.getMaxHealth() * REASSEMBLY_HEALTH_FRACTION));

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + this.getBbHeight() * 0.5D,
                        this.getZ(), 18, 0.35D, 0.3D, 0.35D, 0.02D);
            }
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.SKELETON_HURT, SoundSource.HOSTILE, 1.0F, 0.6F);

            // Register the hit without letting it kill: the frame re-knits instead.
            return super.hurt(source, 0.0F);
        }

        return super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Reassembled", this.reassembled);
        tag.putInt("ReassemblyCooldown", this.reassemblyCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.reassembled = tag.getBoolean("Reassembled");
        this.reassemblyCooldown = tag.getInt("ReassemblyCooldown");
    }

    @Override
    protected double getHitboxVisualYOffset() {
        return 0.18D;
    }
}
