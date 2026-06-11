package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.common.ai.FishAttackMode;
import com.dexer.aquanaut.common.ai.FishResponseMode;
import com.dexer.aquanaut.core.EntityRegistry;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
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
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.phys.Vec3;

public class ElectrofishEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation FLOAT_ANIMATION = RawAnimation.begin().thenLoop("float");

    private static final int STATE_IDLE = 0;
    private static final int STATE_CHARGING = 1;
    private static final int STATE_DISCHARGING = 2;

    private static final EntityDataAccessor<Integer> CHARGE_STATE = SynchedEntityData.defineId(
            ElectrofishEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> CHARGE_PROGRESS = SynchedEntityData.defineId(
            ElectrofishEntity.class, EntityDataSerializers.FLOAT);

    private static final int CHARGE_DURATION_TICKS = 30;
    private static final int DISCHARGE_DURATION_TICKS = 18;
    private static final int MIN_IDLE_TICKS = 80;
    private static final int MAX_IDLE_RANDOM_TICKS = 121;
    private static final int LIGHTNING_COUNT_MIN = 5;
    private static final int LIGHTNING_COUNT_MAX = 8;
    private static final float LIGHTNING_LENGTH_MIN = 4.5F;
    private static final float LIGHTNING_LENGTH_RANDOM = 3.5F;
    private static final float LIGHTNING_DAMAGE = 36.0F;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int stateTimer;

    public ElectrofishEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
        this.stateTimer = MIN_IDLE_TICKS + this.random.nextInt(MAX_IDLE_RANDOM_TICKS);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARGE_STATE, STATE_IDLE);
        builder.define(CHARGE_PROGRESS, 0.0F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_LIGHTNING)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            state.getController().setAnimationSpeed(animSpeed(0.25, 0.52));
            return state.setAndContinue(FLOAT_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (!this.isInWater()) {
            this.resetChargeCycle();
            return;
        }

        int state = this.entityData.get(CHARGE_STATE);
        this.stateTimer--;

        switch (state) {
            case STATE_IDLE:
                if (this.stateTimer <= 0) {
                    this.entityData.set(CHARGE_STATE, STATE_CHARGING);
                    this.stateTimer = CHARGE_DURATION_TICKS;
                }
                break;
            case STATE_CHARGING:
                float progress = 1.0F - (float) Math.max(0, this.stateTimer) / (float) CHARGE_DURATION_TICKS;
                this.entityData.set(CHARGE_PROGRESS, Mth.clamp(progress, 0.0F, 1.0F));
                if (this.stateTimer <= 0) {
                    this.emitLightning();
                    this.entityData.set(CHARGE_STATE, STATE_DISCHARGING);
                    this.stateTimer = DISCHARGE_DURATION_TICKS;
                }
                break;
            case STATE_DISCHARGING:
                if (this.stateTimer <= 0) {
                    this.resetChargeCycle();
                }
                break;
        }
    }

    public float getChargeProgress() {
        return this.entityData.get(CHARGE_PROGRESS);
    }

    public boolean isCharging() {
        return this.entityData.get(CHARGE_STATE) == STATE_CHARGING;
    }

    private void resetChargeCycle() {
        this.entityData.set(CHARGE_STATE, STATE_IDLE);
        this.entityData.set(CHARGE_PROGRESS, 0.0F);
        this.stateTimer = MIN_IDLE_TICKS + this.random.nextInt(MAX_IDLE_RANDOM_TICKS);
    }

    private void emitLightning() {
        int count = LIGHTNING_COUNT_MIN + this.random.nextInt(LIGHTNING_COUNT_MAX - LIGHTNING_COUNT_MIN + 1);
        Vec3[] directions = this.fibonacciSphereDirections(count);
        double originX = this.getX();
        double originY = this.getY() + this.getBbHeight() * 0.5;
        double originZ = this.getZ();

        for (int i = 0; i < count; i++) {
            Vec3 dir = directions[i];
            float yaw = (float) (Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG) - 90.0F;
            float pitch = (float) (-(Mth.atan2(dir.y, dir.horizontalDistance()) * Mth.RAD_TO_DEG));

            LightningEntity lightning = new LightningEntity(EntityRegistry.LIGHTNING.get(), this.level());
            lightning.setPos(originX, originY, originZ);
            lightning.setLightningYaw(yaw);
            lightning.setLightningPitch(pitch);
            lightning.setLightningLength(LIGHTNING_LENGTH_MIN + this.random.nextFloat() * LIGHTNING_LENGTH_RANDOM);
            lightning.setLightningDamage(LIGHTNING_DAMAGE);
            lightning.setLightningActiveTicks(12);
            lightning.setLightningFadeTicks(6);
            lightning.setCausesFire(true);
            lightning.excludeEntity(this.getId());

            this.level().addFreshEntity(lightning);
        }
    }

    private Vec3[] fibonacciSphereDirections(int n) {
        Vec3[] points = new Vec3[n];
        float phi = (float) (Math.PI * (3.0 - Math.sqrt(5.0)));

        for (int i = 0; i < n; i++) {
            float y = 1.0F - ((float) i / (float) (n - 1)) * 2.0F;
            float radius = (float) Math.sqrt(1.0F - y * y);
            float theta = phi * (float) i;
            float x = (float) Math.cos(theta) * radius;
            float z = (float) Math.sin(theta) * radius;
            points[i] = new Vec3(x, y, z);
        }

        float angleX = (this.random.nextFloat() - 0.5F) * 360.0F;
        float angleY = (this.random.nextFloat() - 0.5F) * 360.0F;

        for (int i = 0; i < n; i++) {
            points[i] = points[i]
                    .xRot(angleX * Mth.DEG_TO_RAD)
                    .yRot(angleY * Mth.DEG_TO_RAD);
        }

        return points;
    }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.1D)
                .build();
    }

    protected double getFloatAnimationSpeed() {
        return 0.52D;
    }

    @Override
    protected FishResponseMode getResponseMode() {
        return FishResponseMode.PASSIVE;
    }

    @Override
    protected FishAttackMode getAttackMode() {
        return FishAttackMode.NONE;
    }

    @Override
    protected double getCruiseAcceleration() {
        return 0.003D;
    }

    @Override
    protected double getCruiseMaxSpeed() {
        return 0.052D;
    }

    @Override
    protected double getWaterDrag() {
        return 0.86D;
    }

    @Override
    protected float getCruiseTurnChance() {
        return 0.55F;
    }

    @Override
    protected float getCruiseTurnRangeDegrees() {
        return 56.0F;
    }

    @Override
    protected float getCruiseYawTurnRateDegrees() {
        return 0.45F;
    }

    @Override
    protected int getCruiseYawDecisionMinTicks() {
        return 90;
    }

    @Override
    protected int getCruiseYawDecisionRandomTicks() {
        return 80;
    }

    @Override
    protected float getCruisePitchTurnRateDegrees() {
        return 0.45F;
    }

    @Override
    protected int getCruisePitchDecisionMinTicks() {
        return 70;
    }

    @Override
    protected int getCruisePitchDecisionRandomTicks() {
        return 70;
    }

    @Override
    protected double getCruiseDepthRange() {
        return 4.2D;
    }

    @Override
    protected double getCruiseDepthPitchDistance() {
        return 3.2D;
    }

    @Override
    protected double getCruiseDepthEmergencyOffset() {
        return 2.4D;
    }

    @Override
    protected double getCruiseVerticalAssist() {
        return 0.008D;
    }

    @Override
    protected float getBodyTurnRateDegrees() {
        return 2.0F;
    }

    @Override
    protected float getHitboxPickInflation() {
        return 0.35F;
    }

    @Override
    protected double getHitboxVisualYOffset() {
        return -0.45D;
    }

    @Override
    protected double getHitboxPitchPivotOffsetY() {
        return 0.25D;
    }
}
