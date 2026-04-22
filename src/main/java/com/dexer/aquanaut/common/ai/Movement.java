package com.dexer.aquanaut.common.ai;

import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public abstract class Movement {
    public abstract void apply(MovementContext context);

    protected float rotateTowards(float current, float target, float maxChange) {
        float delta = Mth.wrapDegrees(target - current);
        return current + Mth.clamp(delta, -maxChange, maxChange);
    }

    protected float yawFromDirection(Vec3 direction) {
        return (float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0F;
    }

    protected float pitchFromDirection(Vec3 direction) {
        return (float) (-(Mth.atan2(direction.y, direction.horizontalDistance()) * Mth.RAD_TO_DEG));
    }

    protected boolean hasWaterAbove(MovementContext context) {
        return context.fish().level().getFluidState(context.fish().blockPosition().above()).is(FluidTags.WATER)
                && context.fish().level().getFluidState(context.fish().blockPosition().above(2)).is(FluidTags.WATER);
    }

    protected boolean hasWaterBelow(MovementContext context) {
        return context.fish().level().getFluidState(context.fish().blockPosition().below()).is(FluidTags.WATER)
                && context.fish().level().getFluidState(context.fish().blockPosition().below(2)).is(FluidTags.WATER);
    }
}
