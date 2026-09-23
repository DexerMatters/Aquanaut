package com.dexer.aquanaut.common.item;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

/**
 * The submarine drone controller: the handheld link between a pilot and a
 * {@link com.dexer.aquanaut.common.entity.SubmarineDroneEntity}.
 *
 * <p>
 * The pairing lives in the stack's NBT, exactly like the submarine compass's target, so an attached
 * controller keeps pointing at its drone across a drop, a logout or a hand-over.
 *
 * <h3>Attach, then fly</h3>
 * Attaching is a right-click on the drone, within sight and within {@link #REMOTE_RANGE}. It is not
 * a hold and it is not a toggle — the gesture is timed on the client from the raw mouse button and
 * the decision is the server's, so the item itself needs no use state at all.
 *
 * <p>
 * While attached, the controller reads the pilot's own movement keys: forward and back trim the dive
 * planes, left and right the rudder, jump runs the jet. Letting go is its own key, bound by default
 * to sneak, so it never has to be aimed at anything.
 *
 * <h3>Range</h3>
 * {@link #REMOTE_RANGE} is what makes this a remote rather than a leash: the drone can be linked
 * from, and then flown out to, distances far beyond arm's reach.
 */
public class SubmarineDroneControllerItem extends Item {

    private static final String LINK_KEY = "DroneId";

    /** How far the controller can reach a drone to attach to it, in blocks. */
    public static final double REMOTE_RANGE = 48.0D;

    public SubmarineDroneControllerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    // ------------------------------------------------------------------
    // link state
    // ------------------------------------------------------------------

    /** The drone this controller points at, or {@code null} when it is not paired. */
    @Nullable
    public static UUID getLinkedDrone(ItemStack stack) {
        String raw = tagOf(stack).getString(LINK_KEY);
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException malformed) {
            return null;
        }
    }

    /** Whether the controller is paired with a drone. */
    public static boolean isLinked(ItemStack stack) {
        return getLinkedDrone(stack) != null;
    }

    /** Points the controller at a drone. */
    public static void link(ItemStack stack, UUID droneId) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(LINK_KEY, droneId.toString()));
    }

    /** Forgets the pairing, so the controller can take over a different drone. */
    public static void unlink(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(LINK_KEY));
    }

    /**
     * The stack's custom tag, for the read-only path the tooltip and the item model property run on.
     * The tag carries the link id alone, so the copy {@code copyTag()} makes — the only accessor
     * vanilla still supports — is one string.
     */
    private static CompoundTag tagOf(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    // ------------------------------------------------------------------
    // hold-to-link
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // readout
    // ------------------------------------------------------------------

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        boolean linked = isLinked(stack);
        tooltip.add(Component.translatable(linked
                ? "item.aquanaut.submarine_drone_controller.linked"
                : "item.aquanaut.submarine_drone_controller.unlinked").withStyle(
                        linked ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.aquanaut.submarine_drone_controller.hint")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.aquanaut.submarine_drone_controller.controls")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
