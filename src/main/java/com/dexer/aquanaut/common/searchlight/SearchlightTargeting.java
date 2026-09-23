package com.dexer.aquanaut.common.searchlight;

import com.dexer.aquanaut.common.item.SearchlightGeometry;
import com.dexer.aquanaut.core.BlockRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.minecraft.util.Mth;

/** Common lens, raycast, endpoint and server-cell targeting used by both providers. */
public final class SearchlightTargeting {
    private static final int PLACEMENT_SAMPLES = (int) Math.ceil(
            SearchlightGeometry.MAX_PLACEMENT_BACKOFF / SearchlightGeometry.PLACEMENT_STEP);

    private SearchlightTargeting() {
    }

    public static Vec3 continuousTarget(Level level, Player player, InteractionHand hand, float partialTick) {
        return continuousTarget(level, player, hand, player.getViewVector(partialTick), partialTick);
    }

    public static Vec3 continuousTarget(Level level, Player player, InteractionHand hand, Vec3 forward,
            float partialTick) {
        if (forward.lengthSqr() < 1.0E-8D) {
            return null;
        }
        Vec3 lens = SearchlightGeometry.lens(player.getEyePosition(partialTick), forward,
                Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot),
                SearchlightServerProvider.isLeftHand(player, hand));
        double range = SearchlightGeometry.range(player.isEyeInFluidType(NeoForgeMod.WATER_TYPE.value()));
        return continuousTarget(level, lens, forward, range, player);
    }

    /**
     * Computes a smooth endpoint for any forward-facing dynamic light.  The ray deliberately ignores
     * fluids: underwater light travels through water, while a solid surface still occludes it.
     */
    public static Vec3 continuousTarget(Level level, Vec3 lens, Vec3 forward, double range,
            Entity ignored) {
        if (forward.lengthSqr() < 1.0E-8D || !(range > 0.0D)) {
            return null;
        }
        Vec3 end = SearchlightGeometry.targetPoint(lens, forward, range);
        BlockHitResult hit = level.clip(new ClipContext(lens, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, ignored));
        double obstructionDistance = hit.getType() == HitResult.Type.MISS
                ? Double.POSITIVE_INFINITY
                : hit.getLocation().distanceTo(lens);
        return SearchlightGeometry.targetPoint(lens, forward,
                SearchlightGeometry.targetDistance(range, obstructionDistance));
    }

    public static BlockPos blockTarget(Level level, Player player, InteractionHand hand) {
        Vec3 forward = player.getLookAngle();
        if (forward.lengthSqr() < 1.0E-8D) {
            return null;
        }
        boolean leftHand = SearchlightServerProvider.isLeftHand(player, hand);
        Vec3 lens = SearchlightGeometry.lens(player.getEyePosition(), forward, player.yBodyRot, leftHand);
        double range = SearchlightGeometry.range(player.isEyeInFluidType(NeoForgeMod.WATER_TYPE.value()));
        return blockTarget(level, lens, forward, range, player);
    }

    /** Finds a loaded, replaceable source cell along a generic light's ray. */
    public static BlockPos blockTarget(Level level, Vec3 lens, Vec3 forward, double range,
            Entity ignored) {
        if (forward.lengthSqr() < 1.0E-8D || !(range > 0.0D)) {
            return null;
        }
        Vec3 end = SearchlightGeometry.targetPoint(lens, forward, range);
        BlockHitResult hit = level.clip(new ClipContext(lens, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, ignored));
        double obstructionDistance = hit.getType() == HitResult.Type.MISS
                ? Double.POSITIVE_INFINITY
                : hit.getLocation().distanceTo(lens);
        double terminalDistance = SearchlightGeometry.targetDistance(range, obstructionDistance);
        Vec3 terminal = SearchlightGeometry.targetPoint(lens, forward, terminalDistance);
        Vec3 axis = forward.normalize();

        for (int sample = 0; sample <= PLACEMENT_SAMPLES; sample++) {
            double candidateDistance = SearchlightGeometry.placementDistance(terminalDistance, sample);
            Vec3 candidatePoint = terminal.subtract(axis.scale(terminalDistance - candidateDistance));
            BlockPos candidate = BlockPos.containing(candidatePoint);
            if (!level.isInWorldBounds(candidate) || !level.isAreaLoaded(candidate, 0)) {
                continue;
            }
            BlockState state = level.getBlockState(candidate);
            if (state.is(BlockRegistry.DYNAMIC_LIGHT.get()) || state.isAir() || state.is(Blocks.WATER)) {
                return candidate;
            }
        }
        return null;
    }
}
