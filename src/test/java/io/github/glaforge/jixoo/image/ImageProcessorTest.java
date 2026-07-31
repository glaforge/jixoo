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

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class ImageProcessorTest {

    @Test
    void testResizeAndFitArbitraryImage() {
        BufferedImage original = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);

        BufferedImage resized = ImageProcessor.resizeAndFit(original, ImageProcessor.ScaleMode.FIT_CENTER);
        assertEquals(64, resized.getWidth());
        assertEquals(64, resized.getHeight());

        BufferedImage stretched = ImageProcessor.resizeAndFit(original, ImageProcessor.ScaleMode.STRETCH);
        assertEquals(64, stretched.getWidth());
        assertEquals(64, stretched.getHeight());

        PixooAnimation anim = ImageProcessor.processImage(original);
        assertEquals(1, anim.frameCount());
        assertEquals(12288, anim.frames().get(0).rgbData().length);
    }
}
