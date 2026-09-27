package net.minecraft.core.component;

/**
 * The JUnit tests run without the Minecraft runtime, so main classes that the tests touch are linked
 * against this shim. Only the helpers used by those classes are provided, with vanilla semantics.
 */
public final class DataComponentPatch {

    public static final DataComponentPatch EMPTY = new DataComponentPatch();

    private DataComponentPatch() {
    }
}
