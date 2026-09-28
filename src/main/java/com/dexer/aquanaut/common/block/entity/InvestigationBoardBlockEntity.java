package com.dexer.aquanaut.common.block.entity;

import com.dexer.aquanaut.core.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Block entity for one cell of the investigation board.
 *
 * <p>The board is currently a static display. Keeping an entity on every cell means the board can
 * grow into an interactive evidence surface later without changing its placement contract; only
 * the bottom-left cell is rendered, just as the dissection table renders from its master cell.
 */
public class InvestigationBoardBlockEntity extends BlockEntity implements GeoBlockEntity {
    public InvestigationBoardBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.INVESTIGATION_BOARD.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Static board for now; evidence animation can be added without changing the renderer API.
    }

    private AnimatableInstanceCache cache;

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        AnimatableInstanceCache current = this.cache;
        if (current == null) {
            current = this.cache = GeckoLibUtil.createInstanceCache(this);
        }
        return current;
    }
}
