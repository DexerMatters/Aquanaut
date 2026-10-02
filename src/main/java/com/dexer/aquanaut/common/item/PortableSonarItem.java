package com.dexer.aquanaut.common.item;

import java.util.List;

import com.dexer.aquanaut.common.sonar.SonarPulse;
import com.dexer.aquanaut.common.sonar.SonarReturn;
import com.dexer.aquanaut.common.sonar.SonarScan;
import com.dexer.aquanaut.core.SoundRegistry;
import com.dexer.aquanaut.network.SonarPingPayload;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * The portable sonar: a hand-held sounder that reads the water around the diver with a pulse.
 *
 * <p>
 * A right-click fires. The instrument rings, a wavefront leaves the transducer and runs out twenty
 * blocks in about a second and a third, and everything the pulse touches throws a copy of it back:
 * bodies, crystal, and — the reason to carry one — the hollows behind the rock the diver is looking
 * at. What comes back is the server's reading and nobody else's, so two divers watching the same
 * ping are never told two different things.
 *
 * <p>
 * The reading itself lives in {@link SonarScan}, the shape of a ping in {@link SonarPulse}, and the
 * picture in the client's own package. This class is only the trigger: it owns the cool-down and the
 * powered flag, and it says nothing at all about what came back. That is the instrument's whole
 * character — it reports water that answered, not what answered — and it is why a ping is read on
 * the server and handed over as bearings rather than as a list of what is down there.
 *
 * <p>
 * The ranging flag still lives on the stack rather than in the hand, so the lit display survives a
 * drop, a logout and a hand-over, and every other player can see whose sonar is talking. It is now
 * the instrument's own clock that clears it: the display stays lit for exactly as long as the
 * picture lasts, and then goes dark on its own.
 */
public class PortableSonarItem extends Item {

    private static final String RANGING_KEY = "Ranging";
    private static final String PING_KEY = "Ping";

    public PortableSonarItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** Whether the sonar is powered up and ranging. */
    public static boolean isRanging(ItemStack stack) {
        return tagOf(stack).getBoolean(RANGING_KEY);
    }

    /** Powers the sonar up or down. */
    public static void setRanging(ItemStack stack, boolean ranging) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(RANGING_KEY, ranging));
    }

    /** The game time the last pulse was fired at, or zero when the instrument has never spoken. */
    public static long pingTime(ItemStack stack) {
        return tagOf(stack).getLong(PING_KEY);
    }

    private static void setPingTime(ItemStack stack, long time) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong(PING_KEY, time));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (isRanging(stack) && player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                powerDown(level, player, stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        // The instrument charges between pulses and says nothing at all while it does. Refusing the
        // use rather than quietly recharging is what stops a held button from pinging on a timer.
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            fire(level, player, stack);
        }

        player.getCooldowns().addCooldown(this, SonarPulse.COOLDOWN_TICKS);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Fires one pulse: the ring, the reading, and the message the scope has no room for. */
    private static void fire(Level level, Player player, ItemStack stack) {
        setRanging(stack, true);
        setPingTime(stack, level.getGameTime());

        // A sonar pings: a hard, bright ring that carries underwater where a click would not. It is
        // broadcast, because a pulse is loud and a buddy should hear the instrument that fired it.
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundRegistry.SONAR_PING.get(),
                SoundSource.PLAYERS, 0.85F, 1.0F);

        if (level instanceof ServerLevel server) {
            List<SonarReturn> contacts = SonarScan.scan(server, player);
            SonarPingPayload.broadcast(server, player.getUUID(), player.getEyePosition(), contacts);
        }
    }

    private static void powerDown(Level level, Player player, ItemStack stack) {
        setRanging(stack, false);
        setPingTime(stack, 0L);
        // Powering down is the only quiet part of the instrument.
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LEVER_CLICK,
                SoundSource.PLAYERS, 0.4F, 0.75F);
    }

    /**
     * The display goes dark on its own once the picture on the scope has finished. Driven from the
     * tick rather than from a scheduled task so that nothing has to be remembered about a player who
     * logs out or a stack that changes hands mid-ping.
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !isRanging(stack)) {
            return;
        }

        long fired = pingTime(stack);

        if (fired == 0L || level.getGameTime() - fired >= SonarPulse.DISPLAY_TICKS) {
            setRanging(stack, false);
            setPingTime(stack, 0L);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        boolean ranging = isRanging(stack);
        tooltip.add(Component.translatable(ranging
                ? "tooltip.aquanaut.portable_sonar.ranging"
                : "tooltip.aquanaut.portable_sonar.idle")
                .withStyle(ranging ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.aquanaut.portable_sonar.hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /**
     * The stack's custom tag, for the read-only path the tooltip and the item model property run on.
     * The tag carries the ranging flag and the last pulse's time, so the copy {@code copyTag()}
     * makes — the only accessor vanilla still supports — is two primitives.
     */
    private static CompoundTag tagOf(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
