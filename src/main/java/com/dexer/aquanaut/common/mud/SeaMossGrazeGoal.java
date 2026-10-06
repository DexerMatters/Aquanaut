package com.dexer.aquanaut.common.mud;

import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.EnumSet;

/**
 * Lets a hermit crab graze: it wanders over to a nearby sea moss mat, tears it off the sediment
 * and eats it. The scan is throttled and small, so a crab on a bare flat costs almost nothing.
 */
public final class SeaMossGrazeGoal extends Goal {
    private static final int SCAN_INTERVAL_TICKS = 60;
    private static final int EAT_TICKS = 12;
    private static final int SEARCH_RADIUS = 6;
    private static final double REACH_SQR = 2.6D;

    private final Mob mob;
    private BlockPos target;
    private int scanCooldown;
    private int chewTicks;

    public SeaMossGrazeGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (--scanCooldown > 0) {
            return false;
        }
        scanCooldown = SCAN_INTERVAL_TICKS;
        target = findMoss();
        return target != null;
    }

    @Override
    public void start() {
        chewTicks = 0;
        moveToTarget();
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && chewTicks <= EAT_TICKS;
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }
        mob.getLookControl().setLookAt(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);
        if (mob.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D) > REACH_SQR) {
            if (mob.getNavigation().isDone()) {
                moveToTarget();
            }
            return;
        }
        chewTicks++;
        if (chewTicks >= EAT_TICKS) {
            eat();
        }
    }

    @Override
    public void stop() {
        target = null;
        mob.getNavigation().stop();
    }

    private void moveToTarget() {
        if (target != null) {
            mob.getNavigation().moveTo(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, 1.0D);
        }
    }

    private void eat() {
        Level level = mob.level();
        BlockState state = level.getBlockState(target);
        if (!state.is(BlockRegistry.SEA_MOSS.get())) {
            return;
        }
        boolean waterlogged = state.getValue(BlockStateProperties.WATERLOGGED);
        level.setBlockAndUpdate(target, waterlogged ? Blocks.WATER.defaultBlockState()
                : Blocks.AIR.defaultBlockState());
        level.playSound(null, target, SoundEvents.GRASS_BREAK, SoundSource.NEUTRAL, 0.5F,
                0.7F + mob.getRandom().nextFloat() * 0.4F);
        target = null;
    }

    private BlockPos findMoss() {
        BlockPos origin = mob.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-SEARCH_RADIUS, -2, -SEARCH_RADIUS),
                origin.offset(SEARCH_RADIUS, 2, SEARCH_RADIUS))) {
            if (!mob.level().getBlockState(pos).is(BlockRegistry.SEA_MOSS.get())) {
                continue;
            }
            double distance = mob.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos.immutable();
            }
        }
        return best;
    }
}
