package net.minecraft.network.chat;

/**
 * The JUnit tests run without the Minecraft runtime, so main classes that the tests touch are linked
 * against this shim. Only the helpers used by those classes are provided, with vanilla semantics.
 */
public interface Component {

    static MutableComponent translatable(String key) {
        return new MutableComponent(key, true);
    }

    static MutableComponent literal(String text) {
        return new MutableComponent(text, false);
    }

    String getString();
}
