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
import io.github.glaforge.jixoo.model.PixooAnimation;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Image processing utilities for loading, scaling, cropping, and fitting images into 64x64.
 * Uses pure-Java decoders (PNG, GIF, BMP) without AWT/native runtime dependencies.
 */
public class ImageProcessor {

    /**
     * Scale mode for fitting images into the 64x64 canvas.
     */
    public enum ScaleMode {
        FIT_CENTER,
        FILL_CROP,
        STRETCH
    }

    /**
     * Loads an image from the specified path into a {@link PixooImage}.
     * Supports PNG, GIF, and BMP natively with pure-Java decoders.
     *
     * @param path the path to load the image from
     * @return the loaded PixooImage
     * @throws PixooException if the image cannot be loaded
     */
    public static PixooImage loadImage(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            return loadImage(is, path.getFileName().toString());
        } catch (PixooException e) {
            throw e;
        } catch (Exception e) {
            throw new PixooException("Error reading image path: " + path, e);
        }
    }

    /**
     * Loads an image from the specified input stream into a {@link PixooImage}.
     *
     * @param inputStream the stream to load the image from
     * @return the loaded PixooImage
     * @throws PixooException if the image cannot be loaded
     */
    public static PixooImage loadImage(InputStream inputStream) {
        return loadImage(inputStream, null);
    }

    /**
     * Loads an image from stream using file signature detection with an optional filename hint.
     */
    public static PixooImage loadImage(InputStream inputStream, String fileNameHint) {
        try {
            byte[] bytes = inputStream.readAllBytes();
            if (bytes.length < 4) {
                throw new PixooException("Invalid image: Stream too short");
            }

            // Detect format via magic bytes
            if (isPng(bytes)) {
                return PngDecoder.decode(new ByteArrayInputStream(bytes));
            } else if (isBmp(bytes)) {
                return BmpDecoder.decode(new ByteArrayInputStream(bytes));
            } else if (isGif(bytes)) {
                PixooAnimation anim = GifDecoder.decode(bytes);
                // Return first frame of GIF as PixooImage
                byte[] rgbData = anim.frames().get(0).rgbData();
                return rawRgbToPixooImage(rgbData, 64, 64);
            }

            // Fallback to filename extension hint if magic bytes were ambiguous
            if (fileNameHint != null) {
                String lower = fileNameHint.toLowerCase();
                if (lower.endsWith(".png")) {
                    return PngDecoder.decode(new ByteArrayInputStream(bytes));
                } else if (lower.endsWith(".bmp")) {
                    return BmpDecoder.decode(new ByteArrayInputStream(bytes));
                } else if (lower.endsWith(".gif")) {
                    PixooAnimation anim = GifDecoder.decode(bytes);
                    byte[] rgbData = anim.frames().get(0).rgbData();
                    return rawRgbToPixooImage(rgbData, 64, 64);
                }
            }

            throw new PixooException("Unsupported image format. Supported formats: PNG, GIF, BMP.");
        } catch (PixooException e) {
            throw e;
        } catch (Exception e) {
            throw new PixooException("Failed to decode image: " + e.getMessage(), e);
        }
    }

    private static boolean isPng(byte[] bytes) {
        return bytes.length >= 8 &&
                (bytes[0] & 0xFF) == 0x89 &&
                bytes[1] == 0x50 && // 'P'
                bytes[2] == 0x4E && // 'N'
                bytes[3] == 0x47;   // 'G'
    }

    private static boolean isBmp(byte[] bytes) {
        return bytes.length >= 2 && bytes[0] == 0x42 && bytes[1] == 0x4D; // 'BM'
    }

    private static boolean isGif(byte[] bytes) {
        return bytes.length >= 3 && bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46; // 'GIF'
    }

    private static PixooImage rawRgbToPixooImage(byte[] rawRgb, int width, int height) {
        int[] pixels = new int[width * height];
        int src = 0;
        for (int i = 0; i < pixels.length; i++) {
            int r = rawRgb[src++] & 0xFF;
            int g = rawRgb[src++] & 0xFF;
            int b = rawRgb[src++] & 0xFF;
            pixels[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
        return new PixooImage(width, height, pixels);
    }

    /**
     * Resizes and fits a PixooImage into a 64x64 canvas using FIT_CENTER scale mode.
     */
    public static PixooImage resizeAndFit(PixooImage input) {
        return resizeAndFit(input, ScaleMode.FIT_CENTER);
    }

    /**
     * Resizes and fits a PixooImage into a 64x64 canvas using the specified scale mode.
     */
    public static PixooImage resizeAndFit(PixooImage input, ScaleMode scaleMode) {
        return input.resizeAndFit(64, 64, scaleMode);
    }

    /**
     * Processes a PixooImage and wraps it in a single-frame PixooAnimation.
     */
    public static PixooAnimation processImage(PixooImage input) {
        return processImage(input, ScaleMode.FIT_CENTER);
    }

    /**
     * Processes a PixooImage with the specified scale mode and wraps it in a single-frame PixooAnimation.
     */
    public static PixooAnimation processImage(PixooImage input, ScaleMode scaleMode) {
        PixooImage processed = resizeAndFit(input, scaleMode);
        return PixooAnimation.singleImage(processed);
    }

    // --- Backward Compatibility methods for JVM users using BufferedImage ---

    /**
     * Loads a BufferedImage from path (convenience method for JVM environments).
     */
    public static BufferedImage load(Path path) {
        return loadImage(path).toBufferedImage();
    }

    /**
     * Loads a BufferedImage from input stream (convenience method for JVM environments).
     */
    public static BufferedImage load(InputStream inputStream) {
        return loadImage(inputStream).toBufferedImage();
    }

    /**
     * Resizes and fits a BufferedImage using FIT_CENTER.
     */
    public static BufferedImage resizeAndFit(BufferedImage input) {
        return resizeAndFit(input, ScaleMode.FIT_CENTER);
    }

    /**
     * Resizes and fits a BufferedImage using the specified scale mode with pure-Java interpolation.
     */
    public static BufferedImage resizeAndFit(BufferedImage input, ScaleMode scaleMode) {
        PixooImage pix = PixooImage.fromBufferedImage(input);
        return pix.resizeAndFit(64, 64, scaleMode).toBufferedImage();
    }

    /**
     * Processes a BufferedImage into a single-frame PixooAnimation.
     */
    public static PixooAnimation processImage(BufferedImage input) {
        return processImage(PixooImage.fromBufferedImage(input));
    }
}
