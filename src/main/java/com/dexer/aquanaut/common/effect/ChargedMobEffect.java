package com.dexer.aquanaut.common.effect;

import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * The bearer crackles with current: once a second every living creature inside the aura takes a
 * shock that scales with the effect level.
 *
 * <p>
 * The bearer is not spared -- the aura is centred on them and reaches everything in range, which is
 * what makes this a debuff rather than a free damage aura. The shock is delivered as vanilla
 * lightning damage: armour and enchantments still soften it, and that damage type carries no
 * knockback, so a lasting aura does not shove everything nearby across the seabed.
 */
public final class ChargedMobEffect extends MobEffect {

    /** One discharge per second. */
    private static final int DISCHARGE_INTERVAL_TICKS = 20;

    /** Sparks thrown across the aura on each discharge. */
    private static final int DISCHARGE_PARTICLES = 10;

    public ChargedMobEffect() {
        super(MobEffectCategory.HARMFUL, 0xF2C43F);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) {
            return true;
        }
        double radius = EffectLevelScaling.auraRadius(amplifier);
        float damage = EffectLevelScaling.auraDamagePerSecond(amplifier);
        DamageSource shock = entity.damageSources().lightningBolt();
        entity.hurt(shock, damage);
        for (LivingEntity nearby : shockableAround(entity, amplifier)) {
            nearby.hurt(shock, damage);
        }
        if (entity.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(),
                    entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(), DISCHARGE_PARTICLES,
                    radius * 0.5D, entity.getBbHeight() * 0.5D, radius * 0.5D, 0.0D);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % DISCHARGE_INTERVAL_TICKS == 0;
    }

    /**
     * Everything alive inside the sphere, the bearer aside: they are shocked separately so that the
     * self-damage is deliberate rather than a side effect of the query returning its own source.
     */
    private static List<LivingEntity> shockableAround(LivingEntity bearer, int amplifier) {
        AABB area = bearer.getBoundingBox().inflate(EffectLevelScaling.auraRadius(amplifier));
        return bearer.level().getEntitiesOfClass(LivingEntity.class, area,
                target -> target != bearer && target.isAlive() && !target.isSpectator()
                        && EffectLevelScaling.auraReaches(amplifier, target.distanceToSqr(bearer)));
    }
}
