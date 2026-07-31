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
import io.github.glaforge.jixoo.api.PixooText;
import io.github.glaforge.jixoo.model.PixooAnimation;
import io.github.glaforge.jixoo.model.PixooFrame;
import io.github.glaforge.jixoo.model.RawRgbBuffer;

import java.util.ArrayList;
import java.util.List;

public class LiveDemoApp {

    public static void main(String[] args) {
        String ip;
        try {
            ip = ExampleUtils.resolveIpAddress(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return;
        }
        System.out.println("Running Live Demo on Pixoo64 (" + ip + ")...");

        try {
            PixooClient client = PixooClient.create(ip);

            // 1. Turn on screen and set brightness to max (100%)
            System.out.println("1. Setting screen state ON and brightness to 100%...");
            client.setScreenState(true);
            client.setBrightness(100);

            // 2. Display hardware text
            System.out.println("2. Displaying scrolling text: 'JAVA 21 PIXOO 64'...");
            PixooText text = PixooText.builder()
                    .textId(1)
                    .position(0, 24)
                    .scrollLeft()
                    .speed(50)
                    .font(2)
                    .color("#00FF00")
                    .text("JAVA 21 PIXOO 64 READY!")
                    .build();
            client.sendText(text);

            Thread.sleep(3000);

            // 3. Send a bright multi-frame animation (alternating colored frames)
            System.out.println("3. Sending 4-frame animated color sequence...");
            List<PixooFrame> frames = new ArrayList<>();

            ColorFrameBuilder builder = new ColorFrameBuilder();
            frames.add(builder.buildSolidFrame(255, 0, 0, 500));   // Red 500ms
            frames.add(builder.buildSolidFrame(0, 255, 0, 500));   // Green 500ms
            frames.add(builder.buildSolidFrame(0, 0, 255, 500));   // Blue 500ms
            frames.add(builder.buildSolidFrame(255, 255, 0, 500)); // Yellow 500ms

            PixooAnimation animation = new PixooAnimation(frames);
            PixooResponse animResp = client.sendAnimation(animation);

            System.out.println("Animation response: " + animResp);
            System.out.println("Demo complete! The screen should now be animating in bright color loops.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class ColorFrameBuilder {
        public PixooFrame buildSolidFrame(int r, int g, int b, int delayMs) {
            byte[] rgb = new byte[RawRgbBuffer.TOTAL_BYTES];
            int idx = 0;
            for (int i = 0; i < 4096; i++) {
                rgb[idx++] = (byte) r;
                rgb[idx++] = (byte) g;
                rgb[idx++] = (byte) b;
            }
            return new PixooFrame(rgb, delayMs);
        }
    }
}
