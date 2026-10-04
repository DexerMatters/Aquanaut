package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.SieveGeoModel;
import com.dexer.aquanaut.common.block.entity.SieveBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * Draws the static sifting sieve.
 *
 * <p>
 * The model is one block wide and seven sixteenths of a block tall, authored around the block's
 * centre with its floor at the block's bottom - the exact layout GeckoLib's block renderer expects,
 * so this needs no translation of its own, unlike the multi-cell investigation board.
 */
public class SieveBlockEntityRenderer extends GeoBlockRenderer<SieveBlockEntity> {
    public SieveBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(new SieveGeoModel());
    }
}
