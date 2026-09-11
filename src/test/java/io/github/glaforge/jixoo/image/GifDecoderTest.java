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
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class GifDecoderTest {

    @Test
    void testDecodeStaticGif() throws Exception {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 64, 64);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "gif", baos);

        PixooAnimation anim = GifDecoder.decode(new ByteArrayInputStream(baos.toByteArray()));
        assertNotNull(anim);
        assertEquals(1, anim.frameCount());
        assertEquals(12288, anim.frames().get(0).rgbData().length);
    }

    @Test
    void testDecodeRectangularGifWithCrop() throws Exception {
        // Create 200x100 rectangular red GIF
        BufferedImage image = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 200, 100);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "gif", baos);
        byte[] gifBytes = baos.toByteArray();

        // 1. FIT_CENTER: Top row (y=0, x=32) should be letterbox black (0, 0, 0)
        PixooAnimation fitAnim = GifDecoder.decode(new ByteArrayInputStream(gifBytes), ImageProcessor.ScaleMode.FIT_CENTER);
        byte[] fitRgb = fitAnim.frames().get(0).rgbData();
        int topRowFitColor = ((fitRgb[32 * 3] & 0xFF) << 16) | ((fitRgb[32 * 3 + 1] & 0xFF) << 8) | (fitRgb[32 * 3 + 2] & 0xFF);
        assertEquals(0, topRowFitColor, "Top row with FIT_CENTER should be black padding");

        // 2. FILL_CROP: Top row (y=0, x=32) should be filled with red (255, 0, 0) with no black bar!
        PixooAnimation cropAnim = GifDecoder.decode(new ByteArrayInputStream(gifBytes), ImageProcessor.ScaleMode.FILL_CROP);
        byte[] cropRgb = cropAnim.frames().get(0).rgbData();
        int topRowCropRed = cropRgb[32 * 3] & 0xFF;
        assertEquals(255, topRowCropRed, "Top row with FILL_CROP should be filled image pixels");
    }
}
