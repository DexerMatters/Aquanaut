package com.dexer.aquanaut.client.screen;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.client.screen.aquarium.AquariumTab;
import com.dexer.aquanaut.network.OpenAquariumPayload;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Hangs the aquarium tab beside the player's inventory tab.
 *
 * <p>The tabs are ordinary widgets that derive their position from the screen every time it is asked
 * for, so this class only installs them once. In particular no render hook is needed to chase the
 * inventory screen around when the recipe book opens.
 */
@EventBusSubscriber(modid = Aquanaut.MODID, value = Dist.CLIENT)
public final class AquariumScreenEvents {

    private AquariumScreenEvents() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof AbstractContainerScreen<?> container)) {
            return;
        }

        for (AquariumTab tab : AquariumTab.forScreen(container, AquariumScreenEvents::openAquarium,
                AquariumScreen::returnToInventory)) {
            event.addListener(tab);
        }
    }

    private static void openAquarium() {
        PacketDistributor.sendToServer(new OpenAquariumPayload());
    }
}
