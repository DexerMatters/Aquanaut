package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.ai.FishAttackMode;
import com.dexer.aquanaut.common.ai.FishResponseMode;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Passive jellyfish that releases a local humus cloud when struck. */
public final class HumusJellyEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation FLOAT = RawAnimation.begin().thenLoop("float");
    private int cloudCooldown;
    private AnimatableInstanceCache cache;

    public HumusJellyEntity(EntityType<? extends WaterAnimal> type, Level level) { super(type, level); }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.04D).build();
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> state.setAndContinue(FLOAT)));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() {
        if (cache == null) cache = GeckoLibUtil.createInstanceCache(this);
        return cache;
    }

    @Override public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && cloudCooldown <= 0) {
            cloudCooldown = 200;
            for (int i = 0; i < 18; i++) {
                level().addParticle(ParticleTypes.SPORE_BLOSSOM_AIR,
                        getX() + (random.nextDouble() - 0.5D) * 2.0D,
                        getY() + random.nextDouble(),
                        getZ() + (random.nextDouble() - 0.5D) * 2.0D,
                        0.0D, 0.01D, 0.0D);
            }
        }
        return hurt;
    }

    @Override public void tick() {
        super.tick();
        if (cloudCooldown > 0) cloudCooldown--;
    }

    @Override protected FishResponseMode getResponseMode() { return FishResponseMode.PASSIVE; }
    @Override protected FishAttackMode getAttackMode() { return FishAttackMode.NONE; }
    @Override protected double getCruiseMaxSpeed() { return 0.025D; }
    @Override protected double getCruiseAcceleration() { return 0.001D; }
    @Override protected double getCruiseFloorBias() { return 0.6D; }
    @Override protected double getCruiseDepthRange() { return 2.0D; }
}
