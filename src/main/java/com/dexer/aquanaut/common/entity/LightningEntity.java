package com.dexer.aquanaut.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class LightningEntity extends AbstractBranchingLightningEntity {
    public LightningEntity(EntityType<? extends LightningEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected int defaultActiveTicks() {
        return 30;
    }

    @Override
    protected float defaultLength() {
        return 10.0F;
    }

    @Override
    protected float defaultDamage() {
        return 9.0F;
    }

    @Override
    protected float defaultThickness() {
        return 0.12F;
    }

    @Override
    protected int defaultBranchCount() {
        return 4;
    }

    @Override
    protected int defaultBranchDepth() {
        return 3;
    }

    @Override
    protected float defaultBranchScale() {
        return 0.48F;
    }

    @Override
    public boolean isExpired() {
        return this.getLightningAge() >= this.getLightningTotalTicks();
    }
}
