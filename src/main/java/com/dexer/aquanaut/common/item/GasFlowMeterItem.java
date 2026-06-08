package com.dexer.aquanaut.common.item;

import java.util.function.Consumer;

import com.dexer.aquanaut.client.model.GasFlowMeterTargetingHelper;
import com.dexer.aquanaut.client.renderer.item.GasFlowMeterItemRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class GasFlowMeterItem extends Item {
    private static final int MAX_USE_DURATION = 72000;

    public GasFlowMeterItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return MAX_USE_DURATION;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS, 0.8F, 1.0F);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.SPYGLASS_STOP_USING, SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm,
                    ItemStack itemInHand, float partialTick, float equipProcess, float swingProcess) {
                if (!isUsingInArm(player, arm, itemInHand)) {
                    return false;
                }

                GasFlowMeterTargetingHelper.Transform transform =
                        GasFlowMeterTargetingHelper.targeting(arm == HumanoidArm.RIGHT);
                poseStack.translate(transform.translateX(), transform.translateY(), transform.translateZ());
                poseStack.mulPose(Axis.YP.rotationDegrees(transform.yawDegrees()));
                poseStack.mulPose(Axis.XP.rotationDegrees(transform.pitchDegrees()));
                poseStack.mulPose(Axis.ZP.rotationDegrees(transform.rollDegrees()));
                poseStack.scale(transform.scale(), transform.scale(), transform.scale());
                return true;
            }

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return GasFlowMeterItemRenderer.getInstance();
            }
        });
    }

    private static boolean isUsingInArm(LocalPlayer player, HumanoidArm arm, ItemStack itemStack) {
        if (!player.isUsingItem() || !ItemStack.isSameItemSameComponents(player.getUseItem(), itemStack)) {
            return false;
        }
        return switch (player.getUsedItemHand()) {
            case MAIN_HAND -> arm == player.getMainArm();
            case OFF_HAND -> arm != player.getMainArm();
        };
    }
}
