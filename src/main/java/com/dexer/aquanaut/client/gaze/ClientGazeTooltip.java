package com.dexer.aquanaut.client.gaze;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.gaze.GazeHelper;
import com.dexer.aquanaut.common.gaze.GazeInstance;
import com.dexer.aquanaut.common.gaze.GazeType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

@EventBusSubscriber(modid = Aquanaut.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
@SuppressWarnings("removal")
public final class ClientGazeTooltip {

    private ClientGazeTooltip() {
    }

    private static final Style GAZE_LEVEL_STYLE = Style.EMPTY
            .withColor(TextColor.fromRgb(0x40B8C8));

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<GazeInstance> gazes = GazeHelper.getGaze(stack);
        if (gazes.isEmpty()) {
            return;
        }

        List<Component> tooltip = event.getToolTip();
        tooltip.add(Component.empty());

        for (GazeInstance instance : gazes) {
            ResourceLocation id = instance.id();
            String key = "gaze." + id.getNamespace() + "." + id.getPath();
            Component line = Component.translatable("gaze.aquanaut.type." + instance.type().serializedName())
                    .withStyle(instance.type() == GazeType.FAVOR ? ChatFormatting.DARK_AQUA : ChatFormatting.GRAY)
                    .append(Component.literal(": "))
                    .append(Component.translatable(key))
                    .append(Component.literal(" " + toRoman(instance.level()))
                            .withStyle(GAZE_LEVEL_STYLE));
            tooltip.add(line);
        }
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(n);
        };
    }
}
