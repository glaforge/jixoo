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

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineHelper;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.chunks.PngChunkPLTE;
import ar.com.hjg.pngj.chunks.PngChunkTRNS;
import io.github.glaforge.jixoo.api.exception.PixooException;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Pure-Java PNG decoder using PNGJ with zero AWT/native runtime dependencies.
 * Supports Grayscale, Grayscale+Alpha, RGB, RGBA, and Indexed/Paletted PNGs.
 */
public final class PngDecoder {

    private PngDecoder() {}

    /**
     * Decodes a PNG image from an InputStream into a {@link PixooImage}.
     *
     * @param inputStream the stream containing PNG data
     * @return decoded PixooImage
     * @throws PixooException if decoding fails
     */
    public static PixooImage decode(InputStream inputStream) {
        PngReader reader = null;
        try {
            reader = new PngReader(inputStream);
            ImageInfo info = reader.imgInfo;
            int width = info.cols;
            int height = info.rows;
            int channels = info.channels;
            boolean indexed = info.indexed;
            boolean greyscale = info.greyscale;
            boolean hasAlpha = info.alpha;
            int bitDepth = info.bitDepth;

            PngChunkPLTE plte = indexed ? (PngChunkPLTE) reader.getChunksList().getById1("PLTE") : null;
            PngChunkTRNS trns = (PngChunkTRNS) reader.getChunksList().getById1("tRNS");

            int[] argbPixels = new int[width * height];
            int[] rgbaBuffer = null;

            for (int row = 0; row < height; row++) {
                ImageLineInt line = (ImageLineInt) reader.readRow();
                int[] scanline = line.getScanline();
                int rowOffset = row * width;

                if (indexed && plte != null) {
                    rgbaBuffer = ImageLineHelper.palette2rgba(line, plte, trns, rgbaBuffer);
                    for (int col = 0; col < width; col++) {
                        int r = rgbaBuffer[col * 4] & 0xFF;
                        int g = rgbaBuffer[col * 4 + 1] & 0xFF;
                        int b = rgbaBuffer[col * 4 + 2] & 0xFF;
                        int a = rgbaBuffer[col * 4 + 3] & 0xFF;
                        argbPixels[rowOffset + col] = (a << 24) | (r << 16) | (g << 8) | b;
                    }
                } else if (greyscale) {
                    for (int col = 0; col < width; col++) {
                        int g = scanline[col * channels];
                        if (bitDepth == 16) g >>= 8;
                        int a = hasAlpha ? scanline[col * channels + 1] : 255;
                        if (hasAlpha && bitDepth == 16) a >>= 8;
                        g = Math.min(255, Math.max(0, g));
                        a = Math.min(255, Math.max(0, a));
                        argbPixels[rowOffset + col] = (a << 24) | (g << 16) | (g << 8) | g;
                    }
                } else { // RGB or RGBA
                    for (int col = 0; col < width; col++) {
                        int r = scanline[col * channels];
                        int g = scanline[col * channels + 1];
                        int b = scanline[col * channels + 2];
                        int a = hasAlpha ? scanline[col * channels + 3] : 255;
                        if (bitDepth == 16) {
                            r >>= 8;
                            g >>= 8;
                            b >>= 8;
                            if (hasAlpha) a >>= 8;
                        }
                        r = Math.min(255, Math.max(0, r));
                        g = Math.min(255, Math.max(0, g));
                        b = Math.min(255, Math.max(0, b));
                        a = Math.min(255, Math.max(0, a));
                        argbPixels[rowOffset + col] = (a << 24) | (r << 16) | (g << 8) | b;
                    }
                }
            }
            reader.end();
            return new PixooImage(width, height, argbPixels);
        } catch (PixooException e) {
            throw e;
        } catch (Exception e) {
            throw new PixooException("Failed to decode PNG image: " + e.getMessage(), e);
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Decodes a PNG image from a file Path into a {@link PixooImage}.
     *
     * @param path the path to the PNG file
     * @return decoded PixooImage
     * @throws PixooException if reading or decoding fails
     */
    public static PixooImage decode(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            return decode(is);
        } catch (PixooException e) {
            throw e;
        } catch (Exception e) {
            throw new PixooException("Failed to read PNG file: " + path, e);
        }
    }
}
