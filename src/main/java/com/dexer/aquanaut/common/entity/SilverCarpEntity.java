package com.dexer.aquanaut.common.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Silver carp — the schooling defensive variant of the carp rig.
 *
 * <p>
 * Its scales throw a hard glint when struck: the whole nearby shoal flashes and whatever hit it is
 * briefly blinded, which buys the school the moment it needs to scatter.
 */
public class SilverCarpEntity extends AbstractCarpEntity {
    private static final double DAZZLE_RADIUS = 8.0D;
    private static final int DAZZLE_BLINDNESS_TICKS = 60;

    public SilverCarpEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .build();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);

        if (!hurt || this.level().isClientSide) {
            return hurt;
        }

        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, DAZZLE_BLINDNESS_TICKS, 0));
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            List<SilverCarpEntity> shoal = serverLevel.getEntitiesOfClass(SilverCarpEntity.class,
                    this.getBoundingBox().inflate(DAZZLE_RADIUS));
            for (SilverCarpEntity carp : shoal) {
                serverLevel.sendParticles(ParticleTypes.END_ROD, carp.getX(), carp.getY() + 0.4D, carp.getZ(),
                        6, 0.16D, 0.12D, 0.16D, 0.01D);
            }
        }

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.7F, 1.6F);
        return true;
    }

    @Override
    protected double getSchoolingSearchRadius() {
        return 12.0D;
    }

    @Override
    protected double getSchoolingSeparationRadius() {
        return 1.15D;
    }

    @Override
    protected double getSchoolingFollowDistance() {
        return 1.7D;
    }
}
