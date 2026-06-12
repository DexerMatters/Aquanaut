package com.dexer.aquanaut.common.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public record ScaledDensityFunction(DensityFunction wrapped, double horizontalScale) implements DensityFunction {
    @Override
    public double compute(FunctionContext context) {
        double original = wrapped.compute(context);
        double scaled = wrapped.compute(new ScaledContext(context, horizontalScale));
        return Math.min(original, scaled);
    }

    @Override
    public void fillArray(double[] array, ContextProvider provider) {
        wrapped.fillArray(array, provider);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return new ScaledDensityFunction(wrapped.mapAll(visitor), horizontalScale);
    }

    @Override
    public double minValue() {
        return wrapped.minValue();
    }

    @Override
    public double maxValue() {
        return wrapped.maxValue();
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return KeyDispatchDataCodec.of(MapCodec.unit(this));
    }

    private record ScaledContext(FunctionContext delegate, double scale) implements FunctionContext {
        @Override
        public int blockX() {
            return (int) (delegate.blockX() / scale);
        }

        @Override
        public int blockY() {
            return delegate.blockY();
        }

        @Override
        public int blockZ() {
            return (int) (delegate.blockZ() / scale);
        }
    }
}
