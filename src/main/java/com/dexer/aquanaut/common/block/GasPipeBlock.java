package com.dexer.aquanaut.common.block;

import com.mojang.serialization.MapCodec;

public final class GasPipeBlock extends AbstractPipeBlock {
    public static final MapCodec<GasPipeBlock> CODEC = simpleCodec(GasPipeBlock::new);

    public GasPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<GasPipeBlock> codec() {
        return CODEC;
    }
}
