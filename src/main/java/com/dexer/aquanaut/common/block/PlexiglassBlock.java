package com.dexer.aquanaut.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * A thin transparent panel block. Cannot be collected like a fishing net —
 * functions purely as a structural glass pane that forms enclosures.
 * <p>
 * Inherits all panel connection, enclosure detection and auto-break logic
 * from {@link AbstractPanelBlock}. The {@code useWithoutItem} override simply
 * passes through (no collection).
 */
public final class PlexiglassBlock extends AbstractPanelBlock {

    public static final MapCodec<PlexiglassBlock> CODEC =
            simpleCodec(PlexiglassBlock::new);

    public PlexiglassBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<PlexiglassBlock> codec() {
        return CODEC;
    }
}
