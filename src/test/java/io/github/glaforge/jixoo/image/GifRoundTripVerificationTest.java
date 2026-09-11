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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GifRoundTripVerificationTest {

    @Test
    @DisplayName("Verify round-trip encoding and decoding on actual GIFs in output directory")
    void testRoundTripOnOutputGifs() throws Exception {
        Path outputDir = Paths.get("output");
        if (!Files.exists(outputDir)) {
            System.out.println("Output directory not found, skipping.");
            return;
        }

        String[] testGifs = {
                "googleg_gradient_hq.gif",
                "g_to_cloud_hq.gif",
                "devoxx_to_g_hq.gif",
                "triple_morph_hq.gif",
                "cat_comic_hq.gif",
                "axolotl_v2_hq.gif",
                "rainbow_morph_hq.gif",
                "webp_converted_hq.gif"
        };

        System.out.println("========================================================================");
        System.out.println("GIF ROUND-TRIP VERIFICATION REPORT (GifDecoder -> GifEncoder -> GifDecoder)");
        System.out.println("========================================================================");

        int testedCount = 0;
        int exactMatchCount = 0;

        for (String gifName : testGifs) {
            Path gifPath = outputDir.resolve(gifName);
            if (!Files.exists(gifPath)) {
                System.out.printf("Skipping %s (file not found)%n", gifName);
                continue;
            }

            testedCount++;
            byte[] originalBytes = Files.readAllBytes(gifPath);

            // 1. Decode original GIF
            PixooAnimation anim1 = GifDecoder.decode(gifPath);
            int frameCount1 = anim1.frameCount();

            // 2. Encode to new GIF
            byte[] reencodedBytes = GifEncoder.encode(anim1);

            // 3. Decode re-encoded GIF
            PixooAnimation anim2 = GifDecoder.decode(reencodedBytes);
            int frameCount2 = anim2.frameCount();

            assertEquals(frameCount1, frameCount2, "Frame count mismatch for " + gifName);

            // 4. Compare each frame
            int totalPixels = frameCount1 * 64 * 64;
            int identicalPixels = 0;
            int maxChannelDiff = 0;
            double sumSquaredDiff = 0;

            for (int f = 0; f < frameCount1; f++) {
                PixooFrame frame1 = anim1.frames().get(f);
                PixooFrame frame2 = anim2.frames().get(f);

                // Check delays (GIF stores centiseconds, so within 10ms is expected)
                int delayDiff = Math.abs(frame1.delayMs() - frame2.delayMs());
                assertTrue(delayDiff <= 10, "Delay mismatch in frame " + f + " of " + gifName);

                byte[] rgb1 = frame1.rgbData();
                byte[] rgb2 = frame2.rgbData();
                assertEquals(rgb1.length, rgb2.length);

                for (int p = 0; p < 64 * 64; p++) {
                    int r1 = rgb1[p * 3] & 0xFF;
                    int g1 = rgb1[p * 3 + 1] & 0xFF;
                    int b1 = rgb1[p * 3 + 2] & 0xFF;

                    int r2 = rgb2[p * 3] & 0xFF;
                    int g2 = rgb2[p * 3 + 1] & 0xFF;
                    int b2 = rgb2[p * 3 + 2] & 0xFF;

                    int dr = Math.abs(r1 - r2);
                    int dg = Math.abs(g1 - g2);
                    int db = Math.abs(b1 - b2);

                    int pixelMaxDiff = Math.max(dr, Math.max(dg, db));
                    if (pixelMaxDiff == 0) {
                        identicalPixels++;
                    }
                    if (pixelMaxDiff > maxChannelDiff) {
                        maxChannelDiff = pixelMaxDiff;
                    }
                    sumSquaredDiff += dr * dr + dg * dg + db * db;
                }
            }

            double percentIdentical = (identicalPixels * 100.0) / totalPixels;
            double rmse = Math.sqrt(sumSquaredDiff / (totalPixels * 3.0));

            System.out.printf("%-26s | Frames: %2d | Orig size: %6d B | New size: %6d B | Identical: %6.2f%% | Max diff: %3d | RMSE: %5.2f%n",
                    gifName, frameCount1, originalBytes.length, reencodedBytes.length, percentIdentical, maxChannelDiff, rmse);

            if (percentIdentical >= 99.99) {
                exactMatchCount++;
            }
        }

        System.out.println("========================================================================");
        System.out.printf("Summary: %d / %d GIFs verified. Exact 100%% matches: %d / %d%n",
                testedCount, testGifs.length, exactMatchCount, testedCount);
        System.out.println("========================================================================");
    }
}
