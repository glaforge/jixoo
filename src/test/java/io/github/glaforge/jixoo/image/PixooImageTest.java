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
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class PixooImageTest {

    @Test
    void testCreationAndGetPixel() {
        int[] pixels = new int[4];
        pixels[0] = 0xFFFF0000; // Red
        pixels[1] = 0xFF00FF00; // Green
        pixels[2] = 0xFF0000FF; // Blue
        pixels[3] = 0xFFFFFFFF; // White

        PixooImage img = new PixooImage(2, 2, pixels);
        assertEquals(2, img.width());
        assertEquals(2, img.height());
        assertEquals(0xFFFF0000, img.getPixel(0, 0));
        assertEquals(0xFF00FF00, img.getPixel(1, 0));
        assertEquals(0xFF0000FF, img.getPixel(0, 1));
        assertEquals(0xFFFFFFFF, img.getPixel(1, 1));

        assertThrows(IndexOutOfBoundsException.class, () -> img.getPixel(2, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> img.getPixel(0, -1));
    }

    @Test
    void testBlankOf() {
        PixooImage img = PixooImage.of(10, 10);
        assertEquals(10, img.width());
        assertEquals(10, img.height());
        assertEquals(0xFF000000, img.getPixel(0, 0));
        assertEquals(0xFF000000, img.getPixel(9, 9));
    }

    @Test
    void testToRawRgbDimensionsCheck() {
        PixooImage non64 = PixooImage.of(32, 32);
        assertThrows(IllegalArgumentException.class, non64::toRawRgb);

        PixooImage img64 = PixooImage.of(64, 64);
        byte[] raw = img64.toRawRgb();
        assertEquals(RawRgbBuffer.TOTAL_BYTES, raw.length);
    }

    @Test
    void testToRawRgbAlphaCompositing() {
        int[] pixels = new int[64 * 64];
        // 50% transparent pure red: ARGB = (128, 255, 0, 0)
        pixels[0] = (128 << 24) | (255 << 16);
        // Opaque pure green: ARGB = (255, 0, 255, 0)
        pixels[1] = (255 << 24) | (255 << 8);

        PixooImage img = new PixooImage(64, 64, pixels);
        byte[] raw = img.toRawRgb();

        // 50% of 255 composite over black -> ~128
        int r0 = raw[0] & 0xFF;
        int g0 = raw[1] & 0xFF;
        int b0 = raw[2] & 0xFF;
        assertTrue(r0 >= 127 && r0 <= 129);
        assertEquals(0, g0);
        assertEquals(0, b0);

        // Opaque green:
        assertEquals(0, raw[3] & 0xFF);
        assertEquals(255, raw[4] & 0xFF);
        assertEquals(0, raw[5] & 0xFF);
    }

    @Test
    void testToFrame() {
        PixooImage img = PixooImage.of(64, 64);
        PixooFrame frame = img.toFrame(250);
        assertNotNull(frame);
        assertEquals(250, frame.delayMs());
        assertEquals(12288, frame.rgbData().length);
    }

    @Test
    void testResizeAndFitModes() {
        // Create 200x100 horizontal image
        int[] pixels = new int[200 * 100];
        // Fill center with pure red
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = 0xFFFF0000;
        }
        PixooImage wideImg = new PixooImage(200, 100, pixels);

        // FIT_CENTER: 200x100 into 64x64 should scale to 64x32 with vertical letterbox padding
        PixooImage fitted = wideImg.resizeAndFit(64, 64, ImageProcessor.ScaleMode.FIT_CENTER);
        assertEquals(64, fitted.width());
        assertEquals(64, fitted.height());
        // Top row (dy=0) should be letterbox black (0xFF000000)
        assertEquals(0xFF000000, fitted.getPixel(32, 0));
        // Center row (dy=32) should be pure red (0xFFFF0000)
        assertEquals(0xFFFF0000, fitted.getPixel(32, 32));

        // STRETCH: 200x100 stretched to 64x64
        PixooImage stretched = wideImg.resizeAndFit(64, 64, ImageProcessor.ScaleMode.STRETCH);
        assertEquals(64, stretched.width());
        assertEquals(64, stretched.height());
        assertEquals(0xFFFF0000, stretched.getPixel(0, 0));
        assertEquals(0xFFFF0000, stretched.getPixel(32, 32));

        // FILL_CROP: 200x100 covers 64x64
        PixooImage cropped = wideImg.resizeAndFit(64, 64, ImageProcessor.ScaleMode.FILL_CROP);
        assertEquals(64, cropped.width());
        assertEquals(64, cropped.height());
        assertEquals(0xFFFF0000, cropped.getPixel(0, 0));
    }

    @Test
    void testBufferedImageConversionRoundTrip() {
        BufferedImage bi = new BufferedImage(10, 20, BufferedImage.TYPE_INT_ARGB);
        bi.setRGB(5, 5, 0xFF112233);

        PixooImage pix = PixooImage.fromBufferedImage(bi);
        assertEquals(10, pix.width());
        assertEquals(20, pix.height());
        assertEquals(0xFF112233, pix.getPixel(5, 5));

        BufferedImage convertedBack = pix.toBufferedImage();
        assertEquals(10, convertedBack.getWidth());
        assertEquals(20, convertedBack.getHeight());
        assertEquals(0xFF112233, convertedBack.getRGB(5, 5));
    }
}
