package com.dexer.aquanaut.client.photo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.item.DevelopedPhotoItem.PhotoData;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Bounded client-side cache of decoded photograph textures, shared by the handheld photo viewer
 * and the rinsing basin's darkroom preview. Decoding goes through a byte stream rather than
 * {@code NativeImage.read(byte[])} so a 320x180 frame never lands on LWJGL's small memory stack.
 */
public final class PhotoTextureCache {
    private static final int CACHE_SIZE = 16;
    private static final int MAX_FAILED = 64;

    private static final Map<UUID, Cached> TEXTURES = new LinkedHashMap<>(16, 0.75F, true);
    private static final Set<UUID> FAILED = new HashSet<>();

    private PhotoTextureCache() {
    }

    public static Cached get(Minecraft minecraft, PhotoData data) {
        Cached cached = TEXTURES.get(data.id());
        if (cached != null) {
            return cached;
        }
        if (FAILED.contains(data.id())) {
            return null;
        }
        try (ByteArrayInputStream input = new ByteArrayInputStream(data.png())) {
            NativeImage image = NativeImage.read(input);
            if (image.getWidth() != data.width() || image.getHeight() != data.height()) {
                image.close();
                markFailed(data.id());
                return null;
            }
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID,
                    "dynamic/photo/" + data.id().toString().replace("-", ""));
            minecraft.getTextureManager().register(location, new DynamicTexture(image));
            cached = new Cached(location, image.getWidth(), image.getHeight());
            TEXTURES.put(data.id(), cached);
            trim(minecraft);
            return cached;
        } catch (IOException | RuntimeException failure) {
            markFailed(data.id());
            Aquanaut.LOGGER.warn("Could not decode developed photo {}", data.id(), failure);
            return null;
        }
    }

    public static void clear(Minecraft minecraft) {
        for (Cached cached : TEXTURES.values()) {
            minecraft.getTextureManager().release(cached.location());
        }
        TEXTURES.clear();
        FAILED.clear();
    }

    private static void markFailed(UUID id) {
        if (FAILED.size() >= MAX_FAILED) {
            FAILED.clear();
        }
        FAILED.add(id);
    }

    private static void trim(Minecraft minecraft) {
        while (TEXTURES.size() > CACHE_SIZE) {
            Iterator<Cached> iterator = TEXTURES.values().iterator();
            Cached oldest = iterator.next();
            iterator.remove();
            minecraft.getTextureManager().release(oldest.location());
        }
    }

    public record Cached(ResourceLocation location, int width, int height) {
    }
}
