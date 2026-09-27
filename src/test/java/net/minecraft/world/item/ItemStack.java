package net.minecraft.world.item;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.ItemLike;

/**
 * The JUnit tests run without the Minecraft runtime, so main classes that the tests touch are linked
 * against this shim. Only the helpers used by those classes are provided, with vanilla semantics.
 */
public class ItemStack {

    public static final ItemStack EMPTY = new ItemStack((ItemLike) null);

    private final Item item;

    public ItemStack(ItemLike item) {
        this.item = item == null ? null : item.asItem();
    }

    /** Vanilla's empty stack carries no item; a stack is empty when it carries none. */
    public boolean isEmpty() {
        return item == null;
    }

    public Item getItem() {
        return item;
    }

    /** A bare stack carries no components beyond the item defaults. */
    public DataComponentPatch getComponentsPatch() {
        return DataComponentPatch.EMPTY;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
