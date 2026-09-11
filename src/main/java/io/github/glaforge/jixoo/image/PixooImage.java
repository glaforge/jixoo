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

import io.github.glaforge.jixoo.model.PixooFrame;
import io.github.glaforge.jixoo.model.RawRgbBuffer;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Objects;

/**
 * Pure-Java 2D image representation containing 32-bit ARGB pixels with zero AWT/native runtime dependencies.
 *
 * @param width      Image width in pixels
 * @param height     Image height in pixels
 * @param argbPixels 32-bit ARGB pixel array in row-major order (size: width * height)
 */
public record PixooImage(int width, int height, int[] argbPixels) {

    /**
     * Constructs and validates a PixooImage instance.
     */
    public PixooImage {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Image dimensions must be positive. Given: " + width + "x" + height);
        }
        Objects.requireNonNull(argbPixels, "argbPixels cannot be null");
        if (argbPixels.length != width * height) {
            throw new IllegalArgumentException(
                    "argbPixels length must be exactly " + (width * height) + ", but was " + argbPixels.length
            );
        }
        argbPixels = argbPixels.clone();
    }

    @Override
    public int[] argbPixels() {
        return argbPixels.clone();
    }

    /**
     * Creates a new blank (all black) PixooImage with the given dimensions.
     *
     * @param width  Width in pixels
     * @param height Height in pixels
     * @return a new PixooImage instance
     */
    public static PixooImage of(int width, int height) {
        int[] pixels = new int[width * height];
        Arrays.fill(pixels, 0xFF000000); // opaque black
        return new PixooImage(width, height, pixels);
    }

    /**
     * Gets the ARGB pixel color at coordinate (x, y).
     *
     * @param x X coordinate (0 to width - 1)
     * @param y Y coordinate (0 to height - 1)
     * @return 32-bit ARGB pixel value
     */
    public int getPixel(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IndexOutOfBoundsException("Coordinate out of bounds: (" + x + ", " + y + ") for size " + width + "x" + height);
        }
        return argbPixels[y * width + x];
    }

    /**
     * Converts a 64x64 PixooImage directly into the 12,288-byte raw RGB buffer required by the Pixoo display.
     *
     * @return 12,288-byte array in RGB scan order
     * @throws IllegalArgumentException if the image dimensions are not exactly 64x64
     */
    public byte[] toRawRgb() {
        if (width != RawRgbBuffer.WIDTH || height != RawRgbBuffer.HEIGHT) {
            throw new IllegalArgumentException(
                    "Image dimensions must be exactly " + RawRgbBuffer.WIDTH + "x" + RawRgbBuffer.HEIGHT +
                    " to create a Pixoo buffer. Current dimensions: " + width + "x" + height
            );
        }

        byte[] rawRgb = new byte[RawRgbBuffer.TOTAL_BYTES];
        int idx = 0;
        for (int argb : argbPixels) {
            int a = (argb >> 24) & 0xFF;
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;

            // Composite alpha over black background (standard for LED displays)
            if (a < 255) {
                r = (r * a) / 255;
                g = (g * a) / 255;
                b = (b * a) / 255;
            }

            rawRgb[idx++] = (byte) r;
            rawRgb[idx++] = (byte) g;
            rawRgb[idx++] = (byte) b;
        }
        return rawRgb;
    }

    /**
     * Converts this 64x64 image into a PixooFrame with the specified delay.
     *
     * @param delayMs Frame display duration in milliseconds
     * @return a new PixooFrame
     */
    public PixooFrame toFrame(int delayMs) {
        return new PixooFrame(toRawRgb(), delayMs);
    }

    /**
     * Resizes and fits this image into the specified target dimensions using pure-Java bilinear interpolation.
     *
     * @param targetWidth  Target width (e.g. 64)
     * @param targetHeight Target height (e.g. 64)
     * @param mode         Scaling and aspect-ratio strategy
     * @return a new resized PixooImage of size targetWidth x targetHeight
     */
    public PixooImage resizeAndFit(int targetWidth, int targetHeight, ImageProcessor.ScaleMode mode) {
        if (targetWidth <= 0 || targetHeight <= 0) {
            throw new IllegalArgumentException("Target dimensions must be positive");
        }
        if (mode == null) {
            mode = ImageProcessor.ScaleMode.FIT_CENTER;
        }

        if (this.width == targetWidth && this.height == targetHeight) {
            return this;
        }


        int scaledW;
        int scaledH;
        int xOffset;
        int yOffset;

        switch (mode) {
            case STRETCH -> {
                scaledW = targetWidth;
                scaledH = targetHeight;
                xOffset = 0;
                yOffset = 0;
            }
            case FIT_CENTER -> {
                double scale = Math.min((double) targetWidth / this.width, (double) targetHeight / this.height);
                scaledW = Math.max(1, (int) Math.round(this.width * scale));
                scaledH = Math.max(1, (int) Math.round(this.height * scale));
                scaledW = Math.min(scaledW, targetWidth);
                scaledH = Math.min(scaledH, targetHeight);
                xOffset = (targetWidth - scaledW) / 2;
                yOffset = (targetHeight - scaledH) / 2;
            }
            case FILL_CROP -> {
                double scale = Math.max((double) targetWidth / this.width, (double) targetHeight / this.height);
                scaledW = Math.max(1, (int) Math.round(this.width * scale));
                scaledH = Math.max(1, (int) Math.round(this.height * scale));
                xOffset = (targetWidth - scaledW) / 2;
                yOffset = (targetHeight - scaledH) / 2;
            }
            default -> throw new IllegalArgumentException("Unsupported ScaleMode: " + mode);
        }

        int[] destPixels = new int[targetWidth * targetHeight];
        // Fill letterbox margins with solid black (0xFF000000)
        Arrays.fill(destPixels, 0xFF000000);

        for (int dy = 0; dy < targetHeight; dy++) {
            for (int dx = 0; dx < targetWidth; dx++) {
                int lx = dx - xOffset;
                int ly = dy - yOffset;

                if (lx < 0 || lx >= scaledW || ly < 0 || ly >= scaledH) {
                    continue; // Leave background black for letterbox
                }

                // Map to source floating-point coordinate
                double sx = ((lx + 0.5) * this.width) / scaledW - 0.5;
                double sy = ((ly + 0.5) * this.height) / scaledH - 0.5;

                // Clamp to source boundaries
                if (sx < 0) sx = 0;
                if (sy < 0) sy = 0;
                if (sx > this.width - 1) sx = this.width - 1;
                if (sy > this.height - 1) sy = this.height - 1;

                int x0 = (int) Math.floor(sx);
                int y0 = (int) Math.floor(sy);
                int x1 = Math.min(x0 + 1, this.width - 1);
                int y1 = Math.min(y0 + 1, this.height - 1);

                double fx = sx - x0;
                double fy = sy - y0;

                double w00 = (1.0 - fx) * (1.0 - fy);
                double w10 = fx * (1.0 - fy);
                double w01 = (1.0 - fx) * fy;
                double w11 = fx * fy;

                int p00 = this.argbPixels[y0 * this.width + x0];
                int p10 = this.argbPixels[y0 * this.width + x1];
                int p01 = this.argbPixels[y1 * this.width + x0];
                int p11 = this.argbPixels[y1 * this.width + x1];

                int a = (int) Math.round(
                        w00 * ((p00 >> 24) & 0xFF) +
                        w10 * ((p10 >> 24) & 0xFF) +
                        w01 * ((p01 >> 24) & 0xFF) +
                        w11 * ((p11 >> 24) & 0xFF)
                );
                int r = (int) Math.round(
                        w00 * ((p00 >> 16) & 0xFF) +
                        w10 * ((p10 >> 16) & 0xFF) +
                        w01 * ((p01 >> 16) & 0xFF) +
                        w11 * ((p11 >> 16) & 0xFF)
                );
                int g = (int) Math.round(
                        w00 * ((p00 >> 8) & 0xFF) +
                        w10 * ((p10 >> 8) & 0xFF) +
                        w01 * ((p01 >> 8) & 0xFF) +
                        w11 * ((p11 >> 8) & 0xFF)
                );
                int b = (int) Math.round(
                        w00 * (p00 & 0xFF) +
                        w10 * (p10 & 0xFF) +
                        w01 * (p01 & 0xFF) +
                        w11 * (p11 & 0xFF)
                );

                a = Math.min(255, Math.max(0, a));
                r = Math.min(255, Math.max(0, r));
                g = Math.min(255, Math.max(0, g));
                b = Math.min(255, Math.max(0, b));

                destPixels[dy * targetWidth + dx] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }

        return new PixooImage(targetWidth, targetHeight, destPixels);
    }

    /**
     * Converts a standard BufferedImage into a PixooImage.
     * Useful for JVM-based desktop applications.
     *
     * @param bi Source BufferedImage
     * @return Equivalent PixooImage
     */
    public static PixooImage fromBufferedImage(BufferedImage bi) {
        if (bi == null) {
            throw new IllegalArgumentException("BufferedImage cannot be null");
        }
        int w = bi.getWidth();
        int h = bi.getHeight();
        int[] pixels = new int[w * h];
        bi.getRGB(0, 0, w, h, pixels, 0, w);
        return new PixooImage(w, h, pixels);
    }

    /**
     * Converts this PixooImage into a standard BufferedImage (TYPE_INT_ARGB).
     * Useful for JVM-based desktop applications.
     *
     * @return Equivalent BufferedImage
     */
    public BufferedImage toBufferedImage() {
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        bi.setRGB(0, 0, width, height, argbPixels, 0, width);
        return bi;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PixooImage that)) return false;
        return width == that.width && height == that.height && Arrays.equals(argbPixels, that.argbPixels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height, Arrays.hashCode(argbPixels));
    }

    @Override
    public String toString() {
        return "PixooImage[" + width + "x" + height + "]";
    }
}
