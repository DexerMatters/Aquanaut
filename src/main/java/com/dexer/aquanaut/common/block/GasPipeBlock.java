package com.dexer.aquanaut.common.block;

import com.dexer.aquanaut.common.block.entity.GasPipeBlockEntity;
import com.dexer.aquanaut.core.BlockEntityRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class GasPipeBlock extends AbstractPipeBlock implements EntityBlock {
    public static final MapCodec<GasPipeBlock> CODEC = simpleCodec(GasPipeBlock::new);

    public GasPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<GasPipeBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GasPipeBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> blockEntityType) {
        if (level.isClientSide || blockEntityType != BlockEntityRegistry.GAS_PIPE.get()) {
            return null;
        }
        return (tickerLevel, tickerPos, tickerState, blockEntity) -> ((GasPipeBlockEntity) blockEntity).tickServer();
    }
}
