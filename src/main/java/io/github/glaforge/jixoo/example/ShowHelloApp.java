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
import io.github.glaforge.jixoo.api.PixooText;
import io.github.glaforge.jixoo.model.PixooAnimation;
import io.github.glaforge.jixoo.model.PixooFrame;
import io.github.glaforge.jixoo.model.RawRgbBuffer;

import java.util.ArrayList;
import java.util.List;

public class ShowHelloApp {

    public static void main(String[] args) {
        String ip;
        try {
            ip = ExampleUtils.resolveIpAddress(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return;
        }
        System.out.println("Displaying 'HELLO' on Pixoo64 at " + ip + "...");

        try {
            PixooClient client = PixooClient.builder()
                    .ipAddress(ip)
                    .autoSwitchToCustomChannel(false)
                    .build();

            // Create a 1-frame solid black animation to stop any currently playing gifs
            // and give us a clean background to draw the native text on.
            // We set the duration to 60,000ms (60 seconds) so the device doesn't instantly 
            // revert back to the cloud gallery before we can see the text.
            List<PixooFrame> frames = new ArrayList<>();
            byte[] blackPixels = new byte[RawRgbBuffer.TOTAL_BYTES];
            frames.add(new PixooFrame(blackPixels, 60000));
            PixooAnimation background = new PixooAnimation(frames);
            
            client.sendAnimation(background);

            // Wait a brief moment to ensure the background is applied
            Thread.sleep(500);

            // Clear any previously existing text elements
            client.clearText();

            // Render "HELLO" using the native hardware text engine
            PixooText text = PixooText.builder()
                    .textId(1)
                    .position(0, 24)
                    .alignCenter()
                    .staticText()
                    .font(2)
                    .color("#00FFFF") // Cyan color
                    .text("HELLO")
                    .build();

            client.sendText(text);

            System.out.println("Successfully updated the display with 'HELLO'!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
