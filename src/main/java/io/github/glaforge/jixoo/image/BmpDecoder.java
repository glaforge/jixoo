/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.glaforge.jixoo.image;

import io.github.glaforge.jixoo.api.exception.PixooException;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Pure-Java BMP decoder supporting 24-bit and 32-bit uncompressed Windows bitmap images with zero AWT dependencies.
 */
public final class BmpDecoder {

    private BmpDecoder() {}

    /**
     * Decodes a BMP image from an InputStream into a {@link PixooImage}.
     *
     * @param inputStream the stream containing BMP data
     * @return decoded PixooImage
     * @throws PixooException if decoding fails or format is unsupported
     */
    public static PixooImage decode(InputStream inputStream) {
        try {
            byte[] bytes = inputStream.readAllBytes();
            if (bytes.length < 54) {
                throw new PixooException("Invalid BMP: File too small (" + bytes.length + " bytes)");
            }

            // Verify 'BM' signature
            if (bytes[0] != 0x42 || bytes[1] != 0x4D) {
                throw new PixooException("Invalid BMP: Missing 'BM' header magic");
            }

            int pixelOffset = readInt32LE(bytes, 10);
            int dibHeaderSize = readInt32LE(bytes, 14);
            if (dibHeaderSize < 40) {
                throw new PixooException("Unsupported BMP DIB header size: " + dibHeaderSize);
            }

            int width = readInt32LE(bytes, 18);
            int rawHeight = readInt32LE(bytes, 22);
            boolean bottomUp = rawHeight > 0;
            int height = Math.abs(rawHeight);

            int bitsPerPixel = readInt16LE(bytes, 28);
            int compression = readInt32LE(bytes, 30);

            if (compression != 0 && compression != 3) {
                throw new PixooException("Unsupported BMP compression method: " + compression + " (only uncompressed BI_RGB is supported)");
            }
            if (bitsPerPixel != 24 && bitsPerPixel != 32) {
                throw new PixooException("Unsupported BMP bit depth: " + bitsPerPixel + "-bit (only 24-bit and 32-bit are supported)");
            }

            int rowStride = ((width * bitsPerPixel + 31) / 32) * 4;
            int[] argbPixels = new int[width * height];
            int bytesPerPixel = bitsPerPixel / 8;

            for (int r = 0; r < height; r++) {
                int y = bottomUp ? (height - 1 - r) : r;
                int rowStart = pixelOffset + r * rowStride;
                if (rowStart + width * bytesPerPixel > bytes.length) {
                    throw new PixooException("Malformed BMP: Unexpected end of file at row " + r);
                }

                for (int x = 0; x < width; x++) {
                    int pIndex = rowStart + x * bytesPerPixel;
                    int b = bytes[pIndex] & 0xFF;
                    int g = bytes[pIndex + 1] & 0xFF;
                    int red = bytes[pIndex + 2] & 0xFF;
                    int a = (bytesPerPixel == 4) ? (bytes[pIndex + 3] & 0xFF) : 0xFF;

                    argbPixels[y * width + x] = (a << 24) | (red << 16) | (g << 8) | b;
                }
            }

            return new PixooImage(width, height, argbPixels);
        } catch (PixooException e) {
            throw e;
        } catch (Exception e) {
            throw new PixooException("Failed to decode BMP image", e);
        }
    }

    /**
     * Decodes a BMP image from a file Path into a {@link PixooImage}.
     *
     * @param path the path to the BMP file
     * @return decoded PixooImage
     * @throws PixooException if reading or decoding fails
     */
    public static PixooImage decode(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            return decode(is);
        } catch (PixooException e) {
            throw e;
        } catch (Exception e) {
            throw new PixooException("Failed to read BMP file: " + path, e);
        }
    }

    private static int readInt16LE(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8);
    }

    private static int readInt32LE(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) |
               ((bytes[offset + 1] & 0xFF) << 8) |
               ((bytes[offset + 2] & 0xFF) << 16) |
               ((bytes[offset + 3] & 0xFF) << 24);
    }
}
