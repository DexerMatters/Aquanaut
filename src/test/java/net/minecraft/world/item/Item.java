package net.minecraft.world.item;

import net.minecraft.world.level.ItemLike;

/**
 * The JUnit tests run without the Minecraft runtime, so main classes that the tests touch are linked
 * against this shim. Only the helpers used by those classes are provided, with vanilla semantics.
 */
public class Item implements ItemLike {

    public Item(Properties properties) {
    }

    @Override
    public Item asItem() {
        return this;
    }

    public static class Properties {
    }
}
