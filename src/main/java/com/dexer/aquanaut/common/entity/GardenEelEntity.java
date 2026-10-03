package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.ai.FishResponseMode;
import com.dexer.aquanaut.common.mud.MudZoneConfig;
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

public final class GardenEelEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation IDLE_SWAY = RawAnimation.begin().thenLoop("idle_sway");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public GardenEelEntity(EntityType<? extends WaterAnimal> type, Level level) { super(type, level); }
    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, MudZoneConfig.GARDEN_EEL_CRUISE_SPEED).build();
    }
    @Override protected FishResponseMode getResponseMode() { return FishResponseMode.AVOIDANCE; }
    @Override protected boolean getSchoolingEnabled() { return true; }
    @Override protected double getSchoolingSearchRadius() { return 6.0D; }
    @Override protected double getCruiseMaxSpeed() { return MudZoneConfig.GARDEN_EEL_CRUISE_SPEED; }
    @Override protected double getCruiseAcceleration() { return 0.001D; }
    @Override protected double getEscapeMaxSpeed() { return 0.12D; }
    @Override protected double getCruiseFloorBias() { return 1.0D; }
    @Override protected double getCruiseDepthRange() { return 0.8D; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0,
                state -> state.setAndContinue(IDLE_SWAY)));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
