package com.dexer.aquanaut.common.block;

import com.dexer.aquanaut.common.mud.MudZoneConfig;
import com.dexer.aquanaut.core.BlockRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Mud enriched with organic matter that makes anything stepping on it hungry. */
public final class NutrientRichMudBlock extends Block {
    public static final MapCodec<NutrientRichMudBlock> CODEC = simpleCodec(NutrientRichMudBlock::new);

    public NutrientRichMudBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<NutrientRichMudBlock> codec() {
        return CODEC;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(
                    MobEffects.HUNGER,
                    MudZoneConfig.HUNGER_DURATION_TICKS,
                    MudZoneConfig.HUNGER_AMPLIFIER,
                    false,
                    true));
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() >= MudZoneConfig.SEA_MOSS_GROW_CHANCE) {
            return;
        }
        BlockPos above = pos.above();
        if (!level.getFluidState(above).is(FluidTags.WATER)
                || !level.getBlockState(above).canBeReplaced()) {
            return;
        }
        // The moss default state is already waterlogged, which is what a submerged flat wants.
        level.setBlock(above, BlockRegistry.SEA_MOSS.get().defaultBlockState(), 3);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() >= MudZoneConfig.NUTRIENT_BUBBLE_CHANCE) {
            return;
        }

        double x = pos.getX() + 0.2D + random.nextDouble() * 0.6D;
        double y = pos.getY() + 1.02D;
        double z = pos.getZ() + 0.2D + random.nextDouble() * 0.6D;
        level.addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0.0D, 0.025D, 0.0D);
        if (random.nextBoolean()) {
            level.addParticle(ParticleTypes.BUBBLE, x, y, z, 0.0D, 0.02D, 0.0D);
        }
    }
}
