package net.minecraft;

import net.minecraft.resources.ResourceLocation;

/**
 * The JUnit tests run without the Minecraft runtime, so main classes that the tests touch are linked
 * against this shim. Only the helpers used by those classes are provided, with vanilla semantics.
 */
public final class Util {

    public static String makeDescriptionId(String type, ResourceLocation id) {
        return id == null
                ? type + ".unregistered_sadface"
                : type + "." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    private Util() {
    }
}
