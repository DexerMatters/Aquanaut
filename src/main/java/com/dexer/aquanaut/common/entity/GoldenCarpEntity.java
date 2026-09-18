package com.dexer.aquanaut.common.entity;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Golden carp — the lucky variant of the carp rig.
 *
 * <p>
 * It schools like its relatives, but a diver who keeps it in sight is quietly favoured:
 * every ten seconds the nearest onlooker receives {@code Luck}.
 */
public class GoldenCarpEntity extends AbstractCarpEntity {
    private static final int FAVOUR_INTERVAL_TICKS = 200;
    private static final double FAVOUR_RANGE = 6.0D;
    private static final int FAVOUR_DURATION_TICKS = 1200;

    private int favourCooldown;

    public GoldenCarpEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .build();
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.isEffectiveAi() || this.level().isClientSide) {
            return;
        }

        if (this.favourCooldown > 0) {
            this.favourCooldown--;
            return;
        }

        Player onlooker = this.level().getNearestPlayer(this, FAVOUR_RANGE);
        if (onlooker == null || onlooker.isCreative() || onlooker.isSpectator()
                || !this.hasLineOfSight(onlooker)) {
            return;
        }

        onlooker.addEffect(new MobEffectInstance(MobEffects.LUCK, FAVOUR_DURATION_TICKS, 0, true, false));
        this.favourCooldown = FAVOUR_INTERVAL_TICKS;
    }

    @Override
    protected double getSchoolingFollowDistance() {
        return 1.85D;
    }

    @Override
    protected double getEscapeMaxSpeed() {
        return 0.60D;
    }

    @Override
    protected float getEscapeTurnRateDegrees() {
        return 19.0F;
    }
}
