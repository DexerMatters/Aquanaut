package net.minecraft.network.chat;

/**
 * The JUnit tests run without the Minecraft runtime, so main classes that the tests touch are linked
 * against this shim. Only the helpers used by those classes are provided, with vanilla semantics.
 */
public final class MutableComponent implements Component {

    private final String text;
    private final boolean translatable;

    MutableComponent(String text, boolean translatable) {
        this.text = text;
        this.translatable = translatable;
    }

    /**
     * Vanilla resolves a translatable component through the loaded language; with no language
     * loaded, the lookup falls back to the key itself.
     */
    @Override
    public String getString() {
        return text;
    }

    @Override
    public String toString() {
        return translatable ? "TranslatableComponent{key='" + text + "'}" : "LiteralComponent{text='" + text + "'}";
    }
}
