package net.minecraft.resources;

import com.mojang.serialization.Codec;

import java.util.Objects;

public final class ResourceLocation implements Comparable<ResourceLocation> {

    public static final Codec<ResourceLocation> CODEC = Codec.STRING.xmap(ResourceLocation::parse,
            ResourceLocation::toString);

    private final String namespace;
    private final String path;

    private ResourceLocation(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
    }

    public static ResourceLocation fromNamespaceAndPath(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    public static ResourceLocation parse(String value) {
        int separator = value.indexOf(':');
        if (separator < 0) {
            return new ResourceLocation("minecraft", value);
        }
        return new ResourceLocation(value.substring(0, separator), value.substring(separator + 1));
    }

    public static ResourceLocation tryParse(String value) {
        try {
            return parse(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPath() {
        return path;
    }

    public String toLanguageKey(String type) {
        return type + "." + namespace + "." + path.replace('/', '.');
    }

    public ResourceLocation withPrefix(String prefix) {
        return new ResourceLocation(namespace, prefix + path);
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ResourceLocation other)) {
            return false;
        }
        return namespace.equals(other.namespace) && path.equals(other.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, path);
    }

    @Override
    public int compareTo(ResourceLocation other) {
        return this.toString().compareTo(other.toString());
    }
}
