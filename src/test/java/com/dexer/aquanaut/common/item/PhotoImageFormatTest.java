package com.dexer.aquanaut.common.item;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

public final class PhotoImageFormatTest {
    @Test
    void acceptsOnlyCompleteCrcValidRgbaFramesAtTheCaptureResolution() throws IOException {
        byte[] valid = png(PhotoImageFormat.WIDTH, PhotoImageFormat.HEIGHT);
        assertTrue(PhotoImageFormat.validPng(valid), "camera output");

        byte[] corrupted = valid.clone();
        corrupted[corrupted.length - 1] ^= 1;
        assertFalse(PhotoImageFormat.validPng(corrupted), "corrupt IEND CRC");
        assertFalse(PhotoImageFormat.validPng(png(160, 90)), "wrong resolution");
        assertFalse(PhotoImageFormat.validPng(new byte[64]), "not a PNG");
    }

    private static byte[] png(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int red = x * 255 / Math.max(1, width - 1);
                int green = y * 255 / Math.max(1, height - 1);
                int blue = (x + y) & 0xFF;
                image.setRGB(x, y, 0xFF000000 | red << 16 | green << 8 | blue);
            }
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
