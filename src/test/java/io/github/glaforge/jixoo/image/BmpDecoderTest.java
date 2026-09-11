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
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

class BmpDecoderTest {

    @Test
    void testDecode24BitBmp() {
        int width = 2;
        int height = 2;
        int rowStride = ((width * 24 + 31) / 32) * 4; // 8 bytes
        int fileSize = 54 + rowStride * height;

        ByteBuffer buf = ByteBuffer.allocate(fileSize).order(ByteOrder.LITTLE_ENDIAN);
        // Header
        buf.put((byte) 'B');
        buf.put((byte) 'M');
        buf.putInt(fileSize);
        buf.putInt(0); // reserved
        buf.putInt(54); // pixel offset

        // DIB Header
        buf.putInt(40); // header size
        buf.putInt(width);
        buf.putInt(height); // positive = bottom-up
        buf.putShort((short) 1); // planes
        buf.putShort((short) 24); // bits per pixel
        buf.putInt(0); // compression BI_RGB
        buf.putInt(rowStride * height); // image size
        buf.putInt(0);
        buf.putInt(0);
        buf.putInt(0);
        buf.putInt(0);

        // Pixel data (bottom-up: bottom row first, then top row)
        // Bottom row: Bottom-Left (Blue: 0,0,255), Bottom-Right (Green: 0,255,0)
        buf.put((byte) 255); buf.put((byte) 0); buf.put((byte) 0); // BGR -> Blue
        buf.put((byte) 0); buf.put((byte) 255); buf.put((byte) 0); // BGR -> Green
        buf.put((byte) 0); buf.put((byte) 0); // 2 bytes padding

        // Top row: Top-Left (Red: 255,0,0), Top-Right (White: 255,255,255)
        buf.put((byte) 0); buf.put((byte) 0); buf.put((byte) 255); // BGR -> Red
        buf.put((byte) 255); buf.put((byte) 255); buf.put((byte) 255); // BGR -> White
        buf.put((byte) 0); buf.put((byte) 0); // 2 bytes padding

        PixooImage img = BmpDecoder.decode(new ByteArrayInputStream(buf.array()));
        assertNotNull(img);
        assertEquals(2, img.width());
        assertEquals(2, img.height());

        // Verify top row (y=0)
        assertEquals(0xFFFF0000, img.getPixel(0, 0)); // Top-Left Red
        assertEquals(0xFFFFFFFF, img.getPixel(1, 0)); // Top-Right White

        // Verify bottom row (y=1)
        assertEquals(0xFF0000FF, img.getPixel(0, 1)); // Bottom-Left Blue
        assertEquals(0xFF00FF00, img.getPixel(1, 1)); // Bottom-Right Green
    }

    @Test
    void testInvalidMagicThrows() {
        byte[] bad = new byte[60];
        bad[0] = 'X';
        bad[1] = 'Y';
        assertThrows(PixooException.class, () -> BmpDecoder.decode(new ByteArrayInputStream(bad)));
    }
}
