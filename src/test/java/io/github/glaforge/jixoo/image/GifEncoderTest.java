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
import io.github.glaforge.jixoo.model.RawRgbBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GifEncoderTest {

    @Test
    @DisplayName("Should encode synthetic multi-frame animation and decode back accurately")
    void testEncodeAndDecodeRoundtrip() {
        List<PixooFrame> frames = new ArrayList<>();

        // Frame 1: Red with a green box
        byte[] rgb1 = new byte[64 * 64 * 3];
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                int idx = (y * 64 + x) * 3;
                if (x >= 20 && x < 44 && y >= 20 && y < 44) {
                    rgb1[idx] = 0;
                    rgb1[idx + 1] = (byte) 255; // Green
                    rgb1[idx + 2] = 0;
                } else {
                    rgb1[idx] = (byte) 255; // Red
                    rgb1[idx + 1] = 0;
                    rgb1[idx + 2] = 0;
                }
            }
        }
        frames.add(new PixooFrame(rgb1, 150));

        // Frame 2: Blue with a yellow box
        byte[] rgb2 = new byte[64 * 64 * 3];
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                int idx = (y * 64 + x) * 3;
                if (x >= 10 && x < 54 && y >= 10 && y < 54) {
                    rgb2[idx] = (byte) 255; // Yellow
                    rgb2[idx + 1] = (byte) 255;
                    rgb2[idx + 2] = 0;
                } else {
                    rgb2[idx] = 0;
                    rgb2[idx + 1] = 0;
                    rgb2[idx + 2] = (byte) 255; // Blue
                }
            }
        }
        frames.add(new PixooFrame(rgb2, 300));

        PixooAnimation anim = new PixooAnimation(frames);
        byte[] gifBytes = GifEncoder.encode(anim);

        assertNotNull(gifBytes);
        assertTrue(gifBytes.length > 50);

        // Verify GIF header
        String magic = new String(gifBytes, 0, 6, StandardCharsets.US_ASCII);
        assertEquals("GIF89a", magic);

        // Decode back using GifDecoder
        PixooAnimation decoded = GifDecoder.decode(gifBytes);
        assertEquals(2, decoded.frameCount());

        // Check frame delays (rounded to 10ms centiseconds in GIF)
        assertEquals(150, decoded.frames().get(0).delayMs());
        assertEquals(300, decoded.frames().get(1).delayMs());

        // Check pixel values of frame 1
        byte[] decRgb1 = decoded.frames().get(0).rgbData();
        assertEquals(64 * 64 * 3, decRgb1.length);
        // Center pixel (32, 32) should be green (0, 255, 0)
        int centerIdx = (32 * 64 + 32) * 3;
        assertEquals(0, decRgb1[centerIdx] & 0xFF);
        assertEquals(255, decRgb1[centerIdx + 1] & 0xFF);
        assertEquals(0, decRgb1[centerIdx + 2] & 0xFF);


        // Corner pixel (0, 0) should be red (255, 0, 0)
        assertEquals(255, decRgb1[0] & 0xFF);
        assertEquals(0, decRgb1[1] & 0xFF);
        assertEquals(0, decRgb1[2] & 0xFF);
    }

    @Test
    @DisplayName("Should encode Divoom binary asset into valid animated GIF")
    void testEncodeDivoomAssetToGif() throws Exception {
        InputStream is = getClass().getResourceAsStream("/sample_divoom.bin");
        assertNotNull(is, "sample_divoom.bin should exist in test resources");
        byte[] binData = is.readAllBytes();

        PixooAnimation divoomAnim = DivoomAssetDecoder.decode(binData);
        assertTrue(divoomAnim.frameCount() > 0);

        byte[] gifData = GifEncoder.encode(divoomAnim);
        assertNotNull(gifData);
        assertTrue(gifData.length > 100);

        String magic = new String(gifData, 0, 6, StandardCharsets.US_ASCII);
        assertEquals("GIF89a", magic);

        // Decode back with GifDecoder
        PixooAnimation decodedFromGif = GifDecoder.decode(gifData);
        assertEquals(divoomAnim.frameCount(), decodedFromGif.frameCount());
    }

    @Test
    @DisplayName("Should quantize colors when frame has more than 256 unique colors")
    void testEncodeQuantizedColors() {
        byte[] gradientRgb = new byte[64 * 64 * 3];
        // Create 4096 distinct colors
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                int idx = (y * 64 + x) * 3;
                gradientRgb[idx] = (byte) (x * 4);
                gradientRgb[idx + 1] = (byte) (y * 4);
                gradientRgb[idx + 2] = (byte) ((x + y) * 2);
            }
        }
        PixooFrame frame = new PixooFrame(gradientRgb, 100);
        PixooAnimation anim = new PixooAnimation(List.of(frame));

        byte[] gifBytes = GifEncoder.encode(anim);
        assertNotNull(gifBytes);

        PixooAnimation decoded = GifDecoder.decode(gifBytes);
        assertEquals(1, decoded.frameCount());
        assertEquals(64 * 64 * 3, decoded.frames().get(0).rgbData().length);
    }
}

