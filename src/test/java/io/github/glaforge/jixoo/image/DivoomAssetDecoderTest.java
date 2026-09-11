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
import io.github.glaforge.jixoo.model.PixooAnimation;
import io.github.glaforge.jixoo.model.PixooFrame;
import io.github.glaforge.jixoo.model.RawRgbBuffer;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DivoomAssetDecoderTest {

    @Test
    void testDecodeRealSampleAsset() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/sample_divoom.bin")) {
            assertNotNull(in, "sample_divoom.bin test resource not found");
            PixooAnimation anim = DivoomAssetDecoder.decode(in);

            assertNotNull(anim);
            assertEquals(3, anim.frameCount());

            for (PixooFrame frame : anim.frames()) {
                assertEquals(RawRgbBuffer.TOTAL_BYTES, frame.rgbData().length);
                assertEquals(129, frame.delayMs());
            }

            List<BufferedImage> images = DivoomAssetDecoder.toBufferedImages(anim);
            assertEquals(3, images.size());
            assertEquals(64, images.get(0).getWidth());
            assertEquals(64, images.get(0).getHeight());

            List<PixooImage> pixooImages = DivoomAssetDecoder.toPixooImages(anim);
            assertEquals(3, pixooImages.size());
            assertEquals(64, pixooImages.get(0).width());
            assertEquals(64, pixooImages.get(0).height());
        }
    }

    @Test
    void testDecodeInvalidDataThrowsException() {
        assertThrows(PixooException.class, () -> DivoomAssetDecoder.decode((byte[]) null));
        assertThrows(PixooException.class, () -> DivoomAssetDecoder.decode(new byte[]{1, 2, 3}));
        assertThrows(PixooException.class, () -> DivoomAssetDecoder.decode(new byte[]{99, 1, 0, 100, 4, 4}));
    }
}
