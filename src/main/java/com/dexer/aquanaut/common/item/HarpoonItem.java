package com.dexer.aquanaut.common.item;

import com.dexer.aquanaut.common.entity.HarpoonEntity;
import com.dexer.aquanaut.common.gaze.GazeCatalog;
import com.dexer.aquanaut.common.gaze.GazeHelper;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class HarpoonItem extends Item {

    public static final int THROW_THRESHOLD_TIME = 10;
    private static final int SHADOW_SHOOT_INTERVAL = Math.max(1, Math.round(THROW_THRESHOLD_TIME * 0.75F));
    public static final float SHOOT_POWER = 2.5F;

    private final float thrownDamage;

    public HarpoonItem(Properties properties, float thrownDamage) {
        super(properties);
        this.thrownDamage = thrownDamage;
    }

    public float getThrownDamage() {
        return this.thrownDamage;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }

        int shadowLevel = GazeHelper.getLevel(stack, GazeCatalog.SHADOW);
        if (shadowLevel <= 0) {
            return;
        }

        int useDuration = getUseDuration(stack, entity) - remainingUseDuration;
        if (useDuration < THROW_THRESHOLD_TIME || (useDuration - THROW_THRESHOLD_TIME) % SHADOW_SHOOT_INTERVAL != 0) {
            return;
        }

        HarpoonEntity shadow = new HarpoonEntity(level, player, stack);
        shadow.setShadow(shadowLevel);
        shadow.setPos(getThrowPosition(player));
        shadow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, SHOOT_POWER, 1.0F);
        level.addFreshEntity(shadow);
        level.playSound(null, shadow, SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.35F, 1.4F);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isTooDamagedToUse(stack)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return;
        }

        int useDuration = this.getUseDuration(stack, entity) - timeLeft;
        if (useDuration < THROW_THRESHOLD_TIME) {
            return;
        }

        if (isTooDamagedToUse(stack)) {
            return;
        }

        if (!level.isClientSide) {
            HarpoonEntity harpoon = new HarpoonEntity(level, player, stack);
            harpoon.setThrownHand(player.getUsedItemHand());
            harpoon.setPos(getThrowPosition(player));
            harpoon.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, SHOOT_POWER, 1.0F);

            if (player.hasInfiniteMaterials()) {
                harpoon.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            } else {
                stack.hurtAndBreak(1, player, player.getUsedItemHand() == InteractionHand.OFF_HAND
                        ? EquipmentSlot.OFFHAND
                        : EquipmentSlot.MAINHAND);
            }

            level.addFreshEntity(harpoon);
            level.playSound(null, harpoon, SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0F, 1.0F);

            if (!player.hasInfiniteMaterials()) {
                player.getInventory().removeItem(stack);
            }
        }

        player.awardStat(Stats.ITEM_USED.get(this));
    }

    private static boolean isTooDamagedToUse(ItemStack stack) {
        return stack.getDamageValue() >= stack.getMaxDamage() - 1;
    }

    private static Vec3 getThrowPosition(Player player) {
        InteractionHand hand = player.getUsedItemHand();
        boolean rightHand = hand == InteractionHand.MAIN_HAND
                ? player.getMainArm() == HumanoidArm.RIGHT
                : player.getMainArm() == HumanoidArm.LEFT;

        Vec3 forward = Vec3.directionFromRotation(0.0F, player.getYRot()).normalize();
        Vec3 handSide = Vec3.directionFromRotation(0.0F, player.getYRot() + 90.0F).normalize()
                .scale(rightHand ? 1.0D : -1.0D);

        return player.getEyePosition()
                .add(forward.scale(0.35D))
                .add(handSide.scale(0.35D))
                .add(0.0D, 0.45D, 0.0D);
    }
}
