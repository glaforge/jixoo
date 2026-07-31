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
package io.github.glaforge.jixoo.example;

import io.github.glaforge.jixoo.api.PixooClient;
import io.github.glaforge.jixoo.api.PixooResponse;
import io.github.glaforge.jixoo.model.PixooFrame;
import io.github.glaforge.jixoo.model.RawRgbBuffer;

public class TestRealGridApp {

    public static void main(String[] args) {
        String ip;
        try {
            ip = ExampleUtils.resolveIpAddress(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return;
        }
        System.out.println("Sending test 64x64 color quadrants to Pixoo64 at " + ip + "...");

        try {
            PixooClient client = PixooClient.create(ip);

            // Generate full 12,288-byte RGB buffer:
            // Top half (rows 0..31): Left 32 pixels Red (255, 0, 0), Right 32 pixels Green (0, 255, 0)
            // Bottom half (rows 32..63): Left 32 pixels Blue (0, 0, 255), Right 32 pixels Yellow (255, 255, 0)
            byte[] rgbData = new byte[RawRgbBuffer.TOTAL_BYTES];
            int idx = 0;

            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 64; x++) {
                    if (y < 32) {
                        if (x < 32) {
                            // RED
                            rgbData[idx++] = (byte) 255;
                            rgbData[idx++] = (byte) 0;
                            rgbData[idx++] = (byte) 0;
                        } else {
                            // GREEN
                            rgbData[idx++] = (byte) 0;
                            rgbData[idx++] = (byte) 255;
                            rgbData[idx++] = (byte) 0;
                        }
                    } else {
                        if (x < 32) {
                            // BLUE
                            rgbData[idx++] = (byte) 0;
                            rgbData[idx++] = (byte) 0;
                            rgbData[idx++] = (byte) 255;
                        } else {
                            // YELLOW
                            rgbData[idx++] = (byte) 255;
                            rgbData[idx++] = (byte) 255;
                            rgbData[idx++] = (byte) 0;
                        }
                    }
                }
            }

            PixooFrame frame = new PixooFrame(rgbData, 1000);
            PixooResponse response = client.sendFrame(frame);

            System.out.println("Response: " + response);
            if (response.isSuccess()) {
                System.out.println("SUCCESS! Check your Pixoo64 screen for the 4-color grid.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
