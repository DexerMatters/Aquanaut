package com.dexer.aquanaut.common.fluid;

import com.dexer.aquanaut.core.BlockRegistry;
import com.dexer.aquanaut.core.FluidRegistry;
import com.dexer.aquanaut.core.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * Sulfuric acid (硫酸): heavy, slow, sour leachate pooled in the calderas of Brimstone
 * Caldera. Denser than water and far more viscous — it creeps instead of streaming and
 * never regenerates a source.
 */
public abstract class SulfuricAcidFluid extends FlowingFluid {
    /** Shared type descriptor; registered once through {@link FluidRegistry}. */
    public static final FluidType TYPE = new FluidType(FluidType.Properties.create()
            .descriptionId("fluid.aquanaut.sulfuric_acid")
            .density(1800)
            .temperature(340)
            .viscosity(3200)
            .motionScale(0.007D)
            .canPushEntity(false)
            .canSwim(false)
            .canDrown(false)
            .canExtinguish(false)
            .canConvertToSource(false)
            .supportsBoating(false)
            .fallDistanceModifier(0.4F));

    @Override
    public FluidType getFluidType() {
        return TYPE;
    }

    @Override
    public Fluid getFlowing() {
        return FluidRegistry.FLOWING_SULFURIC_ACID.get();
    }

    @Override
    public Fluid getSource() {
        return FluidRegistry.SULFURIC_ACID.get();
    }

    @Override
    public Item getBucket() {
        return ItemRegistry.SULFURIC_ACID_BUCKET.get();
    }

    @Override
    public int getTickDelay(LevelReader level) {
        // Viscous: the pool takes its time before it even thinks of creeping.
        return 32;
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    public boolean canConvertToSource(FluidState state, Level level, BlockPos pos) {
        return false;
    }

    /**
     * Still abstract in {@link FlowingFluid}, so it must be implemented even though the
     * deprecated signature is never called now that the fluid-state-aware form above answers
     * instead. Kept in agreement with it.
     */
    @Override
    @SuppressWarnings("deprecation")
    protected boolean canConvertToSource(Level level) {
        return false;
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid,
            Direction direction) {
        // Heavy leachate: it sinks into anything but water, which it cannot displace.
        return direction == Direction.DOWN && !state.isEmpty();
    }

    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
    }

    @Override
    protected int getSlopeFindDistance(LevelReader level) {
        return 2;
    }

    @Override
    protected int getDropOff(LevelReader level) {
        // Higher than water's 1: thick liquid piles up instead of spreading thin.
        return 3;
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return BlockRegistry.SULFURIC_ACID.get().defaultBlockState()
                .setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    /** A stable pool: full source blocks only. */
    public static final class Source extends SulfuricAcidFluid {
        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    /** The creeping edge of a pool. */
    public static final class Flowing extends SulfuricAcidFluid {
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            // FlowingFluid already contributes FALLING; only the level state is ours to add.
            super.createFluidStateDefinition(builder);
            builder.add(FlowingFluid.LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(FlowingFluid.LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}
