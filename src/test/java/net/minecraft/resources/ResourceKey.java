package net.minecraft.resources;

/**
 * The JUnit tests run without the Minecraft runtime, so main classes that the tests touch are linked
 * against this shim. Only the helpers used by those classes are provided, with vanilla semantics.
 */
public final class ResourceKey<T> {

    private final ResourceLocation registryName;
    private final ResourceLocation location;

    private ResourceKey(ResourceLocation registryName, ResourceLocation location) {
        this.registryName = registryName;
        this.location = location;
    }

    public static <T> ResourceKey<T> create(ResourceKey<?> registryKey, ResourceLocation location) {
        return new ResourceKey<>(registryKey.location(), location);
    }

    public static <T> ResourceKey<T> createRegistryKey(ResourceLocation location) {
        return new ResourceKey<>(ResourceLocation.withDefaultNamespace("root"), location);
    }

    public ResourceLocation registry() {
        return registryName;
    }

    public ResourceLocation location() {
        return location;
    }

    @Override
    public String toString() {
        return "ResourceKey[" + registryName + " / " + location + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ResourceKey<?> other)) {
            return false;
        }
        return registryName.equals(other.registryName) && location.equals(other.location);
    }

    @Override
    public int hashCode() {
        return registryName.hashCode() * 31 + location.hashCode();
    }
}
