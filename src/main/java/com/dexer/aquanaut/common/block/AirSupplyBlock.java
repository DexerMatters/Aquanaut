package com.dexer.aquanaut.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockState;

public final class AirSupplyBlock extends AbstractBottomAirConsumerBlock {
    public static final MapCodec<AirSupplyBlock> CODEC = simpleCodec(AirSupplyBlock::new);

    public AirSupplyBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<AirSupplyBlock> codec() {
        return CODEC;
    }

    @Override
    protected int getDemandStrength(BlockState state) {
        return 12;
    }
}
