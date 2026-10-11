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

import io.github.glaforge.jixoo.model.PixooAnimation;
import io.github.glaforge.jixoo.model.PixooFrame;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LedPostProcessorTest {

    @Test
    void testStandardDefaults() {
        LedPostProcessor proc = LedPostProcessor.standard();
        assertEquals(15, proc.blackThreshold());
        assertEquals(245, proc.maxLuminance());
        assertEquals(1.0, proc.saturationBoost());
    }

    @Test
    void testBuilderValidation() {
        assertThrows(IllegalArgumentException.class, () -> LedPostProcessor.builder().blackThreshold(-1).build());
        assertThrows(IllegalArgumentException.class, () -> LedPostProcessor.builder().blackThreshold(256).build());
        assertThrows(IllegalArgumentException.class, () -> LedPostProcessor.builder().maxLuminance(0).build());
        assertThrows(IllegalArgumentException.class, () -> LedPostProcessor.builder().maxLuminance(256).build());
        assertThrows(IllegalArgumentException.class, () -> LedPostProcessor.builder().saturationBoost(0.0).build());
        assertThrows(IllegalArgumentException.class, () -> LedPostProcessor.builder().saturationBoost(4.0).build());
    }

    @Test
    void testTrueBlackClamping() {
        LedPostProcessor proc = LedPostProcessor.builder().blackThreshold(20).build();

        int[] pixels = new int[4];
        // Near-black murky noise: RGB(12, 15, 18) -> all <= 20 -> should clamp to #000000
        pixels[0] = 0xFF000000 | (12 << 16) | (15 << 8) | 18;
        // Non-black dark color: RGB(25, 10, 10) -> R > 20 -> should NOT clamp to black
        pixels[1] = 0xFF000000 | (25 << 16) | (10 << 8) | 10;
        // Pure red: RGB(220, 0, 0) -> should be kept
        pixels[2] = 0xFFDC0000;
        // Pure black already
        pixels[3] = 0xFF000000;

        PixooImage input = new PixooImage(2, 2, pixels);
        PixooImage output = proc.process(input);

        // Near-black pixel should be exact #000000
        assertEquals(0xFF000000, output.getPixel(0, 0));
        // Above-threshold pixel should NOT be #000000
        assertNotEquals(0xFF000000, output.getPixel(1, 0));
        assertEquals(25, (output.getPixel(1, 0) >> 16) & 0xFF);
        // Pure red kept
        assertEquals(0xFFDC0000, output.getPixel(0, 1));
        // Pure black kept
        assertEquals(0xFF000000, output.getPixel(1, 1));
    }

    @Test
    void testPeakLuminanceSoftLimiting() {
        LedPostProcessor proc = LedPostProcessor.builder().maxLuminance(240).build();

        int[] pixels = new int[1];
        // 100% white glare blowout
        pixels[0] = 0xFFFFFFFF;

        PixooImage input = new PixooImage(1, 1, pixels);
        PixooImage output = proc.process(input);

        int outPixel = output.getPixel(0, 0);
        int r = (outPixel >> 16) & 0xFF;
        int g = (outPixel >> 8) & 0xFF;
        int b = outPixel & 0xFF;

        assertEquals(240, r);
        assertEquals(240, g);
        assertEquals(240, b);
    }

    @Test
    void testProcessPixooFrame() {
        LedPostProcessor proc = LedPostProcessor.standard();

        byte[] rawRgb = new byte[12288];
        // Set pixel 0 to near-black noise (10, 10, 10)
        rawRgb[0] = 10;
        rawRgb[1] = 10;
        rawRgb[2] = 10;

        // Set pixel 1 to pure white (255, 255, 255)
        rawRgb[3] = (byte) 255;
        rawRgb[4] = (byte) 255;
        rawRgb[5] = (byte) 255;

        PixooFrame frame = new PixooFrame(rawRgb, 150);
        PixooFrame processed = proc.process(frame);

        assertEquals(150, processed.delayMs());
        byte[] outBytes = processed.rgbData();

        // Pixel 0 should be clamped to (0, 0, 0)
        assertEquals(0, outBytes[0]);
        assertEquals(0, outBytes[1]);
        assertEquals(0, outBytes[2]);

        // Pixel 1 should be capped to (245, 245, 245)
        assertEquals((byte) 245, outBytes[3]);
        assertEquals((byte) 245, outBytes[4]);
        assertEquals((byte) 245, outBytes[5]);
    }

    @Test
    void testProcessAnimation() {
        LedPostProcessor proc = LedPostProcessor.standard();

        byte[] raw = new byte[12288];
        raw[0] = 12; raw[1] = 12; raw[2] = 12; // near-black

        PixooFrame f1 = new PixooFrame(raw, 100);
        PixooFrame f2 = new PixooFrame(raw, 200);

        PixooAnimation anim = new PixooAnimation(List.of(f1, f2));
        PixooAnimation processed = proc.process(anim);

        assertEquals(2, processed.frameCount());
        assertEquals(100, processed.frames().get(0).delayMs());
        assertEquals(200, processed.frames().get(1).delayMs());
        assertEquals(0, processed.frames().get(0).rgbData()[0]);
    }
}
