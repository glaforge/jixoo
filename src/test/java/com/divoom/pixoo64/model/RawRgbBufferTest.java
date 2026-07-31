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
package com.divoom.pixoo64.model;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class RawRgbBufferTest {

    @Test
    void testBufferDimensionsAndByteCount() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        // Fill top-left pixel with RED (255, 0, 0)
        image.setRGB(0, 0, new Color(255, 0, 0).getRGB());
        // Fill bottom-right pixel with BLUE (0, 0, 255)
        image.setRGB(63, 63, new Color(0, 0, 255).getRGB());

        byte[] rawBytes = RawRgbBuffer.fromImage(image);
        assertEquals(12288, rawBytes.length);

        // Top left pixel (index 0, 1, 2)
        assertEquals((byte) 255, rawBytes[0]);
        assertEquals((byte) 0, rawBytes[1]);
        assertEquals((byte) 0, rawBytes[2]);

        // Bottom right pixel (index 12285, 12286, 12287)
        int lastPixelStart = (63 * 64 + 63) * 3;
        assertEquals((byte) 0, rawBytes[lastPixelStart]);
        assertEquals((byte) 0, rawBytes[lastPixelStart + 1]);
        assertEquals((byte) 255, rawBytes[lastPixelStart + 2]);
    }

    @Test
    void testInvalidDimensionsThrowsException() {
        BufferedImage badImage = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
        assertThrows(IllegalArgumentException.class, () -> RawRgbBuffer.fromImage(badImage));
    }

    @Test
    void testPixooFrameBase64Encoding() {
        byte[] dummyBytes = new byte[12288];
        dummyBytes[0] = (byte) 0x12;
        dummyBytes[1] = (byte) 0x34;

        PixooFrame frame = new PixooFrame(dummyBytes, 150);
        assertEquals(150, frame.delayMs());

        String base64 = frame.toBase64();
        assertNotNull(base64);
        byte[] decoded = Base64.getDecoder().decode(base64);
        assertArrayEquals(dummyBytes, decoded);
    }
}
