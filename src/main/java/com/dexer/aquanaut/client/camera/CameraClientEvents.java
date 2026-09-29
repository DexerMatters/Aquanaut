package com.dexer.aquanaut.client.camera;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.client.photo.PhotoTextureCache;
import com.dexer.aquanaut.common.item.DevelopedPhotoItem;
import com.dexer.aquanaut.common.item.DevelopedPhotoItem.PhotoData;
import com.dexer.aquanaut.common.item.PhotoImageFormat;
import com.dexer.aquanaut.common.item.ShellCameraItem;
import com.dexer.aquanaut.core.ItemRegistry;
import com.dexer.aquanaut.core.SoundRegistry;
import com.dexer.aquanaut.network.TakePhotoPayload;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Captures world-only frames and renders the camera viewfinder and developed-photo viewer. */
@EventBusSubscriber(modid = Aquanaut.MODID, value = Dist.CLIENT)
public final class CameraClientEvents {
    private static final long FLASH_MILLIS = 115L;
    private static final DateTimeFormatter PHOTO_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm");

    private static NativeImage latestWorldFrame;
    private static boolean captureRequested;
    private static InteractionHand captureHand;
    private static long flashUntil;

    private CameraClientEvents() {
    }

    /** Called from the item's client-side release hook. Item swaps while use is still held do not shoot. */
    public static void requestCapture(InteractionHand hand) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.keyUse.isDown()) {
            return;
        }
        captureHand = hand;
        captureRequested = true;
    }

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            discardPreview();
            captureRequested = false;
            captureHand = null;
            return;
        }

        if (captureRequested) {
            captureWorldFrame(minecraft);
            captureRequested = false;
            submitCapture(minecraft);
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        if (event.getPlayer() instanceof LocalPlayer player && (usingCamera(player) || captureRequested)) {
            event.setNewFovModifier(event.getNewFovModifier() * 0.82F);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && (usingCamera(player) || usingPhoto(player))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        if (usingCamera(player)) {
            drawCameraOverlay(event.getGuiGraphics(), minecraft, player);
        } else if (usingPhoto(player)) {
            drawPhotoViewer(event.getGuiGraphics(), minecraft, player.getUseItem());
        }

        long remaining = flashUntil - Util.getMillis();
        if (remaining > 0L) {
            int alpha = Mth.clamp((int) (225.0D * remaining / FLASH_MILLIS), 0, 225);
            event.getGuiGraphics().fill(0, 0, event.getGuiGraphics().guiWidth(),
                    event.getGuiGraphics().guiHeight(), alpha << 24 | 0xFFF7DE);
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && (usingCamera(player) || usingPhoto(player) || captureRequested)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft minecraft = Minecraft.getInstance();
        discardPreview();
        captureRequested = false;
        captureHand = null;
        flashUntil = 0L;
        PhotoTextureCache.clear(minecraft);
    }

    private static boolean usingCamera(LocalPlayer player) {
        return player.isUsingItem() && player.getUseItem().is(ItemRegistry.SHELL_CAMERA.get());
    }

    private static boolean usingPhoto(LocalPlayer player) {
        return player.isUsingItem() && player.getUseItem().is(ItemRegistry.DEVELOPED_PHOTO.get());
    }

    private static void captureWorldFrame(Minecraft minecraft) {
        NativeImage resized = null;
        try (NativeImage source = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
            int sourceWidth = source.getWidth();
            int sourceHeight = source.getHeight();
            double targetAspect = (double) PhotoImageFormat.WIDTH / PhotoImageFormat.HEIGHT;
            int cropWidth = sourceWidth;
            int cropHeight = sourceHeight;
            if ((double) sourceWidth / sourceHeight > targetAspect) {
                cropWidth = Math.max(1, (int) Math.floor(sourceHeight * targetAspect));
            } else {
                cropHeight = Math.max(1, (int) Math.floor(sourceWidth / targetAspect));
            }
            int cropX = (sourceWidth - cropWidth) / 2;
            int cropY = (sourceHeight - cropHeight) / 2;
            resized = new NativeImage(PhotoImageFormat.WIDTH, PhotoImageFormat.HEIGHT, false);
            source.resizeSubRectTo(cropX, cropY, cropWidth, cropHeight, resized);
            replacePreview(resized);
        } catch (RuntimeException failure) {
            if (resized != null) {
                resized.close();
            }
            Aquanaut.LOGGER.warn("Could not capture shell-camera preview", failure);
        }
    }

    private static void submitCapture(Minecraft minecraft) {
        if (latestWorldFrame == null) {
            captureWorldFrame(minecraft);
        }
        if (latestWorldFrame == null) {
            showCaptureFailure(minecraft);
            return;
        }

        try {
            byte[] png = latestWorldFrame.asByteArray();
            if (png.length > PhotoImageFormat.MAX_BYTES) {
                showCaptureFailure(minecraft);
                return;
            }
            boolean offhand = captureHand == InteractionHand.OFF_HAND;
            PacketDistributor.sendToServer(new TakePhotoPayload(offhand, png));
            if (minecraft.player != null) {
                minecraft.player.playSound(SoundRegistry.CAMERA_SHUTTER.get(), 0.9F, 1.0F);
            }
            flashUntil = Util.getMillis() + FLASH_MILLIS;
        } catch (IOException | RuntimeException failure) {
            Aquanaut.LOGGER.warn("Could not encode or send shell-camera photo", failure);
            showCaptureFailure(minecraft);
        } finally {
            captureHand = null;
            discardPreview();
        }
    }

    private static void showCaptureFailure(Minecraft minecraft) {
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable("message.aquanaut.camera.capture_failed"), true);
        }
    }

    private static void replacePreview(NativeImage image) {
        if (latestWorldFrame != null) {
            latestWorldFrame.close();
        }
        latestWorldFrame = image;
    }

    private static void discardPreview() {
        if (latestWorldFrame != null) {
            latestWorldFrame.close();
            latestWorldFrame = null;
        }
    }

    private static void drawCameraOverlay(GuiGraphics graphics, Minecraft minecraft, LocalPlayer player) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int marginX = Math.max(10, width / 18);
        int marginY = Math.max(12, height / 14);
        int left = marginX;
        int right = width - marginX;
        int top = marginY;
        int bottom = height - marginY;
        int line = 0x668FA6A0;
        int bright = 0xFFD9E7DD;

        graphics.fill(0, 0, width, height, 0x240A1513);
        graphics.fill(0, 0, width, top, 0xE6141C1A);
        graphics.fill(0, bottom, width, height, 0xE6141C1A);
        graphics.fill(0, top, left, bottom, 0xD80E1615);
        graphics.fill(right, top, width, bottom, 0xD80E1615);
        outline(graphics, left - 1, top - 1, right + 1, bottom + 1, 0xFF71847E);
        outline(graphics, left, top, right, bottom, 0xFF1F2B28);

        int thirdX1 = left + (right - left) / 3;
        int thirdX2 = left + (right - left) * 2 / 3;
        int thirdY1 = top + (bottom - top) / 3;
        int thirdY2 = top + (bottom - top) * 2 / 3;
        graphics.fill(thirdX1, top, thirdX1 + 1, bottom, line);
        graphics.fill(thirdX2, top, thirdX2 + 1, bottom, line);
        graphics.fill(left, thirdY1, right, thirdY1 + 1, line);
        graphics.fill(left, thirdY2, right, thirdY2 + 1, line);

        drawCorner(graphics, left + 5, top + 5, 1, 1, bright);
        drawCorner(graphics, right - 6, top + 5, -1, 1, bright);
        drawCorner(graphics, left + 5, bottom - 6, 1, -1, bright);
        drawCorner(graphics, right - 6, bottom - 6, -1, -1, bright);

        int centerX = width / 2;
        int centerY = height / 2;
        graphics.fill(centerX - 8, centerY - 1, centerX - 3, centerY, bright);
        graphics.fill(centerX + 3, centerY - 1, centerX + 8, centerY, bright);
        graphics.fill(centerX - 1, centerY - 8, centerX, centerY - 3, bright);
        graphics.fill(centerX - 1, centerY + 3, centerX, centerY + 8, bright);
        outline(graphics, centerX - 3, centerY - 3, centerX + 4, centerY + 4, 0xAAE8D8B8);

        Font font = minecraft.font;
        graphics.drawString(font, "SHELLCAM  01", left + 7, Math.max(3, top - 12), 0xFFD8E2DD, false);
        graphics.drawString(font, "320×180  AUTO", right - 83, Math.max(3, top - 12), 0xFF9FB4AD, false);

        int filmCount = ShellCameraItem.filmCount(player);
        String filmAmount = filmCount < 0 ? "∞" : Integer.toString(filmCount);
        Component filmStatus = Component.translatable("hud.aquanaut.camera.film", filmAmount);
        graphics.drawString(font, filmStatus, right - font.width(filmStatus), bottom + 7,
                0xFFB9D6C8, false);

        String position = "%+d  %+d  %+d".formatted(player.blockPosition().getX(),
                player.blockPosition().getY(), player.blockPosition().getZ());
        graphics.drawString(font, position, left + 7, bottom + 7, 0xFF9FB4AD, false);
        Component release = Component.translatable("hud.aquanaut.camera.release");
        graphics.drawCenteredString(font, release, width / 2, bottom + 7, 0xFFE8DDC4);
    }

    private static void drawPhotoViewer(GuiGraphics graphics, Minecraft minecraft, ItemStack stack) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        graphics.fill(0, 0, width, height, 0xE3111716);
        graphics.fill(0, 0, width, height / 7, 0x99282D29);
        graphics.fill(0, height * 6 / 7, width, height, 0x99282D29);

        PhotoData data = DevelopedPhotoItem.photo(stack).orElse(null);
        int imageWidth = Math.min(width - 72, Math.max(96, (height - 92) * 16 / 9));
        int imageHeight = imageWidth * 9 / 16;
        if (imageHeight > height - 92) {
            imageHeight = height - 92;
            imageWidth = imageHeight * 16 / 9;
        }
        int paperWidth = imageWidth + 20;
        int paperHeight = imageHeight + 42;
        int paperX = (width - paperWidth) / 2;
        int paperY = (height - paperHeight) / 2 - 2;
        int imageX = paperX + 10;
        int imageY = paperY + 10;

        graphics.fill(paperX + 5, paperY + 6, paperX + paperWidth + 7, paperY + paperHeight + 8, 0x8A000000);
        graphics.fill(paperX - 1, paperY - 1, paperX + paperWidth + 1, paperY + paperHeight + 1, 0xFF8E897A);
        graphics.fill(paperX, paperY, paperX + paperWidth, paperY + paperHeight, 0xFFF1EBD9);
        graphics.fill(paperX + 3, paperY + 3, paperX + paperWidth - 3, paperY + 5, 0xFFFFF9E8);
        graphics.fill(imageX - 2, imageY - 2, imageX + imageWidth + 2, imageY + imageHeight + 2, 0xFF343B38);

        CachedPhoto cached = data == null ? null : textureFor(minecraft, data);
        if (cached != null) {
            drawPhotoTexture(graphics, cached, imageX, imageY, imageWidth, imageHeight);
        } else {
            graphics.fill(imageX, imageY, imageX + imageWidth, imageY + imageHeight, 0xFF253431);
            graphics.fill(imageX, imageY + imageHeight / 2, imageX + imageWidth, imageY + imageHeight, 0xFF172320);
            graphics.drawCenteredString(minecraft.font, Component.translatable("hud.aquanaut.photo.missing"),
                    width / 2, imageY + imageHeight / 2 - minecraft.font.lineHeight / 2, 0xFFC9C4B2);
        }

        Font font = minecraft.font;
        if (data != null) {
            String time = PHOTO_TIME.format(Instant.ofEpochMilli(data.takenAt()).atZone(ZoneId.systemDefault()));
            String location = "%s   %+d  %+d  %+d".formatted(shortDimension(data.dimension()),
                    data.x(), data.y(), data.z());
            graphics.drawString(font, time, paperX + 10, imageY + imageHeight + 8, 0xFF34342F, false);
            graphics.drawString(font, location, paperX + 10, imageY + imageHeight + 19, 0xFF666257, false);
            if (!data.photographer().isBlank()) {
                int byWidth = font.width(data.photographer());
                graphics.drawString(font, data.photographer(), paperX + paperWidth - 10 - byWidth,
                        imageY + imageHeight + 8, 0xFF76685B, false);
            }
        }

        graphics.drawCenteredString(font, Component.translatable("hud.aquanaut.photo.release"),
                width / 2, Math.min(height - 12, paperY + paperHeight + 12), 0xFFB9C5BF);
    }

    private static void drawPhotoTexture(GuiGraphics graphics, CachedPhoto cached,
            int x, int y, int width, int height) {
        graphics.blit(cached.location(), x, y, width, height,
                0.0F, 0.0F, cached.width(), cached.height(), cached.width(), cached.height());
    }

    private static CachedPhoto textureFor(Minecraft minecraft, PhotoData data) {
        PhotoTextureCache.Cached cached = PhotoTextureCache.get(minecraft, data);
        return cached == null ? null : new CachedPhoto(cached.location(), cached.width(), cached.height());
    }

    private static String shortDimension(String dimension) {
        int separator = dimension.lastIndexOf(':');
        return separator >= 0 ? dimension.substring(separator + 1) : dimension;
    }

    private static void outline(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
        graphics.fill(left, top, right, top + 1, color);
        graphics.fill(left, bottom - 1, right, bottom, color);
        graphics.fill(left, top, left + 1, bottom, color);
        graphics.fill(right - 1, top, right, bottom, color);
    }

    private static void drawCorner(GuiGraphics graphics, int x, int y, int directionX, int directionY, int color) {
        int horizontalEnd = x + directionX * 10;
        int verticalEnd = y + directionY * 10;
        graphics.fill(Math.min(x, horizontalEnd), y, Math.max(x, horizontalEnd) + 1, y + 2, color);
        graphics.fill(x, Math.min(y, verticalEnd), x + 2, Math.max(y, verticalEnd) + 1, color);
    }

    private record CachedPhoto(ResourceLocation location, int width, int height) {
    }
}
