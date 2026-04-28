package com.dexer.aquanaut.common.item;

import com.dexer.aquanaut.common.entity.AirBubbleEntity;
import com.dexer.aquanaut.core.EntityRegistry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BubbleGunItem extends Item {

    private static final int MAX_USE_DURATION = 72000;
    /** Spawn a bubble every N ticks while holding use. */
    private static final int BUBBLE_SPAWN_INTERVAL = 4;
    private static final double MIN_SPEED = 0.4;
    private static final double MAX_SPEED = 1.0;
    private static final double SPREAD = 0.15;
    /**
     * Ticks a spawned bubble is protected from merging/bursting from the player.
     */
    private static final int PRESERVING_TIME = 5;

    public BubbleGunItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return MAX_USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide)
            return;
        if (!(livingEntity instanceof Player player))
            return;
        if (stack.getDamageValue() >= stack.getMaxDamage())
            return;

        int elapsed = MAX_USE_DURATION - remainingUseDuration;
        if (elapsed % BUBBLE_SPAWN_INTERVAL == 0) {
            spawnBubble(player);
            stack.setDamageValue(Math.min(stack.getDamageValue() + 1, stack.getMaxDamage()));
        }
    }

    private void spawnBubble(Player player) {
        Level level = player.level();
        Vec3 lookVec = player.getLookAngle();

        // Spawn at chest height (~0.25 below eyes) so it looks like it's coming
        // from a handheld gun rather than the player's face
        Vec3 spawnPos = player.position()
                .add(0, player.getEyeHeight() - 0.35, 0)
                .add(lookVec.scale(0.8));

        AirBubbleEntity bubble = new AirBubbleEntity(EntityRegistry.AIR_BUBBLE.get(), level);
        bubble.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        bubble.setSize(1);
        bubble.setPreservingTime(PRESERVING_TIME);

        // Random velocity in the look direction with spread
        double speed = MIN_SPEED + level.random.nextDouble() * (MAX_SPEED - MIN_SPEED);
        Vec3 velocity = new Vec3(
                lookVec.x + (level.random.nextDouble() - 0.5) * SPREAD,
                lookVec.y + (level.random.nextDouble() - 0.5) * SPREAD,
                lookVec.z + (level.random.nextDouble() - 0.5) * SPREAD).normalize().scale(speed);

        bubble.setDeltaMovement(velocity);
        level.addFreshEntity(bubble);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.PLAYERS,
                0.3f, 1.2f + level.random.nextFloat() * 0.6f);
    }
}
