package com.dexer.aquanaut.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.client.screen.TargetPickerScreen;
import com.dexer.aquanaut.network.CompassTargetPayload;
import com.dexer.aquanaut.common.entity.AbstractTaggableEntity;
import com.dexer.aquanaut.common.entity.TagRules;
import com.dexer.aquanaut.common.item.SubmarineCompassItem;
import com.dexer.aquanaut.core.ItemRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Opens the compass target picker.
 *
 * <p>
 * Holding right-click on the compass past {@link SubmarineCompassItem#HOLD_TICKS} opens
 * {@link TargetPickerScreen}, the shared list the controller's link gesture uses too. Every marker is
 * offered with the kind it is — cursor, drone — so a mixed list still reads unambiguously.
 *
 * <p>
 * The list is snapshotted when the picker opens, so a marker that drifts out of range mid-gesture
 * cannot reshuffle the rows under the player's mouse.
 */
@EventBusSubscriber(modid = Aquanaut.MODID, value = Dist.CLIENT)
public final class ClientCompassEvents {

    /** How far away a marker can be and still be offered. */
    private static final double SEARCH_RADIUS = 256.0D;

    /** Most entries the list can hold, including "nearest". */
    private static final int MAX_OPTIONS = 10;

    private ClientCompassEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        boolean holding = player.isUsingItem()
                && player.getUseItem().is(ItemRegistry.SUBMARINE_COMPASS.get());

        if (holding) {
            int held = player.getUseItem().getUseDuration(player) - player.getUseItemRemainingTicks();
            if (held >= SubmarineCompassItem.HOLD_TICKS && !(minecraft.screen instanceof TargetPickerScreen)) {
                minecraft.setScreen(buildPicker(player));
            }
        } else if (minecraft.screen instanceof TargetPickerScreen picker
                && picker.source() == TargetPickerScreen.Source.COMPASS) {
            // The hold ended some other way — the item was swapped, the player died — so drop the
            // picker without applying anything.
            picker.onClose();
        }
    }

    private static TargetPickerScreen buildPicker(LocalPlayer player) {
        List<AbstractTaggableEntity> markers = new ArrayList<>(player.level().getEntitiesOfClass(
                AbstractTaggableEntity.class, player.getBoundingBox().inflate(SEARCH_RADIUS)));
        markers.sort(Comparator.comparingDouble(marker -> marker.distanceToSqr(player)));

        List<TargetPickerScreen.Option> options = new ArrayList<>();
        // "Nearest" is a real choice rather than a fallback, so it is first and it wears the kind of
        // whatever it would actually point at.
        options.add(new TargetPickerScreen.Option(null,
                Component.translatable("gui.aquanaut.compass.nearest"),
                TargetPickerScreen.NEAREST_COLOR,
                markers.isEmpty() ? null : markers.get(0).kindLabel()));

        int room = MAX_OPTIONS - 1;
        for (int i = 0; i < Math.min(markers.size(), room); i++) {
            AbstractTaggableEntity marker = markers.get(i);
            String name = marker.getTagName().isEmpty() ? "?" : marker.getTagName();
            options.add(new TargetPickerScreen.Option(marker.getUUID(), Component.literal(name),
                    TagRules.toArgb(marker.getTagColor()), marker.kindLabel()));
        }

        // Start on whatever the compass already points at, so opening the picker does not silently
        // re-aim it at "nearest" if the player releases without moving.
        int highlighted = 0;
        if (SubmarineCompassItem.getMode(player.getUseItem()) == SubmarineCompassItem.TargetMode.MARKER) {
            UUID current = SubmarineCompassItem.getTargetId(player.getUseItem());
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).id() != null && options.get(i).id().equals(current)) {
                    highlighted = i;
                    break;
                }
            }
        }
        return new TargetPickerScreen(TargetPickerScreen.Source.COMPASS,
                Component.translatable("gui.aquanaut.compass.title"),
                Component.translatable("gui.aquanaut.compass.hint"),
                options, highlighted, id -> PacketDistributor.sendToServer(
                        new CompassTargetPayload(id == null ? "" : id.toString())));
    }
}
