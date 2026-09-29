package com.dexer.aquanaut.common.item;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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

/** A persistent instant photograph whose PNG and capture metadata travel with the stack. */
public final class DevelopedPhotoItem extends Item {
    private static final int MAX_USE_DURATION = 72_000;

    private static final String ID_KEY = "PhotoId";
    private static final String IMAGE_KEY = "Image";
    private static final String WIDTH_KEY = "Width";
    private static final String HEIGHT_KEY = "Height";
    private static final String TAKEN_AT_KEY = "TakenAt";
    private static final String DIMENSION_KEY = "Dimension";
    private static final String X_KEY = "X";
    private static final String Y_KEY = "Y";
    private static final String Z_KEY = "Z";
    private static final String PHOTOGRAPHER_KEY = "Photographer";

    public DevelopedPhotoItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static void setPhoto(ItemStack stack, byte[] png, int width, int height, long takenAt,
            String dimension, int x, int y, int z, String photographer) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(ID_KEY, UUID.randomUUID());
        tag.putByteArray(IMAGE_KEY, png);
        tag.putInt(WIDTH_KEY, width);
        tag.putInt(HEIGHT_KEY, height);
        tag.putLong(TAKEN_AT_KEY, takenAt);
        tag.putString(DIMENSION_KEY, dimension);
        tag.putInt(X_KEY, x);
        tag.putInt(Y_KEY, y);
        tag.putInt(Z_KEY, z);
        tag.putString(PHOTOGRAPHER_KEY, photographer);
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }

    /** Copies an immutable capture payload between exposed film and finished photo items. */
    public static boolean copyPhoto(ItemStack source, ItemStack target) {
        if (photo(source).isEmpty()) {
            return false;
        }
        target.set(DataComponents.CUSTOM_DATA,
                source.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY));
        return true;
    }

    public static Optional<PhotoData> photo(ItemStack stack) {
        CompoundTag tag = tagOf(stack);
        if (!tag.hasUUID(ID_KEY)
                || !tag.contains(IMAGE_KEY, Tag.TAG_BYTE_ARRAY)
                || !tag.contains(WIDTH_KEY, Tag.TAG_INT)
                || !tag.contains(HEIGHT_KEY, Tag.TAG_INT)) {
            return Optional.empty();
        }
        byte[] png = tag.getByteArray(IMAGE_KEY);
        int width = tag.getInt(WIDTH_KEY);
        int height = tag.getInt(HEIGHT_KEY);
        if (png.length == 0 || width <= 0 || height <= 0) {
            return Optional.empty();
        }
        return Optional.of(new PhotoData(
                tag.getUUID(ID_KEY), png, width, height,
                tag.getLong(TAKEN_AT_KEY), tag.getString(DIMENSION_KEY),
                tag.getInt(X_KEY), tag.getInt(Y_KEY), tag.getInt(Z_KEY),
                tag.getString(PHOTOGRAPHER_KEY)));
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
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        player.startUsingItem(context.getHand());
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Optional<PhotoData> photo = photo(stack);
        if (photo.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.aquanaut.developed_photo.empty")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        PhotoData data = photo.get();
        tooltip.add(Component.translatable("tooltip.aquanaut.developed_photo.location",
                data.x(), data.y(), data.z()).withStyle(ChatFormatting.GRAY));
        if (!data.photographer().isBlank()) {
            tooltip.add(Component.translatable("tooltip.aquanaut.developed_photo.by", data.photographer())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.translatable("tooltip.aquanaut.developed_photo.hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @SuppressWarnings("deprecation")
    private static CompoundTag tagOf(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
    }

    /** Read-only view of the immutable custom-data component; callers must not mutate {@link #png()}. */
    public record PhotoData(UUID id, byte[] png, int width, int height, long takenAt,
            String dimension, int x, int y, int z, String photographer) {
    }
}
