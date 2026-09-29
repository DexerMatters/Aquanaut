package com.dexer.aquanaut.common.item;

import java.util.zip.CRC32;

/** Shared, side-safe contract for the bounded PNG embedded in a developed photo stack. */
public final class PhotoImageFormat {
    public static final int WIDTH = 320;
    public static final int HEIGHT = 180;
    public static final int MAX_BYTES = 384 * 1024;

    private PhotoImageFormat() {
    }

    public static boolean validPng(byte[] bytes) {
        if (bytes.length < 45 || bytes.length > MAX_BYTES) {
            return false;
        }
        byte[] signature = { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
        for (int i = 0; i < signature.length; i++) {
            if (bytes[i] != signature[i]) {
                return false;
            }
        }

        boolean foundHeader = false;
        boolean foundImageData = false;
        int offset = signature.length;
        while (offset + 12 <= bytes.length) {
            long unsignedLength = Integer.toUnsignedLong(readInt(bytes, offset));
            if (unsignedLength > Integer.MAX_VALUE) {
                return false;
            }
            int length = (int) unsignedLength;
            int dataOffset = offset + 8;
            int crcOffset = dataOffset + length;
            if (crcOffset < dataOffset || crcOffset + 4 > bytes.length) {
                return false;
            }

            CRC32 crc = new CRC32();
            crc.update(bytes, offset + 4, 4 + length);
            if (crc.getValue() != Integer.toUnsignedLong(readInt(bytes, crcOffset))) {
                return false;
            }

            boolean header = chunkType(bytes, offset, 'I', 'H', 'D', 'R');
            boolean imageData = chunkType(bytes, offset, 'I', 'D', 'A', 'T');
            boolean end = chunkType(bytes, offset, 'I', 'E', 'N', 'D');
            if (header) {
                if (foundHeader || offset != signature.length || length != 13
                        || readInt(bytes, dataOffset) != WIDTH
                        || readInt(bytes, dataOffset + 4) != HEIGHT
                        || bytes[dataOffset + 8] != 8
                        || bytes[dataOffset + 9] != 6
                        || bytes[dataOffset + 10] != 0
                        || bytes[dataOffset + 11] != 0
                        || (bytes[dataOffset + 12] != 0 && bytes[dataOffset + 12] != 1)) {
                    return false;
                }
                foundHeader = true;
            } else if (!foundHeader) {
                return false;
            }
            if (imageData) {
                foundImageData = true;
            }
            if (end) {
                return length == 0 && foundHeader && foundImageData && crcOffset + 4 == bytes.length;
            }
            offset = crcOffset + 4;
        }
        return false;
    }

    private static boolean chunkType(byte[] bytes, int offset, int a, int b, int c, int d) {
        return bytes[offset + 4] == a && bytes[offset + 5] == b
                && bytes[offset + 6] == c && bytes[offset + 7] == d;
    }

    private static int readInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) << 24
                | (bytes[offset + 1] & 0xFF) << 16
                | (bytes[offset + 2] & 0xFF) << 8
                | bytes[offset + 3] & 0xFF;
    }
}
