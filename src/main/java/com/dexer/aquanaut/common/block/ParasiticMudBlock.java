package com.dexer.aquanaut.common.block;

import com.dexer.aquanaut.common.entity.MudSilverfishEntity;
import com.dexer.aquanaut.common.mud.MudZoneConfig;
import com.dexer.aquanaut.core.EntityRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Mud that releases hostile silverfish when mined. */
public final class ParasiticMudBlock extends Block {
    public static final MapCodec<ParasiticMudBlock> CODEC = simpleCodec(ParasiticMudBlock::new);

    public ParasiticMudBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<ParasiticMudBlock> codec() {
        return CODEC;
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity,
            ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (level.isClientSide) {
            return;
        }
        int count = MudZoneConfig.PARASITIC_MIN_SPAWN
                + level.random.nextInt(MudZoneConfig.PARASITIC_MAX_SPAWN - MudZoneConfig.PARASITIC_MIN_SPAWN + 1);
        for (int i = 0; i < count; i++) {
            MudSilverfishEntity entity = new MudSilverfishEntity(EntityRegistry.MUD_SILVERFISH.get(), level);
            entity.moveTo(pos.getX() + 0.5D, pos.getY() + 0.15D, pos.getZ() + 0.5D,
                    level.random.nextFloat() * 360.0F, 0.0F);
            level.addFreshEntity(entity);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < MudZoneConfig.PARASITIC_BUBBLE_CHANCE) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.BUBBLE,
                    pos.getX() + random.nextDouble(), pos.getY() + 1.01D,
                    pos.getZ() + random.nextDouble(), 0.0D, 0.02D, 0.0D);
        }
    }
}
