package net.minecraft.core.registries;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/**
 * The JUnit tests run without the Minecraft runtime, so main classes that the tests touch are linked
 * against this shim. Only the helpers used by those classes are provided, with vanilla semantics.
 */
public final class Registries {

    public static final ResourceKey<Biome> BIOME = ResourceKey.createRegistryKey(
            ResourceLocation.withDefaultNamespace("biome"));

    private Registries() {
    }
}
