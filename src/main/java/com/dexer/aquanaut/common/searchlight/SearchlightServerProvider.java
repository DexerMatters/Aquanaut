package com.dexer.aquanaut.common.searchlight;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.item.HandheldSearchlightItem;
import com.dexer.aquanaut.common.light.DynamicLightOwner;
import com.dexer.aquanaut.common.light.LightTransition;
import com.dexer.aquanaut.common.light.ServerDynamicLightCollector;
import com.dexer.aquanaut.common.light.ServerDynamicLightProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Supplies the shared light engine with one real point light for each lit searchlight player. */
public final class SearchlightServerProvider implements ServerDynamicLightProvider {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "searchlight");

    @Override
    public void collect(ServerLevel level, ServerDynamicLightCollector collector) {
        for (Player player : level.players()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }
            InteractionHand hand = litLamp(player);
            if (hand == null) {
                continue;
            }
            BlockPos target = SearchlightTargeting.blockTarget(level, player, hand);
            if (target != null) {
                collector.submit(new DynamicLightOwner.Entity(player.getUUID()), 0, target, 15,
                        LightTransition.FOUR_TICK_HANDOFF);
            }
        }
    }

    public static InteractionHand litLamp(Player player) {
        if (isLit(player.getItemInHand(InteractionHand.MAIN_HAND))) {
            return InteractionHand.MAIN_HAND;
        }
        if (isLit(player.getItemInHand(InteractionHand.OFF_HAND))) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    public static boolean isLeftHand(Player player, InteractionHand hand) {
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND
                ? player.getMainArm()
                : player.getMainArm().getOpposite();
        return arm == HumanoidArm.LEFT;
    }

    private static boolean isLit(ItemStack stack) {
        return stack.getItem() instanceof HandheldSearchlightItem && HandheldSearchlightItem.isLit(stack);
    }
}
