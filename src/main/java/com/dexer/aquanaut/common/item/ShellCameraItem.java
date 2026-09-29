package com.dexer.aquanaut.common.item;

import java.util.List;

import com.dexer.aquanaut.client.camera.CameraClientEvents;
import com.dexer.aquanaut.core.ItemRegistry;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** A hold-to-frame camera: releasing use asks the client to capture the framed world view. */
public final class ShellCameraItem extends Item {
    /** Wind-on time after a successful exposure, shared by the item and its slot sweep. */
    public static final int SHUTTER_COOLDOWN_TICKS = 12;

    private static final int MAX_USE_DURATION = 72_000;
    private static final int CAPTURE_AUTHORIZATION_TICKS = 10;
    private static final String CAPTURE_RELEASE_KEY = "CaptureReleasedAt";
    private static final String SHUTTER_READY_KEY = "ShutterReadyAt";

    public ShellCameraItem(Properties properties) {
        super(properties.stacksTo(1));
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
        if (isCoolingDown(stack, level.getGameTime())) {
            return InteractionResultHolder.fail(stack);
        }
        if (!hasFilm(player)) {
            showNoFilm(level, player);
            return InteractionResultHolder.fail(stack);
        }
        beginUse(level, player, hand, stack);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (isCoolingDown(context.getItemInHand(), context.getLevel().getGameTime())) {
            return InteractionResult.FAIL;
        }
        if (!hasFilm(player)) {
            showNoFilm(context.getLevel(), player);
            return InteractionResult.FAIL;
        }
        beginUse(context.getLevel(), player, context.getHand(), context.getItemInHand());
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.aquanaut.shell_camera.hold")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.aquanaut.shell_camera.film")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.aquanaut.shell_camera.release")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide) {
            CameraClientEvents.requestCapture(entity.getUsedItemHand());
        } else {
            CustomData.update(DataComponents.CUSTOM_DATA, stack,
                    tag -> tag.putLong(CAPTURE_RELEASE_KEY, level.getGameTime()));
        }
    }

    /**
     * True while this particular camera is still winding on.
     *
     * <p>
     * Vanilla's {@code ItemCooldowns} is keyed by {@code Item}, so using it would sweep every shell
     * camera the player owns. The wind-on lives in the stack's own data instead, which keeps one
     * camera's shutter from blocking another's.
     */
    public static boolean isCoolingDown(ItemStack stack, long gameTime) {
        CompoundTag data = tagOf(stack);
        return data.contains(SHUTTER_READY_KEY) && gameTime < data.getLong(SHUTTER_READY_KEY);
    }

    /**
     * Remaining wind-on as the 0..1 fraction vanilla's slot sweep expects: 1 just after firing,
     * 0 once the camera is ready again.
     */
    public static float cooldownFraction(ItemStack stack, long gameTime, float partialTick) {
        CompoundTag data = tagOf(stack);
        if (!data.contains(SHUTTER_READY_KEY)) {
            return 0.0F;
        }
        float remaining = data.getLong(SHUTTER_READY_KEY) - (gameTime + partialTick);
        if (remaining <= 0.0F) {
            return 0.0F;
        }
        return Math.min(1.0F, remaining / SHUTTER_COOLDOWN_TICKS);
    }

    /** Read-only view of the stack's data; callers must not mutate the returned tag. */
    @SuppressWarnings("deprecation")
    private static CompoundTag tagOf(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
    }

    /** Starts the wind-on for the camera that actually fired. */
    public static void startCooldown(ItemStack stack, long gameTime) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putLong(SHUTTER_READY_KEY, gameTime + SHUTTER_COOLDOWN_TICKS));
    }

    private static void beginUse(Level level, Player player, InteractionHand hand, ItemStack stack) {
        if (!level.isClientSide) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(CAPTURE_RELEASE_KEY));
        }
        player.startUsingItem(hand);
    }

    public static boolean hasFilm(Player player) {
        return filmCount(player) != 0;
    }

    /** Returns -1 for an unlimited creative supply. */
    public static int filmCount(Player player) {
        if (player.getAbilities().instabuild) {
            return -1;
        }
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ItemRegistry.PHOTOSENSITIVE_FILM.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /** Removes one prepared sheet only after a server-validated image arrives. */
    public static boolean consumeFilm(Player player) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack film = player.getInventory().getItem(slot);
            if (film.is(ItemRegistry.PHOTOSENSITIVE_FILM.get())) {
                film.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static void showNoFilm(Level level, Player player) {
        if (level.isClientSide) {
            player.displayClientMessage(Component.translatable("message.aquanaut.camera.no_film"), true);
        }
    }

    /** Consumes the one-shot server authorization created by a genuine use-key release. */
    public static boolean consumeCaptureAuthorization(ItemStack stack, long gameTime) {
        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        boolean authorized = data.contains(CAPTURE_RELEASE_KEY);
        long releasedAt = data.getLong(CAPTURE_RELEASE_KEY);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(CAPTURE_RELEASE_KEY));
        long age = gameTime - releasedAt;
        return authorized && age >= 0L && age <= CAPTURE_AUTHORIZATION_TICKS;
    }
}
