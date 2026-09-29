package com.dexer.aquanaut.common.item;

import java.util.List;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A light-sealed camera negative. It carries a capture until it is rinsed in a photo basin. */
public final class ExposedFilmItem extends Item {
    public ExposedFilmItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Optional<DevelopedPhotoItem.PhotoData> photo = DevelopedPhotoItem.photo(stack);
        if (photo.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.aquanaut.exposed_film.blank")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        DevelopedPhotoItem.PhotoData data = photo.get();
        tooltip.add(Component.translatable("tooltip.aquanaut.exposed_film.location",
                data.x(), data.y(), data.z()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.aquanaut.exposed_film.develop")
                .withStyle(ChatFormatting.AQUA));
    }
}
