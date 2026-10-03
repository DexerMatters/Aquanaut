package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.mud.MudZoneConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.Level;

/** Silverfish variant released from parasitic mud. */
public final class MudSilverfishEntity extends Silverfish {
    public MudSilverfishEntity(EntityType<? extends Silverfish> type, Level level) {
        super(type, level);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && level().getBrightness(LightLayer.BLOCK, BlockPos.containing(position()))
                >= MudZoneConfig.BRIGHT_LIGHT_THRESHOLD) {
            setDeltaMovement(getDeltaMovement().add(0.0D, 0.02D, 0.0D));
            getNavigation().stop();
        }
    }
}
