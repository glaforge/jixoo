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

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class PngDecoderTest {

    @Test
    void testDecodePngImage() throws Exception {
        BufferedImage original = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = original.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 2, 2);
        g.setColor(Color.BLUE);
        g.fillRect(2, 2, 2, 2);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(original, "png", baos);

        PixooImage decoded = PngDecoder.decode(new ByteArrayInputStream(baos.toByteArray()));
        assertNotNull(decoded);
        assertEquals(4, decoded.width());
        assertEquals(4, decoded.height());

        assertEquals(0xFFFF0000, decoded.getPixel(0, 0));
        assertEquals(0xFFFF0000, decoded.getPixel(1, 1));
        assertEquals(0xFF0000FF, decoded.getPixel(2, 2));
        assertEquals(0xFF0000FF, decoded.getPixel(3, 3));
    }

    @Test
    void testDecodeGrayscalePng() throws Exception {
        BufferedImage original = new BufferedImage(2, 2, BufferedImage.TYPE_BYTE_GRAY);
        original.getRaster().setSample(0, 0, 0, 128);
        original.getRaster().setSample(1, 1, 0, 255);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(original, "png", baos);

        PixooImage decoded = PngDecoder.decode(new ByteArrayInputStream(baos.toByteArray()));
        assertNotNull(decoded);
        assertEquals(2, decoded.width());
        assertEquals(2, decoded.height());

        // 128 grayscale -> 0xFF808080
        assertEquals(0xFF808080, decoded.getPixel(0, 0));
        // 255 grayscale -> 0xFFFFFFFF
        assertEquals(0xFFFFFFFF, decoded.getPixel(1, 1));
    }

    @Test
    void testDecodeGrayAlphaPngFile() throws Exception {
        try (var is = getClass().getResourceAsStream("/test.png")) {
            if (is != null) {
                PixooImage decoded = PngDecoder.decode(is);
                assertNotNull(decoded);
                assertEquals(64, decoded.width());
                assertEquals(64, decoded.height());
            }
        }
    }
}
