package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.mud.MudZoneConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Silverfish variant released from parasitic mud. */
public final class MudSilverfishEntity extends Silverfish implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final double WALK_SPEED_SQR = 4.0E-4D;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MudSilverfishEntity(EntityType<? extends Silverfish> type, Level level) {
        super(type, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state ->
                state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() > WALK_SPEED_SQR ? WALK : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
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
