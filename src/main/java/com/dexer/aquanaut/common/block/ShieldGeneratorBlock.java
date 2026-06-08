package com.dexer.aquanaut.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class ShieldGeneratorBlock extends AbstractBottomAirConsumerBlock {
    public static final MapCodec<ShieldGeneratorBlock> CODEC = simpleCodec(ShieldGeneratorBlock::new);

    public ShieldGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected int getDemandStrength(BlockState state) {
        return 24;
    }
}
