package com.dexer.aquanaut.common.inventory.aquarium;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

public class AquariumSlot extends Slot {

    public AquariumSlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }
}
