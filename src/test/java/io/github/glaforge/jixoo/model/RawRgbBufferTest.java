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

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

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

    @Test
    void testPixooFrameEqualityAndDefensiveCopying() {
        byte[] bytes1 = new byte[12288];
        bytes1[0] = (byte) 0xAB;
        byte[] bytes2 = new byte[12288];
        bytes2[0] = (byte) 0xAB;

        PixooFrame frame1 = new PixooFrame(bytes1, 100);
        PixooFrame frame2 = new PixooFrame(bytes2, 100);

        assertEquals(frame1, frame2);
        assertEquals(frame1.hashCode(), frame2.hashCode());
        assertTrue(frame1.toString().contains("delayMs=100"));

        // Verify defensive cloning on constructor input
        bytes1[0] = (byte) 0xFF;
        assertEquals((byte) 0xAB, frame1.rgbData()[0]);

        // Verify defensive cloning on accessor output
        byte[] accessed = frame1.rgbData();
        accessed[0] = (byte) 0x00;
        assertEquals((byte) 0xAB, frame1.rgbData()[0]);
    }

    @Test
    void testPixooAnimationUnmodifiableList() {
        byte[] bytes = new byte[12288];
        PixooFrame frame = new PixooFrame(bytes, 100);
        List<PixooFrame> mutableList = new ArrayList<>();
        mutableList.add(frame);

        PixooAnimation animation = new PixooAnimation(mutableList);
        assertEquals(1, animation.frameCount());

        assertThrows(UnsupportedOperationException.class, () -> animation.frames().add(frame));

        mutableList.add(frame);
        assertEquals(1, animation.frameCount());
    }
}
