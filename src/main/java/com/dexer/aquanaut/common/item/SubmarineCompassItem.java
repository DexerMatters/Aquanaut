package com.dexer.aquanaut.common.item;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * The submarine compass: points at a tagged marker, and lets the player choose which one.
 *
 * <p>
 * A marker is any {@code AbstractTaggableEntity} — the pins a player drops and the drones they fly
 * — so one compass covers both. Targeting lives in the stack's NBT rather than in a client-side
 * field, so a chosen marker survives
 * dropping the compass, logging out and handing it to somebody else.
 *
 * <p>
 * Choosing is a hold, not a click: right-clicking starts "using", the client opens the selection
 * wheel once the hold passes {@link #HOLD_TICKS}, and releasing commits whatever the mouse is
 * pointing at. A tap therefore does nothing, which is the right default for an item you carry while
 * swimming.
 */
public class SubmarineCompassItem extends Item {

    /** How the compass decides what to point at. */
    public enum TargetMode {
        /** Point at whichever marker is closest. */
        NEAREST,
        /** Point at one specific marker. */
        MARKER
    }

    /** Ticks of right-click before the selection wheel appears. */
    public static final int HOLD_TICKS = 6;

    /** Long enough that the hold never expires on its own. */
    private static final int USE_DURATION = 72000;

    private static final String MODE_KEY = "TargetMode";
    private static final String ID_KEY = "TargetId";
    private static final String MARKER_MODE = "marker";

    public SubmarineCompassItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    // ------------------------------------------------------------------
    // targeting state
    // ------------------------------------------------------------------

    public static TargetMode getMode(ItemStack stack) {
        return MARKER_MODE.equals(tagOf(stack).getString(MODE_KEY)) ? TargetMode.MARKER : TargetMode.NEAREST;
    }

    /** The chosen marker's id, or {@code null} when the compass is in nearest mode. */
    @Nullable
    public static UUID getTargetId(ItemStack stack) {
        String raw = tagOf(stack).getString(ID_KEY);
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException malformed) {
            return null;
        }
    }

    /** Pins the compass to one marker, or puts it back into nearest mode. */
    public static void setTarget(ItemStack stack, TargetMode mode, @Nullable UUID targetId) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            boolean specific = mode == TargetMode.MARKER && targetId != null;
            tag.putString(MODE_KEY, specific ? MARKER_MODE : "nearest");
            if (specific) {
                tag.putString(ID_KEY, targetId.toString());
            } else {
                tag.remove(ID_KEY);
            }
        });
    }

    /**
     * The stack's custom tag, on the read-only path the item model property runs once per frame.
     * {@code copyTag()} is the only accessor vanilla still supports; the tag is two short strings,
     * so the per-frame copy is a small allocation next to the rest of a frame's work.
     */
    private static CompoundTag tagOf(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    // ------------------------------------------------------------------
    // hold-to-choose
    // ------------------------------------------------------------------

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    // ------------------------------------------------------------------
    // readout
    // ------------------------------------------------------------------

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        boolean nearest = getMode(stack) == TargetMode.NEAREST;
        tooltip.add(Component.translatable(nearest
                ? "item.aquanaut.submarine_compass.target.nearest"
                : "item.aquanaut.submarine_compass.target.chosen").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.aquanaut.submarine_compass.hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
