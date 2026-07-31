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
package io.github.glaforge.jixoo.model;

import java.awt.image.BufferedImage;

/**
 * Utility for producing raw 64x64 RGB pixel arrays (12,288 bytes) and Base64 strings.
 */
public final class RawRgbBuffer {
    /** Expected width of the screen. */
    public static final int WIDTH = 64;
    /** Expected height of the screen. */
    public static final int HEIGHT = 64;
    /** Number of bytes per pixel (RGB). */
    public static final int BYTES_PER_PIXEL = 3;
    /** Total number of bytes in a frame buffer. */
    public static final int TOTAL_BYTES = WIDTH * HEIGHT * BYTES_PER_PIXEL; // 12,288 bytes

    private RawRgbBuffer() {}

    /**
     * Extracts raw 24-bit RGB pixel data from a 64x64 BufferedImage in top-left to bottom-right raster order.
     *
     * @param image 64x64 BufferedImage
     * @return 12,288-byte array containing RGB sequence
     */
    public static byte[] fromImage(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("Image cannot be null");
        }
        if (image.getWidth() != WIDTH || image.getHeight() != HEIGHT) {
            throw new IllegalArgumentException(
                    "Image dimensions must be exactly 64x64 pixels. Given: " + image.getWidth() + "x" + image.getHeight()
            );
        }

        int[] argbPixels = new int[WIDTH * HEIGHT];
        image.getRGB(0, 0, WIDTH, HEIGHT, argbPixels, 0, WIDTH);

        byte[] rawRgb = new byte[TOTAL_BYTES];
        int index = 0;

        for (int argb : argbPixels) {
            rawRgb[index++] = (byte) ((argb >> 16) & 0xFF); // Red
            rawRgb[index++] = (byte) ((argb >> 8) & 0xFF);  // Green
            rawRgb[index++] = (byte) (argb & 0xFF);         // Blue
        }

        return rawRgb;
    }
}
