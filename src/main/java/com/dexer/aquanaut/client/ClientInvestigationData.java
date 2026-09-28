package com.dexer.aquanaut.client;

import com.dexer.aquanaut.client.screen.InvestigationBoardScreen;
import com.dexer.aquanaut.common.investigation.InvestigationProgress;
import net.minecraft.client.Minecraft;

public final class ClientInvestigationData {
    private static InvestigationProgress progress = InvestigationProgress.EMPTY;

    private ClientInvestigationData() {
    }

    public static InvestigationProgress get() {
        return progress;
    }

    public static void receive(String serialized, boolean openBoard) {
        progress = InvestigationProgress.deserialize(serialized);
        if (openBoard) {
            Minecraft.getInstance().setScreen(new InvestigationBoardScreen());
        }
    }
}
