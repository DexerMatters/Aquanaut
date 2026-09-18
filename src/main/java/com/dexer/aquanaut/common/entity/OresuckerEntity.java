package com.dexer.aquanaut.common.entity;

import com.dexer.aquanaut.Config;
import com.dexer.aquanaut.common.ai.FishAttackMode;
import com.dexer.aquanaut.common.ai.FishResponseMode;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Oresucker — a bottom-grazing mineral feeder.
 *
 * <p>
 * The snout is worn smooth from grinding ore veins: every few seconds it shaves an exposed ore
 * block down to its host stone and keeps what it swallowed. A full specimen carries up to eight
 * units, which is what a hunter actually wants from it — the fish itself never bites.
 */
public class OresuckerEntity extends BaseFishEntity implements GeoEntity {
    private static final RawAnimation SWIM_ANIMATION = RawAnimation.begin().thenLoop("swim");

    private static final int GRAZE_INTERVAL_TICKS = 100;
    private static final int STORAGE_CAPACITY = 8;
    private static final int GRAZE_RADIUS = 2;

    private static final Map<Block, ItemStack> ORE_YIELD = new HashMap<>();
    private static final Map<Block, Block> ORE_HOST = new HashMap<>();

    static {
        register(Blocks.COAL_ORE, Blocks.STONE, new ItemStack(Items.COAL));
        register(Blocks.DEEPSLATE_COAL_ORE, Blocks.DEEPSLATE, new ItemStack(Items.COAL));
        register(Blocks.IRON_ORE, Blocks.STONE, new ItemStack(Items.RAW_IRON));
        register(Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE, new ItemStack(Items.RAW_IRON));
        register(Blocks.COPPER_ORE, Blocks.STONE, new ItemStack(Items.RAW_COPPER));
        register(Blocks.DEEPSLATE_COPPER_ORE, Blocks.DEEPSLATE, new ItemStack(Items.RAW_COPPER));
        register(Blocks.GOLD_ORE, Blocks.STONE, new ItemStack(Items.RAW_GOLD));
        register(Blocks.DEEPSLATE_GOLD_ORE, Blocks.DEEPSLATE, new ItemStack(Items.RAW_GOLD));
        register(Blocks.REDSTONE_ORE, Blocks.STONE, new ItemStack(Items.REDSTONE));
        register(Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.DEEPSLATE, new ItemStack(Items.REDSTONE));
        register(Blocks.LAPIS_ORE, Blocks.STONE, new ItemStack(Items.LAPIS_LAZULI));
        register(Blocks.DEEPSLATE_LAPIS_ORE, Blocks.DEEPSLATE, new ItemStack(Items.LAPIS_LAZULI));
        register(Blocks.DIAMOND_ORE, Blocks.STONE, new ItemStack(Items.DIAMOND));
        register(Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DEEPSLATE, new ItemStack(Items.DIAMOND));
        register(Blocks.EMERALD_ORE, Blocks.STONE, new ItemStack(Items.EMERALD));
        register(Blocks.DEEPSLATE_EMERALD_ORE, Blocks.DEEPSLATE, new ItemStack(Items.EMERALD));
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private ItemStack storedOre = ItemStack.EMPTY;
    private int storedCount;

    public OresuckerEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    private static void register(Block ore, Block host, ItemStack yield) {
        ORE_YIELD.put(ore, yield);
        ORE_HOST.put(ore, host);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 2, state -> {
            state.getController().setAnimationSpeed(animSpeed(0.45, 0.95, 0.9, 1.6));
            return state.setAndContinue(SWIM_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static AttributeSupplier createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.18D)
                .build();
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
        return 0.004D;
    }

    @Override
    protected double getCruiseMaxSpeed() {
        return 0.045D;
    }

    @Override
    protected double getEscapeAcceleration() {
        return 0.020D;
    }

    @Override
    protected double getEscapeMaxSpeed() {
        return 0.22D;
    }

    @Override
    protected double getWaterDrag() {
        return 0.88D;
    }

    @Override
    protected double getPlayerDetectionRange() {
        return 6.0D;
    }

    @Override
    protected double getCruiseDepthRange() {
        return 1.5D;
    }

    @Override
    protected float getBodyTurnRateDegrees() {
        return 4.0F;
    }

    @Override
    protected float getMaxTiltDegrees() {
        return 16.0F;
    }

    @Override
    protected float getHitboxPickInflation() {
        return 0.16F;
    }

    @Override
    protected double getHitboxVisualYOffset() {
        return 0.13D;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.isEffectiveAi() || this.level().isClientSide) {
            return;
        }

        if (this.tickCount % GRAZE_INTERVAL_TICKS == 0) {
            this.tryGraze();
        }
    }

    public int storedCount() {
        return this.storedCount;
    }

    public ItemStack storedOre() {
        return this.storedOre.copy();
    }

    private void tryGraze() {
        if (!Config.ORESUCKER_GRAZING.get() || this.storedCount >= STORAGE_CAPACITY) {
            return;
        }

        BlockPos origin = this.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-GRAZE_RADIUS, -1, -GRAZE_RADIUS),
                origin.offset(GRAZE_RADIUS, 1, GRAZE_RADIUS))) {
            BlockState state = this.level().getBlockState(pos);
            ItemStack yield = ORE_YIELD.get(state.getBlock());
            if (yield == null) {
                continue;
            }

            this.level().setBlockAndUpdate(pos, ORE_HOST.get(state.getBlock()).defaultBlockState());
            this.storedOre = yield.copy();
            this.storedCount++;

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5D, pos.getY() + 0.5D,
                        pos.getZ() + 0.5D, 8, 0.25D, 0.25D, 0.25D, 0.01D);
                serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.3D, this.getZ(),
                        4, 0.1D, 0.05D, 0.1D, 0.005D);
            }
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GRINDSTONE_USE, SoundSource.HOSTILE, 0.6F, 1.4F);

            if (this.storedCount >= STORAGE_CAPACITY) {
                return;
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide && this.storedCount > 0 && !this.storedOre.isEmpty()) {
            ItemStack drop = this.storedOre.copyWithCount(this.storedCount);
            this.level().addFreshEntity(new ItemEntity(this.level(), this.getX(),
                    this.getY() + this.getBbHeight() * 0.5D, this.getZ(), drop));
            this.storedCount = 0;
            this.storedOre = ItemStack.EMPTY;
        }
        super.die(source);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("StoredCount", this.storedCount);
        if (!this.storedOre.isEmpty()) {
            tag.put("StoredOre", this.storedOre.save(this.registryAccess()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.storedCount = tag.getInt("StoredCount");
        this.storedOre = tag.contains("StoredOre")
                ? ItemStack.parseOptional(this.registryAccess(), tag.getCompound("StoredOre"))
                : ItemStack.EMPTY;
    }
}
