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
import io.github.glaforge.jixoo.model.PixooAnimation;
import io.github.glaforge.jixoo.model.PixooFrame;
import io.github.glaforge.jixoo.model.RawRgbBuffer;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collections;

public class ShowHelloGraphicsApp {
    public static void main(String[] args) {
        String ip;
        try {
            ip = ExampleUtils.resolveIpAddress(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return;
        }
        System.out.println("Rendering 'HELLO' locally and sending as raw graphics frame...");
        try {
            PixooClient client = PixooClient.create(ip);

            // 1. Create a 64x64 image
            BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();

            // 2. Draw black background
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, 64, 64);

            // 3. Draw cyan text "HELLO"
            g.setColor(Color.CYAN);
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            String text = "HELLO";
            FontMetrics fm = g.getFontMetrics();
            int x = (64 - fm.stringWidth(text)) / 2;
            int y = ((64 - fm.getHeight()) / 2) + fm.getAscent();
            g.drawString(text, x, y);
            g.dispose();

            // 4. Extract RGB bytes for the device (12288 bytes)
            byte[] rgb = new byte[RawRgbBuffer.TOTAL_BYTES];
            int idx = 0;
            for (int row = 0; row < 64; row++) {
                for (int col = 0; col < 64; col++) {
                    int argb = img.getRGB(col, row);
                    rgb[idx++] = (byte) ((argb >> 16) & 0xFF); // Red
                    rgb[idx++] = (byte) ((argb >> 8) & 0xFF);  // Green
                    rgb[idx++] = (byte) (argb & 0xFF);         // Blue
                }
            }

            // 5. Send as 1 frame animation. Duration 60000ms (60 seconds)
            PixooFrame frame = new PixooFrame(rgb, 60000);
            PixooAnimation anim = new PixooAnimation(Collections.singletonList(frame));
            
            client.sendAnimation(anim);
            System.out.println("Successfully sent the graphic frame!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
