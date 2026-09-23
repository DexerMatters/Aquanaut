package com.dexer.aquanaut.common.block;

import com.dexer.aquanaut.core.ParticleRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * The block face of a sulfuric acid pool. Exhales pale acid mist (硫酸雾) over the
 * surface — the signature particles of the caldera's 硫酸湖.
 */
public final class SulfuricAcidBlock extends LiquidBlock {
    public SulfuricAcidBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) {
            return;
        }
        BlockPos above = pos.above();
        if (!level.getBlockState(above).isAir() && level.getFluidState(above).isEmpty()) {
            return;
        }
        // Heavy vapor: it billows low over the pool and drifts before dissolving.
        level.addParticle(ParticleRegistry.SULFURIC_ACID_MIST.get(),
                pos.getX() + 0.2D + random.nextDouble() * 0.6D,
                pos.getY() + 1.02D,
                pos.getZ() + 0.2D + random.nextDouble() * 0.6D,
                (random.nextDouble() - 0.5D) * 0.004D,
                0.004D + random.nextDouble() * 0.008D,
                (random.nextDouble() - 0.5D) * 0.004D);
    }
}
