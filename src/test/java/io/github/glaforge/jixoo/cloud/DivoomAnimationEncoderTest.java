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
package io.github.glaforge.jixoo.cloud;

import io.github.glaforge.jixoo.model.PixooAnimation;
import io.github.glaforge.jixoo.model.PixooFrame;
import io.github.glaforge.jixoo.model.RawRgbBuffer;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DivoomAnimationEncoderTest {

    @Test
    void testEncodeSingleColorFrame() {
        byte[] redBuffer = RawRgbBuffer.fromColor(Color.RED);
        PixooFrame frame = new PixooFrame(redBuffer, 250);
        PixooAnimation animation = PixooAnimation.singleFrame(frame);

        byte[] encoded = DivoomAnimationEncoder.encode(animation);

        assertNotNull(encoded);
        // Single color: 1 color -> bpp 0 -> 0 packed bytes.
        // Frame length = 8 (header) + 3 (palette) = 11 bytes.
        // Total = 6 (overall header) + 4 (outer frame len) + 11 = 21 bytes.
        assertEquals(21, encoded.length);

        // Verify animation header
        assertEquals((byte) 0x1A, encoded[0]); // Magic
        assertEquals((byte) 1, encoded[1]);    // Total frames
        assertEquals((byte) 0, encoded[2]);    // Speed hi (250 >> 8 = 0)
        assertEquals((byte) 250, encoded[3]);  // Speed lo (250 & 0xFF)
        assertEquals((byte) 4, encoded[4]);    // Row count (4 blocks of 16)
        assertEquals((byte) 4, encoded[5]);    // Col count (4 blocks of 16)

        // Verify frame length (11 big endian: 0x0000000B)
        int frameLen = ((encoded[6] & 0xFF) << 24) | ((encoded[7] & 0xFF) << 16) | ((encoded[8] & 0xFF) << 8) | (encoded[9] & 0xFF);
        assertEquals(11, frameLen);

        // Verify frame payload header
        int offset = 10;
        assertEquals((byte) 0xAA, encoded[offset]);
        assertEquals((byte) 0x0B, encoded[offset + 1]); // frameLen lo
        assertEquals((byte) 0x00, encoded[offset + 2]); // frameLen hi
        assertEquals((byte) 250, encoded[offset + 3]);  // speed lo
        assertEquals((byte) 0, encoded[offset + 4]);    // speed hi
        assertEquals((byte) 0x0C, encoded[offset + 5]); // Encrypt type / format
        assertEquals((byte) 0x01, encoded[offset + 6]); // 1 color lo
        assertEquals((byte) 0x00, encoded[offset + 7]); // 1 color hi

        // Verify single color in palette is Red (255, 0, 0)
        int paletteOffset = offset + 8;
        assertEquals((byte) 255, encoded[paletteOffset]);
        assertEquals((byte) 0, encoded[paletteOffset + 1]);
        assertEquals((byte) 0, encoded[paletteOffset + 2]);
    }

    @Test
    void testEncodeMultiFrameSingleColor() {
        PixooFrame f1 = new PixooFrame(RawRgbBuffer.fromColor(Color.RED), 500);
        PixooFrame f2 = new PixooFrame(RawRgbBuffer.fromColor(Color.GREEN), 500);
        PixooFrame f3 = new PixooFrame(RawRgbBuffer.fromColor(Color.BLUE), 500);

        PixooAnimation animation = new PixooAnimation(List.of(f1, f2, f3));
        byte[] encoded = DivoomAnimationEncoder.encode(animation);

        assertNotNull(encoded);
        // Header: 6 bytes + 3 * (4 bytes length + 11 payload) = 51 bytes (identical to RGBAnimation!)
        assertEquals(51, encoded.length);

        assertEquals((byte) 0x1A, encoded[0]);
        assertEquals((byte) 3, encoded[1]); // 3 frames
    }

    @Test
    void testEncodeMultiColorFrame() {
        byte[] buffer = new byte[RawRgbBuffer.TOTAL_BYTES];
        // Create 2 colors: half RED, half BLUE
        for (int i = 0; i < RawRgbBuffer.TOTAL_BYTES / 2; i += 3) {
            buffer[i] = (byte) 255; // Red
        }
        for (int i = RawRgbBuffer.TOTAL_BYTES / 2; i < RawRgbBuffer.TOTAL_BYTES; i += 3) {
            buffer[i + 2] = (byte) 255; // Blue
        }

        PixooFrame frame = new PixooFrame(buffer, 100);
        PixooAnimation animation = PixooAnimation.singleFrame(frame);

        byte[] encoded = DivoomAnimationEncoder.encode(animation);
        assertNotNull(encoded);

        // 2 colors -> bpp = 1 -> packed bytes = 512 * 1 = 512
        // Frame length = 8 + (2 * 3) + 512 = 526
        int frameLen = ((encoded[6] & 0xFF) << 24) | ((encoded[7] & 0xFF) << 16) | ((encoded[8] & 0xFF) << 8) | (encoded[9] & 0xFF);
        assertEquals(526, frameLen);
    }
}
