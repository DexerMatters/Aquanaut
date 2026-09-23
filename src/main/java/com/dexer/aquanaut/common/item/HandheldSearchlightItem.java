package com.dexer.aquanaut.common.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * The handheld searchlight: a diver's lamp that places a real block-light pool where it is aimed.
 *
 * <p>
 * A right-click switches it on or off. The switch is a lamp state on the stack rather than a hold,
 * so it survives a drop, a logout and a hand-over, and the server owns it: the client only reads
 * the state, which is what lets every other player see whose lamp is burning. While lit, the server
 * moves an invisible block-light source to the aimed endpoint so the surrounding world and all
 * connected clients see ordinary Minecraft light, with a smooth client visual layer over the core.
 */
public class HandheldSearchlightItem extends Item {

    private static final String LIT_KEY = "Lit";

    public HandheldSearchlightItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** Whether the lamp is switched on. */
    public static boolean isLit(ItemStack stack) {
        return tagOf(stack).getBoolean(LIT_KEY);
    }

    /** Switches the lamp on or off. */
    public static void setLit(ItemStack stack, boolean lit) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(LIT_KEY, lit));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            boolean lit = !isLit(stack);
            setLit(stack, lit);
            // The switch is a brass lever, and it sounds like one: the pitch tells on from off
            // without the player having to look at the lamp.
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LEVER_CLICK,
                    SoundSource.PLAYERS, 0.55F, lit ? 1.2F : 0.85F);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        boolean lit = isLit(stack);
        tooltip.add(Component.translatable(lit
                ? "tooltip.aquanaut.handheld_searchlight.on"
                : "tooltip.aquanaut.handheld_searchlight.off")
                .withStyle(lit ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.aquanaut.handheld_searchlight.hint")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.aquanaut.handheld_searchlight.beam",
                (int) SearchlightGeometry.AIR_RANGE, (int) SearchlightGeometry.SUBMERGED_RANGE)
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /**
     * The stack's custom tag, for the read-only path the tooltip and the item model property run on.
     * The tag carries the lit flag alone, so the copy {@code copyTag()} makes — the only accessor
     * vanilla still supports — is a single boolean.
     */
    private static CompoundTag tagOf(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
